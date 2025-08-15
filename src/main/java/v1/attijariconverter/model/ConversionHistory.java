package v1.attijariconverter.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "conversion_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversionHistory {
    @Id
    private String id;

    private LocalDateTime conversionDate;
    private String status;
    private String inputFormat;
    private String outputFormat;
    private Long inputSize;
    private Long outputSize;
    private String errorMessage;
    private String inputFilename;
    private Long processingTimeMs;

    // Propriétés additionnelles pour l'historique détaillé
    private String originalFileName;
    private String convertedFileName;
    private ValidationStatus mxValidationStatus;
    private ValidationStatus mtValidationStatus;

    private String mxContent;

    private String mtContent;

    private List<String> mxValidationErrors;

    private List<String> mtValidationErrors;

    private String messageId;
    private String creationDateTime;
    private String numberOfTransactions;
    private String controlSum;
    private String debtorName;
    private String debtorAccount;
    private String requestedExecutionDate;

    // Nouvel attribut: identifiant de l'utilisateur propriétaire
    private String ownerUsername;

    // Constructeur avec paramètres principaux
    public ConversionHistory(String status, String inputFormat, String outputFormat) {
        this.conversionDate = LocalDateTime.now();
        this.status = status;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
    }
}
