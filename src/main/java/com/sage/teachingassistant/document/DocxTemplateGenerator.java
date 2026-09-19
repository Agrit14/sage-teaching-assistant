package com.sage.teachingassistant.document;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableWidthType;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Populates educational content into the branded Alpha Tutor Word template.
 */
@Component
public class DocxTemplateGenerator {

    private static final Logger log = LoggerFactory.getLogger(DocxTemplateGenerator.class);
    private static final String TEMPLATE_PATH = "/template/Alpha_Tutor_A4_Worksheet_Template.docx";
    private static final String FALLBACK_DISK_PATH = "src/main/resources/template/Alpha_Tutor_A4_Worksheet_Template.docx";

    private static final String COLOR_NAVY = "1B365D";
    private static final String COLOR_DARK_GRAY = "333333";
    private static final String FONT_FAMILY = "Calibri";

    /**
     * Builds a complete styled Word document starting from the Alpha Tutor template.
     *
     * @param documentType "WORKSHEET", "TEST PAPER", or "REVISION NOTES"
     * @param requestInfo  original user prompt / class & topic info
     * @param contentBody  the generated questions, sections, or notes text
     * @return byte array of the .docx file
     */
    public byte[] generateDocument(String documentType, String requestInfo, String contentBody) throws IOException {
        try (XWPFDocument doc = loadBaseTemplate();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Add separator spacing after template header
            XWPFParagraph spacing = doc.createParagraph();
            spacing.setSpacingBefore(100);

            // Add Title
            addDocumentTitle(doc, documentType.toUpperCase(), requestInfo);

            // Append the formatted body content
            appendFormattedContent(doc, contentBody);

            doc.write(out);
            return out.toByteArray();
        }
    }

    private XWPFDocument loadBaseTemplate() {
        // Try classpath first
        try (InputStream is = getClass().getResourceAsStream(TEMPLATE_PATH)) {
            if (is != null) {
                return new XWPFDocument(is);
            }
        } catch (Exception e) {
            log.warn("Could not load template from classpath {}: {}", TEMPLATE_PATH, e.getMessage());
        }

        // Try local disk fallback
        Path diskPath = Paths.get(FALLBACK_DISK_PATH);
        if (Files.exists(diskPath)) {
            try (InputStream is = Files.newInputStream(diskPath)) {
                return new XWPFDocument(is);
            } catch (Exception e) {
                log.warn("Could not load template from disk {}: {}", FALLBACK_DISK_PATH, e.getMessage());
            }
        }

        log.warn("Alpha Tutor template not found, creating blank document.");
        return new XWPFDocument();
    }

    private void addDocumentTitle(XWPFDocument doc, String docType, String requestInfo) {
        XWPFParagraph titlePara = doc.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        titlePara.setSpacingBefore(120);
        titlePara.setSpacingAfter(80);

        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText(docType);
        titleRun.setBold(true);
        titleRun.setFontSize(16);
        titleRun.setFontFamily(FONT_FAMILY);
        titleRun.setColor(COLOR_NAVY);
        titleRun.setUnderline(UnderlinePatterns.SINGLE);

        XWPFParagraph subPara = doc.createParagraph();
        subPara.setAlignment(ParagraphAlignment.CENTER);
        subPara.setSpacingAfter(140);

        XWPFRun subRun = subPara.createRun();
        subRun.setText(requestInfo);
        subRun.setBold(true);
        subRun.setFontSize(12);
        subRun.setFontFamily(FONT_FAMILY);
        subRun.setColor(COLOR_DARK_GRAY);
    }

    private void appendFormattedContent(XWPFDocument doc, String content) {
        if (content == null || content.isBlank()) {
            return;
        }

        String[] lines = content.split("\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            XWPFParagraph para = doc.createParagraph();

            // Heading 1 / Section - Bold & Center-Aligned
            if (line.startsWith("# ") || line.toUpperCase().startsWith("SECTION") || line.toUpperCase().startsWith("PART ") || line.toUpperCase().startsWith("CHAPTER")) {
                para.setAlignment(ParagraphAlignment.CENTER);
                para.setSpacingBefore(160);
                para.setSpacingAfter(60);
                XWPFRun run = para.createRun();
                run.setText(cleanMarkdown(line));
                run.setBold(true);
                run.setFontSize(13);
                run.setFontFamily(FONT_FAMILY);
                run.setColor(COLOR_NAVY);
            }
            // Heading 2 / Sub-section - Bold & Center-Aligned
            else if (line.startsWith("## ") || line.startsWith("### ")) {
                para.setAlignment(ParagraphAlignment.CENTER);
                para.setSpacingBefore(120);
                para.setSpacingAfter(40);
                XWPFRun run = para.createRun();
                run.setText(cleanMarkdown(line));
                run.setBold(true);
                run.setFontSize(11);
                run.setFontFamily(FONT_FAMILY);
                run.setColor(COLOR_NAVY);
            }
            // Numbered question or bullet
            else if (line.matches("^\\d+[\\.\\)]\\s+.*") || line.startsWith("- ") || line.startsWith("* ")) {
                para.setSpacingBefore(60);
                para.setSpacingAfter(40);
                para.setIndentationLeft(240);
                XWPFRun run = para.createRun();
                run.setText(cleanMarkdown(line));
                run.setFontSize(11);
                run.setFontFamily(FONT_FAMILY);
                run.setColor(COLOR_DARK_GRAY);
            }
            // Question Options (A), (B), etc.
            else if (line.matches("^[\\(\\[]?[A-Da-d][\\)\\]\\.]\\s+.*")) {
                para.setSpacingBefore(20);
                para.setSpacingAfter(20);
                para.setIndentationLeft(480);
                XWPFRun run = para.createRun();
                run.setText(cleanMarkdown(line));
                run.setFontSize(10);
                run.setFontFamily(FONT_FAMILY);
                run.setColor(COLOR_DARK_GRAY);
            }
            // Regular text / Instructions
            else {
                para.setSpacingBefore(40);
                para.setSpacingAfter(40);
                XWPFRun run = para.createRun();
                run.setText(cleanMarkdown(line));
                run.setFontSize(10);
                run.setFontFamily(FONT_FAMILY);
                run.setColor(COLOR_DARK_GRAY);
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
}
