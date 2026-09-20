package com.sage.teachingassistant.document;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentGenerationTest {

    @Test
    void testDocxWorksheetGeneration() throws Exception {
        DocxTemplateGenerator generator = new DocxTemplateGenerator();

        String body = """
                # SECTION A: Multiple Choice Questions
                1. What is the powerhouse of the cell?
                   (A) Nucleus
                   (B) Mitochondria
                   (C) Ribosome
                   (D) Chloroplast

                # SECTION B: Short Answer
                2. State Ohm's Law and write its formula. [2 Marks]
                """;

        byte[] docxBytes = generator.generateDocument(
                "CLASSROOM WORKSHEET",
                "Class 9 Science - Cell & Electricity",
                body);

        assertThat(docxBytes).isNotEmpty();

        // Verify valid XWPF document
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            assertThat(doc.getParagraphs()).isNotEmpty();
            // Verify title is the very first paragraph (starts directly on page 1)
            assertThat(doc.getParagraphs().get(0).getText()).isEqualTo("CLASSROOM WORKSHEET");
            assertThat(doc.getParagraphs().get(1).getText()).isEqualTo("Class 9 Science - Cell & Electricity");
            // Verify no template artifacts
            for (var p : doc.getParagraphs()) {
                assertThat(p.getText()).doesNotContain("VIDUSHI KHANNA");
            }
        }
    }

    @Test
    void testDocxTableGeneration() throws Exception {
        DocxTemplateGenerator generator = new DocxTemplateGenerator();

        String body = """
                # SECTION A: Study Table
                | Quantity | Unit | Symbol |
                |---|---|---|
                | Force | Newton | N |
                | Energy | Joule | J |

                1. What is the unit of Force?
                """;

        byte[] docxBytes = generator.generateDocument("STUDY SHEET", "Class 10 Physics", body);
        assertThat(docxBytes).isNotEmpty();

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            assertThat(doc.getTables()).hasSize(1);
            var table = doc.getTables().get(0);
            assertThat(table.getNumberOfRows()).isEqualTo(3);
            assertThat(table.getRow(0).getCell(0).getText()).isEqualTo("Quantity");
            assertThat(table.getRow(1).getCell(0).getText()).isEqualTo("Force");
            assertThat(table.getRow(1).getCell(1).getText()).isEqualTo("Newton");
            assertThat(table.getRow(1).getCell(2).getText()).isEqualTo("N");
        }
    }

    @Test
    void testAnswersOrganizedAtEndOfDocument() throws Exception {
        DocxTemplateGenerator generator = new DocxTemplateGenerator();

        String bodyWithInlineAnswers = """
                # SECTION A: Multiple Choice Questions
                1. What is the powerhouse of the cell?
                   (A) Nucleus
                   (B) Mitochondria
                   (C) Ribosome
                   (D) Chloroplast
                   Answer: (B) Mitochondria

                # SECTION B: Conceptual Questions
                2. State Ohm's Law and write its formula. [2 Marks]
                   **Ans:** V = I * R, where V is voltage, I is current, and R is resistance.
                """;

        byte[] docxBytes = generator.generateDocument(
                "CLASSROOM WORKSHEET",
                "Class 9 Science",
                bodyWithInlineAnswers);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            var paragraphs = doc.getParagraphs();
            
            // Collect all paragraph texts
            var texts = paragraphs.stream().map(org.apache.poi.xwpf.usermodel.XWPFParagraph::getText).toList();

            // Verify that immediately following question 1 options, Answer is NOT present
            int q1Index = -1;
            int answerKeyIndex = -1;
            for (int i = 0; i < texts.size(); i++) {
                if (texts.get(i).contains("What is the powerhouse of the cell?")) {
                    q1Index = i;
                }
                if (texts.get(i).contains("ANSWER KEY & SOLUTIONS")) {
                    answerKeyIndex = i;
                }
            }

            assertThat(q1Index).isGreaterThanOrEqualTo(0);
            assertThat(answerKeyIndex).isGreaterThan(q1Index);

            // Verify paragraph immediately following option (D) is not the answer
            assertThat(texts.get(q1Index + 4)).contains("(D) Chloroplast");
            assertThat(texts.get(q1Index + 5)).doesNotContain("Answer: (B) Mitochondria");

            // Verify Answer Key is at the end
            assertThat(texts.get(answerKeyIndex)).contains("ANSWER KEY & SOLUTIONS");
            assertThat(texts.subList(answerKeyIndex, texts.size()).toString()).contains("1. Answer: (B) Mitochondria");
            assertThat(texts.subList(answerKeyIndex, texts.size()).toString()).contains("2. Ans: V = I * R");

            // Verify page break was set on the Answer Key header
            assertThat(paragraphs.get(answerKeyIndex).isPageBreak()).isTrue();
        }
    }

    @Test
    void testPdfExportAndDocxConversion() throws Exception {
        DocxTemplateGenerator generator = new DocxTemplateGenerator();
        PdfExportService pdfService = new PdfExportService();

        String body = """
                # SECTION A: Objective
                1. Force is defined as mass times acceleration. [1 Mark]
                """;

        // Test direct PDF export
        byte[] directPdf = pdfService.exportPdf("CLASSROOM WORKSHEET", "Class 9 Physics", body);
        assertThat(directPdf).isNotEmpty();
        String pdfHeader = new String(directPdf, 0, 5);
        assertThat(pdfHeader).isEqualTo("%PDF-");

        // Test DOCX to PDF conversion
        byte[] docxBytes = generator.generateDocument("CLASSROOM WORKSHEET", "Class 9 Physics", body);
        byte[] convertedPdf = pdfService.convertDocxToPdf(docxBytes);
        assertThat(convertedPdf).isNotEmpty();
        String convertedHeader = new String(convertedPdf, 0, 5);
        assertThat(convertedHeader).isEqualTo("%PDF-");

        // Test multi-page PDF export: verify header golden line and footer text on every page
        StringBuilder longBody = new StringBuilder();
        for (int i = 1; i <= 60; i++) {
            longBody.append(i).append(". Question number ").append(i).append(": Explain the principle with a detailed diagram and proof.\n\n");
        }
        byte[] multiPagePdf = pdfService.exportPdf("EXAMINATION PAPER", "Class 10 Board Exam", longBody.toString());
        com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(multiPagePdf);
        int numPages = reader.getNumberOfPages();
        assertThat(numPages).isGreaterThan(1);
        for (int p = 1; p <= numPages; p++) {
            String pageContent = new String(reader.getPageContent(p));
            assertThat(pageContent).contains("VIDUSHI KHANNA");
            assertThat(pageContent).contains("9266973332");
            assertThat(pageContent).contains("Katwaria Sarai");
            // Verify dummy student box is NOT present
            assertThat(pageContent).doesNotContain("Student Name: _____________________");
        }
        reader.close();
    }

    @Test
    void testTableRenderingAndHeadingCentering() throws Exception {
        PdfExportService pdfService = new PdfExportService();

        // 1. Direct PDF with Markdown Table and Headings
        String contentWithTable = """
                # SECTION A: DIFFERENCES
                Please study the following table carefully:

                | Parameter | Mitosis | Meiosis |
                |---|---|---|
                | Occurs in | Somatic cells | Reproductive cells |
                | Divisions | Single division | Two divisions |
                | Daughter cells | 2 diploid cells | 4 haploid cells |

                # SECTION B: APPLICATION
                1. Which division is responsible for genetic variation?
                """;

        byte[] pdfBytes = pdfService.exportPdf("CLASSROOM WORKSHEET", "Class 10 Biology - Cell Division", contentWithTable);
        assertThat(pdfBytes).isNotEmpty();

        com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(pdfBytes);
        String page1 = new String(reader.getPageContent(1));
        assertThat(page1).doesNotContain("Student Name: _____________________");
        assertThat(page1).contains("Mitosis");
        assertThat(page1).contains("Meiosis");
        assertThat(page1).contains("Somatic cells");
        reader.close();

        // 2. DOCX with Table converted to PDF
        try (XWPFDocument doc = new XWPFDocument();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            var titleP = doc.createParagraph();
            titleP.createRun().setText("SECTION A: FORMULAS");

            var table = doc.createTable(3, 2);
            table.getRow(0).getCell(0).setText("Quantity");
            table.getRow(0).getCell(1).setText("SI Unit");
            table.getRow(1).getCell(0).setText("Speed");
            table.getRow(1).getCell(1).setText("m/s");
            table.getRow(2).getCell(0).setText("Force");
            table.getRow(2).getCell(1).setText("Newton");

            var afterP = doc.createParagraph();
            afterP.createRun().setText("1. Define velocity.");

            doc.write(out);
            byte[] convertedPdf = pdfService.convertDocxToPdf(out.toByteArray());
            assertThat(convertedPdf).isNotEmpty();

            com.lowagie.text.pdf.PdfReader docxPdfReader = new com.lowagie.text.pdf.PdfReader(convertedPdf);
            String docxPage1 = new String(docxPdfReader.getPageContent(1));
            assertThat(docxPage1).contains("Quantity");
            assertThat(docxPage1).contains("SI Unit");
            assertThat(docxPage1).contains("Newton");
            assertThat(docxPage1).doesNotContain("Student Name: _____________________");
            docxPdfReader.close();
        }
    }

    @Test
    void testFileStorageService(@TempDir Path tempDir) throws Exception {
        FileStorageService storage = new FileStorageService(tempDir.toString());

        byte[] sample = "sample content".getBytes();
        File docx = storage.saveDocx("run-123", "worksheet", sample);
        File pdf = storage.savePdf("run-123", "worksheet", sample);

        assertThat(docx).exists();
        assertThat(pdf).exists();

        assertThat(storage.findDocx("run-123")).isPresent();
        assertThat(storage.findPdf("run-123")).isPresent();
        assertThat(storage.findDocx("non-existent")).isEmpty();
    }
}
