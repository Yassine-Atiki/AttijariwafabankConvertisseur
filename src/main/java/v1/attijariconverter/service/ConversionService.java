package v1.attijariconverter.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import v1.attijariconverter.model.ConversionHistory;
import v1.attijariconverter.model.MXMessage;
import v1.attijariconverter.model.MTMessage;
import v1.attijariconverter.model.ValidationStatus;
import v1.attijariconverter.repository.ConversionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
public class ConversionService {

    private static final Logger logger = LoggerFactory.getLogger(ConversionService.class);

    @Autowired
    private ConversionHistoryRepository conversionHistoryRepository;

    @Autowired
    private MXParsingService mxParsingService;

    @Value("${swift.receiver.bic:BMCEMAMCXXX}")
    private String receiverBic;

    @Value("${swift.block2.suffix:N}")
    private String block2Suffix;

    public static class ConversionResult {
        private final boolean success;
        private final String mtMessage;
        private final String errorMessage;
        private final List<String> validationErrors;

        public ConversionResult(boolean success, String mtMessage, String errorMessage, List<String> validationErrors) {
            this.success = success;
            this.mtMessage = mtMessage;
            this.errorMessage = errorMessage;
            this.validationErrors = validationErrors != null ? validationErrors : new ArrayList<>();
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMtMessage() { return mtMessage; }
        public String getErrorMessage() { return errorMessage; }
        public List<String> getValidationErrors() { return validationErrors; }
    }

    public ConversionResult convertMXToMT101(String mxContent) {
        List<String> validationErrors = new ArrayList<>();

        try {
            logger.info("Début de la conversion MX vers MT101");

            // Parser le message MX
            MXMessage mxMessage = mxParsingService.parseMXMessage(mxContent);
            if (mxMessage == null) {
                // Échec parsing => sauvegarde comme erreur
                saveConversionHistory(null, mxContent, null, "ERROR", "Impossible de parser le message MX", validationErrors, null);
                return new ConversionResult(false, null, "Impossible de parser le message MX", validationErrors);
            }

            // Générer le message MT101
            String mt101Message = generateMT101Message(mxMessage, validationErrors);

            // Valider la structure MT101
            if (!validateMT101Structure(mt101Message, validationErrors)) {
                // Sauvegarder l'échec de validation MT
                saveConversionHistory(mxMessage, mxContent, mt101Message, "ERROR", "Erreurs de validation MT101", validationErrors, null);
                return new ConversionResult(false, mt101Message, "Erreurs de validation MT101", validationErrors);
            }

            // Sauvegarder dans l'historique (succès)
            saveConversionHistory(mxMessage, mxContent, mt101Message, "SUCCESS", null, validationErrors, null);

            logger.info("Conversion MT101 réussie");
            return new ConversionResult(true, mt101Message, null, validationErrors);

        } catch (Exception e) {
            logger.error("Erreur lors de la conversion MX vers MT101", e);
            // Sauvegarder l'erreur inattendue
            saveConversionHistory(null, mxContent, null, "ERROR", e.getMessage(), validationErrors, null);
            return new ConversionResult(false, null, "Erreur lors de la conversion: " + e.getMessage(), validationErrors);
        }
    }

    private String generateMT101Message(MXMessage mxMessage, List<String> validationErrors) {
        StringBuilder mt101 = new StringBuilder();

        // Bloc 1: Basic Header Block (obligatoire)
        String bloc1 = generateBloc1(mxMessage);
        mt101.append(bloc1).append("\n");

        // Bloc 2: Application Header Block (obligatoire)
        String bloc2 = generateBloc2(mxMessage);
        mt101.append(bloc2).append("\n");

        // Bloc 3: User Header Block (optionnel)
        String bloc3 = generateBloc3();
        if (!bloc3.isEmpty()) {
            mt101.append(bloc3).append("\n");
        }

        // Bloc 4: Text Block (obligatoire)
        String bloc4 = generateBloc4(mxMessage, validationErrors);
        mt101.append(bloc4).append("\n");

        // Bloc 5: Trailer Block (obligatoire)
       // String bloc5 = generateBloc5(mt101.toString());
        //mt101.append(bloc5);

        return mt101.toString();
    }

