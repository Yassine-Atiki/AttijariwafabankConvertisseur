package v1.attijariconverter.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "conversion_history")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversionHistory {
    @Id
    private String id;
    private String originalFileName;
    private boolean validMx;
    private boolean validMt;
    private String mt101Path;
    private String errorMessage;
    private LocalDateTime conversionDate;
}
