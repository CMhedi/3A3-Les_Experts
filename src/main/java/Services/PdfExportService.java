package Services;

import Entities.Pack;
import Entities.Inscription;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PdfExportService {

    public enum Kind { PACKS, INSCRIPTIONS, BOTH }

    private final PackService packService = new PackService();
    private final InscriptionService inscriptionService = new InscriptionService(); // لازم تكون موجودة عندك

    public byte[] buildPdf(Kind kind) {
        try (PDDocument doc = new PDDocument()) {

            List<String> lines = new ArrayList<>();
            lines.add("EcoAdventure — Export " + kind);
            lines.add("Generated: " + LocalDateTime.now());
            lines.add(" ");

            if (kind == Kind.PACKS || kind == Kind.BOTH) {
                lines.add("=== LISTE DES PACKS ===");
                List<Pack> packs = packService.getAll(); // عدّلها إذا اسمها مختلف

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
                List<Inscription> ins = inscriptionService.getAll(); // عدّلها إذا اسمها مختلف

                if (ins.isEmpty()) lines.add("(Aucune inscription)");

                for (Inscription i : ins) {
                    // ✅ نحاول نلقاو القيم بأي getter موجود عندك
                    String id = pick(i,
                            "getIdInscription", "getId", "getIdIns", "getId_inscription",
                            "getIdInscriptionPk"
                    );

                    String user = pick(i,
                            "getUserId", "getIdUser", "getIdUtilisateur", "getUtilisateurId",
                            "getUser", "getUtilisateur"
                    );

                    String pack = pick(i,
                            "getPackId", "getIdPack", "getPack", "getPackRef"
                    );

                    String statut = pick(i,
                            "getStatut", "getStatus", "getEtat", "getStatutInscription"
                    );

                    String date = pick(i,
                            "getDateInscription", "getDate", "getCreatedAt", "getDateCreation"
                    );

                    // fallback إذا الكل فشل
                    if (isEmpty(id) && isEmpty(user) && isEmpty(pack) && isEmpty(statut) && isEmpty(date)) {
                        lines.add(i.toString());
                    } else {
                        lines.add("ID: " + nz(id)
                                + " | User: " + nz(user)
                                + " | Pack: " + nz(pack)
                                + " | Statut: " + nz(statut)
                                + " | Date: " + nz(date));
                    }
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

    /* ===================== PDF writing ===================== */

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
        return s.replace("\t", " ").replace("\r", " ").replace("\n", " ");
    }

    /* ===================== Reflection helpers ===================== */

    private static String pick(Object obj, String... methodNames) {
        if (obj == null) return "";
        for (String m : methodNames) {
            Object v = invokeNoArg(obj, m);
            if (v == null) continue;

            // إذا رجّع Object (User/Pack) نحاول نجيب id منه
            if (!(v instanceof String) && !isPrimitiveLike(v)) {
                String nestedId = pick(v, "getId", "getIdUser", "getIdUtilisateur", "getIdPack", "getIdInscription");
                if (!isEmpty(nestedId)) return nestedId;
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

    private static boolean isPrimitiveLike(Object v) {
        return v instanceof Number || v instanceof Boolean || v instanceof Character || v.getClass().isPrimitive();
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String nz(String s) {
        return isEmpty(s) ? "-" : s.trim();
    }
}