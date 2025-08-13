package v1.attijariconverter.service;

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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ConversionService {

    private static final Logger logger = LoggerFactory.getLogger(ConversionService.class);

    @Autowired
    private ConversionHistoryRepository conversionHistoryRepository;

    @Autowired
    private MXParsingService mxParsingService;

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
                return new ConversionResult(false, null, "Impossible de parser le message MX", validationErrors);
            }

            // Générer le message MT101
            String mt101Message = generateMT101Message(mxMessage, validationErrors);

            // Valider la structure MT101
            if (!validateMT101Structure(mt101Message, validationErrors)) {
                return new ConversionResult(false, mt101Message, "Erreurs de validation MT101", validationErrors);
            }

            // Sauvegarder dans l'historique
            saveConversionHistory(mxMessage, mt101Message, "SUCCESS");

            logger.info("Conversion MT101 réussie");
            return new ConversionResult(true, mt101Message, null, validationErrors);

        } catch (Exception e) {
            logger.error("Erreur lors de la conversion MX vers MT101", e);
            saveConversionHistory(null, null, "ERROR: " + e.getMessage());
            return new ConversionResult(false, null, "Erreur lors de la conversion: " + e.getMessage(), validationErrors);
        }
    }

    private String generateMT101Message(MXMessage mxMessage, List<String> validationErrors) {
        StringBuilder mt101 = new StringBuilder();

        // Bloc 1: Basic Header Block (obligatoire)
        String bloc1 = generateBloc1();
        mt101.append(bloc1).append("\n");

        // Bloc 2: Application Header Block (obligatoire)
        String bloc2 = generateBloc2();
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
        String bloc5 = generateBloc5(mt101.toString());
        mt101.append(bloc5);

        return mt101.toString();
    }

    private String generateBloc1() {
        // {1:F01BANKBEBBAXXX1234567890}
        return "{1:F01BMCEMAMCXXX1234567890}";
    }

    private String generateBloc2() {
        // {2:I101BANKFRPPXXXXN}
        return "{2:I101BMCEMAMCXXXXN}";
    }

    private String generateBloc3() {
        // {3:{113:XXXX}{108:REF12345678}} (optionnel)
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "{3:{113:SEPA}{108:REF" + timestamp + "}}";
    }

    private String generateBloc4(MXMessage mxMessage, List<String> validationErrors) {
        StringBuilder bloc4 = new StringBuilder();
        bloc4.append("{4:\n");

        // Champs obligatoires de l'en-tête
        // :20: Transaction Reference Number (obligatoire)
        String transactionRef = mxMessage.getMessageId();
        if (transactionRef == null || transactionRef.trim().isEmpty()) {
            transactionRef = "REF" + System.currentTimeMillis();
            validationErrors.add("Référence de transaction manquante, générée automatiquement: " + transactionRef);
        }
        bloc4.append(":20:").append(transactionRef).append("\n");

        // :28D: Message Index/Total (obligatoire)
        bloc4.append(":28D:1/1\n");

        // :30: Requested Execution Date (obligatoire)
        String executionDate = mxMessage.getRequestedExecutionDate();
        if (executionDate == null || executionDate.trim().isEmpty()) {
            executionDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            validationErrors.add("Date d'exécution manquante, date actuelle utilisée: " + executionDate);
        } else {
            // Convertir la date ISO au format YYYYMMDD
            try {
                if (executionDate.contains("-")) {
                    executionDate = executionDate.replaceAll("-", "").substring(0, 8);
                }
            } catch (Exception e) {
                executionDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                validationErrors.add("Format de date invalide, date actuelle utilisée: " + executionDate);
            }
        }
        bloc4.append(":30:").append(executionDate).append("\n");

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
        // :21: Transaction Reference Number par transaction (obligatoire)
        String txnRef = payment.getInstructionId();
        if (txnRef == null || txnRef.trim().isEmpty()) {
            txnRef = "TXN" + System.currentTimeMillis();
            validationErrors.add("Référence de transaction manquante, générée: " + txnRef);
        }
        bloc4.append(":21:").append(txnRef).append("\n");

        // :32B: Currency Code and Amount (obligatoire pour MT101)
        String currency = payment.getCurrency();
        String amount = payment.getAmount();
        if (currency == null || currency.trim().isEmpty()) {
            currency = "EUR";
            validationErrors.add("Devise manquante, EUR utilisée par défaut");
        }
        if (amount == null || amount.trim().isEmpty()) {
            validationErrors.add("Montant manquant pour la transaction " + txnRef);
            amount = "0,00";
        }
        // Formater le montant pour MT101 (pas de point décimal)
        amount = amount.replace(".", ",");
        bloc4.append(":32B:").append(currency).append(amount).append("\n");

        // :50K: Ordering Customer (optionnel mais recommandé)
        String debtorName = payment.getDebtorName();
        if (debtorName != null && !debtorName.trim().isEmpty()) {
            bloc4.append(":50K:").append(debtorName).append("\n");
        }

        // :59: Beneficiary Customer (obligatoire)
        String creditorName = payment.getCreditorName();
        if (creditorName == null || creditorName.trim().isEmpty()) {
            validationErrors.add("Nom du bénéficiaire manquant pour la transaction " + txnRef);
            creditorName = "CREDITOR NAME";
        }
        bloc4.append(":59:").append(creditorName).append("\n");

        // :71A: Details of Charges (obligatoire)
        String chargeBearer = payment.getChargeBearer();
        if (chargeBearer == null || chargeBearer.trim().isEmpty()) {
            chargeBearer = "SHA";
            validationErrors.add("Type de frais manquant, SHA utilisé par défaut pour " + txnRef);
        }
        // Convertir les codes ISO vers MT
        switch (chargeBearer.toUpperCase()) {
            case "DEBT":
                chargeBearer = "OUR";
                break;
            case "CRED":
                chargeBearer = "BEN";
                break;
            case "SHAR":
            case "SLEV":
                chargeBearer = "SHA";
                break;
            default:
                chargeBearer = "SHA";
                break;
        }
        bloc4.append(":71A:").append(chargeBearer).append("\n");

        // :70: Remittance Information (optionnel)
        String remittanceInfo = payment.getRemittanceInfo();
        if (remittanceInfo != null && !remittanceInfo.trim().isEmpty()) {
            bloc4.append(":70:").append(remittanceInfo).append("\n");
        }
    }

    private String generateBloc5(String fullMessage) {
        // Calculer un checksum simple
        int checksum = Math.abs(fullMessage.hashCode()) % 1000000000;
        return "{5:{CHK:" + String.format("%09d", checksum) + "ABC}}";
    }

    private boolean validateMT101Structure(String mt101Message, List<String> validationErrors) {
        boolean isValid = true;

        // Vérifier la présence des blocs obligatoires
        if (!mt101Message.contains("{1:")) {
            validationErrors.add("Bloc 1 (Basic Header) manquant");
            isValid = false;
        }

        if (!mt101Message.contains("{2:")) {
            validationErrors.add("Bloc 2 (Application Header) manquant");
            isValid = false;
        }

        if (!mt101Message.contains("{4:")) {
            validationErrors.add("Bloc 4 (Text Block) manquant");
            isValid = false;
        }

        if (!mt101Message.contains("{5:")) {
            validationErrors.add("Bloc 5 (Trailer) manquant");
            isValid = false;
        }

        // Vérifier les champs obligatoires
        if (!mt101Message.contains(":20:")) {
            validationErrors.add("Champ :20: (Transaction Reference) manquant");
            isValid = false;
        }

        if (!mt101Message.contains(":28D:")) {
            validationErrors.add("Champ :28D: (Message Index/Total) manquant");
            isValid = false;
        }

        if (!mt101Message.contains(":30:")) {
            validationErrors.add("Champ :30: (Requested Execution Date) manquant");
            isValid = false;
        }

        if (!mt101Message.contains(":21:")) {
            validationErrors.add("Champ :21: (Transaction Reference par transaction) manquant");
            isValid = false;
        }

        if (!mt101Message.contains(":59:")) {
            validationErrors.add("Champ :59: (Beneficiary Customer) manquant");
            isValid = false;
        }

        if (!mt101Message.contains(":71A:")) {
            validationErrors.add("Champ :71A: (Details of Charges) manquant");
            isValid = false;
        }

        return isValid;
    }

    private void saveConversionHistory(MXMessage mxMessage, String mtMessage, String status) {
        try {
            ConversionHistory history = new ConversionHistory();
            history.setConversionDate(LocalDateTime.now());
            history.setStatus(status);
            history.setInputFormat("pain.001");
            history.setOutputFormat("MT101");

            if (mxMessage != null) {
                history.setInputSize((long) mxMessage.toString().length());
            }

            if (mtMessage != null) {
                history.setOutputSize((long) mtMessage.length());
            }

            conversionHistoryRepository.save(history);
        } catch (Exception e) {
            logger.error("Erreur lors de la sauvegarde de l'historique", e);
        }
    }

    // Méthodes pour le dashboard
    public List<ConversionHistory> getConversionHistory() {
        try {
            return conversionHistoryRepository.findTop10ByOrderByConversionDateDesc();
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de l'historique", e);
            return new ArrayList<>();
        }
    }

    public List<ConversionHistory> getValidConversions() {
        try {
            return conversionHistoryRepository.findByStatusOrderByConversionDateDesc("SUCCESS");
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des conversions valides", e);
            return new ArrayList<>();
        }
    }

    public List<ConversionHistory> getInvalidConversions() {
        try {
            return conversionHistoryRepository.findByStatusOrderByConversionDateDesc("ERROR");
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des conversions invalides", e);
            return new ArrayList<>();
        }
    }

    public long getTotalConversions() {
        try {
            return conversionHistoryRepository.count();
        } catch (Exception e) {
            logger.error("Erreur lors du comptage total", e);
            return 0;
        }
    }

    public long getSuccessfulConversions() {
        try {
            return conversionHistoryRepository.findByStatusOrderByConversionDateDesc("SUCCESS").size();
        } catch (Exception e) {
            logger.error("Erreur lors du comptage des succès", e);
            return 0;
        }
    }

    public long getFailedConversions() {
        try {
            return conversionHistoryRepository.findByStatusOrderByConversionDateDesc("FAILED").size();
        } catch (Exception e) {
            logger.error("Erreur lors du comptage des échecs", e);
            return 0;
        }
    }

    public long getPendingConversions() {
        try {
            return conversionHistoryRepository.findByStatusOrderByConversionDateDesc("PENDING").size();
        } catch (Exception e) {
            logger.error("Erreur lors du comptage des en attente", e);
            return 0;
        }
    }

    public List<ConversionHistory> getRecentConversions(int limit) {
        try {
            List<ConversionHistory> all = conversionHistoryRepository.findAll();
            return all.stream()
                .sorted((a, b) -> b.getConversionDate().compareTo(a.getConversionDate()))
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des conversions récentes", e);
            return new ArrayList<>();
        }
    }

    // Nouvelles méthodes pour les graphiques
    public List<ConversionHistory> getAllConversions() {
        try {
            return conversionHistoryRepository.findAll();
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de toutes les conversions", e);
            return new ArrayList<>();
        }
    }

    public List<ConversionHistory> getTodayConversions() {
        try {
            LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            return conversionHistoryRepository.findByConversionDateBetween(startOfDay, endOfDay);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des conversions d'aujourd'hui", e);
            return new ArrayList<>();
        }
    }

    public List<ConversionHistory> getConversionsLast7Days() {
        try {
            LocalDateTime startDate = LocalDateTime.now().minusDays(7);
            LocalDateTime endDate = LocalDateTime.now();
            return conversionHistoryRepository.findByConversionDateBetween(startDate, endDate);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des conversions des 7 derniers jours", e);
            return new ArrayList<>();
        }
    }
}
