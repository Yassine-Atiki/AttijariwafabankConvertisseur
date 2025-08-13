package v1.attijariconverter.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.ArrayList;

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

    // Liste des instructions de paiement
    private List<PaymentInstruction> paymentInstructions = new ArrayList<>();

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

        // Getters supplémentaires pour compatibilité
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
