package v1.attijariconverter.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import v1.attijariconverter.model.MXMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;

/**
 * Service responsable du parsing d'un fichier pain.001 (SEPA Credit Transfer).
 * Utilise DOM pour extraire les informations nécessaires et construire un MXMessage.
 * Hypothèses:
 *  - Pas de gestion avancée des namespaces multiples.
 *  - Prend le premier élément matching (getElementsByTagName) pour chaque tag.
 *  - Loggue et relance une Exception si un élément critique manque.
 */
@Service
public class MXParsingService {

    private static final Logger logger = LoggerFactory.getLogger(MXParsingService.class);

    /**
     * Parse le XML pain.001 et retourne un MXMessage peuplé.
     * @param xmlContent contenu XML du fichier.
     * @throws Exception si parsing impossible ou éléments obligatoires manquants.
     */
    public MXMessage parseMXMessage(String xmlContent) throws Exception {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlContent.getBytes("UTF-8")));

            logger.info("Parsing du message MX pain.001");

            MXMessage mxMessage = new MXMessage();

            // Parse Group Header
            Element grpHdr = getElement(document, "GrpHdr");
            if (grpHdr == null) {
                throw new Exception("Element GrpHdr non trouvé dans le message MX");
            }

            mxMessage.setMessageId(getTextContent(grpHdr, "MsgId"));
            mxMessage.setCreationDateTime(getTextContent(grpHdr, "CreDtTm"));
            mxMessage.setNumberOfTransactions(getTextContent(grpHdr, "NbOfTxs"));
            mxMessage.setControlSum(getTextContent(grpHdr, "CtrlSum"));

            Element initgPty = getElement(grpHdr, "InitgPty");
            if (initgPty != null) {
                mxMessage.setInitiatingPartyName(getTextContent(initgPty, "Nm"));
            }

            // Parse Payment Information
            NodeList pmtInfList = document.getElementsByTagName("PmtInf");
            if (pmtInfList.getLength() == 0) {
                throw new Exception("Aucune information de paiement trouvée dans le message MX");
            }

            for (int i = 0; i < pmtInfList.getLength(); i++) {
                Element pmtInf = (Element) pmtInfList.item(i);

                mxMessage.setPaymentInformationId(getTextContent(pmtInf, "PmtInfId"));
                mxMessage.setPaymentMethod(getTextContent(pmtInf, "PmtMtd"));
                mxMessage.setRequestedExecutionDate(getTextContent(pmtInf, "ReqdExctnDt"));

                // Debtor information
                Element dbtr = getElement(pmtInf, "Dbtr");
                if (dbtr != null) {
                    mxMessage.setDebtorName(getTextContent(dbtr, "Nm"));
                }

                Element dbtrAcct = getElement(pmtInf, "DbtrAcct");
                if (dbtrAcct != null) {
                    Element id = getElement(dbtrAcct, "Id");
                    if (id != null) {
                        mxMessage.setDebtorAccount(getTextContent(id, "IBAN"));
                    }
                    mxMessage.setCurrency(getTextContent(dbtrAcct, "Ccy"));
                }

                Element dbtrAgt = getElement(pmtInf, "DbtrAgt");
                if (dbtrAgt != null) {
                    Element finInstnId = getElement(dbtrAgt, "FinInstnId");
                    if (finInstnId != null) {
                        mxMessage.setDebtorBIC(getTextContent(finInstnId, "BIC"));
                    }
                }

                // Parse Credit Transfer Transaction Information
                NodeList cdtTrfTxInfList = pmtInf.getElementsByTagName("CdtTrfTxInf");
                for (int j = 0; j < cdtTrfTxInfList.getLength(); j++) {
                    Element cdtTrfTxInf = (Element) cdtTrfTxInfList.item(j);
                    MXMessage.PaymentInstruction instruction = parsePaymentInstruction(cdtTrfTxInf, pmtInf);
                    mxMessage.addPaymentInstruction(instruction);
                }
            }

            logger.info("Message MX parsé avec succès: {} transactions", mxMessage.getPaymentInstructions().size());
            return mxMessage;

        } catch (Exception e) {
            logger.error("Erreur lors du parsing du message MX", e);
            throw new Exception("Erreur lors du parsing du message MX: " + e.getMessage(), e);
        }
    }

    /**
     * Construit une PaymentInstruction à partir d'un noeud <CdtTrfTxInf> et de son parent <PmtInf>.
     */
    private MXMessage.PaymentInstruction parsePaymentInstruction(Element cdtTrfTxInf, Element pmtInf) {
        MXMessage.PaymentInstruction instruction = new MXMessage.PaymentInstruction();

        // Payment ID
        Element pmtId = getElement(cdtTrfTxInf, "PmtId");
        if (pmtId != null) {
            instruction.setInstructionId(getTextContent(pmtId, "InstrId"));
            instruction.setEndToEndId(getTextContent(pmtId, "EndToEndId"));
        }

        // Amount
        Element amt = getElement(cdtTrfTxInf, "Amt");
        if (amt != null) {
            Element instdAmt = getElement(amt, "InstdAmt");
            if (instdAmt != null) {
                instruction.setAmount(instdAmt.getTextContent());
                instruction.setCurrency(instdAmt.getAttribute("Ccy"));
            }
        }

        // Creditor Agent
        Element cdtrAgt = getElement(cdtTrfTxInf, "CdtrAgt");
        if (cdtrAgt != null) {
            Element finInstnId = getElement(cdtrAgt, "FinInstnId");
            if (finInstnId != null) {
                instruction.setCreditorBIC(getTextContent(finInstnId, "BIC"));
            }
        }

        // Creditor
        Element cdtr = getElement(cdtTrfTxInf, "Cdtr");
        if (cdtr != null) {
            instruction.setCreditorName(getTextContent(cdtr, "Nm"));
        }

        // Creditor Account
        Element cdtrAcct = getElement(cdtTrfTxInf, "CdtrAcct");
        if (cdtrAcct != null) {
            Element id = getElement(cdtrAcct, "Id");
            if (id != null) {
                instruction.setCreditorAccount(getTextContent(id, "IBAN"));
            }
        }

        // Remittance Information
        Element rmtInf = getElement(cdtTrfTxInf, "RmtInf");
        if (rmtInf != null) {
            instruction.setRemittanceInfo(getTextContent(rmtInf, "Ustrd"));
        }

        // Charge Bearer from parent PmtInf
        instruction.setChargeBearer(getTextContent(pmtInf, "ChrgBr"));

        // Get debtor info from parent PmtInf
        Element dbtr = getElement(pmtInf, "Dbtr");
        if (dbtr != null) {
            instruction.setDebtorName(getTextContent(dbtr, "Nm"));
        }

        Element dbtrAcct = getElement(pmtInf, "DbtrAcct");
        if (dbtrAcct != null) {
            Element id = getElement(dbtrAcct, "Id");
            if (id != null) {
                instruction.setDebtorAccount(getTextContent(id, "IBAN"));
            }
        }

        Element dbtrAgt = getElement(pmtInf, "DbtrAgt");
        if (dbtrAgt != null) {
            Element finInstnId = getElement(dbtrAgt, "FinInstnId");
            if (finInstnId != null) {
                instruction.setDebtorBIC(getTextContent(finInstnId, "BIC"));
            }
        }

        // Requested Execution Date from parent PmtInf
        instruction.setRequestedExecutionDate(getTextContent(pmtInf, "ReqdExctnDt"));

        return instruction;
    }

    /** Récupère le premier élément enfant par tag (ou null). */
    private Element getElement(Element parent, String tagName) {
        if (parent == null) return null;
        NodeList nodeList = parent.getElementsByTagName(tagName);
        return nodeList.getLength() > 0 ? (Element) nodeList.item(0) : null;
    }

    /** Récupère le premier élément global par tag. */
    private Element getElement(Document document, String tagName) {
        NodeList nodeList = document.getElementsByTagName(tagName);
        return nodeList.getLength() > 0 ? (Element) nodeList.item(0) : null;
    }

    /** Valeur texte d'un sous-élément. */
    private String getTextContent(Element parent, String tagName) {
        if (parent == null) return null;
        Element element = getElement(parent, tagName);
        return element != null ? element.getTextContent().trim() : null;
    }
}