    private String generateBloc1(MXMessage mxMessage) {
        // Déterminer le BIC du débiteur
        String bic = mxMessage != null ? mxMessage.getDebtorBIC() : null;
        if ((bic == null || bic.isBlank()) && mxMessage != null && mxMessage.getPaymentInstructions() != null && !mxMessage.getPaymentInstructions().isEmpty()) {
            // fallback: essayer le BIC débiteur au niveau transaction
            String fromPayment = mxMessage.getPaymentInstructions().get(0).getDebtorBIC();
            if (fromPayment != null && !fromPayment.isBlank()) bic = fromPayment;
        }
        // Valeur par défaut si introuvable
        if (bic == null || bic.isBlank()) bic = "BMCEMAMCXXX";
        // Normaliser: si BIC8, ajouter XXX
        String normBic = bic.trim();
        if (normBic.length() == 8) normBic = normBic + "XXX";
        // Conserver 11 caractères max
        if (normBic.length() > 11) normBic = normBic.substring(0, 11);
        // Session/Sequence (10 chiffres)
        String sessionSequence = "1234567890";
        return "{1:F01" + normBic + sessionSequence + "}";
    }

    private String generateBloc2(MXMessage mxMessage) {
        // BIC receiver = BIC du destinataire (Créditeur Agent)
        String bic = null;
        String source = null;
        if (mxMessage != null && mxMessage.getPaymentInstructions() != null && !mxMessage.getPaymentInstructions().isEmpty()) {
            MXMessage.PaymentInstruction p = mxMessage.getPaymentInstructions().get(0);
            if (p.getCreditorBIC() != null && !p.getCreditorBIC().isBlank()) {
                bic = p.getCreditorBIC();
                source = "CdtrAgt";
            }
        }
        // Fallback vers DbtrAgt si manquant
        if ((bic == null || bic.isBlank()) && mxMessage != null) {
            if (mxMessage.getDebtorBIC() != null && !mxMessage.getDebtorBIC().isBlank()) {
                bic = mxMessage.getDebtorBIC();
                source = "DbtrAgt(Header)";
            } else if (mxMessage.getPaymentInstructions() != null && !mxMessage.getPaymentInstructions().isEmpty()) {
                String fromPayment = mxMessage.getPaymentInstructions().get(0).getDebtorBIC();
                if (fromPayment != null && !fromPayment.isBlank()) {
                    bic = fromPayment;
                    source = "DbtrAgt(Payment)";
                }
            }
        }
        // Fallback vers config si toujours manquant
        if (bic == null || bic.isBlank()) {
            bic = receiverBic != null ? receiverBic : "BMCEMAMCXXX";
            source = "ConfigFallback";
        }

        String b = bic.trim();
        if (b.length() < 8) {
            b = String.format("%-8s", b).replace(' ', 'X');
        }
        String receiver12 = b.substring(0, 8) + "XXXX"; // LT addr
        String priority = (block2Suffix != null && !block2Suffix.isBlank()) ? block2Suffix.trim() : "N";
        if (priority.length() > 1) priority = priority.substring(0, 1);

        logger.info("[MT101] Bloc2 receiver source={} bic={} -> {}{}{}", source, bic, "{2:I101", receiver12, priority + "}");
        return "{2:I101" + receiver12 + priority + "}";
    }

    private String generateBloc3() {
        // {3:{113:XXXX}{108:REF12345678}} (optionnel)
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "{3:{108:REF" + timestamp + "}}";
    }

