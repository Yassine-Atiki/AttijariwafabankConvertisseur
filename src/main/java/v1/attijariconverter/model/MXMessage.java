package v1.attijariconverter.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.ArrayList;

/**
 * Représentation simplifiée (POJO) d'un message pain.001 (SEPA Credit Transfer) après parsing.
 * Contient également une liste d'instructions de paiement unitaires (PaymentInstruction).
 * Sert d'étape intermédiaire avant la transformation vers MT101.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MXMessage {
    private String messageId;
    private String creationDateTime;
    private String numberOfTransactions;
    private String controlSum;
    private String initiatingPartyName;
    private String paymentInformationId;
    private String paymentMethod;
    private String requestedExecutionDate;
    private String debtorName;
    private String debtorAccount;
    private String debtorBIC;
    private String creditorName;
    private String creditorAccount;
    private String creditorBIC;
    private String amount;
    private String currency;
    private String remittanceInformation;

    /**
     * Instructions de paiement individuelles (chaque <CdtTrfTxInf> du XML source).
     */
    private List<PaymentInstruction> paymentInstructions = new ArrayList<>();

    /**
     * Éléments d'une transaction crédit SEPA unique.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInstruction {
        private String instructionId;
        private String endToEndId;
        private String amount;
        private String currency;
        private String debtorName;
        private String debtorAccount;
        private String debtorBIC;
        private String creditorName;
        private String creditorAccount;
        private String creditorBIC;
        private String chargeBearer;
        private String remittanceInfo;
        private String requestedExecutionDate;
        private String categoryPurpose;
        private String ultimateDebtor;
        private String ultimateCreditor;

        // Getters supplémentaires pour compatibilit��
        public String getInstructionId() { return instructionId; }
        public String getAmount() { return amount; }
        public String getCurrency() { return currency; }
        public String getDebtorName() { return debtorName; }
        public String getCreditorName() { return creditorName; }
        public String getChargeBearer() { return chargeBearer; }
        public String getRemittanceInfo() { return remittanceInfo; }
    }

    // Méthodes utilitaires
    public void addPaymentInstruction(PaymentInstruction instruction) {
        if (paymentInstructions == null) {
            paymentInstructions = new ArrayList<>();
        }
        paymentInstructions.add(instruction);
    }

    // Méthode pour obtenir l'EndToEndId du premier paiement
    public String getEndToEndId() {
        if (paymentInstructions != null && !paymentInstructions.isEmpty()) {
            return paymentInstructions.get(0).getEndToEndId();
        }
        return null;
    }

    @Override
    public String toString() {
        return "MXMessage{" +
                "messageId='" + messageId + '\'' +
                ", creationDateTime='" + creationDateTime + '\'' +
                ", numberOfTransactions='" + numberOfTransactions + '\'' +
                ", controlSum='" + controlSum + '\'' +
                ", initiatingPartyName='" + initiatingPartyName + '\'' +
                ", paymentInformationId='" + paymentInformationId + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", requestedExecutionDate='" + requestedExecutionDate + '\'' +
                ", debtorName='" + debtorName + '\'' +
                ", debtorAccount='" + debtorAccount + '\'' +
                ", debtorBIC='" + debtorBIC + '\'' +
                ", creditorName='" + creditorName + '\'' +
                ", creditorAccount='" + creditorAccount + '\'' +
                ", creditorBIC='" + creditorBIC + '\'' +
                ", amount='" + amount + '\'' +
                ", currency='" + currency + '\'' +
                ", remittanceInformation='" + remittanceInformation + '\'' +
                ", paymentInstructions=" + paymentInstructions +
                '}';
    }
}
