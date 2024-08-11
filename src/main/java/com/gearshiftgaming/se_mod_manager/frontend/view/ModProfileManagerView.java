package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.ModProfile;
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

import java.util.Objects;
import java.util.Properties;

public class ModProfileManagerView {

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

    private UiService uiService;

    private ModProfileInputView modProfileInputView;

    private MainWindowView mainWindowView;

    private ObservableList<ModProfile> modProfiles;

    public void initView(Parent root, UiService uiService, ModProfileInputView modProfileInputView, Properties properties, MainWindowView mainWindowView) {
        Scene scene = new Scene(root);
        this.uiService = uiService;
        modProfiles = uiService.getModProfiles();
        this.modProfileInputView = modProfileInputView;
        this.mainWindowView = mainWindowView;
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
        uiService.getLogger().info("Successfully initialized ModProfileView.");
    }

    @FXML
    private void createNewProfile() {
        boolean duplicateProfileName;

        do {
            modProfileInputView.getProfileNameInput().requestFocus();
            modProfileInputView.getStage().showAndWait();
            ModProfile newModProfile = new ModProfile(modProfileInputView.getProfileNameInput().getText());
            duplicateProfileName = profileNameExists(modProfileInputView.getProfileNameInput().getText());

            if (duplicateProfileName) {
                Popup.displaySimpleAlert("Profile name already exists!", stage, MessageType.WARN);
            } else if (!modProfileInputView.getProfileNameInput().getText().isBlank()) {
                modProfiles.add(newModProfile);
                uiService.log("Successfully created profile " + modProfileInputView.getProfileNameInput().getText(), MessageType.INFO);
                modProfileInputView.getProfileNameInput().clear();
            }
        } while (duplicateProfileName);
    }

    @FXML
    private void copyProfile() {
        boolean duplicateProfileName;
        String copyProfileName = profileList.getSelectionModel().getSelectedItem().getProfileName() + "_Copy";
        do {
            duplicateProfileName = profileNameExists(copyProfileName);
            if (duplicateProfileName) {
                copyProfileName += "_Copy";
            }
        } while (duplicateProfileName);

        ModProfile copyProfile = new ModProfile(profileList.getSelectionModel().getSelectedItem());
        copyProfile.setProfileName(copyProfileName);

        modProfiles.add(copyProfile);
    }

    @FXML
    private void removeProfile() {
        if (mainWindowView.getCurrentModProfile().equals(profileList.getSelectionModel().getSelectedItem())) {
            Popup.displaySimpleAlert("You cannot remove the active profile.", stage, MessageType.WARN);
        } else {
            int choice = Popup.displayYesNoDialog("Are you sure you want to delete this profile?", stage, MessageType.WARN);
            if (choice == 1) {
                int profileIndex = profileList.getSelectionModel().getSelectedIndex();
                modProfiles.remove(profileIndex);
                if (profileIndex > modProfiles.size()) {
                    profileList.getSelectionModel().select(profileIndex - 1);
                } else {
                    profileList.getSelectionModel().select(profileIndex);
                }
            }
        }
    }

    @FXML
    private void renameProfile() {
        boolean duplicateProfileName;

        do {
            modProfileInputView.getProfileNameInput().clear();
            modProfileInputView.getProfileNameInput().requestFocus();
            modProfileInputView.getStage().showAndWait();
            duplicateProfileName = profileNameExists(modProfileInputView.getProfileNameInput().getText());

            if (duplicateProfileName) {
                Popup.displaySimpleAlert("Profile name already exists!", stage, MessageType.WARN);
            } else if (!modProfileInputView.getProfileNameInput().getText().isBlank()) {
                //We retrieve the index here instead of the item itself as an observable list only updates when you update it, not the list underlying it.
                int profileIndex = profileList.getSelectionModel().getSelectedIndex();
                modProfiles.get(profileIndex).setProfileName(modProfileInputView.getProfileNameInput().getText());

                //We manually refresh here because the original profile won't update its name while it's selected in the list
                profileList.refresh();

                //If we don't do this then the mod profile dropdown in the main window won't show the renamed profile if we rename the active profile
                mainWindowView.getModProfileDropdown().getSelectionModel().selectNext();
                mainWindowView.getModProfileDropdown().getSelectionModel().selectPrevious();

                modProfileInputView.getProfileNameInput().clear();
                uiService.log("Successfully renamed profile.", MessageType.INFO);
            }
        } while (duplicateProfileName);
    }

    @FXML
    private void selectProfile() {
        mainWindowView.setCurrentModProfile(profileList.getSelectionModel().getSelectedItem());
        mainWindowView.getModProfileDropdown().getSelectionModel().select(profileList.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void closeProfileWindow() {
        stage.close();
    }

    private boolean profileNameExists(String profileName) {
        return modProfiles.stream()
                .anyMatch(modProfile -> modProfile.getProfileName().equals(profileName));
    }
}
