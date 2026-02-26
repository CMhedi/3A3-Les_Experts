package Utiles;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.InternetAddress;

import java.util.Properties;
// etc.

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
            // ✅ Tasli7 el Message (javax.mail.Message)
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(myEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Récupération de mot de passe - EcoAdventure");
            message.setText("Votre code de vérification est : " + otpCode);

            Transport.send(message);
            System.out.println("✅ Email envoyé avec succès !");
        } catch (MessagingException e) {
            System.out.println("❌ Erreur d'envoi: " + e.getMessage());
            e.printStackTrace();
        }
    }
}