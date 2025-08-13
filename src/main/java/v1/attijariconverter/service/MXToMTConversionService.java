package v1.attijariconverter.service;

import org.springframework.stereotype.Service;
import v1.attijariconverter.model.MXMessage;
import v1.attijariconverter.model.MTMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MXToMTConversionService {

    private static final Logger logger = LoggerFactory.getLogger(MXToMTConversionService.class);

    public String convertMXToMT101(MXMessage mxMessage) {
        MTMessage mtMessage = convertToMTMessage(mxMessage);
        return buildMT101Message(mtMessage);
    }

    private MTMessage convertToMTMessage(MXMessage mxMessage) {
        MTMessage mtMessage = new MTMessage();

        // Field 20: Transaction Reference Number - utiliser l'ID du message
        String transactionRef = mxMessage.getMessageId();
        if (transactionRef == null || transactionRef.trim().isEmpty()) {
            transactionRef = generateTransactionReference("DEFAULT");
        }
        mtMessage.setTransactionReferenceNumber(transactionRef);

        // Field 30: Value Date (format YYMMDD)
        mtMessage.setValueDate(formatDateForMT(mxMessage.getRequestedExecutionDate()));

        // Field 52A/52D: Ordering Institution
        mtMessage.setOrderingInstitution(mxMessage.getDebtorBIC());

        // Field 50F/50K: Ordering Customer - utiliser les vraies données
        mtMessage.setOrderingCustomer(formatOrderingCustomer(mxMessage.getDebtorName(), mxMessage.getDebtorAccount()));

        // Field 59/59A: Beneficiary Institution - utiliser les vraies données du premier paiement
        if (mxMessage.getPaymentInstructions() != null && !mxMessage.getPaymentInstructions().isEmpty()) {
            MXMessage.PaymentInstruction firstPayment = mxMessage.getPaymentInstructions().get(0);
            mtMessage.setBeneficiaryInstitution(formatBeneficiary(firstPayment.getCreditorName(), firstPayment.getCreditorAccount()));

            // Field 57A/57D: Account With Institution
            mtMessage.setAccountWithInstitution(firstPayment.getCreditorBIC());

            // Field 32A: Value Date and Amount
            mtMessage.setValueDateAndAmount(formatValueDateAndAmount(
                mxMessage.getRequestedExecutionDate(),
                firstPayment.getCurrency(),
                firstPayment.getAmount()
            ));
        } else {
            // Utiliser les données générales si pas d'instructions de paiement
            mtMessage.setBeneficiaryInstitution(formatBeneficiary(mxMessage.getCreditorName(), mxMessage.getCreditorAccount()));
            mtMessage.setAccountWithInstitution(mxMessage.getCreditorBIC());
            mtMessage.setValueDateAndAmount(formatValueDateAndAmount(
                mxMessage.getRequestedExecutionDate(),
                mxMessage.getCurrency(),
                mxMessage.getAmount()
            ));
        }

        logger.info("Conversion MX vers MT réussie pour message: {}", mxMessage.getMessageId());
        return mtMessage;
    }

    private String buildMT101Message(MTMessage mtMessage) {
        StringBuilder mt101 = new StringBuilder();

        // Bloc 1: Basic Header Block
        mt101.append("{1:F01BMCEMAMCXXX1234567890}\n");

        // Bloc 2: Application Header Block
        mt101.append("{2:I101BMCEMAMCXXXXN}\n");

        // Bloc 3: User Header Block (optionnel)
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        mt101.append("{3:{113:SEPA}{108:REF").append(timestamp).append("}}\n");

        // Bloc 4: Text Block
        mt101.append("{4:\n");

        // Champs MT101
        if (mtMessage.getTransactionReferenceNumber() != null) {
            mt101.append(":20:").append(mtMessage.getTransactionReferenceNumber()).append("\n");
        }

        // Message Index/Total
        mt101.append(":28D:1/1\n");

        if (mtMessage.getValueDate() != null) {
            mt101.append(":30:").append(mtMessage.getValueDate()).append("\n");
        }

        if (mtMessage.getOrderingCustomer() != null) {
            mt101.append(":50K:").append(mtMessage.getOrderingCustomer()).append("\n");
        }

        if (mtMessage.getOrderingInstitution() != null) {
            mt101.append(":52A:").append(mtMessage.getOrderingInstitution()).append("\n");
        }

        if (mtMessage.getAccountWithInstitution() != null) {
            mt101.append(":57A:").append(mtMessage.getAccountWithInstitution()).append("\n");
        }

        if (mtMessage.getBeneficiaryInstitution() != null) {
            mt101.append(":59:").append(mtMessage.getBeneficiaryInstitution()).append("\n");
        }

        if (mtMessage.getValueDateAndAmount() != null) {
            mt101.append(":32B:").append(mtMessage.getValueDateAndAmount()).append("\n");
        }

        // Details of Charges
        mt101.append(":71A:SHA\n");

        mt101.append("-}\n");

        // Bloc 5: Trailer Block
        int checksum = Math.abs(mt101.toString().hashCode()) % 1000000000;
        mt101.append("{5:{CHK:").append(String.format("%09d", checksum)).append("ABC}}");

        return mt101.toString();
    }

    private String generateTransactionReference(String messageId) {
        if (messageId != null && !messageId.trim().isEmpty()) {
            return "REF" + messageId.replaceAll("[^A-Za-z0-9]", "").substring(0, Math.min(messageId.length(), 10));
        }
        return "REF" + System.currentTimeMillis();
    }

    private String formatDateForMT(String isoDate) {
        if (isoDate == null || isoDate.trim().isEmpty()) {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }

        try {
            // Convertir de YYYY-MM-DD vers YYYYMMDD
            if (isoDate.contains("-")) {
                return isoDate.replaceAll("-", "").substring(0, 8);
            }
            return isoDate.substring(0, Math.min(isoDate.length(), 8));
        } catch (Exception e) {
            logger.warn("Erreur de formatage de date: {}, utilisation de la date actuelle", isoDate);
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
    }

    private String formatOrderingCustomer(String debtorName, String debtorAccount) {
        StringBuilder result = new StringBuilder();

        if (debtorName != null && !debtorName.trim().isEmpty()) {
            result.append(debtorName.trim());
        } else {
            result.append("ORDERING CUSTOMER");
        }

        if (debtorAccount != null && !debtorAccount.trim().isEmpty()) {
            result.append("\n").append(debtorAccount.trim());
        }

        return result.toString();
    }

    private String formatBeneficiary(String creditorName, String creditorAccount) {
        StringBuilder result = new StringBuilder();

        if (creditorName != null && !creditorName.trim().isEmpty()) {
            result.append(creditorName.trim());
        } else {
            result.append("BENEFICIARY");
        }

        if (creditorAccount != null && !creditorAccount.trim().isEmpty()) {
            result.append("\n").append(creditorAccount.trim());
        }

        return result.toString();
    }

    private String formatValueDateAndAmount(String date, String currency, String amount) {
        StringBuilder result = new StringBuilder();

        // Date au format YYMMDD
        String formattedDate = formatDateForMT(date);
        if (formattedDate.length() >= 8) {
            formattedDate = formattedDate.substring(2, 8); // Convertir YYYYMMDD vers YYMMDD
        }

        // Currency (3 caractères)
        String curr = (currency != null && !currency.trim().isEmpty()) ? currency.trim() : "EUR";
        if (curr.length() > 3) {
            curr = curr.substring(0, 3);
        }

        // Amount
        String amt = (amount != null && !amount.trim().isEmpty()) ? amount.trim() : "0,00";
        amt = amt.replace(".", ","); // Format MT utilise la virgule

        result.append(formattedDate).append(curr).append(amt);

        return result.toString();
    }
}
