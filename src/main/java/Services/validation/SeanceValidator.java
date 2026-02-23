package Services.validation;

import Entities.Seance;
import exceptions.ValidationException;

public class SeanceValidator {

    public static void validate(Seance s) {

        // 1️⃣ Séance non null
        if (s == null) {
            throw new ValidationException("La séance ne peut pas être null");
        }

        // 2️⃣ Date obligatoire
        if (s.getDateSeance() == null) {
            throw new ValidationException("La date de la séance est obligatoire");
        }

        // 3️⃣ Heures obligatoires
        if (s.getHeureDebut() == null || s.getHeureFin() == null) {
            throw new ValidationException("Les heures de début et de fin sont obligatoires");
        }

        // 4️⃣ heure début < heure fin
        if (!s.getHeureDebut().isBefore(s.getHeureFin())) {
            throw new ValidationException("L'heure de début doit être avant l'heure de fin");
        }

        // 5️⃣ Capacité > 0
        if (s.getCapacite() <= 0) {
            throw new ValidationException("La capacité doit être strictement positive");
        }

        // 6️⃣ Planning obligatoire
        if (s.getIdPlanning() <= 0) {
            throw new ValidationException("Un planning valide est obligatoire");
        }

        // 7️⃣ Coach obligatoire
        if (s.getIdCoach() <= 0) {
            throw new ValidationException("Un coach valide est obligatoire");
        }

        // 8️⃣ Statut obligatoire
        if (s.getStatutSeance() == null) {
            throw new ValidationException("Le statut de la séance est obligatoire");
        }
    }
}
