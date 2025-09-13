package v1.attijariconverter.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Document MongoDB traçant chaque tentative de conversion.
 * Regroupe métadonnées techniques (tailles, dates, status) et fonctionnelles (identifiants paiements).
 */
@Document(collection = "conversion_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversionHistory {
    @Id
    private String id; // Identifiant MongoDB

    private LocalDateTime conversionDate; // Horodatage de la tentative
    private String status; // SUCCESS ou ERROR
    private String inputFormat; // ex: pain.001
    private String outputFormat; // ex: MT101
    private Long inputSize; // Taille contenu source (caractères)
    private Long outputSize; // Taille contenu cible (caractères)
    private String errorMessage; // Message fonctionnel si échec
    private String inputFilename; // Nom fichier uploadé si disponible
    private Long processingTimeMs; // Durée traitement (optionnel)

    // Fichiers / noms générés
    private String originalFileName; // Nom initial
    private String convertedFileName; // Nom fichier MT généré
    private ValidationStatus mxValidationStatus; // Statut validation MX
    private ValidationStatus mtValidationStatus; // Statut validation MT

    private String mxContent; // XML original (peut être volumineux)
    private String mtContent; // Message MT généré

    private List<String> mxValidationErrors; // Erreurs XSD / parsing MX
    private List<String> mtValidationErrors; // Erreurs structure MT

    // Champs métier extraits (facilitent reporting / filtrage)
    private String messageId;
    private String creationDateTime;
    private String numberOfTransactions;
    private String controlSum;
    private String debtorName;
    private String debtorAccount;
    private String requestedExecutionDate;

    // Propriétaire (username Spring Security) - null/"anonymous" si non authentifié
    private String ownerUsername;

    // Constructeur avec paramètres principaux
    public ConversionHistory(String status, String inputFormat, String outputFormat) {
        this.conversionDate = LocalDateTime.now();
        this.status = status;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
    }
}
