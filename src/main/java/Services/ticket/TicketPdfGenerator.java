package Services.ticket;

import Entities.Pack;
import Entities.UserApp;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * EcoAdventure Ticket PDF Generator (PRO)
 * - PDF stylé
 * - QR Code
 * - Logo optionnel (/assets/eco_logo.png)
 */
public final class TicketPdfGenerator {

    private TicketPdfGenerator() {}

    // =========================================
    // ✅ OVERLOAD 5 PARAMS (pour ton Controller)
    // =========================================
    public static void generateTicketPdf(int idInscription,
                                         UserApp user,
                                         Pack pack,
                                         BigDecimal total,
                                         int nbPersonnes) throws Exception {

        // Fallback safe si tel est null
        String tel = "-";
        try {
            // si ton UserApp a getTelephone()
            var m = user.getClass().getMethod("getTelephone");
            Object v = m.invoke(user);
            if (v != null && !String.valueOf(v).trim().isEmpty()) tel = String.valueOf(v);
        } catch (Exception ignored) {}

        generateTicketPdf(
                idInscription,
                user.getIdUser(),
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                tel,
                pack.getNom(),
                pack.getTypePack() == null ? "-" : pack.getTypePack().toString(),
                total,
                nbPersonnes
        );
    }

    // =========================================
    // ✅ MÉTHODE OFFICIELLE 10 PARAMÈTRES
    // =========================================
    public static void generateTicketPdf(int idInscription,
                                         int idUser,
                                         String nom,
                                         String prenom,
                                         String email,
                                         String telephone,
                                         String packNom,
                                         String packType,
                                         BigDecimal total,
                                         int nbPersonnes) throws Exception {

        // ✅ dossier sortie
        Path outDir = Path.of(System.getProperty("user.home"), "Documents", "EcoAdventureTickets");
        Files.createDirectories(outDir);

        // ✅ QR payload (scan)
        String ts = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String qrText = "ECOADVENTURE|INSCR=" + idInscription
                + "|USER=" + idUser
                + "|PACK=" + safe(packNom)
                + "|TYPE=" + safe(packType)
                + "|NB=" + nbPersonnes
                + "|TOTAL=" + (total == null ? "0" : total.toPlainString())
                + "|TS=" + ts;

        // ✅ generate QR png
        File qrPng = outDir.resolve("qr_" + idInscription + ".png").toFile();
        writeQrPng(qrText, 320, 320, qrPng);

        // ✅ pdf output
        File pdfFile = outDir.resolve("Ticket_" + idInscription + ".pdf").toFile();

        try (PDDocument doc = new PDDocument()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            float w = page.getMediaBox().getWidth();
            float h = page.getMediaBox().getHeight();

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // ===== Background (soft dark)
                cs.setNonStrokingColor(9, 21, 16);
                cs.addRect(0, 0, w, h);
                cs.fill();

                // ===== Header (Eco green)
                cs.setNonStrokingColor(20, 61, 48);
                cs.addRect(0, h - 140, w, 140);
                cs.fill();

                // Title
                cs.beginText();
                cs.setNonStrokingColor(255, 255, 255);
                cs.setFont(PDType1Font.HELVETICA_BOLD, 26);
                cs.newLineAtOffset(42, h - 82);
                cs.showText("EcoAdventure");
                cs.endText();

                cs.beginText();
                cs.setNonStrokingColor(230, 255, 245);
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.newLineAtOffset(42, h - 110);
                cs.showText("Ticket d'inscription • Scan QR à l'entrée");
                cs.endText();

                // Logo optionnel
                try {
                    var logoUrl = TicketPdfGenerator.class.getResource("/assets/eco_logo.png");
                    if (logoUrl != null) {
                        PDImageXObject logo = PDImageXObject.createFromFileByContent(new File(logoUrl.toURI()), doc);
                        cs.drawImage(logo, w - 150, h - 125, 95, 95);
                    }
                } catch (Exception ignored) {}

                // ===== Main Card (white glass)
                float cardX = 40, cardY = 120, cardW = w - 80, cardH = h - 300;
                cs.setNonStrokingColor(255, 255, 255);
                cs.addRect(cardX, cardY, cardW, cardH);
                cs.fill();

                // inner header bar
                cs.setNonStrokingColor(236, 255, 247);
                cs.addRect(cardX, cardY + cardH - 56, cardW, 56);
                cs.fill();

                // Pack Name
                writeBold(cs, 60, cardY + cardH - 38, 16, "Pack: " + safe(packNom));

                // Left info block
                float leftX = 60;
                float startY = cardY + cardH - 90;
                float line = 22;

                writeKVDark(cs, leftX, startY, "Type", safe(packType)); startY -= line;
                writeKVDark(cs, leftX, startY, "Utilisateur", safe(prenom) + " " + safe(nom)); startY -= line;
                writeKVDark(cs, leftX, startY, "Email", safe(email)); startY -= line;
                writeKVDark(cs, leftX, startY, "Téléphone", safe(telephone)); startY -= line;
                writeKVDark(cs, leftX, startY, "Nb personnes", String.valueOf(nbPersonnes)); startY -= line;
                writeKVDark(cs, leftX, startY, "Total payé", (total == null ? "0" : total.toPlainString()) + " DT"); startY -= line;
                writeKVDark(cs, leftX, startY, "ID Inscription", String.valueOf(idInscription)); startY -= line;

                // Right block: QR card
                float qrSize = 220;
                float qrX = cardX + cardW - (qrSize + 60);
                float qrY = cardY + 120;

                cs.setNonStrokingColor(245, 245, 245);
                cs.addRect(qrX - 15, qrY - 15, qrSize + 30, qrSize + 30);
                cs.fill();

                PDImageXObject qrImg = PDImageXObject.createFromFile(qrPng.getAbsolutePath(), doc);
                cs.drawImage(qrImg, qrX, qrY, qrSize, qrSize);

                // Note under QR
                cs.beginText();
                cs.setNonStrokingColor(40, 40, 40);
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                cs.newLineAtOffset(qrX - 10, qrY - 28);
                cs.showText("Scan ce QR à l'entrée");
                cs.endText();

                // Footer
                cs.beginText();
                cs.setNonStrokingColor(220, 240, 232);
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.newLineAtOffset(40, 50);
                cs.showText("EcoAdventure • Merci pour ta confiance • " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                cs.endText();
            }

            doc.save(pdfFile);
        }
    }

    // ============ helpers ============

    private static String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s.trim();
    }

    private static void writeBold(PDPageContentStream cs, float x, float y, int size, String text) throws Exception {
        cs.beginText();
        cs.setNonStrokingColor(15, 30, 24);
        cs.setFont(PDType1Font.HELVETICA_BOLD, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private static void writeKVDark(PDPageContentStream cs, float x, float y, String k, String v) throws Exception {
        // key
        cs.beginText();
        cs.setNonStrokingColor(100, 100, 100);
        cs.setFont(PDType1Font.HELVETICA, 11);
        cs.newLineAtOffset(x, y);
        cs.showText(k + " :");
        cs.endText();

        // value
        cs.beginText();
        cs.setNonStrokingColor(25, 25, 25);
        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
        cs.newLineAtOffset(x + 110, y);
        cs.showText(v);
        cs.endText();
    }

    private static void writeQrPng(String text, int w, int h, File out) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bm = writer.encode(text, BarcodeFormat.QR_CODE, w, h);
        BufferedImage img = MatrixToImageWriter.toBufferedImage(bm);
        ImageIO.write(img, "png", out);
    }
}