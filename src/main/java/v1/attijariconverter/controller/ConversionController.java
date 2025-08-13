package v1.attijariconverter.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import v1.attijariconverter.service.ConversionService;
import v1.attijariconverter.service.XSDValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/conversion")
@CrossOrigin(origins = "*")
public class ConversionController {

    private static final Logger logger = LoggerFactory.getLogger(ConversionController.class);

    @Autowired
    private ConversionService conversionService;

    @Autowired
    private XSDValidationService xsdValidationService;

    public static class ConversionResponse {
        private boolean success;
        private String mtMessage;
        private String errorMessage;
        private List<String> validationErrors;

        // Constructors
        public ConversionResponse() {}

        public ConversionResponse(boolean success, String mtMessage, String errorMessage, List<String> validationErrors) {
            this.success = success;
            this.mtMessage = mtMessage;
            this.errorMessage = errorMessage;
            this.validationErrors = validationErrors;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMtMessage() { return mtMessage; }
        public void setMtMessage(String mtMessage) { this.mtMessage = mtMessage; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public List<String> getValidationErrors() { return validationErrors; }
        public void setValidationErrors(List<String> validationErrors) { this.validationErrors = validationErrors; }
    }

    public static class ValidationResponse {
        private boolean valid;
        private String message;
        private List<String> errors;

        public ValidationResponse() {}

        public ValidationResponse(boolean valid, String message, List<String> errors) {
            this.valid = valid;
            this.message = message;
            this.errors = errors;
        }

        // Getters and Setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validatePain001(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("Début de la validation du fichier: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ValidationResponse(false, "Le fichier est vide", null));
            }

            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            XSDValidationService.ValidationResult result = xsdValidationService.validatePain001(content);

            ValidationResponse response = new ValidationResponse(
                result.isValid(),
                result.isValid() ? "✅ Fichier pain.001 valide" : "❌ Erreurs de validation détectées",
                result.getErrors()
            );

            logger.info("Validation terminée. Valide: {}", result.isValid());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Erreur lors de la validation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ValidationResponse(false, "❌ Erreur: " + e.getMessage(), null));
        }
    }

    @PostMapping("/convert")
    public ResponseEntity<ConversionResponse> convertToMT101(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("Début de la conversion vers MT101: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ConversionResponse(false, null, "Le fichier est vide", null));
            }

            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            // Valider d'abord le fichier pain.001
            XSDValidationService.ValidationResult validationResult = xsdValidationService.validatePain001(content);
            if (!validationResult.isValid()) {
                return ResponseEntity.badRequest()
                    .body(new ConversionResponse(false, null, "Fichier pain.001 invalide", validationResult.getErrors()));
            }

            // Convertir vers MT101
            ConversionService.ConversionResult conversionResult = conversionService.convertMXToMT101(content);

            ConversionResponse response = new ConversionResponse(
                conversionResult.isSuccess(),
                conversionResult.getMtMessage(),
                conversionResult.getErrorMessage(),
                conversionResult.getValidationErrors()
            );

            if (conversionResult.isSuccess()) {
                logger.info("Conversion MT101 réussie");
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Échec de la conversion: {}", conversionResult.getErrorMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("Erreur lors de la conversion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ConversionResponse(false, null, "Erreur lors de la conversion: " + e.getMessage(), null));
        }
    }

    @PostMapping("/convert/text")
    public ResponseEntity<ConversionResponse> convertTextToMT101(@RequestBody String xmlContent) {
        try {
            logger.info("Début de la conversion de texte vers MT101");

            if (xmlContent == null || xmlContent.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ConversionResponse(false, null, "Le contenu XML est vide", null));
            }

            // Valider d'abord le contenu pain.001
            XSDValidationService.ValidationResult validationResult = xsdValidationService.validatePain001(xmlContent);
            if (!validationResult.isValid()) {
                return ResponseEntity.badRequest()
                    .body(new ConversionResponse(false, null, "Contenu pain.001 invalide", validationResult.getErrors()));
            }

            // Convertir vers MT101
            ConversionService.ConversionResult conversionResult = conversionService.convertMXToMT101(xmlContent);

            ConversionResponse response = new ConversionResponse(
                conversionResult.isSuccess(),
                conversionResult.getMtMessage(),
                conversionResult.getErrorMessage(),
                conversionResult.getValidationErrors()
            );

            if (conversionResult.isSuccess()) {
                logger.info("Conversion de texte MT101 réussie");
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Échec de la conversion de texte: {}", conversionResult.getErrorMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("Erreur lors de la conversion de texte", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ConversionResponse(false, null, "Erreur lors de la conversion: " + e.getMessage(), null));
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("API de conversion opérationnelle");
    }
}
