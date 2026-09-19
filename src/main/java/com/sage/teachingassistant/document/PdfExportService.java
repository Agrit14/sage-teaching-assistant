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
import com.lowagie.text.pdf.PdfContentByte;
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
 * Generates formatted, publication-ready A4 PDFs for Alpha Tutor documents
 * with consistent branding header and footer on EVERY page.
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

    // Margins: Left 36, Right 36, Top 105 (for header banner on every page), Bottom 62 (for footer on every page)
    private static final float MARGIN_LEFT = 36f;
    private static final float MARGIN_RIGHT = 36f;
    private static final float MARGIN_TOP = 105f;
    private static final float MARGIN_BOTTOM = 62f;

    /**
     * Renders a PDF directly from text content and document metadata.
     */
    public byte[] exportPdf(String documentType, String requestInfo, String contentBody) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, MARGIN_LEFT, MARGIN_RIGHT, MARGIN_TOP, MARGIN_BOTTOM);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            // Set page event to draw Alpha Tutor header & footer on EVERY page
            byte[] headerImgBytes = loadHeaderImageBytes();
            writer.setPageEvent(new AlphaTutorHeaderFooterEvent(headerImgBytes));

            document.open();

            // Document Title & Subtitle (placed below header)
            Paragraph title = new Paragraph(documentType.toUpperCase(), FONT_HEADER_TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(6);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph subtitle = new Paragraph(requestInfo, FONT_SUBTITLE);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(12);
            document.add(subtitle);

            // Student Details Block (for worksheets and tests)
            if (!"REVISION NOTES".equalsIgnoreCase(documentType)) {
                addStudentBox(document);
            }

            // Formatted Body Content
            addBodyContent(document, contentBody);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to export PDF: {}", e.getMessage(), e);
            throw new IllegalStateException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Converts an existing Word .docx (e.g. uploaded by user in pdf-print) to PDF
     * ensuring Alpha Tutor header and footer appear on EVERY page.
     */
    public byte[] convertDocxToPdf(byte[] docxBytes) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(docxBytes);
             XWPFDocument docx = new XWPFDocument(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4, MARGIN_LEFT, MARGIN_RIGHT, MARGIN_TOP, MARGIN_BOTTOM);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            // Set page event to draw Alpha Tutor header & footer on EVERY page
            byte[] headerImgBytes = loadHeaderImageBytes();
            writer.setPageEvent(new AlphaTutorHeaderFooterEvent(headerImgBytes));

            document.open();

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

                // Skip header duplicates if template was already embedded in text
                if (text.contains("VIDUSHI KHANNA") || text.contains("9266973332") || text.contains("Katwaria Sarai")) {
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
        table.setSpacingBefore(4);
        table.setSpacingAfter(10);

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
     * Alpha Tutor Page Event: Renders the Alpha Tutor header banner at the top
     * and the contact/address footer with page numbering at the bottom of EVERY PAGE.
     */
    private static class AlphaTutorHeaderFooterEvent extends PdfPageEventHelper {

        private final byte[] headerImageBytes;

        public AlphaTutorHeaderFooterEvent(byte[] headerImageBytes) {
            this.headerImageBytes = headerImageBytes;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float pageWidth = document.getPageSize().getWidth();
            float pageHeight = document.getPageSize().getHeight();
            float contentWidth = pageWidth - MARGIN_LEFT - MARGIN_RIGHT;

            // =================================================================
            // 1. HEADER ON EVERY PAGE
            // =================================================================
            boolean imageRendered = false;
            if (headerImageBytes != null && headerImageBytes.length > 0) {
                try {
                    Image headerImg = Image.getInstance(headerImageBytes);
                    // Aspect ratio 3574 / 506 = ~7.06
                    float imgHeight = contentWidth / (3574f / 506f);
                    headerImg.scaleAbsolute(contentWidth, imgHeight);
                    // Place 14pt from top edge of page
                    headerImg.setAbsolutePosition(MARGIN_LEFT, pageHeight - 14f - imgHeight);
                    cb.addImage(headerImg);
                    imageRendered = true;
                } catch (Exception e) {
                    log.warn("Could not render header image on page {}: {}", writer.getPageNumber(), e.getMessage());
                }
            }

            if (!imageRendered) {
                // Fallback header banner if image file is not present
                cb.setColorFill(COLOR_PRIMARY);
                Font fbTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COLOR_PRIMARY);
                Font fbSub = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_MUTED);

                PdfPTable fallbackTable = new PdfPTable(1);
                fallbackTable.setTotalWidth(contentWidth);
                PdfPCell titleCell = new PdfPCell(new Phrase("ALPHA TUTOR", fbTitle));
                titleCell.setBorder(Rectangle.NO_BORDER);
                titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                fallbackTable.addCell(titleCell);

                PdfPCell subCell = new PdfPCell(new Phrase("CLASSES 6th–10th | 11th–12th MATHS • VIDUSHI KHANNA", fbSub));
                subCell.setBorder(Rectangle.NO_BORDER);
                subCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                fallbackTable.addCell(subCell);

                fallbackTable.writeSelectedRows(0, -1, MARGIN_LEFT, pageHeight - 16f, cb);
            }

            // =================================================================
            // 2. FOOTER ON EVERY PAGE
            // =================================================================
            // Divider rule above footer
            cb.setColorStroke(new Color(27, 54, 93)); // Alpha Tutor Navy
            cb.setLineWidth(0.75f);
            cb.moveTo(MARGIN_LEFT, 50f);
            cb.lineTo(pageWidth - MARGIN_RIGHT, 50f);
            cb.stroke();

            // 2-column footer table: Contact info on left, Page number on right
            try {
                PdfPTable footerTable = new PdfPTable(new float[]{82f, 18f});
                footerTable.setTotalWidth(contentWidth);
                footerTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

                Font footerBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, COLOR_PRIMARY);
                Font footerText = FontFactory.getFont(FontFactory.HELVETICA, 7.0f, new Color(90, 90, 90));
                Font pageFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.0f, COLOR_PRIMARY);

                Paragraph contactPara = new Paragraph();
                contactPara.setLeading(9f);
                contactPara.add(new Chunk("CLASSES 6th–10th | 11th–12th MATHS • VIDUSHI KHANNA\n", footerBold));
                contactPara.add(new Chunk("9266973332 / 9266987111 • B-54, LIG Flats, Phase-1, Katwaria Sarai, Near Food Point", footerText));

                PdfPCell leftCell = new PdfPCell(contactPara);
                leftCell.setBorder(Rectangle.NO_BORDER);
                leftCell.setPaddingTop(3);
                leftCell.setPaddingLeft(0);
                footerTable.addCell(leftCell);

                PdfPCell rightCell = new PdfPCell(new Phrase("Page " + writer.getPageNumber(), pageFont));
                rightCell.setBorder(Rectangle.NO_BORDER);
                rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                rightCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                rightCell.setPaddingTop(3);
                rightCell.setPaddingRight(0);
                footerTable.addCell(rightCell);

                footerTable.writeSelectedRows(0, -1, MARGIN_LEFT, 47f, cb);
            } catch (Exception e) {
                log.warn("Could not render footer table on page {}: {}", writer.getPageNumber(), e.getMessage());
            }
        }
    }
}
