package com.project.examportalbackend.report;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reports how much of each page the report actually fills, so pagination
 * changes can be judged on numbers instead of by flipping through the PDF.
 *
 * <p>Run against the harness output by default; point it at any other file with
 * {@code -Dreport.audit.pdf=/path/to.pdf}.
 *
 * <p>ponytail: fill is measured from TEXT extents only -- a page whose lowest
 * element is a bare chart bar or an image with no label under it reads as
 * emptier than it looks. Every block in this template ends in text (labels,
 * captions, list items), so the approximation holds; if that stops being true,
 * swap in PDFGraphicsStreamEngine to pick up shape and image bounds too.
 */
class PageFillAuditTest {

    /** A4 with the template's @page margin: 30mm 16mm 16mm. */
    private static final float MM = 72f / 25.4f;
    private static final float PAGE_H_MM = 297f;
    private static final float TOP_MARGIN_MM = 30f;
    private static final float BOTTOM_MARGIN_MM = 16f;
    private static final float USABLE_MM = PAGE_H_MM - TOP_MARGIN_MM - BOTTOM_MARGIN_MM;
    /** Ignore the running header/footer: they sit in the @page margin boxes. */
    private static final float BAND_TOP_MM = TOP_MARGIN_MM - 6f;
    private static final float BAND_BOTTOM_MM = PAGE_H_MM - BOTTOM_MARGIN_MM + 4f;
    /** Below this fill a normal content page is worth investigating. */
    private static final int GOOD_FILL_PCT = 70;

    @Test
    void auditPageFill() throws IOException {
        Path pdf = Paths.get(System.getProperty("report.audit.pdf",
                "target/report-qa/mentalist-report-qa.pdf"));
        if (!Files.exists(pdf)) {
            System.out.println("No PDF at " + pdf.toAbsolutePath()
                    + " -- run ReportRenderHarnessTest first. Skipping audit.");
            return;
        }

        List<Integer> fills = fillPercentages(pdf);
        List<String> headings = firstLines(pdf);

        StringBuilder table = new StringBuilder("\n=== page fill audit: " + pdf + " ===\n");
        table.append("Page | Used | Blank | Status    | Opens with\n");
        int bad = 0;
        for (int i = 0; i < fills.size(); i++) {
            int used = fills.get(i);
            // Page 1 is the cover and the last page is the closing: both are
            // spacious by design, so they are reported but never flagged.
            boolean exempt = i == 0 || i == fills.size() - 1;
            String status = exempt ? "BY DESIGN" : used >= GOOD_FILL_PCT ? "GOOD" : "SPARSE";
            if (!exempt && used < GOOD_FILL_PCT) {
                bad++;
            }
            table.append(String.format("%4d | %3d%% | %3d%%  | %-9s | %s%n",
                    i + 1, used, 100 - used, status, headings.get(i)));
        }
        table.append(String.format("pages=%d sparse=%d (below %d%% fill)%n",
                fills.size(), bad, GOOD_FILL_PCT));
        System.out.println(table);

        // Deliberately not asserting a fill target: content length varies per
        // student, so a threshold here would fail on data rather than on layout.
        // The one real defect is a page with nothing on it at all.
        for (int i = 0; i < fills.size(); i++) {
            assertTrue(fills.get(i) > 0, "page " + (i + 1) + " is blank");
        }
    }

    /** First readable line of each page, to identify what a sparse page holds. */
    private List<String> firstLines(Path pdf) throws IOException {
        List<String> out = new ArrayList<>();
        try (PDDocument doc = PDDocument.load(pdf.toFile())) {
            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String first = "";
                for (String line : stripper.getText(doc).split("\\r?\\n")) {
                    String trimmed = line.trim();
                    // Skip the running header/footer lines, which repeat verbatim.
                    if (trimmed.isEmpty()
                            || trimmed.contains("Confidential Student Assessment Report")
                            || trimmed.matches("Page \\d+ of \\d+")) {
                        continue;
                    }
                    first = trimmed.length() > 44 ? trimmed.substring(0, 44) : trimmed;
                    break;
                }
                out.add(first);
            }
        }
        return out;
    }

    /** Percentage of the usable text box each page fills, top of box to last glyph. */
    private List<Integer> fillPercentages(Path pdf) throws IOException {
        List<Integer> out = new ArrayList<>();
        try (PDDocument doc = PDDocument.load(pdf.toFile())) {
            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                float lowestMm = lowestTextMm(doc, p);
                float usedMm = Math.max(0f, Math.min(lowestMm, BAND_BOTTOM_MM) - TOP_MARGIN_MM);
                out.add(Math.round(usedMm / USABLE_MM * 100f));
            }
        }
        return out;
    }

    /** Y of the lowest glyph on the page, in mm from the page top, header/footer excluded. */
    private float lowestTextMm(PDDocument doc, int pageNo) throws IOException {
        float[] lowest = {TOP_MARGIN_MM};
        PDFTextStripper stripper = new PDFTextStripper() {
            @Override
            protected void writeString(String text, List<TextPosition> positions) {
                for (TextPosition tp : positions) {
                    // getYDirAdj() is top-down page space, which is what the mm
                    // margins above are expressed in.
                    float mm = tp.getYDirAdj() / MM;
                    if (mm > BAND_TOP_MM && mm < BAND_BOTTOM_MM && mm > lowest[0]) {
                        lowest[0] = mm;
                    }
                }
            }
        };
        stripper.setStartPage(pageNo);
        stripper.setEndPage(pageNo);
        stripper.getText(doc);
        return lowest[0];
    }

    /** Convenience for the cover-to-closing page count while iterating on layout. */
    static int pageCount(Path pdf) throws IOException {
        try (PDDocument doc = PDDocument.load(pdf.toFile())) {
            return doc.getNumberOfPages();
        }
    }

    @SuppressWarnings("unused")
    private static float heightMm(PDPage page) {
        return page.getMediaBox().getHeight() / MM;
    }
}