    private String generateBloc4(MXMessage mxMessage, List<String> validationErrors) {
        StringBuilder bloc4 = new StringBuilder();
        bloc4.append("{4:\n");

        // :20: Transaction Reference Number (obligatoire)
        String transactionRef = mxMessage.getMessageId();
        if (transactionRef != null && !transactionRef.trim().isEmpty()) {
            bloc4.append(":20:").append(transactionRef).append("\n");
        } else {
            validationErrors.add("Champ :20: (Transaction Reference) manquant");
        }

        // :28D: Message Index/Total (obligatoire) — valeur fixe
        bloc4.append(":28D:1/1\n");

        // :30: Requested Execution Date (obligatoire)
        String executionDate = mxMessage.getRequestedExecutionDate();
        if (executionDate != null && !executionDate.trim().isEmpty()) {
            // Accepter AAAAMMJJ ou AAAA-MM-JJ
            try {
                String normalized = executionDate.contains("-") ? executionDate.replaceAll("-", "").substring(0, 8) : executionDate;
                if (normalized.matches("\\d{8}")) {
                    bloc4.append(":30:").append(normalized).append("\n");
                } else {
                    validationErrors.add("Format de :30: invalide (attendu AAAAMMJJ)");
                }
            } catch (Exception e) {
                validationErrors.add("Format de :30: invalide (exception de parsing)");
            }
        } else {
            validationErrors.add("Champ :30: (Requested Execution Date) manquant");
        }

        // Séquences B - Détails des transactions
        List<MXMessage.PaymentInstruction> payments = mxMessage.getPaymentInstructions();
        if (payments == null || payments.isEmpty()) {
            validationErrors.add("Aucune instruction de paiement trouvée");
            bloc4.append("-}\n");
            return bloc4.toString();
        }

        for (MXMessage.PaymentInstruction payment : payments) {
            generateSequenceB(bloc4, payment, validationErrors);
        }

        bloc4.append("-}\n");
        return bloc4.toString();
    }

    private void generateSequenceB(StringBuilder bloc4, MXMessage.PaymentInstruction payment, List<String> validationErrors) {
        // :21: EndToEndId / InstructionId (obligatoire)
        String txnRef = payment.getInstructionId();
        if (txnRef != null && !txnRef.trim().isEmpty()) {
            bloc4.append(":21:").append(txnRef).append("\n");
        } else {
            validationErrors.add("Champ :21: (Transaction Reference par transaction) manquant");
        }

        // :32B: Devise et Montant (obligatoire)
        String currency = payment.getCurrency();
        String amount = payment.getAmount();
        boolean hasCurrency = currency != null && !currency.trim().isEmpty();
        boolean hasAmount = amount != null && !amount.trim().isEmpty();
        if (hasCurrency && hasAmount) {
            // Format MT: utiliser virgule décimale
            String amt = amount.replace('.', ',');
            bloc4.append(":32B:").append(currency).append(amt).append("\n");
        } else {
            if (!hasCurrency) validationErrors.add("Champ devise manquant pour :32B:");
            if (!hasAmount) validationErrors.add("Champ montant manquant pour :32B:");
        }

        // :50K: Donneur d'ordre (optionnel)
        String debtorName = payment.getDebtorName();
        if (debtorName != null && !debtorName.trim().isEmpty()) {
            bloc4.append(":50K:").append(debtorName).append("\n");
        }

        // :59: Bénéficiaire (obligatoire)
        String creditorName = payment.getCreditorName();
        if (creditorName != null && !creditorName.trim().isEmpty()) {
            bloc4.append(":59:").append(creditorName).append("\n");
        } else {
            validationErrors.add("Champ :59: (Beneficiary Customer) manquant");
        }

        // :71A: Frais (obligatoire) — pas de valeur par défaut
        String chargeBearer = payment.getChargeBearer();
        if (chargeBearer != null && !chargeBearer.trim().isEmpty()) {
            String cb = chargeBearer.toUpperCase();
            switch (cb) {
                case "DEBT": cb = "OUR"; break;
                case "CRED": cb = "BEN"; break;
                case "SHAR":
                case "SLEV": cb = "SHA"; break;
                case "OUR":
                case "BEN":
                case "SHA": break; // déjà au bon format
                default:
                    validationErrors.add("Code de frais inconnu pour :71A: ('" + chargeBearer + "')");
                    cb = null; // ne pas écrire de valeur par défaut
            }
            if (cb != null) {
                bloc4.append(":71A:").append(cb).append("\n");
            }
        } else {
            validationErrors.add("Champ :71A: (Details of Charges) manquant");
        }

        // :70: Infos (optionnel)
        String remittanceInfo = payment.getRemittanceInfo();
        if (remittanceInfo != null && !remittanceInfo.trim().isEmpty()) {
            bloc4.append(":70:").append(remittanceInfo).append("\n");
        }
    }

//    private String generateBloc5(String fullMessage) {
//        // Calculer un checksum simple
//        int checksum = Math.abs(fullMessage.hashCode()) % 1000000000;
//        return "{5:{CHK:" + String.format("%09d", checksum) + "ABC}}";
//    }

