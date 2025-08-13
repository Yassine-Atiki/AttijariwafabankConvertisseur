package v1.attijariconverter.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MTMessage {

    // Field 20: Transaction Reference Number
    private String transactionReferenceNumber;

    // Field 21: Related Reference (optionnel)
    private String relatedReference;

    // Field 28D: Message Index/Total
    private String messageIndexTotal;

    // Field 30: Value Date
    private String valueDate;

    // Field 32A/32B: Value Date and Amount
    private String valueDateAndAmount;

    // Field 50F/50K: Ordering Customer
    private String orderingCustomer;

    // Field 52A/52D: Ordering Institution
    private String orderingInstitution;

    // Field 56A/56C/56D: Intermediary Institution
    private String intermediaryInstitution;

    // Field 57A/57B/57C/57D: Account With Institution
    private String accountWithInstitution;

    // Field 59/59A: Beneficiary Customer
    private String beneficiaryCustomer;

    // Field 59: Beneficiary Institution (pour compatibilité)
    private String beneficiaryInstitution;

    // Field 70: Remittance Information
    private String remittanceInformation;

    // Field 71A: Details of Charges
    private String detailsOfCharges;

    // Field 72: Sender to Receiver Information
    private String senderToReceiverInfo;

    // Champs additionnels pour MT101
    private String instructionCode;
    private String currency;
    private String amount;

    // Constructeur avec les champs principaux
    public MTMessage(String transactionReferenceNumber, String valueDate, String orderingCustomer, String beneficiaryInstitution) {
        this.transactionReferenceNumber = transactionReferenceNumber;
        this.valueDate = valueDate;
        this.orderingCustomer = orderingCustomer;
        this.beneficiaryInstitution = beneficiaryInstitution;
        this.messageIndexTotal = "1/1";
        this.detailsOfCharges = "SHA";
    }

    // Méthodes utilitaires
    public void setDefaultValues() {
        if (messageIndexTotal == null) {
            messageIndexTotal = "1/1";
        }
        if (detailsOfCharges == null) {
            detailsOfCharges = "SHA";
        }
    }

    @Override
    public String toString() {
        return "MTMessage{" +
                "transactionReferenceNumber='" + transactionReferenceNumber + '\'' +
                ", valueDate='" + valueDate + '\'' +
                ", orderingCustomer='" + orderingCustomer + '\'' +
                ", beneficiaryInstitution='" + beneficiaryInstitution + '\'' +
                ", amount='" + amount + '\'' +
                ", currency='" + currency + '\'' +
                '}';
    }
}
