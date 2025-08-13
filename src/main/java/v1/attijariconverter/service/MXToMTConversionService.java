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

        // Field 20: Transaction Reference Number - utiliser l'ID du message sans générer
        String transactionRef = mxMessage.getMessageId();
        if (transactionRef != null && !transactionRef.trim().isEmpty()) {
            mtMessage.setTransactionReferenceNumber(transactionRef.trim());
        }

        // Field 30: Value Date (format YYYYMMDD) — pas de valeur par défaut
        String valDate = formatDateForMT(mxMessage.getRequestedExecutionDate());
        if (valDate != null && !valDate.isEmpty()) {
            mtMessage.setValueDate(valDate);
        }

        // Field 52A/52D: Ordering Institution
        if (mxMessage.getDebtorBIC() != null && !mxMessage.getDebtorBIC().trim().isEmpty()) {
            mtMessage.setOrderingInstitution(mxMessage.getDebtorBIC().trim());
        }

        // Field 50F/50K: Ordering Customer - pas de valeur par défaut
        String ordering = formatOrderingCustomer(mxMessage.getDebtorName(), mxMessage.getDebtorAccount());
        if (ordering != null) {
            mtMessage.setOrderingCustomer(ordering);
        }

        // Field 59/59A: Beneficiary Institution / Customer - pas de valeur par défaut
        if (mxMessage.getPaymentInstructions() != null && !mxMessage.getPaymentInstructions().isEmpty()) {
            MXMessage.PaymentInstruction firstPayment = mxMessage.getPaymentInstructions().get(0);
            String beneficiary = formatBeneficiary(firstPayment.getCreditorName(), firstPayment.getCreditorAccount());
            if (beneficiary != null) {
                mtMessage.setBeneficiaryInstitution(beneficiary);
            }

            // Field 57A/57D: Account With Institution
            if (firstPayment.getCreditorBIC() != null && !firstPayment.getCreditorBIC().trim().isEmpty()) {
                mtMessage.setAccountWithInstitution(firstPayment.getCreditorBIC().trim());
            }

            // Field 32B: Currency and Amount (ne pas inclure si manquant)
            String ccy = firstPayment.getCurrency();
            String amt = firstPayment.getAmount();
            if (ccy != null && !ccy.trim().isEmpty() && amt != null && !amt.trim().isEmpty()) {
                String formatted = formatCurrencyAmount(ccy, amt);
                mtMessage.setValueDateAndAmount(formatted);
            }
        } else {
            // Données générales si présentes, sans valeurs par défaut
            String beneficiary = formatBeneficiary(mxMessage.getCreditorName(), mxMessage.getCreditorAccount());
            if (beneficiary != null) {
                mtMessage.setBeneficiaryInstitution(beneficiary);
            }
            if (mxMessage.getCreditorBIC() != null && !mxMessage.getCreditorBIC().trim().isEmpty()) {
                mtMessage.setAccountWithInstitution(mxMessage.getCreditorBIC().trim());
            }
            if (mxMessage.getCurrency() != null && !mxMessage.getCurrency().trim().isEmpty()
                    && mxMessage.getAmount() != null && !mxMessage.getAmount().trim().isEmpty()) {
                mtMessage.setValueDateAndAmount(formatCurrencyAmount(mxMessage.getCurrency(), mxMessage.getAmount()));
            }
        }

        logger.info("Conversion MX vers MT (sans valeurs par défaut) pour message: {}", mxMessage.getMessageId());
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

        // Details of Charges: seulement si fourni
        if (mtMessage.getDetailsOfCharges() != null && !mtMessage.getDetailsOfCharges().trim().isEmpty()) {
            mt101.append(":71A:").append(mtMessage.getDetailsOfCharges().trim()).append("\n");
        }

        mt101.append("-}\n");

        // Bloc 5: Trailer Block
        int checksum = Math.abs(mt101.toString().hashCode()) % 1000000000;
        mt101.append("{5:{CHK:").append(String.format("%09d", checksum)).append("ABC}}");

        return mt101.toString();
    }

    private String formatDateForMT(String isoDate) {
        if (isoDate == null || isoDate.trim().isEmpty()) {
            return null; // pas de fallback
        }
        try {
            String s = isoDate.contains("-") ? isoDate.replaceAll("-", "").substring(0, 8) : isoDate;
            if (s.matches("\\d{8}")) return s;
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String formatOrderingCustomer(String debtorName, String debtorAccount) {
        String name = (debtorName != null && !debtorName.trim().isEmpty()) ? debtorName.trim() : null;
        String acc = (debtorAccount != null && !debtorAccount.trim().isEmpty()) ? debtorAccount.trim() : null;
        if (name == null && acc == null) return null;
        return acc == null ? name : name + "\n" + acc;
    }

    private String formatBeneficiary(String creditorName, String creditorAccount) {
        String name = (creditorName != null && !creditorName.trim().isEmpty()) ? creditorName.trim() : null;
        String acc = (creditorAccount != null && !creditorAccount.trim().isEmpty()) ? creditorAccount.trim() : null;
        if (name == null && acc == null) return null;
        return acc == null ? name : name + "\n" + acc;
    }

    private String formatCurrencyAmount(String currency, String amount) {
        if (currency == null || currency.trim().isEmpty() || amount == null || amount.trim().isEmpty()) {
            return null;
        }
        String curr = currency.trim();
        String amt = amount.trim().replace('.', ',');
        return curr + amt; // MT101 :32B: <CCY><AMT>
    }
}
