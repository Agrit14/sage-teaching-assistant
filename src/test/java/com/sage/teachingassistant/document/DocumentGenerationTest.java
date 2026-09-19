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