    private boolean validateMT101Structure(String mt101Message, List<String> validationErrors) {
        boolean isValid = true;

        if (!mt101Message.contains("{1:")) { validationErrors.add("Bloc 1 (Basic Header) manquant"); isValid = false; }
        if (!mt101Message.contains("{2:")) { validationErrors.add("Bloc 2 (Application Header) manquant"); isValid = false; }
        if (!mt101Message.contains("{4:")) { validationErrors.add("Bloc 4 (Text Block) manquant"); isValid = false; }
       // if (!mt101Message.contains("{5:")) { validationErrors.add("Bloc 5 (Trailer) manquant"); isValid = false; }

        if (!mt101Message.contains(":20:")) { validationErrors.add("Champ :20: (Transaction Reference) manquant"); isValid = false; }
        if (!mt101Message.contains(":28D:")) { validationErrors.add("Champ :28D: (Message Index/Total) manquant"); isValid = false; }
        if (!mt101Message.contains(":30:")) { validationErrors.add("Champ :30: (Requested Execution Date) manquant"); isValid = false; }
        if (!mt101Message.contains(":21:")) { validationErrors.add("Champ :21: (Transaction Reference par transaction) manquant"); isValid = false; }
        if (!mt101Message.contains(":32B:")) { validationErrors.add("Champ :32B: (Currency and Amount) manquant"); isValid = false; }
        if (!mt101Message.contains(":59:")) { validationErrors.add("Champ :59: (Beneficiary Customer) manquant"); isValid = false; }
        if (!mt101Message.contains(":71A:")) { validationErrors.add("Champ :71A: (Details of Charges) manquant"); isValid = false; }

        return isValid;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return "anonymous";
        return auth.getName();
    }

    private boolean isAdmin(Authentication auth){
        if(auth==null) return false;
        for(GrantedAuthority ga: auth.getAuthorities()){
            if("ROLE_ADMIN".equals(ga.getAuthority())) return true;
        }
        return false;
    }

    // Sauvegarde centralisée de l'historique
    private void saveConversionHistory(MXMessage mxMessage,
                                       String mxRawContent,
                                       String mtMessage,
                                       String status,
                                       String errorMessage,
                                       List<String> mtValidationErrors,
                                       List<String> mxValidationErrors) {
        try {
            ConversionHistory history = new ConversionHistory();
            history.setConversionDate(LocalDateTime.now());
            history.setStatus(status); // "SUCCESS" ou "ERROR"
            history.setInputFormat("pain.001");
            history.setOutputFormat("MT101");
            history.setOwnerUsername(currentUsername());

            // Contenus
            if (mxRawContent != null) {
                history.setMxContent(mxRawContent);
                history.setInputSize((long) mxRawContent.length());
            } else if (mxMessage != null) {
                // fallback si nécessaire
                String approx = String.valueOf(mxMessage);
                history.setInputSize((long) approx.length());
            }

            if (mtMessage != null) {
                history.setMtContent(mtMessage);
                history.setOutputSize((long) mtMessage.length());
            }

            // Erreurs / Détails
            history.setErrorMessage(errorMessage);
            if (mtValidationErrors != null && !mtValidationErrors.isEmpty()) {
                history.setMtValidationErrors(mtValidationErrors);
            }
            if (mxValidationErrors != null && !mxValidationErrors.isEmpty()) {
                history.setMxValidationErrors(mxValidationErrors);
            }

            conversionHistoryRepository.save(history);
        } catch (Exception e) {
            logger.error("Erreur lors de la sauvegarde de l'historique", e);
        }
    }

    // Sauvegarde dédiée pour échec de validation XSD côté MX
    public void saveValidationFailure(String mxRawContent, List<String> errors, String message) {
        saveConversionHistory(null, mxRawContent, null, "ERROR", message, null, errors);
    }

    // Méthodes pour le dashboard
    public List<ConversionHistory> getConversionHistory() {
        try {
            return conversionHistoryRepository.findTop10ByOwnerUsernameOrderByConversionDateDesc(currentUsername());
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de l'historique", e);
            return new ArrayList<>();
        }
    }

    public List<ConversionHistory> getValidConversions() {
        try {
            return conversionHistoryRepository.findByOwnerUsernameAndStatusOrderByConversionDateDesc(currentUsername(), "SUCCESS");
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des conversions valides", e);
            return new ArrayList<>();
        }
    }

