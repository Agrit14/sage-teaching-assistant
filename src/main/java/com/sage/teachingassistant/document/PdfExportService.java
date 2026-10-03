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
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates formatted, publication-ready A4 PDFs for Alpha Tutor documents
 * with consistent branding header and footer on EVERY page.
 */
@Service
public class PdfExportService {

    private static final Logger log = LoggerFactory.getLogger(PdfExportService.class);

    private static final Color COLOR_PRIMARY = new Color(27, 54, 93);         // Navy #1B365D
    private static final Color COLOR_TEXT = new Color(50, 50, 50);            // Dark gray #323232
    private static final Color COLOR_MUTED = new Color(110, 110, 110);        // Gray
    private static final Color COLOR_GOLD = new Color(244, 179, 0);            // Golden accent line #F4B300
    private static final Color COLOR_FOOTER_GREEN = new Color(7, 93, 42);      // Footer Green #075D2A
    private static final Color COLOR_FOOTER_RED = new Color(214, 24, 31);      // Footer Red #D6181F

    private static final Font FONT_HEADER_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, COLOR_PRIMARY);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_TEXT);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_PRIMARY);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_TEXT);
    private static final Font FONT_BOLD_BODY = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_TEXT);
    private static final Font FONT_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, COLOR_MUTED);

    // Margins: Left 36, Right 36, Top 105 (for header banner on every page), Bottom 65 (for footer on every page)
    private static final float MARGIN_LEFT = 36f;
    private static final float MARGIN_RIGHT = 36f;
    private static final float MARGIN_TOP = 105f;
    private static final float MARGIN_BOTTOM = 65f;

    /**
     * Renders a PDF directly from text content and document metadata.
     */
    public byte[] exportPdf(String documentType, String requestInfo, String contentBody) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            contentBody = DocxTemplateGenerator.organizeAnswersAtEnd(contentBody);

            Document document = new Document(PageSize.A4, MARGIN_LEFT, MARGIN_RIGHT, MARGIN_TOP, MARGIN_BOTTOM);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            // Set page event to draw Alpha Tutor header & footer on EVERY page
            byte[] headerImgBytes = loadHeaderImageBytes();
            writer.setPageEvent(new AlphaTutorHeaderFooterEvent(headerImgBytes));

            document.open();

            // Document Title & Subtitle (placed below header, bold and center-aligned)
            Paragraph title = new Paragraph(documentType.toUpperCase(), FONT_HEADER_TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(6);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph subtitle = new Paragraph(requestInfo, FONT_SUBTITLE);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(14);
            document.add(subtitle);

            // Formatted Body Content (no dummy/unrelated starting table)
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

            // Render elements from docx in sequential order (preserving table placement)
            for (IBodyElement elem : docx.getBodyElements()) {
                if (elem.getElementType() == BodyElementType.PARAGRAPH) {
                    renderDocxParagraph(document, (XWPFParagraph) elem);
                } else if (elem.getElementType() == BodyElementType.TABLE) {
                    renderDocxTable(document, (XWPFTable) elem);
                }
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to convert DOCX to PDF: {}", e.getMessage(), e);
            throw new IllegalStateException("DOCX to PDF conversion failed: " + e.getMessage(), e);
        }
    }

    private void renderDocxParagraph(Document document, XWPFParagraph p) throws Exception {
        String text = p.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        // Skip header duplicates and footer duplicates if template was already embedded in text
        String upper = text.toUpperCase();
        if (upper.contains("VIDUSHI KHANNA")
                || upper.contains("9266973332")
                || upper.contains("9266987111")
                || upper.contains("KATWARIA SARAI")
                || upper.contains("FOOD POINT")
                || upper.contains("CLASSES 6TH")
                || upper.equals("ALPHA TUTOR")) {
            return;
        }

        // Skip any leftover dummy student info table text
        if (upper.contains("STUDENT NAME:") && upper.contains("ROLL NO:")) {
            return;
        }

        // Headings: MUST be bold and centre aligned
        if (isHeadingParagraph(p, text, upper)) {
            if (upper.contains("ANSWER KEY") || upper.contains("MARKING SCHEME") || upper.contains("SOLUTIONS")) {
                document.newPage();
            }
            Paragraph pdfPara = buildPdfParagraphFromDocx(p, FONT_SECTION, null);
            pdfPara.setAlignment(Element.ALIGN_CENTER);
            pdfPara.setSpacingBefore(14);
            pdfPara.setSpacingAfter(6);
            document.add(pdfPara);
            return;
        }

        // Check if this is a Question (e.g., "1. ", "1) ", "Q1. ", "Question 1: ")
        if (isQuestionPattern(text)) {
            Paragraph pdfPara = buildPdfParagraphFromDocx(p, FONT_BOLD_BODY, null);
            pdfPara.setSpacingBefore(7);
            pdfPara.setSpacingAfter(3);
            pdfPara.setIndentationLeft(10);
            document.add(pdfPara);
            return;
        }

        // Check if this is a Bullet point (Word native bullet or bullet character in text)
        boolean isNativeBullet = (p.getNumID() != null && !p.getNumID().equals(BigInteger.ZERO))
                || (p.getStyle() != null && p.getStyle().toLowerCase().contains("bullet"));
        boolean isTextBullet = text.matches("^[-*+•▪▫–—]\\s+.*");

        if (isNativeBullet || isTextBullet) {
            String prepend = (!isTextBullet) ? "• " : null;
            Paragraph pdfPara = buildPdfParagraphFromDocx(p, FONT_BODY, prepend);
            pdfPara.setSpacingBefore(2.5f);
            pdfPara.setSpacingAfter(2.5f);

            int ilvl = 0;
            try {
                if (p.getNumIlvl() != null) {
                    ilvl = p.getNumIlvl().intValue();
                }
            } catch (Exception ignored) {}

            float indent = 20f + (ilvl * 14f);
            pdfPara.setIndentationLeft(indent);
            document.add(pdfPara);
            return;
        }

        // Check if this is a Sub-item or Option: (a), (b), (i), (ii), etc.
        if (isSubItemPattern(text)) {
            Paragraph pdfPara = buildPdfParagraphFromDocx(p, FONT_BODY, null);
            pdfPara.setSpacingBefore(2);
            pdfPara.setSpacingAfter(2);
            pdfPara.setIndentationLeft(22);
            document.add(pdfPara);
            return;
        }

        // Regular paragraph: preserve runs, styles, and alignment
        Paragraph pdfPara = buildPdfParagraphFromDocx(p, FONT_BODY, null);
        pdfPara.setSpacingBefore(3.5f);
        pdfPara.setSpacingAfter(3.5f);
        if (p.getAlignment() == ParagraphAlignment.CENTER) {
            pdfPara.setAlignment(Element.ALIGN_CENTER);
        } else if (p.getAlignment() == ParagraphAlignment.RIGHT) {
            pdfPara.setAlignment(Element.ALIGN_RIGHT);
        } else if (p.getAlignment() == ParagraphAlignment.BOTH) {
            pdfPara.setAlignment(Element.ALIGN_JUSTIFIED);
        }
        document.add(pdfPara);
    }

    private Paragraph buildPdfParagraphFromDocx(XWPFParagraph p, Font baseFont, String prependText) {
        Paragraph pdfPara = new Paragraph();
        if (prependText != null && !prependText.isEmpty()) {
            pdfPara.add(new Chunk(prependText, baseFont));
        }

        List<XWPFRun> runs = p.getRuns();
        if (runs == null || runs.isEmpty()) {
            String text = p.getText().trim();
            if (prependText == null && text.matches("^[-*+•▪▫–—]\\s+.*")) {
                text = text.replaceFirst("^[-*+•▪▫–—]\\s+", "• ");
            }
            return buildPdfParagraphFromMarkdown(text, baseFont, null);
        }

        boolean firstRun = true;
        for (XWPFRun r : runs) {
            String rText = r.text();
            if (rText == null || rText.isEmpty()) {
                continue;
            }

            if (firstRun && prependText == null) {
                if (rText.matches("^[-*+•▪▫–—]\\s+.*")) {
                    rText = rText.replaceFirst("^[-*+•▪▫–—]\\s+", "• ");
                }
            }
            firstRun = false;

            int style = Font.NORMAL;
            if (baseFont.getStyle() == Font.BOLD) {
                style = Font.BOLD;
            }
            if (r.isBold() && r.isItalic()) {
                style = Font.BOLDITALIC;
            } else if (r.isBold()) {
                style = Font.BOLD;
            } else if (r.isItalic()) {
                style = (style == Font.BOLD) ? Font.BOLDITALIC : Font.ITALIC;
            }

            float fontSize = baseFont.getSize();
            if (r.getFontSize() > 0 && r.getFontSize() <= 28) {
                fontSize = Math.min(Math.max((float) r.getFontSize(), 8f), 18f);
            }

            Color color = baseFont.getColor();
            if (r.getColor() != null && r.getColor().matches("[0-9A-Fa-f]{6}")) {
                try {
                    color = new Color(Integer.parseInt(r.getColor(), 16));
                } catch (Exception ignored) {}
            }

            Font font = FontFactory.getFont(FontFactory.HELVETICA, fontSize, style, color);
            Chunk chunk = new Chunk(rText, font);
            if (r.getUnderline() != null && r.getUnderline() != UnderlinePatterns.NONE) {
                chunk.setUnderline(0.8f, -1.5f);
            }
            pdfPara.add(chunk);
        }

        return pdfPara;
    }

    private boolean isQuestionPattern(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.matches("^(\\d+[\\.\\)]|(Q|QUE|QUESTION)\\s*\\.?\\s*\\d+[:\\.]?)\\s+.*");
    }

    private boolean isSubItemPattern(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.matches("^([\\(\\[]?[a-zA-Z][\\)\\]\\.]|[\\(\\[]?[ivxIVX]+[\\)\\]\\.])\\s+.*");
    }

    private boolean isHeadingParagraph(XWPFParagraph p, String text, String upper) {
        String style = p.getStyle();
        if (style != null && (style.toLowerCase().contains("heading") || style.toLowerCase().contains("title"))) {
            return true;
        }
        if (text.startsWith("#")
                || upper.startsWith("SECTION")
                || upper.startsWith("PART ")
                || upper.startsWith("CHAPTER")
                || upper.startsWith("GENERAL INSTRUCTION")
                || upper.startsWith("INSTRUCTION")
                || upper.startsWith("ANSWER KEY")
                || upper.startsWith("PRACTICE QUESTION")
                || upper.startsWith("CLASSROOM WORKSHEET")
                || upper.startsWith("EXAMINATION PAPER")
                || upper.startsWith("REVISION NOTES")) {
            return true;
        }
        // Short standalone line (<= 60 chars) where all runs are bold
        if (text.length() <= 60 && !p.getRuns().isEmpty()) {
            boolean allBold = true;
            for (XWPFRun r : p.getRuns()) {
                if (!r.isBold() && !r.text().trim().isEmpty()) {
                    allBold = false;
                    break;
                }
            }
            if (allBold && (p.getAlignment() == ParagraphAlignment.CENTER || text.equals(upper))) {
                return true;
            }
        }
        return false;
    }

    private void renderDocxTable(Document document, XWPFTable table) throws Exception {
        if (table.getRows().isEmpty()) {
            return;
        }

        // Filter out dummy student box table if present
        String tableText = table.getText().toUpperCase();
        if (tableText.contains("STUDENT NAME:") && tableText.contains("ROLL NO:")) {
            return;
        }

        int maxCols = 0;
        for (XWPFTableRow row : table.getRows()) {
            int rowCols = 0;
            for (XWPFTableCell cell : row.getTableCells()) {
                rowCols += getCellColSpan(cell);
            }
            if (rowCols > maxCols) {
                maxCols = rowCols;
            }
        }
        if (maxCols == 0) {
            maxCols = 1;
        }

        PdfPTable pdfTable = new PdfPTable(maxCols);
        pdfTable.setWidthPercentage(100);
        pdfTable.setSpacingBefore(10f);
        pdfTable.setSpacingAfter(12f);

        // Proportional column weights
        float[] colWidths = new float[maxCols];
        for (int c = 0; c < maxCols; c++) {
            colWidths[c] = 8f;
        }
        for (XWPFTableRow row : table.getRows()) {
            int colIdx = 0;
            for (XWPFTableCell cell : row.getTableCells()) {
                int colSpan = getCellColSpan(cell);
                if (colIdx < maxCols && colSpan == 1) {
                    float len = Math.max(cell.getText().trim().length(), 4);
                    colWidths[colIdx] = Math.max(colWidths[colIdx], len);
                }
                colIdx += colSpan;
            }
        }
        for (int c = 0; c < maxCols; c++) {
            colWidths[c] = (float) Math.pow(colWidths[c], 0.60);
        }
        pdfTable.setWidths(colWidths);

        int rowIndex = 0;
        for (XWPFTableRow row : table.getRows()) {
            boolean isHeader = (rowIndex == 0);
            int colsAdded = 0;

            for (XWPFTableCell cell : row.getTableCells()) {
                int colSpan = getCellColSpan(cell);

                Font defaultFont = isHeader
                        ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, COLOR_PRIMARY)
                        : FontFactory.getFont(FontFactory.HELVETICA, 9.0f, COLOR_TEXT);

                PdfPCell pdfCell = new PdfPCell();
                pdfCell.setPadding(6f);
                pdfCell.setBorderColor(new Color(190, 200, 215));
                pdfCell.setBorderWidth(0.8f);

                // Alternating row background colors
                if (isHeader) {
                    pdfCell.setBackgroundColor(new Color(238, 243, 250));
                    pdfCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                } else if (rowIndex % 2 == 1) {
                    pdfCell.setBackgroundColor(new Color(250, 252, 255));
                    pdfCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                } else {
                    pdfCell.setBackgroundColor(Color.WHITE);
                    pdfCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                }

                if (colSpan > 1) {
                    pdfCell.setColspan(colSpan);
                }

                // Render all paragraphs inside the cell to preserve runs, bold, italic, and multi-line content
                List<XWPFParagraph> paras = cell.getParagraphs();
                if (paras == null || paras.isEmpty() || cell.getText().trim().isEmpty()) {
                    pdfCell.addElement(new Paragraph("", defaultFont));
                } else {
                    for (XWPFParagraph cp : paras) {
                        if (cp.getText().trim().isEmpty()) continue;
                        Paragraph p = buildPdfParagraphFromDocx(cp, defaultFont, null);
                        if (isHeader || cp.getAlignment() == ParagraphAlignment.CENTER) {
                            p.setAlignment(Element.ALIGN_CENTER);
                        }
                        pdfCell.addElement(p);
                    }
                }

                pdfTable.addCell(pdfCell);
                colsAdded += colSpan;
            }

            // Fill missing columns
            while (colsAdded < maxCols) {
                PdfPCell empty = new PdfPCell(new Phrase(""));
                empty.setPadding(6f);
                empty.setBorderColor(new Color(190, 200, 215));
                empty.setBorderWidth(0.8f);
                pdfTable.addCell(empty);
                colsAdded++;
            }

            rowIndex++;
        }

        document.add(pdfTable);
    }

    private int getCellColSpan(XWPFTableCell cell) {
        try {
            if (cell.getCTTc() != null && cell.getCTTc().getTcPr() != null && cell.getCTTc().getTcPr().getGridSpan() != null) {
                var val = cell.getCTTc().getTcPr().getGridSpan().getVal();
                if (val != null) {
                    return val.intValue();
                }
            }
        } catch (Exception ignored) {
        }
        return 1;
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

    private void addBodyContent(Document document, String content) throws Exception {
        if (content == null || content.isBlank()) {
            return;
        }

        String[] lines = content.split("\n");
        List<String> tableBuffer = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String rawLine = lines[i];
            String line = rawLine.trim();

            // Detect Markdown Table lines
            if (line.startsWith("|") && line.endsWith("|") && line.length() > 2) {
                tableBuffer.add(line);
                boolean isLastLine = (i == lines.length - 1);
                String nextLine = isLastLine ? "" : lines[i + 1].trim();
                if (isLastLine || !nextLine.startsWith("|") || !nextLine.endsWith("|")) {
                    renderMarkdownTable(document, tableBuffer);
                    tableBuffer.clear();
                }
                continue;
            } else if (!tableBuffer.isEmpty()) {
                renderMarkdownTable(document, tableBuffer);
                tableBuffer.clear();
            }

            if (line.isEmpty()) {
                Paragraph blank = new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4f));
                blank.setSpacingAfter(4f);
                document.add(blank);
                continue;
            }

            String upper = line.toUpperCase();

            // Heading 1 / Section - Bold & Center-Aligned
            if (line.startsWith("# ")
                    || upper.startsWith("SECTION")
                    || upper.startsWith("PART ")
                    || upper.startsWith("CHAPTER")
                    || upper.startsWith("GENERAL INSTRUCTION")
                    || upper.startsWith("INSTRUCTION")
                    || upper.startsWith("ANSWER KEY")
                    || upper.startsWith("PRACTICE QUESTION")) {
                if (upper.contains("ANSWER KEY") || upper.contains("MARKING SCHEME") || upper.contains("SOLUTIONS")) {
                    document.newPage();
                }
                String cleanH = cleanMarkdownHeading(line);
                Paragraph p = new Paragraph(cleanH, FONT_SECTION);
                p.setAlignment(Element.ALIGN_CENTER);
                p.setSpacingBefore(14);
                p.setSpacingAfter(6);
                document.add(p);
            }
            // Heading 2 / Sub-section - Bold & Center-Aligned
            else if (line.startsWith("## ") || line.startsWith("### ")) {
                String cleanH = cleanMarkdownHeading(line);
                Paragraph p = new Paragraph(cleanH, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, COLOR_PRIMARY));
                p.setAlignment(Element.ALIGN_CENTER);
                p.setSpacingBefore(10);
                p.setSpacingAfter(4);
                document.add(p);
            }
            // Numbered question or list item
            else if (isQuestionPattern(line)) {
                Paragraph p = buildPdfParagraphFromMarkdown(line, FONT_BOLD_BODY, null);
                p.setSpacingBefore(7);
                p.setSpacingAfter(3);
                p.setIndentationLeft(10);
                document.add(p);
            }
            // Bullets with hierarchical indentation support
            else if (line.matches("^[-*+•▪▫–—]\\s+.*") || rawLine.matches("^\\s+[-*+•▪▫–—]\\s+.*")) {
                int spaces = 0;
                while (spaces < rawLine.length() && Character.isWhitespace(rawLine.charAt(spaces))) {
                    spaces++;
                }
                float indent = 18f + (spaces / 2) * 10f;
                String strippedBullet = line.replaceFirst("^[-*+•▪▫–—]\\s+", "");
                Paragraph p = buildPdfParagraphFromMarkdown(strippedBullet, FONT_BODY, "• ");
                p.setSpacingBefore(2.5f);
                p.setSpacingAfter(2.5f);
                p.setIndentationLeft(indent);
                document.add(p);
            }
            // Options (A), (B), (i), (ii)
            else if (isSubItemPattern(line)) {
                Paragraph p = buildPdfParagraphFromMarkdown(line, FONT_BODY, null);
                p.setSpacingBefore(2);
                p.setSpacingAfter(2);
                p.setIndentationLeft(22);
                document.add(p);
            }
            // Regular text
            else {
                Paragraph p = buildPdfParagraphFromMarkdown(line, FONT_BODY, null);
                p.setSpacingBefore(3.5f);
                p.setSpacingAfter(3.5f);
                document.add(p);
            }
        }

        if (!tableBuffer.isEmpty()) {
            renderMarkdownTable(document, tableBuffer);
            tableBuffer.clear();
        }
    }

    private void renderMarkdownTable(Document document, List<String> tableLines) throws Exception {
        if (tableLines.isEmpty()) {
            return;
        }

        List<String[]> parsedRows = new ArrayList<>();
        for (String raw : tableLines) {
            String stripped = raw.replaceAll("^\\|", "").replaceAll("\\|$", "");
            // Skip markdown delimiter line like |---|---|
            if (stripped.matches("^[\\s\\-:\\|]+$")) {
                continue;
            }
            String[] cols = stripped.split("\\|", -1);
            for (int c = 0; c < cols.length; c++) {
                cols[c] = cols[c].trim();
            }
            parsedRows.add(cols);
        }

        if (parsedRows.isEmpty()) {
            return;
        }

        int maxCols = 0;
        for (String[] r : parsedRows) {
            if (r.length > maxCols) {
                maxCols = r.length;
            }
        }
        if (maxCols == 0) {
            maxCols = 1;
        }

        PdfPTable pdfTable = new PdfPTable(maxCols);
        pdfTable.setWidthPercentage(100);
        pdfTable.setSpacingBefore(10f);
        pdfTable.setSpacingAfter(12f);

        // Calculate proportional column weights
        float[] colWidths = new float[maxCols];
        for (int c = 0; c < maxCols; c++) {
            colWidths[c] = 8f;
        }
        for (String[] rowData : parsedRows) {
            for (int c = 0; c < maxCols; c++) {
                if (c < rowData.length) {
                    float len = Math.max(cleanMarkdown(rowData[c]).length(), 4);
                    colWidths[c] = Math.max(colWidths[c], len);
                }
            }
        }
        for (int c = 0; c < maxCols; c++) {
            colWidths[c] = (float) Math.pow(colWidths[c], 0.60);
        }
        pdfTable.setWidths(colWidths);

        for (int r = 0; r < parsedRows.size(); r++) {
            boolean isHeader = (r == 0);
            String[] rowData = parsedRows.get(r);
            Font cellFont = isHeader
                    ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, COLOR_PRIMARY)
                    : FontFactory.getFont(FontFactory.HELVETICA, 9.0f, COLOR_TEXT);

            for (int c = 0; c < maxCols; c++) {
                String val = (c < rowData.length) ? rowData[c] : "";
                Paragraph p = buildPdfParagraphFromMarkdown(val, cellFont, null);
                if (isHeader) {
                    p.setAlignment(Element.ALIGN_CENTER);
                }

                PdfPCell pdfCell = new PdfPCell(p);
                pdfCell.setPadding(6f);
                pdfCell.setBorderColor(new Color(190, 200, 215));
                pdfCell.setBorderWidth(0.8f);

                if (isHeader) {
                    pdfCell.setBackgroundColor(new Color(238, 243, 250));
                    pdfCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                } else if (r % 2 == 1) {
                    pdfCell.setBackgroundColor(new Color(250, 252, 255));
                    pdfCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                } else {
                    pdfCell.setBackgroundColor(Color.WHITE);
                    pdfCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                }

                pdfTable.addCell(pdfCell);
            }
        }

        document.add(pdfTable);
    }

    private Paragraph buildPdfParagraphFromMarkdown(String text, Font defaultFont, String prependText) {
        Paragraph paragraph = new Paragraph();
        if (prependText != null && !prependText.isEmpty()) {
            paragraph.add(new Chunk(prependText, defaultFont));
        }

        if (text == null || text.isEmpty()) {
            return paragraph;
        }

        // Tokenize markdown bold-italic (***), bold (**), italic (* or _), and code (`)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(\\*\\*\\*(.+?)\\*\\*\\*)|(\\*\\*(.+?)\\*\\*)|(\\*([^*]+?)\\*)|(_([^_]+?)_)|(`([^`]+?)`)"
        );

        java.util.regex.Matcher matcher = pattern.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String plain = text.substring(lastEnd, matcher.start());
                paragraph.add(new Chunk(plain, defaultFont));
            }

            if (matcher.group(1) != null) { // ***bold italic***
                Font biFont = FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, defaultFont.getSize(), defaultFont.getColor());
                paragraph.add(new Chunk(matcher.group(2), biFont));
            } else if (matcher.group(3) != null) { // **bold**
                Font bFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, defaultFont.getSize(), defaultFont.getColor());
                paragraph.add(new Chunk(matcher.group(4), bFont));
            } else if (matcher.group(5) != null) { // *italic*
                Font iFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, defaultFont.getSize(), defaultFont.getColor());
                paragraph.add(new Chunk(matcher.group(6), iFont));
            } else if (matcher.group(7) != null) { // _italic_
                Font iFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, defaultFont.getSize(), defaultFont.getColor());
                paragraph.add(new Chunk(matcher.group(8), iFont));
            } else if (matcher.group(9) != null) { // `code`
                Font cFont = FontFactory.getFont(FontFactory.COURIER, defaultFont.getSize() * 0.95f, defaultFont.getColor());
                paragraph.add(new Chunk(matcher.group(10), cFont));
            }

            lastEnd = matcher.end();
        }

        if (lastEnd < text.length()) {
            paragraph.add(new Chunk(text.substring(lastEnd), defaultFont));
        }

        return paragraph;
    }

    private String cleanMarkdownHeading(String text) {
        return text.replaceAll("^#{1,6}\\s+", "").replaceAll("\\*\\*", "").trim();
    }

    private String cleanMarkdown(String text) {
        return text
                .replaceAll("^#{1,6}\\s+", "")
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
                .replaceAll("\\*(.*?)\\*", "$1")
                .trim();
    }

    /**
     * Alpha Tutor Page Event: Renders the Alpha Tutor header banner with golden accent line
     * at the top, and the authentic green/red contact footer with golden separator line
     * at the bottom of EVERY PAGE.
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
            float headerBottomY = pageHeight - 14f;

            if (headerImageBytes != null && headerImageBytes.length > 0) {
                try {
                    Image headerImg = Image.getInstance(headerImageBytes);
                    // Aspect ratio 3574 / 506 = ~7.06
                    float imgHeight = contentWidth / (3574f / 506f);
                    headerImg.scaleAbsolute(contentWidth, imgHeight);
                    headerBottomY = pageHeight - 14f - imgHeight;
                    headerImg.setAbsolutePosition(MARGIN_LEFT, headerBottomY);
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
                headerBottomY = pageHeight - 16f - 24f;
            }

            // Golden accent line below Alpha Tutor header (matching template #F4B300, 1.6pt)
            float headerLineY = headerBottomY - 4f;
            cb.setColorStroke(COLOR_GOLD);
            cb.setLineWidth(1.6f);
            cb.moveTo(MARGIN_LEFT, headerLineY);
            cb.lineTo(pageWidth - MARGIN_RIGHT, headerLineY);
            cb.stroke();

            // =================================================================
            // 2. FOOTER ON EVERY PAGE
            // =================================================================
            // Golden divider rule above footer (matching template #F4B300, 1.6pt)
            float footerLineY = 54f;
            cb.setColorStroke(COLOR_GOLD);
            cb.setLineWidth(1.6f);
            cb.moveTo(MARGIN_LEFT, footerLineY);
            cb.lineTo(pageWidth - MARGIN_RIGHT, footerLineY);
            cb.stroke();

            // Authentic Alpha Tutor footer: centered, matching template fonts and colors
            try {
                PdfPTable footerTable = new PdfPTable(1);
                footerTable.setTotalWidth(contentWidth);
                footerTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
                footerTable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);

                // Line 1: Classes & Teacher in Forest Green (#075D2A), Bold, 8pt, Centered
                Font greenFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.0f, COLOR_FOOTER_GREEN);
                Paragraph line1Para = new Paragraph("CLASSES 6th–10th | 11th–12th MATHS • VIDUSHI KHANNA", greenFont);
                line1Para.setAlignment(Element.ALIGN_CENTER);
                line1Para.setLeading(10.5f);
                PdfPCell cell1 = new PdfPCell(line1Para);
                cell1.setBorder(Rectangle.NO_BORDER);
                cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell1.setPaddingTop(3f);
                cell1.setPaddingBottom(1f);
                footerTable.addCell(cell1);

                // Line 2: Phone & Address in Crimson Red (#D6181F), Regular, 7.5pt, Centered
                Font redFont = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, COLOR_FOOTER_RED);
                Paragraph line2Para = new Paragraph("9266973332 / 9266987111 • B-54, LIG Flats, Phase-1, Katwaria Sarai, Near Food Point", redFont);
                line2Para.setAlignment(Element.ALIGN_CENTER);
                line2Para.setLeading(10.0f);
                PdfPCell cell2 = new PdfPCell(line2Para);
                cell2.setBorder(Rectangle.NO_BORDER);
                cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell2.setPaddingTop(1f);
                cell2.setPaddingBottom(0f);
                footerTable.addCell(cell2);

                footerTable.writeSelectedRows(0, -1, MARGIN_LEFT, footerLineY - 2f, cb);

                // Page number placed right-aligned in bottom margin
                Font pageFont = FontFactory.getFont(FontFactory.HELVETICA, 7.0f, COLOR_MUTED);
                Phrase pagePhrase = new Phrase("Page " + writer.getPageNumber(), pageFont);
                ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, pagePhrase, pageWidth - MARGIN_RIGHT, 12f, 0);
            } catch (Exception e) {
                log.warn("Could not render footer table on page {}: {}", writer.getPageNumber(), e.getMessage());
            }
        }
    }
}
