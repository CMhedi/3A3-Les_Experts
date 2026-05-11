package GUI;

import Entities.UserApp;
import enums.RoleUser;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.time.format.DateTimeFormatter;

public class UserDetailsPopupController {

    @FXML private Label lblInitials, lblName, lblRole, lblEmail, lblPhone, lblDate;
    @FXML private VBox coachInfoBox;
    @FXML private Label lblAge, lblExp, lblSpec;

    public void initData(UserApp user) {
        if (user == null) return;

        String initials = user.getNom().substring(0, 1).toUpperCase();
        if (user.getPrenom() != null && !user.getPrenom().isEmpty()) {
            initials = user.getPrenom().substring(0, 1).toUpperCase() + initials;
        }
        lblInitials.setText(initials);
        lblName.setText(user.getPrenom() + " " + user.getNom());
        
        String roleText = user.getRole() != null ? user.getRole().name() : "USER_SIMPLE";
        lblRole.setText(roleText);
        if ("ADMIN".equals(roleText)) {
            lblRole.setStyle(lblRole.getStyle() + "-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626;");
        }

        lblEmail.setText(user.getEmail());
        lblPhone.setText(user.getTelephone() != null ? user.getTelephone() : "Non renseigné");
        
        if (user.getDateCreation() != null) {
            lblDate.setText(user.getDateCreation().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        } else {
            lblDate.setText("Inconnue");
        }

        if (user.getRole() == RoleUser.COACH) {
            coachInfoBox.setVisible(true);
            coachInfoBox.setManaged(true);
            lblAge.setText(user.getAge() + " ans");
            lblExp.setText(user.getExperience() + " ans");
            lblSpec.setText(user.getSpecialite());
        }
    }

    @FXML
    private void handleClose() {
        ((Stage) lblName.getScene().getWindow()).close();
    }
}
