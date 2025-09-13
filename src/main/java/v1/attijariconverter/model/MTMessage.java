package v1.attijariconverter.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Représentation objet d'un message MT (ici MT101) avant sérialisation SWIFT.
 * Chaque champ correspond à un tag standard (ex: :20:, :32B:, :50K:...).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MTMessage {

    // Field 20: Transaction Reference Number (obligatoire)
    private String transactionReferenceNumber;

    // Field 21: Related Reference (optionnel)
    private String relatedReference;

    // Field 28D: Message Index/Total (ex: 1/1)
    private String messageIndexTotal;

    // Field 30: Value Date AAAAMMJJ
    private String valueDate;

    // Field 32A/32B: Value Date and Amount / Currency+Amount
    private String valueDateAndAmount;

    // Field 50F/50K: Ordering Customer (ligne nom + éventuellement compte)
    private String orderingCustomer;

    // Field 52A/52D: Ordering Institution (BIC)
    private String orderingInstitution;

    // Field 56A/56C/56D: Intermediary Institution
    private String intermediaryInstitution;

    // Field 57A/57B/57C/57D: Account With Institution (BIC banque destinataire)
    private String accountWithInstitution;

    // Field 59/59A: Beneficiary Customer (nom + IBAN)
    private String beneficiaryCustomer;

    // Field 59: Beneficiary Institution (alternative / compatibilité interne)
    private String beneficiaryInstitution;

    // Field 70: Remittance Information (motif / référence)
    private String remittanceInformation;

    // Field 71A: Details of Charges (OUR / BEN / SHA)
    private String detailsOfCharges;

    // Field 72: Sender to Receiver Information (instructions libres)
    private String senderToReceiverInfo;

    // Champs supplémentaires
    private String instructionCode; // Code instruction bancaire
    private String currency;        // Devise principale
    private String amount;          // Montant principal

    // Constructeur sans valeurs par défaut
    public MTMessage(String transactionReferenceNumber, String valueDate, String orderingCustomer, String beneficiaryInstitution) {
        this.transactionReferenceNumber = transactionReferenceNumber;
        this.valueDate = valueDate;
        this.orderingCustomer = orderingCustomer;
        this.beneficiaryInstitution = beneficiaryInstitution;
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
