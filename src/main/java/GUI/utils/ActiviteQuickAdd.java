package GUI.utils;

import Models.Activite;
import controllers.ActiviteQuickAddDialogController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import Services.interfaces.ActiviteService;

import java.net.URL;

/**
 * Ouvre le dialogue d'activité (ajout rapide, création pro, modification) via {@link ActiviteService}.
 */
public final class ActiviteQuickAdd {

    public enum FormMode {
        /** Champs essentiels, sans pack ni GPS */
        ADD_BASIC,
        /** Formule catalogue + coordonnées GPS optionnelles */
        ADD_PRO,
        /** Relecture base + mise à jour avec résumé des changements */
        EDIT
    }

    private ActiviteQuickAdd() {
    }

    /**
     * Ajout rapide (comportement historique).
     */
    public static void openDialog(Window owner, Runnable onSaved) {
        open(owner, FormMode.ADD_BASIC, null, onSaved);
    }

    public static void open(Window owner, FormMode mode, Integer idActiviteIfEdit, Runnable onSaved) {
        try {
            FormMode m = mode == null ? FormMode.ADD_BASIC : mode;
            if (m == FormMode.EDIT) {
                if (idActiviteIfEdit == null || idActiviteIfEdit <= 0) {
                    return;
                }
                Activite probe = new ActiviteService().getById(idActiviteIfEdit);
                if (probe == null) {
                    DialogUtils.showError("Introuvable", "Cette activité n'existe pas ou a été supprimée.");
                    return;
                }
            }

            URL url = ActiviteQuickAdd.class.getResource("/GUI/ActiviteQuickAddDialog.fxml");
            if (url == null) {
                return;
            }
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            ActiviteQuickAddDialogController c = loader.getController();
            if (c == null) {
                return;
            }
            c.setOnSaved(onSaved);
            if (!c.configure(m, idActiviteIfEdit)) {
                return;
            }

            Stage stage = new Stage();
            stage.setTitle(switch (m) {
                case ADD_BASIC -> "Ajout rapide — activité";
                case ADD_PRO -> "Création professionnelle — activité";
                case EDIT -> "Modifier l'activité";
            });
            stage.initModality(Modality.APPLICATION_MODAL);
            if (owner != null) {
                stage.initOwner(owner);
            }
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
