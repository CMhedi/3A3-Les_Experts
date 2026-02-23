package Services;

import Entities.Inscription;
import Entities.Pack;
import Entities.UserApp;

import java.time.LocalDate;
import java.util.Objects;

public class EcoWhatsAppTemplates {

    public enum Type { CONFIRMATION, UPDATE, CANCELED }

    public static String build(Type type, UserApp user, Pack pack, Inscription insc) {
        String userName = safe(getUserDisplayName(user));
        String packTitle = safe(getPackTitle(pack));
        String date = safe(getPackDate(pack));
        String status = safe(getInscriptionStatus(insc));
        String amount = safe(getInscriptionAmount(insc));

        return switch (type) {
            case CONFIRMATION -> """
                    EcoAdventure 🌿✅
                    Salut %s !
                    Ta réservation est confirmée.
                    Pack: %s
                    Date: %s
                    Montant: %s
                    Statut: %s
                    Merci pour une aventure responsable 💚
                    """.formatted(userName, packTitle, date, amount, status);

            case UPDATE -> """
                    EcoAdventure 🌿🔄
                    Salut %s !
                    Mise à jour de ton inscription.
                    Pack: %s
                    Date: %s
                    Montant: %s
                    Statut: %s
                    """.formatted(userName, packTitle, date, amount, status);

            case CANCELED -> """
                    EcoAdventure 🌿❌
                    Salut %s !
                    Ton inscription a été annulée.
                    Pack: %s
                    Date: %s
                    Si besoin, tu peux réserver un autre pack.
                    """.formatted(userName, packTitle, date);
        };
    }

    // --- safe getters (best-effort) ---
    private static String safe(String s) { return (s == null || s.isBlank()) ? "-" : s; }

    private static String getUserDisplayName(UserApp u) {
        try { return Objects.toString(u); } catch (Exception ignored) {}
        return "participant";
    }

    private static String getPackTitle(Pack p) {
        try { return (String) p.getClass().getMethod("getTitre").invoke(p); } catch (Exception ignored) {}
        try { return (String) p.getClass().getMethod("getNom").invoke(p); } catch (Exception ignored) {}
        return "Pack";
    }

    private static String getPackDate(Pack p) {
        try { return String.valueOf(p.getClass().getMethod("getDate").invoke(p)); } catch (Exception ignored) {}
        try { return String.valueOf(p.getClass().getMethod("getDateDebut").invoke(p)); } catch (Exception ignored) {}
        return LocalDate.now().toString();
    }

    private static String getInscriptionStatus(Inscription i) {
        try { return String.valueOf(i.getClass().getMethod("getStatut").invoke(i)); } catch (Exception ignored) {}
        return "PENDING";
    }

    private static String getInscriptionAmount(Inscription i) {
        try { return String.valueOf(i.getClass().getMethod("getMontant").invoke(i)); } catch (Exception ignored) {}
        try { return String.valueOf(i.getClass().getMethod("getPrix").invoke(i)); } catch (Exception ignored) {}
        return "-";
    }
}