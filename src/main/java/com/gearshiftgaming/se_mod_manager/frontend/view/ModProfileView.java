package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.ModProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

public class ModProfileView {

    @FXML
    private ListView<ModProfile> profileList;

    @FXML
    private Button createNewProfile;

    @FXML
    private Button copyProfile;

    @FXML
    private Button removeProfile;

    @FXML
    private Button renameProfile;

    @FXML
    private Button selectProfile;

    @FXML
    private Button closeProfileWindow;

    @Getter
    private Stage stage;

    private Scene scene;

    private ObservableList<ModProfile> modProfiles;

    private UiService uiService;

    @Setter
    private UserConfiguration userConfiguration;

    public void initController(ObservableList<ModProfile> modProfiles, Parent root, UiService uiService) {
        this.modProfiles = modProfiles;
        this.scene = new Scene(root);
        this.uiService = uiService;
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        profileList.setItems(modProfiles);

        stage.setScene(scene);
    }

    @FXML
    private void createNewProfile() {
        //TODO: Implement
    }

    @FXML
    private void copyProfile() {
        //TODO: Implement
    }

    @FXML
    private void removeProfile() {
        //TODO: Prevent users from removing active profile
        //TODO: Implement
    }

    @FXML
    private void renameProfile() {
        //TODO: Implement
    }

    @FXML
    private void selectProfile() {
        userConfiguration.setLastUsedSaveProfileId(profileList.getSelectionModel().getSelectedItem().getId());
    }

    @FXML
    private void closeProfileWindow() {
        stage.close();
    }

}
