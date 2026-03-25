package br.com.estoqueti.service;

import br.com.estoqueti.dto.delivery.DeliveryProtocolDocumentData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class DeliveryProtocolDocumentService {

    private static final String WORD_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final String XML_NS = XMLConstants.XML_NS_URI;
    private static final String ROOT_TEMPLATE_NAME = "PROTOCOLO DE ENTREGA DE EQUIPAMENTO_NITROLUX_.docx";
    private static final String RESOURCE_TEMPLATE_PATH = "/br/com/estoqueti/template/protocolo-entrega-template.docx";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM", Locale.forLanguageTag("pt-BR"));

    public void generateDocument(DeliveryProtocolDocumentData documentData, Path outputPath) {
        if (documentData == null) {
            throw new IllegalArgumentException("Os dados do protocolo sao obrigatorios para gerar o documento.");
        }
        if (outputPath == null) {
            throw new IllegalArgumentException("O caminho de saida do protocolo e obrigatorio.");
        }

        Path normalizedPath = outputPath.toAbsolutePath().normalize();
        try {
            Path parent = normalizedPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.deleteIfExists(normalizedPath);

            try (InputStream templateStream = openTemplateStream();
                 ZipInputStream zipInputStream = new ZipInputStream(templateStream);
                 OutputStream fileOutputStream = Files.newOutputStream(normalizedPath);
                 ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {

                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    ZipEntry outputEntry = new ZipEntry(entry.getName());
                    zipOutputStream.putNextEntry(outputEntry);

                    if ("word/document.xml".equals(entry.getName())) {
                        byte[] documentXml = zipInputStream.readAllBytes();
                        zipOutputStream.write(buildDocumentXml(documentXml, documentData));
                    } else {
                        zipInputStream.transferTo(zipOutputStream);
                    }

                    zipOutputStream.closeEntry();
                    zipInputStream.closeEntry();
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel gerar o arquivo do protocolo de entrega.", exception);
        }
    }

    private InputStream openTemplateStream() throws IOException {
        Path externalTemplate = Path.of(ROOT_TEMPLATE_NAME);
        if (Files.exists(externalTemplate)) {
            return Files.newInputStream(externalTemplate);
        }

        InputStream resourceStream = DeliveryProtocolDocumentService.class.getResourceAsStream(RESOURCE_TEMPLATE_PATH);
        if (resourceStream == null) {
            throw new IllegalStateException("Modelo de protocolo nao encontrado no projeto nem nos recursos da aplicacao.");
        }
        return resourceStream;
    }

    private byte[] buildDocumentXml(byte[] originalXml, DeliveryProtocolDocumentData documentData) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(originalXml));

            fillProtocolNumber(document, documentData.protocolNumber());
            fillDeclaration(document, documentData.recipientName(), documentData.recipientCpf());
            fillItemsTable(document, documentData);
            fillDateLine(document, documentData.deliveryAt().atZoneSameInstant(ZoneId.systemDefault()).toLocalDate());
            fillSignatureTable(document, documentData.recipientName(), documentData.recipientRole(), documentData.recipientCpf());

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(document), new StreamResult(outputStream));
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Nao foi possivel preencher o modelo DOCX do protocolo.", exception);
        }
    }

    private void fillProtocolNumber(Document document, String protocolNumber) {
        Element titleParagraph = findParagraphContaining(document, "PROTOCOLO DE ENTREGA DE EQUIPAMENTO");
        if (titleParagraph == null) {
            return;
        }

        Element protocolParagraph = findNextParagraph(titleParagraph);
        if (protocolParagraph != null) {
            setParagraphText(protocolParagraph, "Protocolo no " + protocolNumber);
        }
    }

    private void fillDeclaration(Document document, String recipientName, String recipientCpf) {
        Element declarationParagraph = findParagraphContaining(document, "declaro para os devidos fins");
        if (declarationParagraph == null) {
            return;
        }

        String text = getElementText(declarationParagraph);
        text = replaceFirstBlank(text, recipientName);
        text = replaceFirstBlank(text, recipientCpf);
        setParagraphText(declarationParagraph, text);
    }

    private void fillItemsTable(Document document, DeliveryProtocolDocumentData documentData) {
        Element itemsTable = findTableWithAtLeastColumns(document, 5);
        if (itemsTable == null) {
            return;
        }

        List<Element> rows = childElements(itemsTable, "tr");
        if (rows.size() < 2) {
            return;
        }

        Element templateRow = (Element) rows.get(1).cloneNode(true);
        for (int index = rows.size() - 1; index >= 1; index--) {
            itemsTable.removeChild(rows.get(index));
        }

        List<String> values = List.of(
                "1",
                String.valueOf(documentData.itemQuantity()),
                defaultValue(documentData.itemDescription()),
                defaultValue(documentData.itemIdentifier()),
                defaultValue(documentData.itemObservations())
        );
        fillRow(templateRow, values);
        itemsTable.appendChild(templateRow);
    }

    private void fillDateLine(Document document, LocalDate deliveryDate) {
        Element dateParagraph = findParagraphContaining(document, "Recife,");
        if (dateParagraph == null) {
            return;
        }

        String monthName = MONTH_FORMATTER.format(deliveryDate);
        String line = "Recife, " + deliveryDate.getDayOfMonth() + " de " + monthName + " de " + deliveryDate.getYear() + ".";
        setParagraphText(dateParagraph, line);
    }

    private void fillSignatureTable(Document document, String recipientName, String recipientRole, String recipientCpf) {
        Element signatureTable = findTableContaining(document, "Assinatura do(a) Colaborador(a)");
        if (signatureTable == null) {
            return;
        }

        List<Element> rows = childElements(signatureTable, "tr");
        if (rows.size() < 4) {
            return;
        }

        setSecondCellText(rows.get(1), recipientName);
        setSecondCellText(rows.get(2), recipientRole);
        setSecondCellText(rows.get(3), recipientCpf);
    }

    private void setSecondCellText(Element row, String value) {
        List<Element> cells = childElements(row, "tc");
        if (cells.size() < 2) {
            return;
        }
        setCellText(cells.get(1), value);
    }

    private void fillRow(Element row, List<String> values) {
        List<Element> cells = childElements(row, "tc");
        for (int index = 0; index < cells.size(); index++) {
            String value = index < values.size() ? values.get(index) : "";
            setCellText(cells.get(index), value);
        }
    }

    private void setCellText(Element cell, String text) {
        List<Element> paragraphs = childElements(cell, "p");
        if (paragraphs.isEmpty()) {
            Element paragraph = cell.getOwnerDocument().createElementNS(WORD_NS, "w:p");
            cell.appendChild(paragraph);
            setParagraphText(paragraph, text);
            return;
        }

        setParagraphText(paragraphs.get(0), text);
        for (int index = paragraphs.size() - 1; index >= 1; index--) {
            cell.removeChild(paragraphs.get(index));
        }
    }

    private void setParagraphText(Element paragraph, String text) {
        Element templateRun = firstChildElement(paragraph, "r");
        List<Node> removableNodes = new ArrayList<>();
        NodeList childNodes = paragraph.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index++) {
            Node child = childNodes.item(index);
            if (child.getNodeType() == Node.ELEMENT_NODE && WORD_NS.equals(child.getNamespaceURI()) && "r".equals(child.getLocalName())) {
                removableNodes.add(child);
            }
        }
        removableNodes.forEach(paragraph::removeChild);

        Element run = templateRun != null
                ? (Element) templateRun.cloneNode(true)
                : paragraph.getOwnerDocument().createElementNS(WORD_NS, "w:r");
        clearRunContent(run);

        Element textElement = paragraph.getOwnerDocument().createElementNS(WORD_NS, "w:t");
        textElement.setTextContent(text == null ? "" : text);
        textElement.setAttributeNS(XML_NS, "xml:space", "preserve");
        run.appendChild(textElement);
        paragraph.appendChild(run);
    }

    private void clearRunContent(Element run) {
        List<Node> removableNodes = new ArrayList<>();
        NodeList childNodes = run.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index++) {
            Node child = childNodes.item(index);
            if (child.getNodeType() == Node.ELEMENT_NODE && WORD_NS.equals(child.getNamespaceURI()) && !"rPr".equals(child.getLocalName())) {
                removableNodes.add(child);
            }
        }
        removableNodes.forEach(run::removeChild);
    }

    private Element findParagraphContaining(Document document, String searchText) {
        NodeList paragraphs = document.getElementsByTagNameNS(WORD_NS, "p");
        for (int index = 0; index < paragraphs.getLength(); index++) {
            Element paragraph = (Element) paragraphs.item(index);
            if (getElementText(paragraph).contains(searchText)) {
                return paragraph;
            }
        }
        return null;
    }

    private Element findTableContaining(Document document, String searchText) {
        NodeList tables = document.getElementsByTagNameNS(WORD_NS, "tbl");
        for (int index = 0; index < tables.getLength(); index++) {
            Element table = (Element) tables.item(index);
            if (getElementText(table).contains(searchText)) {
                return table;
            }
        }
        return null;
    }

    private Element findTableWithAtLeastColumns(Document document, int minimumColumns) {
        NodeList tables = document.getElementsByTagNameNS(WORD_NS, "tbl");
        for (int index = 0; index < tables.getLength(); index++) {
            Element table = (Element) tables.item(index);
            List<Element> rows = childElements(table, "tr");
            if (!rows.isEmpty() && childElements(rows.get(0), "tc").size() >= minimumColumns) {
                return table;
            }
        }
        return null;
    }

    private Element findNextParagraph(Element currentParagraph) {
        Node sibling = currentParagraph.getNextSibling();
        while (sibling != null) {
            if (sibling.getNodeType() == Node.ELEMENT_NODE && WORD_NS.equals(sibling.getNamespaceURI()) && "p".equals(sibling.getLocalName())) {
                return (Element) sibling;
            }
            sibling = sibling.getNextSibling();
        }
        return null;
    }

    private Element firstChildElement(Element parent, String localName) {
        for (Element child : childElements(parent, localName)) {
            return child;
        }
        return null;
    }

    private List<Element> childElements(Element parent, String localName) {
        List<Element> elements = new ArrayList<>();
        NodeList childNodes = parent.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index++) {
            Node child = childNodes.item(index);
            if (child.getNodeType() == Node.ELEMENT_NODE && WORD_NS.equals(child.getNamespaceURI()) && localName.equals(child.getLocalName())) {
                elements.add((Element) child);
            }
        }
        return elements;
    }

    private String getElementText(Node node) {
        StringBuilder builder = new StringBuilder();
        appendText(node, builder);
        return builder.toString();
    }

    private void appendText(Node node, StringBuilder builder) {
        if (node.getNodeType() == Node.ELEMENT_NODE && WORD_NS.equals(node.getNamespaceURI()) && "t".equals(node.getLocalName())) {
            builder.append(node.getTextContent());
        }

        NodeList childNodes = node.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index++) {
            appendText(childNodes.item(index), builder);
        }
    }

    private String replaceFirstBlank(String text, String value) {
        return text.replaceFirst("_{5,}", value == null ? "" : value);
    }

    private String defaultValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}