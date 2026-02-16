package Services.interfaces.validation;

import Entities.Planning;
import exceptions.ValidationException;

public class PlanningValidator {

    public static void validate(Planning p) {

        // 1️⃣ Planning non null
        if (p == null) {
            throw new ValidationException("Le planning ne peut pas être null");
        }

        // 2️⃣ Période obligatoire
        if (p.getPeriode() == null || p.getPeriode().trim().isEmpty()) {
            throw new ValidationException("La période du planning est obligatoire");
        }

        // 3️⃣ Longueur minimale
        if (p.getPeriode().trim().length() < 3) {
            throw new ValidationException("La période doit contenir au moins 3 caractères");
        }

        // 4️⃣ Description (optionnelle mais contrôlée)
        if (p.getDescription() != null && p.getDescription().length() > 500) {
            throw new ValidationException("La description ne doit pas dépasser 500 caractères");
        }
    }
}
