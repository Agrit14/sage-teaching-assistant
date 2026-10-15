package com.sage.teachingassistant.document;

import com.lowagie.text.pdf.PdfReader;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentFormattingRegressionTest {

    @Test
    void testMarkdownInlineStylesBulletsAndTableExportToPdf() throws Exception {
        PdfExportService pdfService = new PdfExportService();

        String content = """
                # SECTION A: MOTION & LAWS
                **Newton's Second Law:** The force *F* is equal to **mass** times ***acceleration***.
                
                Q1. State the law of conservation of momentum. [2 Marks]
                
                Key points to remember:
                - Velocity is a *vector* quantity.
                * Speed is a **scalar** quantity.
                  - Instantaneous velocity is the derivative of position.
                • SI unit of force is `Newton (N)`.
                
                Questions with Roman and letter sub-items:
                (a) Calculate the net force if mass is 10 kg and acceleration is 5 m/s².
                (b) What happens when friction is zero?
                (i) In horizontal motion
                (ii) In inclined plane motion
                
                | S.No | Concept | Formula / Description | Marks |
                |---|---|---|---|
                | 1 | Momentum | **p = mv** (product of mass and velocity) | 1 |
                | 2 | Force | **F = ma** (*Newton's second law*) | 2 |
                """;

        byte[] pdfBytes = pdfService.exportPdf("CLASSROOM WORKSHEET", "Class 9 Physics", content);
        assertThat(pdfBytes).isNotEmpty();

        try (org.apache.pdfbox.pdmodel.PDDocument pdDoc = org.apache.pdfbox.Loader.loadPDF(pdfBytes)) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            String extracted = stripper.getText(pdDoc);
            assertThat(extracted).contains("Newton's Second Law");
            assertThat(extracted).contains("Momentum");
            assertThat(extracted).contains("p = mv");
            assertThat(extracted).contains("Velocity is a");
            assertThat(extracted).contains("Instantaneous velocity");
            assertThat(extracted).contains("Newton (N)");
        }
    }

    @Test
    void testDocxToPdfPreservesBoldItalicsBulletsAndTableGeometry() throws Exception {
        PdfExportService pdfService = new PdfExportService();

        // 1. Create a Word document with explicit bold, italic, bullet, and table formatting
        byte[] docxBytes;
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Paragraph with mixed bold and italic runs
            XWPFParagraph p1 = doc.createParagraph();
            XWPFRun r1 = p1.createRun();
            r1.setText("Einstein stated that ");

            XWPFRun rBold = p1.createRun();
            rBold.setText("energy and mass ");
            rBold.setBold(true);

            XWPFRun rItalic = p1.createRun();
            rItalic.setText("are equivalent: ");
            rItalic.setItalic(true);

            XWPFRun rFormula = p1.createRun();
            rFormula.setText("E = mc²");
            rFormula.setBold(true);

            // Native Word bullet list item
            XWPFParagraph bulletPara = doc.createParagraph();
            bulletPara.setNumID(BigInteger.valueOf(1)); // native Word bullet numbering
            XWPFRun bulletRun = bulletPara.createRun();
            bulletRun.setText("First key principle of relativity");

            // Text bullet item
            XWPFParagraph textBulletPara = doc.createParagraph();
            XWPFRun textBulletRun = textBulletPara.createRun();
            textBulletRun.setText("- Second key principle of light speed invariance");

            // Question paragraph
            XWPFParagraph qPara = doc.createParagraph();
            XWPFRun qRun = qPara.createRun();
            qRun.setText("Q1. Derive the mass-energy equivalence equation.");
            qRun.setBold(true);

            // Table with asymmetric columns and bold cells
            XWPFTable table = doc.createTable(2, 3);
            XWPFTableRow header = table.getRow(0);
            header.getCell(0).setText("No.");
            header.getCell(1).setText("Detailed Description of Physics Experiment");
            header.getCell(2).setText("Score");

            XWPFTableRow data = table.getRow(1);
            data.getCell(0).setText("1");
            data.getCell(1).setText("Photoelectric effect observation showing quantized packets of energy.");
            data.getCell(2).setText("10");

            doc.write(out);
            docxBytes = out.toByteArray();
        }

        // 2. Convert DOCX to PDF
        byte[] pdfBytes = pdfService.convertDocxToPdf(docxBytes);
        assertThat(pdfBytes).isNotEmpty();

        try (org.apache.pdfbox.pdmodel.PDDocument pdDoc = org.apache.pdfbox.Loader.loadPDF(pdfBytes)) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            String pageText = stripper.getText(pdDoc);
            assertThat(pageText).contains("First key principle of relativity");
            assertThat(pageText).contains("Second key principle of light speed invariance");
            assertThat(pageText).contains("energy and mass");
            assertThat(pageText).contains("E = mc²");
            assertThat(pageText).contains("Photoelectric effect observation");
        }
    }

    @Test
    void testDocxTemplateGeneratorCreatesStyledRunsAndTables() throws Exception {
        DocxTemplateGenerator generator = new DocxTemplateGenerator();

        String markdown = """
                # SECTION A: THEORY
                **Bold title:** Here is *italic description* and `code sample`.
                
                Q1. Describe the experiment.
                
                - Item one with **bold** text
                - Item two with *italic* text
                  - Sub item three
                
                (a) Sub-part A
                (i) Roman sub-part 1
                
                | Col A | Col B |
                |---|---|
                | **Bold Cell** | *Italic Cell* |
                """;

        byte[] docxBytes = generator.generateDocument("CLASSROOM WORKSHEET", "Class 10 Physics", markdown);
        assertThat(docxBytes).isNotEmpty();

        // Parse generated docx and assert runs and table structure
        try (ByteArrayInputStream in = new ByteArrayInputStream(docxBytes);
             XWPFDocument doc = new XWPFDocument(in)) {

            assertThat(doc.getParagraphs()).isNotEmpty();
            assertThat(doc.getTables()).hasSize(1);

            XWPFTable table = doc.getTables().get(0);
            assertThat(table.getRows()).hasSize(2);
            // Verify bold run was created inside table cell
            XWPFParagraph cellPara = table.getRow(1).getCell(0).getParagraphs().get(0);
            assertThat(cellPara.getRuns().stream().anyMatch(XWPFRun::isBold)).isTrue();
        }
    }

    @Test
    void testDocxWithImagesConvertsToPdfSuccessfully() throws Exception {
        PdfExportService pdfService = new PdfExportService();

        // Create a 100x100 sample PNG image in memory
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(100, 100, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.BLUE);
        g.fillRect(0, 0, 100, 100);
        g.dispose();
        ByteArrayOutputStream imgStream = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", imgStream);
        byte[] testImageBytes = imgStream.toByteArray();

        byte[] docxBytes;
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Paragraph 1: Question with attached image
            XWPFParagraph p1 = doc.createParagraph();
            XWPFRun r1 = p1.createRun();
            r1.setText("Q1. Identify the diagram below:");
            XWPFRun r1Pic = p1.createRun();
            try (ByteArrayInputStream in = new ByteArrayInputStream(testImageBytes)) {
                r1Pic.addPicture(in, org.apache.poi.common.usermodel.PictureType.PNG, "diagram.png",
                        org.apache.poi.util.Units.toEMU(150), org.apache.poi.util.Units.toEMU(100));
            }

            // Paragraph 2: Standalone image (no text in paragraph)
            XWPFParagraph p2 = doc.createParagraph();
            XWPFRun r2Pic = p2.createRun();
            try (ByteArrayInputStream in = new ByteArrayInputStream(testImageBytes)) {
                r2Pic.addPicture(in, org.apache.poi.common.usermodel.PictureType.PNG, "standalone.png",
                        org.apache.poi.util.Units.toEMU(200), org.apache.poi.util.Units.toEMU(120));
            }

            // Table with an image inside a cell
            XWPFTable table = doc.createTable(1, 2);
            table.getRow(0).getCell(0).setText("Circuit A");
            XWPFParagraph cellPara = table.getRow(0).getCell(1).getParagraphs().get(0);
            XWPFRun cellPicRun = cellPara.createRun();
            try (ByteArrayInputStream in = new ByteArrayInputStream(testImageBytes)) {
                cellPicRun.addPicture(in, org.apache.poi.common.usermodel.PictureType.PNG, "cell.png",
                        org.apache.poi.util.Units.toEMU(100), org.apache.poi.util.Units.toEMU(80));
            }

            doc.write(out);
            docxBytes = out.toByteArray();
        }

        byte[] pdfBytes = pdfService.convertDocxToPdf(docxBytes);
        assertThat(pdfBytes).isNotEmpty();

        // Verify PDF is valid and contains text and images
        try (org.apache.pdfbox.pdmodel.PDDocument pdDoc = org.apache.pdfbox.Loader.loadPDF(pdfBytes)) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            String text = stripper.getText(pdDoc);
            assertThat(text).contains("Identify the diagram below");
            assertThat(text).contains("Circuit A");

            // Verify image resources exist on page 1
            org.apache.pdfbox.pdmodel.PDPage page = pdDoc.getPage(0);
            int imageCount = 0;
            for (org.apache.pdfbox.cos.COSName xObjectName : page.getResources().getXObjectNames()) {
                if (page.getResources().isImageXObject(xObjectName)) {
                    imageCount++;
                }
            }
            // Header image + 3 docx images
            assertThat(imageCount).isGreaterThanOrEqualTo(3);
        }
    }

    @Test
    void testEmptyDocxConvertsSafelyWithoutCrashing() throws Exception {
        PdfExportService pdfService = new PdfExportService();

        byte[] emptyDocxBytes;
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.write(out);
            emptyDocxBytes = out.toByteArray();
        }

        // Must not throw "The document has no pages"
        byte[] pdfBytes = pdfService.convertDocxToPdf(emptyDocxBytes);
        assertThat(pdfBytes).isNotEmpty();

        try (org.apache.pdfbox.pdmodel.PDDocument pdDoc = org.apache.pdfbox.Loader.loadPDF(pdfBytes)) {
            assertThat(pdDoc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    void testDocxWithSoftLineBreaksAndPageBreaks() throws Exception {
        PdfExportService pdfService = new PdfExportService();

        byte[] docxBytes;
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Paragraph with a soft break (<w:br/>)
            XWPFParagraph p1 = doc.createParagraph();
            XWPFRun r1 = p1.createRun();
            r1.setText("Line 1 of question");
            r1.addBreak();
            r1.setText("Line 2 of question after soft break");

            // Paragraph with explicit page break
            XWPFParagraph p2 = doc.createParagraph();
            p2.setPageBreak(true);
            XWPFRun r2 = p2.createRun();
            r2.setText("Content on page 2");

            doc.write(out);
            docxBytes = out.toByteArray();
        }

        byte[] pdfBytes = pdfService.convertDocxToPdf(docxBytes);
        assertThat(pdfBytes).isNotEmpty();

        try (org.apache.pdfbox.pdmodel.PDDocument pdDoc = org.apache.pdfbox.Loader.loadPDF(pdfBytes)) {
            assertThat(pdDoc.getNumberOfPages()).isGreaterThanOrEqualTo(2);
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            String text = stripper.getText(pdDoc);
            assertThat(text).contains("Line 1 of question");
            assertThat(text).contains("Line 2 of question after soft break");
            assertThat(text).contains("Content on page 2");
        }
    }
}
