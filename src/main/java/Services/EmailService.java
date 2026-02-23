package Services;

import enums.StatutPack;
import enums.TypePack;
import jakarta.mail.*;
import jakarta.mail.Flags.Flag;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.FlagTerm;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class EmailService {

    /* =========================================================
       EXECUTORS (send + read) to avoid blocking JavaFX UI
       ========================================================= */
    private static final ExecutorService EXEC_SEND = Executors.newSingleThreadExecutor();
    private static final ExecutorService EXEC_READ = Executors.newSingleThreadExecutor();

    /* =========================================================
       SMTP CONFIG (SEND)
       ========================================================= */
    private static final String SMTP_HOST = getEnv("EA_SMTP_HOST", "smtp.gmail.com");
    private static final int SMTP_PORT = Integer.parseInt(getEnv("EA_SMTP_PORT", "587"));

    private static final String SMTP_USER = getEnv("EA_SMTP_USER", "hedicheikh14@gmail.com");
    private static final String SMTP_PASS = getEnvPass("EA_SMTP_PASS", "iyxr btso iocx qfqo"); // App Password (sans espaces)

    private static final String ADMIN_EMAIL = getEnv("EA_ADMIN_EMAIL", "hedicheikh14@gmail.com");

    /* =========================================================
       IMAP CONFIG (READ DRAFT FROM ADMIN MAIL)
       ========================================================= */
    private static final String IMAP_HOST = getEnv("EA_IMAP_HOST", "imap.gmail.com");
    private static final int IMAP_PORT = Integer.parseInt(getEnv("EA_IMAP_PORT", "993"));

    // Inbox account that the APP will read (can be same as SMTP_USER)
    private static final String IMAP_USER = getEnv("EA_IMAP_USER", "hedicheikh14@gmail.com");
    private static final String IMAP_PASS = getEnvPass("EA_IMAP_PASS", "iyxr btso iocx qfqo"); // App Password (sans espaces)

    // Subject that admin uses from phone
    private static final String PACK_FORM_SUBJECT = getEnv("EA_PACK_FORM_SUBJECT", "EA_PACK_FORM");

    // Optional security: only accept drafts from this sender (admin). If empty => accept any sender.
    private static final String PACK_FORM_ALLOWED_SENDER = getEnv("EA_PACK_FORM_ALLOWED_SENDER", ADMIN_EMAIL);

    /* =========================================================
       ENV HELPERS
       ========================================================= */
    private static String getEnv(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.trim().isEmpty()) ? fallback : v.trim();
    }

    // Gmail App Password is often shown with spaces: "abcd efgh ijkl mnop"
    // We remove spaces for reliability.
    private static String getEnvPass(String key, String fallback) {
        String v = System.getenv(key);
        if (v == null || v.trim().isEmpty()) v = fallback;
        return (v == null) ? "" : v.replace(" ", "").trim();
    }

    private static void assertSmtpConfigured() {
        if (SMTP_USER.isEmpty() || SMTP_PASS.isEmpty()) {
            throw new IllegalStateException("SMTP non configuré: définis EA_SMTP_USER et EA_SMTP_PASS (App Password).");
        }
    }

    private static void assertImapConfigured() {
        if (IMAP_USER.isEmpty() || IMAP_PASS.isEmpty()) {
            throw new IllegalStateException("IMAP non configuré: définis EA_IMAP_USER et EA_IMAP_PASS (App Password).");
        }
    }

    /* =========================================================
       1) SEND EMAIL TO ADMIN (Pack validation)
       ========================================================= */
    public static void sendPackValidationAsync(String action, int packId, String packTitre, double prix) {
        EXEC_SEND.submit(() -> {
            try {
                sendPackValidation(action, packId, packTitre, prix);
            } catch (Exception e) {
                System.err.println("[EmailService] Envoi mail échoué: " + e.getMessage());
            }
        });
    }

    private static void sendPackValidation(String action, int packId, String packTitre, double prix)
            throws MessagingException, UnsupportedEncodingException {

        assertSmtpConfigured();

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(SMTP_PORT));
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", SMTP_HOST);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
            }
        });

        String subject = "[EcoAdventure] Validation Pack: " + action + " (ID=" + packId + ")";
        String body =
                "Bonjour Admin,\n\n" +
                        "Une demande de validation a été générée.\n\n" +
                        "Action : " + action + "\n" +
                        "Pack ID : " + packId + "\n" +
                        "Titre   : " + packTitre + "\n" +
                        "Prix    : " + prix + "\n" +
                        "Date    : " + LocalDateTime.now() + "\n\n" +
                        "Merci de valider ce pack depuis le back-office.\n\n" +
                        "— EcoAdventure System";

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(SMTP_USER, "EcoAdventure"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(ADMIN_EMAIL));
        msg.setSubject(subject);
        msg.setText(body);

        Transport.send(msg);
        System.out.println("[EmailService] Mail envoyé à l'admin: " + ADMIN_EMAIL);
    }

    /* =========================================================
       2) READ ADMIN MAIL => BUILD PackMailDraft (fill JavaFX form)
       ========================================================= */

    /** Draft object to fill your PackForm fields. */
    public static class PackMailDraft {
        public int id; // 0 => creation
        public String nom;
        public TypePack type;
        public BigDecimal prixBase;
        public BigDecimal reduction;
        public int max;
        public StatutPack statut;

        public String from;
        public LocalDateTime receivedAt;

        @Override
        public String toString() {
            return "PackMailDraft{id=" + id + ", nom='" + nom + "', type=" + type +
                    ", prixBase=" + prixBase + ", reduction=" + reduction +
                    ", max=" + max + ", statut=" + statut + ", from='" + from + "'}";
        }
    }

    /**
     * Read the latest unread email with subject PACK_FORM_SUBJECT, parse it, mark it as READ,
     * and return the draft to fill your form.
     *
     * Mail format (admin from phone):
     * Subject: EA_PACK_FORM
     * Body:
     * ID: 0
     * NOM: Pack Été
     * TYPE: (must match enum)
     * PRIX_BASE: 50.00
     * REDUCTION: 5.00
     * MAX: 10
     * STATUT: (must match enum)
     */
    public static Optional<PackMailDraft> fetchLatestPackDraft() throws Exception {
        assertImapConfigured();

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", IMAP_HOST);
        props.put("mail.imaps.port", String.valueOf(IMAP_PORT));
        props.put("mail.imaps.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props);
        Store store = session.getStore("imaps");
        store.connect(IMAP_HOST, IMAP_USER, IMAP_PASS);

        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);

        // unread mails only
        Message[] unread = inbox.search(new FlagTerm(new Flags(Flag.SEEN), false));

        Message best = null;
        for (Message m : unread) {
            String subject = safe(m.getSubject()).trim();
            if (!subject.equalsIgnoreCase(PACK_FORM_SUBJECT)) continue;

            // optional: check sender
            if (!PACK_FORM_ALLOWED_SENDER.isEmpty()) {
                String from = getFromSafe(m);
                if (from != null && !from.toLowerCase(Locale.ROOT).contains(PACK_FORM_ALLOWED_SENDER.toLowerCase(Locale.ROOT))) {
                    continue;
                }
            }

            if (best == null) {
                best = m;
            } else {
                Date d1 = best.getSentDate();
                Date d2 = m.getSentDate();
                if (d1 == null || (d2 != null && d2.after(d1))) best = m;
            }
        }

        if (best == null) {
            inbox.close(true);
            store.close();
            return Optional.empty();
        }

        String body = extractText(best);
        Map<String, String> kv = parseKeyValues(body);

        PackMailDraft d = new PackMailDraft();
        d.id = parseInt(kv.getOrDefault("ID", "0"));
        d.nom = req(kv, "NOM");
        d.type = parseEnum(TypePack.class, req(kv, "TYPE"));
        d.prixBase = parseMoney(req(kv, "PRIX_BASE"));
        d.reduction = parseMoney(kv.getOrDefault("REDUCTION", "0"));
        d.max = parseInt(req(kv, "MAX"));
        d.statut = parseEnum(StatutPack.class, req(kv, "STATUT"));

        d.from = getFromSafe(best);
        d.receivedAt = LocalDateTime.now();

        // Mark as read so we don't re-import it
        best.setFlag(Flag.SEEN, true);

        inbox.close(true);
        store.close();

        return Optional.of(d);
    }

    /**
     * Async version (for JavaFX): callbacks called in background thread.
     * In controller, wrap UI updates with Platform.runLater(...)
     */
    public static void fetchLatestPackDraftAsync(Consumer<Optional<PackMailDraft>> onDone, Consumer<Exception> onError) {
        EXEC_READ.submit(() -> {
            try {
                Optional<PackMailDraft> d = fetchLatestPackDraft();
                if (onDone != null) onDone.accept(d);
            } catch (Exception e) {
                if (onError != null) onError.accept(e);
                else System.err.println("[EmailService] fetchLatestPackDraftAsync error: " + e.getMessage());
            }
        });
    }

    /* =========================================================
       IMAP HELPERS
       ========================================================= */
    private static String extractText(Message message) throws Exception {
        Object content = message.getContent();

        if (content instanceof String s) {
            return s;
        }

        if (content instanceof Multipart mp) {
            // prefer text/plain
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    Object c = bp.getContent();
                    return c == null ? "" : c.toString();
                }
            }
            // fallback: sometimes only html is present
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/html")) {
                    Object c = bp.getContent();
                    return stripHtml(c == null ? "" : c.toString());
                }
            }
            // last resort
            BodyPart bp0 = mp.getBodyPart(0);
            Object c0 = bp0.getContent();
            return c0 == null ? "" : c0.toString();
        }

        return "";
    }

    private static String stripHtml(String html) {
        // simple stripping (good enough for key:value lines)
        return html.replaceAll("(?s)<[^>]*>", "\n").replace("&nbsp;", " ").trim();
    }

    private static Map<String, String> parseKeyValues(String body) {
        Map<String, String> map = new HashMap<>();
        for (String line : safe(body).split("\\R")) {
            String l = line.trim();
            if (l.isEmpty() || !l.contains(":")) continue;
            int idx = l.indexOf(':');
            String k = l.substring(0, idx).trim().toUpperCase(Locale.ROOT);
            String v = l.substring(idx + 1).trim();
            map.put(k, v);
        }
        return map;
    }

    private static String req(Map<String, String> kv, String key) {
        String v = kv.get(key);
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException("Champ manquant dans le mail: " + key);
        }
        return v.trim();
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private static BigDecimal parseMoney(String raw) {
        String s = safe(raw).trim();
        if (s.isEmpty()) return BigDecimal.ZERO;
        return new BigDecimal(s.replace(',', '.'));
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumClass, String raw) {
        String r = safe(raw).trim();
        if (r.isEmpty()) throw new IllegalArgumentException("Valeur enum vide: " + enumClass.getSimpleName());

        // Try normalized valueOf
        String normalized = r.toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        try {
            return Enum.valueOf(enumClass, normalized);
        } catch (Exception ignored) {}

        // Try case-insensitive match
        for (E c : enumClass.getEnumConstants()) {
            if (c.name().equalsIgnoreCase(r)) return c;
        }

        // Error with allowed values
        StringBuilder allowed = new StringBuilder();
        for (E c : enumClass.getEnumConstants()) {
            if (!allowed.isEmpty()) allowed.append(", ");
            allowed.append(c.name());
        }
        throw new IllegalArgumentException("Valeur invalide pour " + enumClass.getSimpleName() +
                ": '" + r + "'. Valeurs possibles: " + allowed);
    }

    private static String getFromSafe(Message m) {
        try {
            Address[] from = m.getFrom();
            return (from != null && from.length > 0) ? from[0].toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }

    /* =========================================================
       SHUTDOWN
       ========================================================= */
    public static void shutdown() {
        EXEC_SEND.shutdownNow();
        EXEC_READ.shutdownNow();
    }
}