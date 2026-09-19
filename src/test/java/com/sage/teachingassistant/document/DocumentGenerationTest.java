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

        // Test multi-page PDF export
        StringBuilder longBody = new StringBuilder();
        for (int i = 1; i <= 60; i++) {
            longBody.append(i).append(". Question number ").append(i).append(": Explain the principle with a detailed diagram and proof.\n\n");
        }
        byte[] multiPagePdf = pdfService.exportPdf("EXAMINATION PAPER", "Class 10 Board Exam", longBody.toString());
        com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(multiPagePdf);
        int numPages = reader.getNumberOfPages();
        assertThat(numPages).isGreaterThan(1);
        reader.close();
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
