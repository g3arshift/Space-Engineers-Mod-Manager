package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.ModProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.ModProfileCell;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

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

    private ModProfileInput modProfileInputView;

    public void initController(ObservableList<ModProfile> modProfiles, Parent root, UiService uiService, ModProfileInput modProfileInput, Properties properties) throws IOException, XmlPullParserException {
        this.modProfiles = modProfiles;
        this.scene = new Scene(root);
        this.uiService = uiService;
        this.modProfileInputView = modProfileInput;
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        stage.setTitle("Mod Profile Manager");
        stage.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResourceAsStream("/icons/logo.png"))));

        stage.setMinWidth(Double.parseDouble(properties.getProperty("semm.profileView.resolution.minWidth")));
        stage.setMinHeight(Double.parseDouble(properties.getProperty("semm.profileView.resolution.minHeight")));

        profileList.setItems(modProfiles);
        profileList.setCellFactory(param -> new ModProfileCell());

        profileList.setStyle("-fx-background-color: -color-bg-default;");

        stage.setScene(scene);
    }

    @FXML
    private void createNewProfile() {
        boolean duplicateProfileName;

        //TODO: When clicking the close in the top right corner of the window it runs our code anyways. We want it to just cancel.

        do {
            modProfileInputView.getStage().showAndWait();
            ModProfile newModProfile = new ModProfile(modProfileInputView.getProfileNameInput().getText());
            duplicateProfileName = modProfiles.stream()
                    .anyMatch(modProfile -> modProfile.getProfileName().equals(modProfileInputView.getProfileNameInput().getText()));

            if (duplicateProfileName) {
                Alert.display("Profile name already exists!", stage, MessageType.WARN);
            } else if(!modProfileInputView.getProfileNameInput().getText().isBlank()){
                modProfiles.add(newModProfile);
                uiService.log("Successfully created profile " + modProfileInputView.getProfileNameInput().getText(), MessageType.INFO);
                modProfileInputView.getProfileNameInput().clear();
            }
        } while(duplicateProfileName);
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
        boolean duplicateProfileName;

        //TODO: When clicking the close in the top right corner of the window it runs our code anyways. We want it to just cancel.

        do {
            modProfileInputView.getProfileNameInput().clear();
            modProfileInputView.getStage().showAndWait();
            duplicateProfileName = modProfiles.stream()
                    .anyMatch(modProfile -> modProfile.getProfileName().equals(modProfileInputView.getProfileNameInput().getText()));

            if (duplicateProfileName) {
                Alert.display("Profile name already exists!", stage, MessageType.WARN);
            } else if(!modProfileInputView.getProfileNameInput().getText().isBlank()){
                ModProfile selectedProfile = profileList.getSelectionModel().getSelectedItem();
                Objects.requireNonNull(modProfiles.stream()
                                .filter(modProfile -> selectedProfile.getId().equals(modProfile.getId()))
                                .findAny()
                                .orElse(null))
                        .setProfileName(modProfileInputView.getProfileNameInput().getText());

                //We manually refresh here because the original profile won't update its name while it's selected in the list
                profileList.refresh();
                modProfileInputView.getProfileNameInput().clear();
            }
        } while (duplicateProfileName);
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
