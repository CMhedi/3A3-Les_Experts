import Entities.Planning;
import Entities.Seance;
import Entities.UserApp;
import enums.RoleUser;
import enums.StatutSeance;
import Services.PlanningService;
import Services.SeanceService;
import Services.UserService;
import exceptions.ValidationException;

import java.time.LocalDate;
import java.time.LocalTime;

public class Main {

    public static void main(String[] args) {

        try {
            System.out.println("====== START VALIDATION TEST ======");

            // ===============================
            // 1️⃣ Planning VALIDE
            // ===============================
            PlanningService planningService = new PlanningService();

            Planning planning = new Planning();
            planning.setPeriode("Mars 2026");
            planning.setDescription("Planning valide");

            planningService.add(planning);
            System.out.println("✅ Planning valide ajouté");

            int planningId = planningService.getAll()
                    .get(planningService.getAll().size() - 1)
                    .getIdPlanning();

            // ===============================
            // 2️⃣ Coach VALIDE (User test)
            // ===============================
            UserService userService = new UserService();

            UserApp coach = new UserApp();
            coach.setNom("Test");
            coach.setPrenom("Coach");
            coach.setEmail("coach.validation@esprit.tn");
            coach.setTelephone("22222222");
            coach.setRole(RoleUser.COACH);
            coach.setMotDePasse("1234");

            userService.add(coach);
            System.out.println("✅ Coach valide ajouté");

            int coachId = userService.getAllCoachs()
                    .get(userService.getAllCoachs().size() - 1)
                    .getIdUser();

            // ===============================
            // 3️⃣ Séance VALIDE
            // ===============================
            SeanceService seanceService = new SeanceService();

            Seance seanceValide = new Seance();
            seanceValide.setDateSeance(LocalDate.now());
            seanceValide.setHeureDebut(LocalTime.of(9, 0));
            seanceValide.setHeureFin(LocalTime.of(10, 30));
            seanceValide.setCapacite(15);
            seanceValide.setStatutSeance(StatutSeance.PLANIFIEE);
            seanceValide.setIdPlanning(planningId);
            seanceValide.setIdCoach(coachId);

            seanceService.add(seanceValide);
            System.out.println("✅ Séance valide ajoutée");

            // ===============================
            // 4️⃣ TEST ERREURS (VALIDATION)
            // ===============================

            // ❌ heure début >= heure fin
            try {
                Seance s1 = new Seance();
                s1.setDateSeance(LocalDate.now());
                s1.setHeureDebut(LocalTime.of(11, 0));
                s1.setHeureFin(LocalTime.of(10, 0));
                s1.setCapacite(10);
                s1.setStatutSeance(StatutSeance.PLANIFIEE);
                s1.setIdPlanning(planningId);
                s1.setIdCoach(coachId);

                seanceService.add(s1);
            } catch (ValidationException e) {
                System.out.println("❌ Test heure invalide : " + e.getMessage());
            }

            // ❌ capacité <= 0
            try {
                Seance s2 = new Seance();
                s2.setDateSeance(LocalDate.now());
                s2.setHeureDebut(LocalTime.of(9, 0));
                s2.setHeureFin(LocalTime.of(10, 0));
                s2.setCapacite(0);
                s2.setStatutSeance(StatutSeance.PLANIFIEE);
                s2.setIdPlanning(planningId);
                s2.setIdCoach(coachId);

                seanceService.add(s2);
            } catch (ValidationException e) {
                System.out.println("❌ Test capacité invalide : " + e.getMessage());
            }

            // ❌ planning manquant
            try {
                Seance s3 = new Seance();
                s3.setDateSeance(LocalDate.now());
                s3.setHeureDebut(LocalTime.of(9, 0));
                s3.setHeureFin(LocalTime.of(10, 0));
                s3.setCapacite(10);
                s3.setStatutSeance(StatutSeance.PLANIFIEE);
                s3.setIdPlanning(0); // invalide
                s3.setIdCoach(coachId);

                seanceService.add(s3);
            } catch (ValidationException e) {
                System.out.println("❌ Test planning manquant : " + e.getMessage());
            }

            // ❌ coach manquant
            try {
                Seance s4 = new Seance();
                s4.setDateSeance(LocalDate.now());
                s4.setHeureDebut(LocalTime.of(9, 0));
                s4.setHeureFin(LocalTime.of(10, 0));
                s4.setCapacite(10);
                s4.setStatutSeance(StatutSeance.PLANIFIEE);
                s4.setIdPlanning(planningId);
                s4.setIdCoach(0); // invalide

                seanceService.add(s4);
            } catch (ValidationException e) {
                System.out.println("❌ Test coach manquant : " + e.getMessage());
            }

            System.out.println("====== VALIDATION TEST FINISHED ======");

        } catch (Exception e) {
            System.out.println("❌ ERREUR SYSTEME");
            e.printStackTrace();
        }
    }
}
