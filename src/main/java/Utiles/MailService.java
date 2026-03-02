package Utiles;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.InternetAddress;

import java.util.Properties;


public class MailService {
    public static void sendOTP(String recipientEmail, String otpCode) {
        // 1. Configuration mta3 Gmail
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2"); // Zid hedhi bech t'dhamen el sécurité

        // 2. Credentials
        String myEmail = "salmalahmar5@gmail.com";
        String password = "ndjn tmoz wgih kfqx"; // Lezem App Password mel Google

        // ✅ Tasli7 el Session wel Authenticator (javax.mail)
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(myEmail, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(myEmail, "EcoAdventure Support")); // Zidna Ism el App
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("🔒 Code de Vérification - EcoAdventure");

            // El Design mta3 l'email b HTML/CSS
            String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 10px; padding: 20px;'>"
                    + "<h2 style='color: #2ecc71; text-align: center;'>EcoAdventure</h2>"
                    + "<p>Bonjour,</p>"
                    + "<p>Vous avez demandé la récupération de votre mot de passe. Voici votre code de vérification :</p>"
                    + "<div style='background-color: #f4f4f4; padding: 15px; text-align: center; border-radius: 5px;'>"
                    + "  <span style='font-size: 24px; font-weight: bold; letter-spacing: 5px; color: #333;'>" + otpCode + "</span>"
                    + "</div>"
                    + "<p style='margin-top: 20px;'> Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.</p>"
                    + "<hr style='border: 0; border-top: 1px dotted #ccc; margin: 20px 0;'>"
                    + "<p style='font-size: 12px; color: #888; text-align: center;'>L'équipe EcoAdventure - Explorez la nature avec nous.</p>"
                    + "</div>";

            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✅ Email professionnel envoyé !");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}