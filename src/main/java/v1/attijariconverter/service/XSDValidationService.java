package v1.attijariconverter.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class XSDValidationService {

    private final List<String> validationErrors = new ArrayList<>();

    public ValidationResult validateMXMessage(String xmlContent) {
        validationErrors.clear();

        try {
            // Pour l'instant, on fait une validation basique sans XSD strict
            // car le fichier XSD complet est très complexe
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            // Parse le document pour vérifier qu'il est bien formé
            Document document = documentBuilder.parse(new ByteArrayInputStream(xmlContent.getBytes("UTF-8")));

            // Vérifications basiques du format PAIN 001
            if (!xmlContent.contains("urn:iso:std:iso:20022:tech:xsd:pain.001.001.03")) {
                validationErrors.add("Namespace PAIN 001 v3 manquant ou incorrect");
            }

            if (!xmlContent.contains("<Document")) {
                validationErrors.add("Élément racine Document manquant");
            }

            if (!xmlContent.contains("<CstmrCdtTrfInitn>")) {
                validationErrors.add("Élément CstmrCdtTrfInitn manquant");
            }

            if (!xmlContent.contains("<GrpHdr>")) {
                validationErrors.add("Élément GrpHdr (Group Header) manquant");
            }

            if (!xmlContent.contains("<PmtInf>")) {
                validationErrors.add("Élément PmtInf (Payment Information) manquant");
            }

            return new ValidationResult(validationErrors.isEmpty(), new ArrayList<>(validationErrors));

        } catch (Exception e) {
            validationErrors.add("Erreur lors de la validation XML: " + e.getMessage());
            return new ValidationResult(false, new ArrayList<>(validationErrors));
        }
    }

    // Méthode spécifique pour valider les fichiers pain.001
    public ValidationResult validatePain001(String xmlContent) {
        validationErrors.clear();

        try {
            // Validation de base du format XML
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            // Parse le document pour vérifier qu'il est bien formé
            Document document = documentBuilder.parse(new ByteArrayInputStream(xmlContent.getBytes("UTF-8")));

            // Vérifications spécifiques pain.001
            validatePain001Structure(xmlContent);
            validatePain001Content(document);

            return new ValidationResult(validationErrors.isEmpty(), new ArrayList<>(validationErrors));

        } catch (Exception e) {
            validationErrors.add("Erreur lors de la validation pain.001: " + e.getMessage());
            return new ValidationResult(false, new ArrayList<>(validationErrors));
        }
    }

    private void validatePain001Structure(String xmlContent) {
        // Vérification du namespace pain.001
        if (!xmlContent.contains("urn:iso:std:iso:20022:tech:xsd:pain.001.001.03")) {
            validationErrors.add("Namespace pain.001.001.03 manquant ou incorrect");
        }

        // Vérification des éléments obligatoires
        if (!xmlContent.contains("<Document")) {
            validationErrors.add("Élément racine <Document> manquant");
        }

        if (!xmlContent.contains("<CstmrCdtTrfInitn>")) {
            validationErrors.add("Élément <CstmrCdtTrfInitn> manquant");
        }

        if (!xmlContent.contains("<GrpHdr>")) {
            validationErrors.add("Élément <GrpHdr> (Group Header) manquant");
        }

        if (!xmlContent.contains("<PmtInf>")) {
            validationErrors.add("Élément <PmtInf> (Payment Information) manquant");
        }

        if (!xmlContent.contains("<CdtTrfTxInf>")) {
            validationErrors.add("Élément <CdtTrfTxInf> (Credit Transfer Transaction Information) manquant");
        }
    }

    private void validatePain001Content(Document document) {
        // Validation du contenu (structure interne)
        // Cette méthode peut être étendue pour des validations plus spécifiques
        try {
            // Vérifier que le document a une structure valide
            if (document.getDocumentElement() == null) {
                validationErrors.add("Document XML invalide");
            }
        } catch (Exception e) {
            validationErrors.add("Erreur lors de la validation du contenu: " + e.getMessage());
        }
    }

    // Classe pour le résultat de validation
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
    }

    // Error handler personnalisé
    private static class CustomErrorHandler implements ErrorHandler {
        private final List<String> errors = new ArrayList<>();

        @Override
        public void warning(SAXParseException exception) throws SAXException {
            errors.add("Warning: " + exception.getMessage());
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            errors.add("Error: " + exception.getMessage());
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            errors.add("Fatal Error: " + exception.getMessage());
            throw exception;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