    public List<ConversionHistory> getInvalidConversions() {
        try {
            return conversionHistoryRepository.findByOwnerUsernameAndStatusOrderByConversionDateDesc(currentUsername(), "ERROR");
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des conversions invalides", e);
            return new ArrayList<>();
        }
    }

    public long getTotalConversions() {
        try {
            return conversionHistoryRepository.findByOwnerUsernameOrderByConversionDateDesc(currentUsername()).size();
        } catch (Exception e) {
            logger.error("Erreur lors du comptage total", e);
            return 0;
        }
    }

    public long getSuccessfulConversions() {
        try {
            return conversionHistoryRepository.findByOwnerUsernameAndStatusOrderByConversionDateDesc(currentUsername(), "SUCCESS").size();
        } catch (Exception e) {
            logger.error("Erreur lors du comptage des succès", e);
            return 0;
        }
    }

    public long getFailedConversions() {
        try {
            return conversionHistoryRepository.findByOwnerUsernameAndStatusOrderByConversionDateDesc(currentUsername(), "ERROR").size();
        } catch (Exception e) {
            logger.error("Erreur lors du comptage des échecs", e);
            return 0;
        }
    }

    public List<ConversionHistory> getRecentConversions(int limit) {
        try {
            List<ConversionHistory> all = conversionHistoryRepository.findByOwnerUsernameOrderByConversionDateDesc(currentUsername());
            return all.stream().limit(limit).collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des conversions récentes", e);
            return new ArrayList<>();
        }
    }

    public List<ConversionHistory> getAllConversions() {
        try {
            return conversionHistoryRepository.findByOwnerUsernameOrderByConversionDateDesc(currentUsername());
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de toutes les conversions", e);
            return new ArrayList<>();
        }
    }

    public List<ConversionHistory> getTodayConversions() {
        try {
            LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            return conversionHistoryRepository.findByOwnerUsernameAndConversionDateBetween(currentUsername(), startOfDay, endOfDay);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des conversions d'aujourd'hui", e);
            return new ArrayList<>();
        }
    }

    public List<ConversionHistory> getConversionsLast7Days() {
        try {
            LocalDateTime startDate = LocalDateTime.now().minusDays(7);
            LocalDateTime endDate = LocalDateTime.now();
            return conversionHistoryRepository.findByOwnerUsernameAndConversionDateBetween(currentUsername(), startDate, endDate);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des conversions des 7 derniers jours", e);
            return new ArrayList<>();
        }
    }

    public List<ConversionHistory> getOtherUsersHistory(){
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if(!isAdmin(auth)) return new ArrayList<>();
            return conversionHistoryRepository.findByOwnerUsernameNotOrderByConversionDateDesc(currentUsername());
        } catch (Exception e){
            logger.error("Erreur récupération autres historiques", e);
            return new ArrayList<>();
        }
    }

    public long deleteHistoryForUser(String username){
        try {
            if(username == null) {
                return conversionHistoryRepository.deleteByOwnerUsernameIsNull();
            }
            String trimmed = username.trim();
            if(trimmed.isEmpty() || "_ANONYMOUS_".equalsIgnoreCase(trimmed) || "anonymous".equalsIgnoreCase(trimmed) || "null".equalsIgnoreCase(trimmed)) {
                long c1 = conversionHistoryRepository.deleteByOwnerUsernameIsNull();
                long c2 = conversionHistoryRepository.deleteByOwnerUsername("");
                return c1 + c2;
            }
            return conversionHistoryRepository.deleteByOwnerUsername(trimmed);
        } catch(Exception e){
            logger.error("Erreur suppression historique utilisateur {}", username, e);
            return 0;
        }
    }

    public long deleteOwnHistory(){
        return deleteHistoryForUser(currentUsername());
    }

    public boolean deleteHistoryEntry(String id){
        try {
            if(id == null || id.isBlank()) return false;
            if(!conversionHistoryRepository.existsById(id)) return false;
            conversionHistoryRepository.deleteById(id);
            return true;
        } catch(Exception e){
            logger.error("Erreur suppression entrée {}", id, e);
            return false;
        }
    }

