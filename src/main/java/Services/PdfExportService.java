package Services;

import Entities.Pack;
import Entities.Inscription;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PdfExportService {

    public enum Kind { PACKS, INSCRIPTIONS, BOTH }

    private final PackService packService = new PackService();
    private final InscriptionService inscriptionService = new InscriptionService(); // adapte si autre nom

    // ====== Design constants ======
    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float MARGIN = 42f;
    private static final float HEADER_H = 70f;
    private static final float FOOTER_H = 28f;

    private static final float CARD_PAD = 14f;
    private static final float CARD_GAP = 14f;

    private static final float TABLE_ROW_H = 22f;
    private static final float TABLE_HEAD_H = 24f;
    private static final float CELL_PAD_X = 6f;

    private static final Color C_BG = new Color(245, 247, 250);
    private static final Color C_HEADER = new Color(112, 72, 255); // violet
    private static final Color C_HEADER_DARK = new Color(82, 42, 220);
    private static final Color C_CARD = Color.WHITE;
    private static final Color C_BORDER = new Color(220, 226, 235);
    private static final Color C_TEXT = new Color(30, 34, 40);
    private static final Color C_MUTED = new Color(90, 98, 110);
    private static final Color C_TH_BG = new Color(238, 240, 246);
    private static final Color C_ROW_ALT = new Color(250, 251, 253);

    // Fonts (Type1 = simple & stable)
    private static final PDType1Font FONT_B = PDType1Font.HELVETICA_BOLD;
    private static final PDType1Font FONT_R = PDType1Font.HELVETICA;

    public byte[] buildPdf(Kind kind) {
        try (PDDocument doc = new PDDocument()) {

            // fetch data
            List<Pack> packs = Collections.emptyList();
            List<Inscription> inscriptions = Collections.emptyList();

            if (kind == Kind.PACKS || kind == Kind.BOTH) {
                packs = packService.getAll(); // adapte si autre nom
            }
            if (kind == Kind.INSCRIPTIONS || kind == Kind.BOTH) {
                inscriptions = inscriptionService.getAll(); // adapte si autre nom
            }

            // create first page
            PDPage page = new PDPage(PAGE_SIZE);
            doc.addPage(page);

            // For page numbering, we render pages then add footers later:
            List<PageCursor> pages = new ArrayList<>();
            pages.add(new PageCursor(page, startY(page)));

            // Draw background + header each page as needed on demand
            Renderer r = new Renderer(doc);

            // ====== Title header (page 1) ======
            r.decoratePage(pages.get(0).page, kind, packs.size(), inscriptions.size());

            // ====== Content ======
            float y = pages.get(0).y;

            if (kind == Kind.PACKS || kind == Kind.BOTH) {
                y = r.drawSectionCard(pages, y, "Liste des Packs", "Total: " + packs.size(),
                        buildPackTable(packs));
                y -= CARD_GAP;
            }

            if (kind == Kind.INSCRIPTIONS || kind == Kind.BOTH) {
                y = r.drawSectionCard(pages, y, "Liste des Inscriptions", "Total: " + inscriptions.size(),
                        buildInscriptionTable(inscriptions));
                y -= CARD_GAP;
            }

            // ====== Add footer Page X/Y ======
            int total = pages.size();
            for (int i = 0; i < pages.size(); i++) {
                r.drawFooter(pages.get(i).page, i + 1, total);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("PDF export failed: " + e.getMessage(), e);
        }
    }

    // =========================
    // Build tables (data → rows)
    // =========================

    private Table buildPackTable(List<Pack> packs) {
        // ⚠️ si tu ne veux pas afficher ID, on l’enlève du tableau.
        Table t = new Table();
        t.columns = new String[] {"Nom", "Type", "Prix", "Réduction", "Max", "Statut"};

        for (Pack p : packs) {
            t.rows.add(new String[]{
                    safe(p.getNom()),
                    p.getTypePack() == null ? "-" : p.getTypePack().name(),
                    (p.getPrixBase() == null ? "0" : p.getPrixBase().toPlainString()) + " DT",
                    (p.getReduction() == null ? "0" : p.getReduction().toPlainString()) + " DT",
                    String.valueOf(p.getNbActivitesMax()),
                    p.getStatutPack() == null ? "-" : p.getStatutPack().name()
            });
        }
        return t;
    }

    private Table buildInscriptionTable(List<Inscription> ins) {
        // هنا نستعمل reflection باش ما يصيرش "Cannot resolve method" حسب Entities متاعك
        Table t = new Table();
        t.columns = new String[] {"Inscription", "Utilisateur", "Pack", "Statut", "Date"};

        for (Inscription i : ins) {
            String id = pick(i, "getIdInscription", "getId", "getIdIns");
            String user = pick(i, "getUser", "getUtilisateur", "getIdUtilisateur", "getUserId");
            String pack = pick(i, "getPack", "getIdPack", "getPackId");
            String statut = pick(i, "getStatut", "getEtat", "getStatus", "getStatutInscription");
            String date = pick(i, "getDateInscription", "getDate", "getCreatedAt", "getDateCreation");

            t.rows.add(new String[]{
                    nz(id), nz(user), nz(pack), nz(statut), nz(date)
            });
        }
        return t;
    }

    // =========================
    // Rendering engine
    // =========================

    private static class Table {
        String[] columns;
        List<String[]> rows = new ArrayList<>();
    }

    private static class PageCursor {
        PDPage page;
        float y;
        PageCursor(PDPage p, float y) { this.page = p; this.y = y; }
    }

    private static float startY(PDPage page) {
        return page.getMediaBox().getHeight() - MARGIN - HEADER_H - 12f;
    }

    private static float minY() {
        return MARGIN + FOOTER_H + 8f;
    }

    private class Renderer {
        private final PDDocument doc;

        Renderer(PDDocument doc) {
            this.doc = doc;
        }

        void decoratePage(PDPage page, Kind kind, int packsCount, int inscCount) throws Exception {
            try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                // Background
                cs.setNonStrokingColor(C_BG);
                cs.addRect(0, 0, PAGE_SIZE.getWidth(), PAGE_SIZE.getHeight());
                cs.fill();

                // Header bar
                cs.setNonStrokingColor(C_HEADER);
                cs.addRect(0, PAGE_SIZE.getHeight() - HEADER_H, PAGE_SIZE.getWidth(), HEADER_H);
                cs.fill();

                // Small accent
                cs.setNonStrokingColor(C_HEADER_DARK);
                cs.addRect(0, PAGE_SIZE.getHeight() - HEADER_H, 12, HEADER_H);
                cs.fill();

                // Title
                String title = "EcoAdventure — Export " + kind;
                drawText(cs, FONT_B, 18, Color.WHITE, MARGIN, PAGE_SIZE.getHeight() - 40, title);

                String sub = "Packs: " + packsCount + "   |   Inscriptions: " + inscCount;
                drawText(cs, FONT_R, 11, new Color(238, 236, 255), MARGIN, PAGE_SIZE.getHeight() - 58, sub);

                // Date
                String dt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                drawText(cs, FONT_R, 10, new Color(238, 236, 255),
                        PAGE_SIZE.getWidth() - MARGIN - textWidth(FONT_R, 10, dt),
                        PAGE_SIZE.getHeight() - 58, dt);
            }
        }

        float drawSectionCard(List<PageCursor> pages, float y, String title, String badge, Table table) throws Exception {
            // ensure current page cursor
            PageCursor cur = pages.get(pages.size() - 1);

            // Compute needed height roughly (header + rows). We'll paginate inside table drawing.
            float cardTop = y;
            float cardX = MARGIN;
            float cardW = PAGE_SIZE.getWidth() - 2 * MARGIN;

            // Card header space
            float headerSpace = 38f;

            // If not enough room for header + table header, new page
            if (cardTop - (headerSpace + TABLE_HEAD_H + 2 * TABLE_ROW_H) < minY()) {
                cur = newPage(pages);
                decoratePage(cur.page, Kind.BOTH, 0, 0); // header already nice background; counts not critical here
                cardTop = cur.y;
            }

            // Draw card container (we draw per page, for big tables card continues)
            // We'll draw the card top header on current page, then table.

            float cardYBottomGuess = minY(); // we will draw table lines and not rely on fixed rect
            // Card header
            try (PDPageContentStream cs = new PDPageContentStream(doc, cur.page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                // card bg
                cs.setNonStrokingColor(C_CARD);
                cs.setStrokingColor(C_BORDER);
                cs.addRect(cardX, cardTop - headerSpace, cardW, headerSpace);
                cs.fillAndStroke();

                drawText(cs, FONT_B, 13, C_TEXT, cardX + CARD_PAD, cardTop - 22, title);

                // badge (right)
                String b = badge;
                float bw = textWidth(FONT_B, 10, b) + 16;
                float bx = cardX + cardW - CARD_PAD - bw;
                float by = cardTop - 28;
                cs.setNonStrokingColor(new Color(112, 72, 255, 40));
                cs.setStrokingColor(new Color(112, 72, 255, 120));
                cs.addRect(bx, by, bw, 18);
                cs.fillAndStroke();
                drawText(cs, FONT_B, 10, new Color(112, 72, 255), bx + 8, by + 5, b);
            }

            // Draw table just under header
            float tableTop = cardTop - headerSpace - 10;
            float newY = drawTablePaginated(pages, tableTop, cardX, cardW, table);

            // Draw bottom card line on last page
            PageCursor last = pages.get(pages.size() - 1);
            try (PDPageContentStream cs = new PDPageContentStream(doc, last.page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                cs.setStrokingColor(C_BORDER);
                cs.moveTo(cardX, newY);
                cs.lineTo(cardX + cardW, newY);
                cs.stroke();
            }

            // return y for next section
            last.y = newY - 8;
            return last.y;
        }

        float drawTablePaginated(List<PageCursor> pages, float y, float x, float w, Table table) throws Exception {
            PageCursor cur = pages.get(pages.size() - 1);

            // column widths (simple responsive weights)
            float[] colW = computeColWidths(table.columns, w);

            // draw header row
            y = ensureSpaceOrNewPage(pages, y, TABLE_HEAD_H + TABLE_ROW_H);
            cur = pages.get(pages.size() - 1);

            y = drawTableHeader(cur.page, x, y, colW, table.columns);

            // rows
            int rowIndex = 0;
            for (String[] row : table.rows) {
                y = ensureSpaceOrNewPage(pages, y, TABLE_ROW_H);
                cur = pages.get(pages.size() - 1);

                boolean alt = (rowIndex % 2 == 1);
                y = drawTableRow(cur.page, x, y, colW, row, alt);
                rowIndex++;
            }

            return y;
        }

        float ensureSpaceOrNewPage(List<PageCursor> pages, float y, float needed) throws Exception {
            if (y - needed < minY()) {
                PageCursor cur = newPage(pages);
                decoratePage(cur.page, Kind.BOTH, 0, 0); // keep same background/header style
                y = cur.y;
            }
            return y;
        }

        PageCursor newPage(List<PageCursor> pages) {
            PDPage p = new PDPage(PAGE_SIZE);
            doc.addPage(p);
            PageCursor c = new PageCursor(p, startY(p));
            pages.add(c);
            return c;
        }

        float drawTableHeader(PDPage page, float x, float yTop, float[] colW, String[] cols) throws Exception {
            float y = yTop;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                cs.setNonStrokingColor(C_TH_BG);
                cs.setStrokingColor(C_BORDER);
                cs.addRect(x, y - TABLE_HEAD_H, sum(colW), TABLE_HEAD_H);
                cs.fillAndStroke();

                float cx = x;
                for (int i = 0; i < cols.length; i++) {
                    // vertical separators
                    if (i > 0) {
                        cs.setStrokingColor(C_BORDER);
                        cs.moveTo(cx, y - TABLE_HEAD_H);
                        cs.lineTo(cx, y);
                        cs.stroke();
                    }
                    String txt = fitText(FONT_B, 10, colW[i] - 2 * CELL_PAD_X, cols[i]);
                    drawText(cs, FONT_B, 10, C_TEXT, cx + CELL_PAD_X, y - 16, txt);
                    cx += colW[i];
                }
            }

            return y - TABLE_HEAD_H;
        }

        float drawTableRow(PDPage page, float x, float yTop, float[] colW, String[] vals, boolean alt) throws Exception {
            float y = yTop;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                // background
                cs.setNonStrokingColor(alt ? C_ROW_ALT : Color.WHITE);
                cs.setStrokingColor(C_BORDER);
                cs.addRect(x, y - TABLE_ROW_H, sum(colW), TABLE_ROW_H);
                cs.fillAndStroke();

                float cx = x;
                for (int i = 0; i < colW.length; i++) {
                    if (i > 0) {
                        cs.setStrokingColor(C_BORDER);
                        cs.moveTo(cx, y - TABLE_ROW_H);
                        cs.lineTo(cx, y);
                        cs.stroke();
                    }

                    String value = (i < vals.length) ? safe(vals[i]) : "";
                    String txt = fitText(FONT_R, 10, colW[i] - 2 * CELL_PAD_X, value);
                    drawText(cs, FONT_R, 10, C_TEXT, cx + CELL_PAD_X, y - 15, txt);

                    cx += colW[i];
                }
            }

            return y - TABLE_ROW_H;
        }

        void drawFooter(PDPage page, int pageNo, int total) throws Exception {
            try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                // footer line
                cs.setStrokingColor(new Color(210, 215, 225));
                cs.moveTo(MARGIN, MARGIN + FOOTER_H);
                cs.lineTo(PAGE_SIZE.getWidth() - MARGIN, MARGIN + FOOTER_H);
                cs.stroke();

                String left = "EcoAdventure";
                String right = "Page " + pageNo + " / " + total;

                drawText(cs, FONT_R, 10, C_MUTED, MARGIN, MARGIN + 10, left);
                float rw = textWidth(FONT_R, 10, right);
                drawText(cs, FONT_R, 10, C_MUTED, PAGE_SIZE.getWidth() - MARGIN - rw, MARGIN + 10, right);
            }
        }

        void drawText(PDPageContentStream cs, PDType1Font font, int size, Color color,
                      float x, float y, String text) throws Exception {
            cs.beginText();
            cs.setFont(font, size);
            cs.setNonStrokingColor(color);
            cs.newLineAtOffset(x, y);
            cs.showText(safePdf(text));
            cs.endText();
        }
    }

    // =========================
    // Helpers
    // =========================

    private static float[] computeColWidths(String[] cols, float totalWidth) {
        // simple weights: first columns wider
        int n = cols.length;
        float[] w = new float[n];

        // default weights
        float[] weights = new float[n];
        Arrays.fill(weights, 1f);

        if (n >= 1) weights[0] = 2.2f; // Nom
        if (n >= 2) weights[1] = 1.2f;
        if (n >= 3) weights[2] = 1.0f;
        if (n >= 4) weights[3] = 1.0f;
        if (n >= 5) weights[4] = 0.9f;
        if (n >= 6) weights[5] = 1.0f;

        float sum = 0;
        for (float v : weights) sum += v;

        for (int i = 0; i < n; i++) {
            w[i] = totalWidth * (weights[i] / sum);
        }
        return w;
    }

    private static float sum(float[] a) {
        float s = 0;
        for (float v : a) s += v;
        return s;
    }

    private static String fitText(PDType1Font font, int size, float maxWidth, String text) {
        String s = safe(text);
        if (textWidth(font, size, s) <= maxWidth) return s;

        String ell = "...";
        float ellW = textWidth(font, size, ell);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            sb.append(s.charAt(i));
            if (textWidth(font, size, sb.toString()) + ellW > maxWidth) {
                sb.setLength(Math.max(0, sb.length() - 1));
                return sb + ell;
            }
        }
        return s;
    }

    private static float textWidth(PDType1Font font, int size, String text) {
        try {
            return (font.getStringWidth(safe(text)) / 1000f) * size;
        } catch (Exception e) {
            return 0;
        }
    }

    private static String safePdf(String s) {
        if (s == null) return "";
        return s.replace("\t", " ").replace("\r", " ").replace("\n", " ");
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String nz(String s) {
        String v = safe(s).trim();
        return v.isEmpty() ? "-" : v;
    }

    // Reflection: try getters without knowing your exact entity structure
    private static String pick(Object obj, String... methodNames) {
        if (obj == null) return "";
        for (String m : methodNames) {
            Object v = invokeNoArg(obj, m);
            if (v == null) continue;

            // if returns complex object (User/Pack), try nested id/name
            if (!(v instanceof String) && !(v instanceof Number) && !(v instanceof Boolean)) {
                String nested = pick(v, "getId", "getIdUser", "getIdUtilisateur", "getIdPack", "getNom", "getName");
                if (!nested.isBlank()) return nested;
                return v.toString();
            }
            String s = String.valueOf(v).trim();
            if (!s.isEmpty()) return s;
        }
        return "";
    }

    private static Object invokeNoArg(Object obj, String methodName) {
        try {
            Method m = obj.getClass().getMethod(methodName);
            return m.invoke(obj);
        } catch (Exception ignored) {
            return null;
        }
    }
}