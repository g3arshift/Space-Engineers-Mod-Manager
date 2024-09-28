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

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 * @author Gear Shift
 */
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

    private final UiService UI_SERVICE;

    private final ProfileInputView PROFILE_INPUT_VIEW;

    private MenuBarView menuBarView;

    private final ObservableList<ModProfile> MOD_PROFILES;

    public ModProfileManagerView(UiService UI_SERVICE, ProfileInputView PROFILE_INPUT_VIEW) {
        this.UI_SERVICE = UI_SERVICE;
        MOD_PROFILES = UI_SERVICE.getMOD_PROFILES();
        this.PROFILE_INPUT_VIEW = PROFILE_INPUT_VIEW;
    }

    public void initView(Parent root, Properties properties, MenuBarView mainWindowView) {
        Scene scene = new Scene(root);
        this.menuBarView = mainWindowView;
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        stage.setTitle("Mod Profile Manager");
        stage.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResourceAsStream("/icons/logo.png"))));

        stage.setMinWidth(Double.parseDouble(properties.getProperty("semm.profileView.resolution.minWidth")));
        stage.setMinHeight(Double.parseDouble(properties.getProperty("semm.profileView.resolution.minHeight")));

        profileList.setItems(MOD_PROFILES);
        profileList.setCellFactory(param -> new ModProfileCell());

        profileList.setStyle("-fx-background-color: -color-bg-default;");

        stage.setScene(scene);
        UI_SERVICE.logPrivate("Successfully initialized mod profile manager.", MessageType.INFO);
    }

    @FXML
    private void createNewProfile() {
        boolean duplicateProfileName;

        do {
            PROFILE_INPUT_VIEW.getProfileNameInput().requestFocus();
            PROFILE_INPUT_VIEW.getStage().showAndWait();
            ModProfile newModProfile = new ModProfile(PROFILE_INPUT_VIEW.getProfileNameInput().getText());
            duplicateProfileName = profileNameExists(PROFILE_INPUT_VIEW.getProfileNameInput().getText());

            if (duplicateProfileName) {
                Popup.displaySimpleAlert("Profile name already exists!", stage, MessageType.WARN);
            } else if (!PROFILE_INPUT_VIEW.getProfileNameInput().getText().isBlank()) {
                MOD_PROFILES.add(newModProfile);
                UI_SERVICE.log("Successfully created profile " + PROFILE_INPUT_VIEW.getProfileNameInput().getText(), MessageType.INFO);
                PROFILE_INPUT_VIEW.getProfileNameInput().clear();
                UI_SERVICE.saveUserData();
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

        MOD_PROFILES.add(copyProfile);
        UI_SERVICE.saveUserData();
    }

    @FXML
    private void removeProfile() {
        if (UI_SERVICE.getCurrentModProfile().equals(profileList.getSelectionModel().getSelectedItem())) {
            Popup.displaySimpleAlert("You cannot remove the active profile.", stage, MessageType.WARN);
        } else {
            int choice = Popup.displayYesNoDialog("Are you sure you want to delete this profile?", stage, MessageType.WARN);
            if (choice == 1) {
                int profileIndex = profileList.getSelectionModel().getSelectedIndex();
                MOD_PROFILES.remove(profileIndex);
                if (profileIndex > MOD_PROFILES.size()) {
                    profileList.getSelectionModel().select(profileIndex - 1);
                } else {
                    profileList.getSelectionModel().select(profileIndex);
                }
                UI_SERVICE.saveUserData();
            }
        }
    }

    @FXML
    private void renameProfile() {
        boolean duplicateProfileName;

        do {
            PROFILE_INPUT_VIEW.getProfileNameInput().clear();
            PROFILE_INPUT_VIEW.getProfileNameInput().requestFocus();
            PROFILE_INPUT_VIEW.getStage().showAndWait();
            duplicateProfileName = profileNameExists(PROFILE_INPUT_VIEW.getProfileNameInput().getText());

            if (duplicateProfileName) {
                Popup.displaySimpleAlert("Profile name already exists!", stage, MessageType.WARN);
            } else if (!PROFILE_INPUT_VIEW.getProfileNameInput().getText().isBlank()) {
                //We retrieve the index here instead of the item itself as an observable list only updates when you update it, not the list underlying it.
                int profileIndex = profileList.getSelectionModel().getSelectedIndex();
                MOD_PROFILES.get(profileIndex).setProfileName(PROFILE_INPUT_VIEW.getProfileNameInput().getText());

                //We manually refresh here because the original profile won't update its name while it's selected in the list
                profileList.refresh();

                //If we don't do this then the mod profile dropdown in the main window won't show the renamed profile if we rename the active profile
                menuBarView.getModProfileDropdown().getSelectionModel().selectNext();
                menuBarView.getModProfileDropdown().getSelectionModel().selectPrevious();

                PROFILE_INPUT_VIEW.getProfileNameInput().clear();
                UI_SERVICE.log("Successfully renamed profile.", MessageType.INFO);
                UI_SERVICE.saveUserData();
            }
        } while (duplicateProfileName);
    }

    @FXML
    private void selectProfile() {
        UI_SERVICE.setCurrentModProfile(profileList.getSelectionModel().getSelectedItem());
        menuBarView.getModProfileDropdown().getSelectionModel().select(profileList.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void closeProfileWindow() {
        stage.close();
    }

    private boolean profileNameExists(String profileName) {
        return MOD_PROFILES.stream()
                .anyMatch(modProfile -> modProfile.getProfileName().equals(profileName));
    }
}
