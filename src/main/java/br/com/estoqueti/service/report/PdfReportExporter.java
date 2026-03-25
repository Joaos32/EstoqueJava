package br.com.estoqueti.service.report;

import br.com.estoqueti.dto.report.ReportDocumentDto;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class PdfReportExporter {

    public void export(ReportDocumentDto document, Path outputPath) {
        try {
            if (outputPath.getParent() != null) {
                Files.createDirectories(outputPath.getParent());
            }

            try (OutputStream outputStream = Files.newOutputStream(outputPath)) {
                Document pdfDocument = new Document(PageSize.A4.rotate(), 28, 28, 32, 28);
                PdfWriter.getInstance(pdfDocument, outputStream);
                pdfDocument.open();

                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
                Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
                Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

                pdfDocument.add(new Paragraph(document.title(), titleFont));
                if (document.subtitle() != null && !document.subtitle().isBlank()) {
                    Paragraph subtitle = new Paragraph(document.subtitle(), subtitleFont);
                    subtitle.setSpacingAfter(12);
                    pdfDocument.add(subtitle);
                }

                PdfPTable table = new PdfPTable(document.headers().size());
                table.setWidthPercentage(100f);
                table.setSpacingBefore(6f);

                for (String header : document.headers()) {
                    PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                    cell.setPadding(6f);
                    table.addCell(cell);
                }

                for (var row : document.rows()) {
                    for (String value : row) {
                        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, bodyFont));
                        cell.setPadding(5f);
                        table.addCell(cell);
                    }
                }

                pdfDocument.add(table);
                pdfDocument.close();
            }
        } catch (IOException | DocumentException exception) {
            throw new IllegalStateException("Nao foi possivel exportar o relatorio em PDF.", exception);
        }
    }
}