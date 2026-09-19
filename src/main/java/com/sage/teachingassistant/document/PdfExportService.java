package com.sage.teachingassistant.document;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates formatted, publication-ready A4 PDFs for Alpha Tutor documents.
 */
@Service
public class PdfExportService {

    private static final Logger log = LoggerFactory.getLogger(PdfExportService.class);

    private static final Color COLOR_PRIMARY = new Color(27, 54, 93);    // Navy #1B365D
    private static final Color COLOR_TEXT = new Color(50, 50, 50);       // Dark gray #323232
    private static final Color COLOR_MUTED = new Color(110, 110, 110);   // Gray

    private static final Font FONT_HEADER_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, COLOR_PRIMARY);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_TEXT);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_PRIMARY);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_TEXT);
    private static final Font FONT_BOLD_BODY = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_TEXT);
    private static final Font FONT_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, COLOR_MUTED);

    /**
     * Renders a PDF directly from text content and document metadata.
     */
    public byte[] exportPdf(String documentType, String requestInfo, String contentBody) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 40, 45);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new HeaderFooterPageEvent());

            document.open();

            // 1. Header Banner / Logo
            addHeaderImage(document);

            // 2. Document Title & Subtitle
            Paragraph title = new Paragraph(documentType.toUpperCase(), FONT_HEADER_TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(10);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph subtitle = new Paragraph(requestInfo, FONT_SUBTITLE);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(14);
            document.add(subtitle);

            // 3. Student Details Block (for worksheets and tests)
            if (!"REVISION NOTES".equalsIgnoreCase(documentType)) {
                addStudentBox(document);
            }

            // 4. Formatted Body Content
            addBodyContent(document, contentBody);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to export PDF: {}", e.getMessage(), e);
            throw new IllegalStateException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Converts an existing Word .docx (e.g. uploaded by user in pdf-print) to PDF.
     */
    public byte[] convertDocxToPdf(byte[] docxBytes) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(docxBytes);
             XWPFDocument docx = new XWPFDocument(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4, 40, 40, 40, 45);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new HeaderFooterPageEvent());

            document.open();

            // Add Header Banner
            addHeaderImage(document);

            // Render tables from docx
            for (XWPFTable table : docx.getTables()) {
                PdfPTable pdfTable = new PdfPTable(table.getNumberOfRows() > 0 ? table.getRow(0).getTableCells().size() : 1);
                pdfTable.setWidthPercentage(100);
                pdfTable.setSpacingBefore(8);
                pdfTable.setSpacingAfter(8);

                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        PdfPCell pdfCell = new PdfPCell(new Phrase(cell.getText(), FONT_BODY));
                        pdfCell.setPadding(4);
                        pdfTable.addCell(pdfCell);
                    }
                }
                document.add(pdfTable);
            }

            // Render paragraphs from docx
            for (XWPFParagraph p : docx.getParagraphs()) {
                String text = p.getText().trim();
                if (text.isEmpty()) {
                    continue;
                }

                // Skip header duplicates if template was already embedded
                if (text.contains("VIDUSHI KHANNA") || text.contains("9266973332")) {
                    continue;
                }

                Paragraph pdfPara;
                if (text.toUpperCase().startsWith("SECTION") || text.startsWith("#")) {
                    pdfPara = new Paragraph(cleanMarkdown(text), FONT_SECTION);
                    pdfPara.setSpacingBefore(12);
                    pdfPara.setSpacingAfter(6);
                } else if (text.matches("^\\d+[\\.\\)]\\s+.*")) {
                    pdfPara = new Paragraph(cleanMarkdown(text), FONT_BOLD_BODY);
                    pdfPara.setSpacingBefore(6);
                    pdfPara.setSpacingAfter(3);
                    pdfPara.setIndentationLeft(10);
                } else {
                    pdfPara = new Paragraph(cleanMarkdown(text), FONT_BODY);
                    pdfPara.setSpacingBefore(3);
                    pdfPara.setSpacingAfter(3);
                }
                document.add(pdfPara);
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to convert DOCX to PDF: {}", e.getMessage(), e);
            throw new IllegalStateException("DOCX to PDF conversion failed: " + e.getMessage(), e);
        }
    }

    private void addHeaderImage(Document document) {
        byte[] imgBytes = loadHeaderImageBytes();
        if (imgBytes != null && imgBytes.length > 0) {
            try {
                Image img = Image.getInstance(imgBytes);
                float pageWidth = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin();
                img.scaleToFit(pageWidth, 120);
                img.setAlignment(Element.ALIGN_CENTER);
                img.setSpacingAfter(8);
                document.add(img);
                return;
            } catch (Exception e) {
                log.warn("Could not insert header image: {}", e.getMessage());
            }
        }

        // Fallback textual header if image is not loadable
        Paragraph banner = new Paragraph("ALPHA TUTOR", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, COLOR_PRIMARY));
        banner.setAlignment(Element.ALIGN_CENTER);
        document.add(banner);
        Paragraph tagline = new Paragraph("CLASSES 6th–10th | 11th–12th MATHS • VIDUSHI KHANNA\nPhone: 9266973332 / 9266987111", FONT_SMALL);
        tagline.setAlignment(Element.ALIGN_CENTER);
        tagline.setSpacingAfter(10);
        document.add(tagline);
    }

    private byte[] loadHeaderImageBytes() {
        try (InputStream is = getClass().getResourceAsStream("/template/alpha_tutor_header.jpeg")) {
            if (is != null) {
                return is.readAllBytes();
            }
        } catch (Exception ignored) {
        }

        Path diskPath = Paths.get("src/main/resources/template/alpha_tutor_header.jpeg");
        if (Files.exists(diskPath)) {
            try {
                return Files.readAllBytes(diskPath);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private void addStudentBox(Document document) throws Exception {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.setSpacingAfter(12);

        table.addCell(createCell("Student Name: _____________________"));
        table.addCell(createCell("Roll No: ____________"));
        table.addCell(createCell("Date: ____________"));

        table.addCell(createCell("Class & Sec: _______________________"));
        table.addCell(createCell("Time: 45 Mins"));
        table.addCell(createCell("Max Marks: ________"));

        document.add(table);
    }

    private PdfPCell createCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_SMALL));
        cell.setPadding(5);
        cell.setBorderColor(new Color(200, 200, 200));
        return cell;
    }

    private void addBodyContent(Document document, String content) throws Exception {
        if (content == null || content.isBlank()) {
            return;
        }

        String[] lines = content.split("\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("# ") || line.toUpperCase().startsWith("SECTION") || line.toUpperCase().startsWith("PART ")) {
                Paragraph p = new Paragraph(cleanMarkdown(line), FONT_SECTION);
                p.setSpacingBefore(12);
                p.setSpacingAfter(5);
                document.add(p);
            } else if (line.startsWith("## ") || line.startsWith("### ")) {
                Paragraph p = new Paragraph(cleanMarkdown(line), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f, COLOR_PRIMARY));
                p.setSpacingBefore(8);
                p.setSpacingAfter(3);
                document.add(p);
            } else if (line.matches("^\\d+[\\.\\)]\\s+.*")) {
                Paragraph p = new Paragraph(cleanMarkdown(line), FONT_BOLD_BODY);
                p.setSpacingBefore(6);
                p.setSpacingAfter(3);
                p.setIndentationLeft(8);
                document.add(p);
            } else if (line.matches("^[\\(\\[]?[A-Da-d][\\)\\]\\.]\\s+.*") || line.startsWith("- ") || line.startsWith("* ")) {
                Paragraph p = new Paragraph(cleanMarkdown(line), FONT_BODY);
                p.setSpacingBefore(2);
                p.setSpacingAfter(2);
                p.setIndentationLeft(20);
                document.add(p);
            } else {
                Paragraph p = new Paragraph(cleanMarkdown(line), FONT_BODY);
                p.setSpacingBefore(3);
                p.setSpacingAfter(3);
                document.add(p);
            }
        }
    }

    private String cleanMarkdown(String text) {
        return text
                .replaceAll("^#{1,6}\\s+", "")
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
                .replaceAll("\\*(.*?)\\*", "$1")
                .trim();
    }

    /**
     * Adds page numbers and subtle bottom branding on each page.
     */
    private static class HeaderFooterPageEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Rectangle rect = document.getPageSize();
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_MUTED);

            // Top subtle rule
            // Bottom footer
            PdfPTable footer = new PdfPTable(2);
            footer.setTotalWidth(rect.getWidth() - 80);
            footer.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            PdfPCell leftCell = new PdfPCell(new Phrase("Alpha Tutor — Academic Excellence", footerFont));
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            footer.addCell(leftCell);

            PdfPCell rightCell = new PdfPCell(new Phrase("Page " + writer.getPageNumber(), footerFont));
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            footer.addCell(rightCell);

            footer.writeSelectedRows(0, -1, 40, 30, writer.getDirectContent());
        }
    }
}
