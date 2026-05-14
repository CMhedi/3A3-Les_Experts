package Entities;

import java.time.LocalDateTime;

/** Ligne agrégée pour l'écran admin messagerie (liste + stats). */
public record AdminConversationListItem(
        int idConversation,
        String titre,
        int estGroupe,
        String createurNom,
        int nbParticipants,
        int nbMessages,
        LocalDateTime dateCreation,
        String dernierMessage
) {}
