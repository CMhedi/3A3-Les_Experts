package Services;
import Entities.UserApp;
import Entities.Pack;
import Entities.Inscription;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PdfExportService {

    public enum Kind { PACKS, INSCRIPTIONS, BOTH }

    private final PackService packService = new PackService();
    private final InscriptionService inscriptionService = new InscriptionService(); // ✅ لازم تكون موجودة عندك

    public byte[] buildPdf(Kind kind) {
        try (PDDocument doc = new PDDocument()) {

            List<String> lines = new ArrayList<>();
            lines.add("EcoAdventure — Export " + kind);
            lines.add("Generated: " + LocalDateTime.now());
            lines.add(" ");

            if (kind == Kind.PACKS || kind == Kind.BOTH) {
                lines.add("=== LISTE DES PACKS ===");
                // ✅ عدّل حسب اسم الميثود عندك
                List<Pack> packs = packService.getAll();

                if (packs.isEmpty()) lines.add("(Aucun pack)");
                for (Pack p : packs) {
                    lines.add("ID: " + p.getIdPack()
                            + " | Nom: " + p.getNom()
                            + " | Type: " + p.getTypePack()
                            + " | Prix: " + p.getPrixBase()
                            + " | Red: " + p.getReduction()
                            + " | Max: " + p.getNbActivitesMax()
                            + " | Statut: " + p.getStatutPack());
                }
                lines.add(" ");
            }

            if (kind == Kind.INSCRIPTIONS || kind == Kind.BOTH) {
                lines.add("=== LISTE DES INSCRIPTIONS ===");
                // ✅ عدّل حسب اسم الميثود عندك
                List<Inscription> ins = inscriptionService.getAll();

                if (ins.isEmpty()) lines.add("(Aucune inscription)");
                for (Inscription i : ins) {
                    lines.add("ID: " + i.getIdInscription()
                            + " | User: " + i.getUserId()
                            + " | Pack: " + i.getPackId()
                            + " | Statut: " + i.getStatut()
                            + " | Date: " + i.getDateInscription());
                }
                lines.add(" ");
            }

            writeLines(doc, lines);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("PDF export failed: " + e.getMessage(), e);
        }
    }

    private void writeLines(PDDocument doc, List<String> lines) throws Exception {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);

        float margin = 50;
        float yStart = page.getMediaBox().getHeight() - margin;
        float y = yStart;
        float leading = 14;

        PDPageContentStream cs = new PDPageContentStream(doc, page);
        cs.setFont(PDType1Font.HELVETICA, 11);

        for (String line : lines) {
            if (y < margin) {
                cs.close();
                page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                cs = new PDPageContentStream(doc, page);
                cs.setFont(PDType1Font.HELVETICA, 11);
                y = yStart;
            }

            cs.beginText();
            cs.newLineAtOffset(margin, y);
            cs.showText(safePdf(line));
            cs.endText();

            y -= leading;
        }

        cs.close();
    }

    private String safePdf(String s) {
        if (s == null) return "";
        // PDFBox hates some control chars
        return s.replace("\t", " ").replace("\r", " ").replace("\n", " ");
    }
}