    // Méthodes de pagination ajoutées
    public Page<ConversionHistory> getConversionHistoryPaginated(int page, int size) {
        try {
            String username = currentUsername();
            Pageable pageable = PageRequest.of(page, size, Sort.by("conversionDate").descending());
            return conversionHistoryRepository.findByOwnerUsername(username, pageable);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de l'historique paginé", e);
            return Page.empty();
        }
    }

    public Page<ConversionHistory> getOtherUsersHistoryPaginated(int page, int size) {
        try {
            String currentUser = currentUsername();
            Pageable pageable = PageRequest.of(page, size, Sort.by("conversionDate").descending());
            return conversionHistoryRepository.findByOwnerUsernameNot(currentUser, pageable);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de l'historique des autres utilisateurs", e);
            return Page.empty();
        }
    }

    // Méthodes statistiques pour admin
    public long getTotalActiveUsers() {
        try {
            return conversionHistoryRepository.countDistinctOwnerUsername();
        } catch (Exception e) {
            logger.error("Erreur lors du comptage des utilisateurs actifs", e);
            return 0;
        }
    }

    public long getTotalUsersWithSuccess() {
        try {
            return conversionHistoryRepository.countDistinctOwnerUsernameByStatus("SUCCESS");
        } catch (Exception e) {
            logger.error("Erreur lors du comptage des utilisateurs avec succès", e);
            return 0;
        }
    }

    public long getTotalUsersWithErrors() {
        try {
            return conversionHistoryRepository.countDistinctOwnerUsernameByStatus("ERROR");
        } catch (Exception e) {
            logger.error("Erreur lors du comptage des utilisateurs avec erreurs", e);
            return 0;
        }
    }

    // Nouvelles méthodes pour les statistiques par utilisateur
    public List<String> getAllUsers() {
        try {
            return conversionHistoryRepository.findAllWithUsernameOnly()
                    .stream()
                    .map(ConversionHistory::getOwnerUsername)
                    .distinct()
                    .filter(username -> username != null && !username.trim().isEmpty())
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des utilisateurs", e);
            return new ArrayList<>();
        }
    }

    public Map<String, Object> getUserStatistics(String username) {
        try {
            Map<String, Object> stats = new HashMap<>();

            long totalConversions = conversionHistoryRepository.countByOwnerUsername(username);
            long successfulConversions = conversionHistoryRepository.countByOwnerUsernameAndStatus(username, "SUCCESS");
            long failedConversions = conversionHistoryRepository.countByOwnerUsernameAndStatus(username, "ERROR");

            stats.put("username", username);
            stats.put("totalConversions", totalConversions);
            stats.put("successfulConversions", successfulConversions);
            stats.put("failedConversions", failedConversions);
            stats.put("successRate", totalConversions > 0 ? (double)successfulConversions / totalConversions * 100 : 0.0);

            // Récupérer les dernières conversions
            List<ConversionHistory> recentConversions = conversionHistoryRepository.findTop5ByOwnerUsernameOrderByConversionDateDesc(username);
            stats.put("recentConversions", recentConversions);

            return stats;
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des statistiques utilisateur pour {}", username, e);
            return new HashMap<>();
        }
    }

    public Map<String, Object> getUserStatsByDate(String username, String date) {
        try {
            LocalDateTime startOfDay = LocalDate.parse(date).atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1);

            Map<String, Object> stats = new HashMap<>();
            long totalDay = conversionHistoryRepository.countByOwnerUsernameAndConversionDateBetween(username, startOfDay, endOfDay);
            long successDay = conversionHistoryRepository.countByOwnerUsernameAndStatusAndConversionDateBetween(username, "SUCCESS", startOfDay, endOfDay);
            long errorDay = conversionHistoryRepository.countByOwnerUsernameAndStatusAndConversionDateBetween(username, "ERROR", startOfDay, endOfDay);

            stats.put("total", totalDay);
            stats.put("valid", successDay);
            stats.put("error", errorDay);
            stats.put("date", date);
            stats.put("username", username);

            return stats;
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des statistiques utilisateur par date", e);
            Map<String, Object> emptyStats = new HashMap<>();
            emptyStats.put("total", 0);
            emptyStats.put("valid", 0);
            emptyStats.put("error", 0);
            return emptyStats;
        }
    }
}
