package com.sage.teachingassistant.document;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableRowAlign;
import org.apache.poi.xwpf.usermodel.TableWidthType;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds clean, professional Word (.docx) documents with structured headings,
 * questions, tables, and typography, starting immediately on page 1 without template overhead.
 */
@Component
public class DocxTemplateGenerator {

    private static final Logger log = LoggerFactory.getLogger(DocxTemplateGenerator.class);

    private static final String COLOR_NAVY = "1B365D";
    private static final String COLOR_DARK_GRAY = "333333";
    private static final String FONT_FAMILY = "Calibri";

    /**
     * Builds a complete styled Word document starting directly from page 1.
     *
     * @param documentType "WORKSHEET", "TEST PAPER", or "REVISION NOTES"
     * @param requestInfo  original user prompt / class & topic info
     * @param contentBody  the generated questions, sections, or notes text
     * @return byte array of the .docx file
     */
    public byte[] generateDocument(String documentType, String requestInfo, String contentBody) throws IOException {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Set standard 1-inch margins (1440 dxa/twips)
            setDocumentMargins(doc);

            // Add Title and Subtitle directly at the top of the first page
            addDocumentTitle(doc, documentType.toUpperCase(), requestInfo);

            // Organize answers to the end of the document if present inline after questions
            String organizedContent = organizeAnswersAtEnd(contentBody);

            // Append the formatted body content
            appendFormattedContent(doc, organizedContent);

            doc.write(out);
            return out.toByteArray();
        }
    }

    private void setDocumentMargins(XWPFDocument doc) {
        try {
            CTBody body = doc.getDocument().getBody();
            CTSectPr sectPr = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
            CTPageMar pageMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
            pageMar.setTop(BigInteger.valueOf(1440));
            pageMar.setBottom(BigInteger.valueOf(1440));
            pageMar.setLeft(BigInteger.valueOf(1440));
            pageMar.setRight(BigInteger.valueOf(1440));
        } catch (Exception e) {
            log.warn("Could not set custom margins on document: {}", e.getMessage());
        }
    }

    private void addDocumentTitle(XWPFDocument doc, String docType, String requestInfo) {
        XWPFParagraph titlePara = doc.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        titlePara.setSpacingBefore(60);
        titlePara.setSpacingAfter(40);

        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText(cleanMarkdown(docType));
        titleRun.setBold(true);
        titleRun.setFontSize(16);
        titleRun.setFontFamily(FONT_FAMILY);
        titleRun.setColor(COLOR_NAVY);
        titleRun.setUnderline(UnderlinePatterns.SINGLE);

        if (requestInfo != null && !requestInfo.isBlank()) {
            XWPFParagraph subPara = doc.createParagraph();
            subPara.setAlignment(ParagraphAlignment.CENTER);
            subPara.setSpacingBefore(20);
            subPara.setSpacingAfter(120);

            XWPFRun subRun = subPara.createRun();
            subRun.setText(cleanMarkdown(requestInfo));
            subRun.setBold(true);
            subRun.setFontSize(12);
            subRun.setFontFamily(FONT_FAMILY);
            subRun.setColor(COLOR_DARK_GRAY);
        }
    }

    private void appendFormattedContent(XWPFDocument doc, String content) {
        if (content == null || content.isBlank()) {
            return;
        }

        String[] lines = content.split("\n");
        List<String> tableBuffer = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.startsWith("|") && line.endsWith("|")) {
                tableBuffer.add(line);
                continue;
            } else if (!tableBuffer.isEmpty()) {
                appendMarkdownTable(doc, tableBuffer);
                tableBuffer.clear();
            }

            if (line.isEmpty()) {
                continue;
            }

            XWPFParagraph para = doc.createParagraph();
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
                
                // If this is an Answer Key / Solutions section at the end, start on a fresh page
                if (upper.contains("ANSWER KEY") || upper.contains("MARKING SCHEME") || upper.contains("SOLUTIONS")) {
                    para.setPageBreak(true);
                }

                para.setAlignment(ParagraphAlignment.CENTER);
                para.setSpacingBefore(140);
                para.setSpacingAfter(50);
                appendFormattedRuns(para, cleanMarkdownHeading(line), 13, COLOR_NAVY, true);
            }
            // Heading 2 / Sub-section - Bold & Center-Aligned
            else if (line.startsWith("## ") || line.startsWith("### ")) {
                para.setAlignment(ParagraphAlignment.CENTER);
                para.setSpacingBefore(100);
                para.setSpacingAfter(30);
                appendFormattedRuns(para, cleanMarkdownHeading(line), 11, COLOR_NAVY, true);
            }
            // Numbered question
            else if (line.matches("^\\d+[\\.\\)]\\s+.*")) {
                para.setSpacingBefore(50);
                para.setSpacingAfter(30);
                para.setIndentationLeft(240);
                appendFormattedRuns(para, line, 11, COLOR_DARK_GRAY, false);
            }
            // Question Options (A), (B), etc. or bullets
            else if (line.matches("^[\\(\\[]?[A-Da-d][\\)\\]\\.]\\s+.*") || line.startsWith("- ") || line.startsWith("* ")) {
                para.setSpacingBefore(20);
                para.setSpacingAfter(20);
                para.setIndentationLeft(480);
                String bulletLine = line.replaceFirst("^[-*]\\s+", "• ");
                appendFormattedRuns(para, bulletLine, 10, COLOR_DARK_GRAY, false);
            }
            // Regular text / Instructions
            else {
                para.setSpacingBefore(30);
                para.setSpacingAfter(30);
                appendFormattedRuns(para, line, 10, COLOR_DARK_GRAY, false);
            }
        }

        if (!tableBuffer.isEmpty()) {
            appendMarkdownTable(doc, tableBuffer);
            tableBuffer.clear();
        }
    }

    private void appendFormattedRuns(XWPFParagraph para, String text, int fontSize, String color, boolean forceBold) {
        if (text == null || text.isEmpty()) {
            return;
        }

        // Split text by markdown bold delimiter **
        String[] parts = text.split("(?<=\\*\\*)|(?=\\*\\*)");
        boolean isBold = forceBold;

        for (String part : parts) {
            if ("**".equals(part)) {
                if (!forceBold) {
                    isBold = !isBold;
                }
                continue;
            }
            if (part.isEmpty()) {
                continue;
            }
            XWPFRun run = para.createRun();
            run.setText(part);
            run.setBold(isBold);
            run.setFontSize(fontSize);
            run.setFontFamily(FONT_FAMILY);
            run.setColor(color);
        }
    }

    private void appendMarkdownTable(XWPFDocument doc, List<String> tableLines) {
        List<List<String>> rows = new ArrayList<>();
        for (String raw : tableLines) {
            String line = raw.trim();
            if (line.startsWith("|")) {
                line = line.substring(1);
            }
            if (line.endsWith("|")) {
                line = line.substring(0, line.length() - 1);
            }
            // Skip separator row |---|---|
            if (line.replace("-", "").replace("|", "").replace(":", "").trim().isEmpty()) {
                continue;
            }
            String[] rawCells = line.split("\\|", -1);
            List<String> cells = new ArrayList<>();
            for (String c : rawCells) {
                cells.add(cleanMarkdown(c));
            }
            rows.add(cells);
        }

        if (rows.isEmpty()) {
            return;
        }

        int maxCols = 0;
        for (List<String> r : rows) {
            if (r.size() > maxCols) {
                maxCols = r.size();
            }
        }
        if (maxCols == 0) {
            return;
        }

        XWPFTable table = doc.createTable(rows.size(), maxCols);
        table.setWidthType(TableWidthType.PCT);
        table.setWidth("100%");
        table.setTableAlignment(TableRowAlign.CENTER);

        for (int r = 0; r < rows.size(); r++) {
            List<String> rowData = rows.get(r);
            XWPFTableRow tableRow = table.getRow(r);
            boolean isHeader = (r == 0);

            for (int c = 0; c < maxCols; c++) {
                XWPFTableCell cell = tableRow.getCell(c);
                if (cell == null) {
                    cell = tableRow.createCell();
                }
                String cellText = c < rowData.size() ? rowData.get(c) : "";

                if (isHeader) {
                    cell.setColor("EEF3FA");
                }

                XWPFParagraph cellPara = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
                cellPara.setSpacingBefore(40);
                cellPara.setSpacingAfter(40);
                if (isHeader) {
                    cellPara.setAlignment(ParagraphAlignment.CENTER);
                }

                XWPFRun cellRun = cellPara.createRun();
                cellRun.setText(cellText);
                cellRun.setBold(isHeader);
                cellRun.setFontSize(isHeader ? 10 : 9);
                cellRun.setFontFamily(FONT_FAMILY);
                cellRun.setColor(isHeader ? COLOR_NAVY : COLOR_DARK_GRAY);
            }
        }

        // Spacing after table
        XWPFParagraph postTablePara = doc.createParagraph();
        postTablePara.setSpacingBefore(30);
        postTablePara.setSpacingAfter(30);
    }

    private String cleanMarkdownHeading(String text) {
        return text.replaceAll("^#{1,6}\\s+", "").trim();
    }

    private String cleanMarkdown(String text) {
        return text
                .replaceAll("^#{1,6}\\s+", "")
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
                .replaceAll("\\*(.*?)\\*", "$1")
                .trim();
    }

    /**
     * Ensures answers are positioned at the end of the document under a dedicated Answer Key section.
     * If answers are placed inline directly below questions, they are extracted and moved to the end.
     */
    public static String organizeAnswersAtEnd(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }

        String[] lines = content.split("\n");
        StringBuilder questionsBody = new StringBuilder();
        List<String> extractedAnswers = new ArrayList<>();
        boolean inAnswerKeySection = false;
        String currentQuestionNum = null;

        for (String rawLine : lines) {
            String line = rawLine.trim();

            String upper = line.toUpperCase();
            if (upper.contains("ANSWER KEY") || upper.contains("MARKING SCHEME") || (upper.contains("SECTION") && upper.contains("SOLUTION"))) {
                inAnswerKeySection = true;
            }

            if (inAnswerKeySection) {
                questionsBody.append(rawLine).append("\n");
                continue;
            }

            // Track current question number e.g. "1.", "2)", "Q1."
            java.util.regex.Matcher qMatcher = java.util.regex.Pattern.compile("^(\\d+[\\.\\)]|Q\\d+[\\.:]?)\\s*").matcher(line);
            if (qMatcher.find()) {
                currentQuestionNum = qMatcher.group(1).replaceAll("[^0-9]", "");
            }

            if (isInlineAnswerLine(line)) {
                String cleanAns = line.replaceAll("\\*\\*", "").trim();
                if (currentQuestionNum != null && !cleanAns.matches("^\\d+[\\.\\)].*")) {
                    extractedAnswers.add(currentQuestionNum + ". " + cleanAns);
                } else {
                    extractedAnswers.add(cleanAns);
                }
            } else {
                questionsBody.append(rawLine).append("\n");
            }
        }

        if (!extractedAnswers.isEmpty()) {
            if (!inAnswerKeySection) {
                questionsBody.append("\n\n# ANSWER KEY & SOLUTIONS\n");
            }
            for (String ans : extractedAnswers) {
                questionsBody.append(ans).append("\n");
            }
        }

        return questionsBody.toString().trim();
    }

    private static boolean isInlineAnswerLine(String line) {
        if (line.isEmpty()) {
            return false;
        }
        String clean = line.replaceAll("\\*\\*", "").trim().toLowerCase();
        return clean.startsWith("answer:")
                || clean.startsWith("answer -")
                || clean.startsWith("answer –")
                || clean.startsWith("ans:")
                || clean.startsWith("ans -")
                || clean.startsWith("ans.")
                || clean.startsWith("correct option:")
                || clean.startsWith("correct answer:")
                || clean.startsWith("solution:")
                || clean.startsWith("explanation:");
    }
}
