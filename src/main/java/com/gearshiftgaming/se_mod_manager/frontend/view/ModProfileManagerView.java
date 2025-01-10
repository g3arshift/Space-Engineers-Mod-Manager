package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.ModlistProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.ModProfileManagerCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.utility.ModImportUtility;
import com.gearshiftgaming.se_mod_manager.frontend.view.helper.ModlistManagerHelper;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.Popup;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.NativeWindowUtility;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.WindowDressingUtility;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;

import java.io.File;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ModProfileManagerView {

    @FXML
    private ListView<ModlistProfile> profileList;

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
    private Button importModlist;

    @FXML
    private Button exportModlist;

    @FXML
    private Button closeProfileWindow;

    @Getter
    private Stage stage;

    private final UiService UI_SERVICE;

    private final SimpleInputView PROFILE_INPUT_VIEW;

    private ModTableContextBarView modTableContextBarView;

    private final ObservableList<ModlistProfile> MOD_PROFILES;

    public ModProfileManagerView(UiService UI_SERVICE, SimpleInputView PROFILE_INPUT_VIEW) {
        this.UI_SERVICE = UI_SERVICE;
        MOD_PROFILES = UI_SERVICE.getMODLIST_PROFILES();
        this.PROFILE_INPUT_VIEW = PROFILE_INPUT_VIEW;
    }

    public void initView(Parent root, Properties properties, ModTableContextBarView modTableContextBarView) {
        Scene scene = new Scene(root);
        this.modTableContextBarView = modTableContextBarView;
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        stage.setTitle("Mod Profile Manager");
        WindowDressingUtility.appendStageIcon(stage);

        stage.setMinWidth(Double.parseDouble(properties.getProperty("semm.profileView.resolution.minWidth")));
        stage.setMinHeight(Double.parseDouble(properties.getProperty("semm.profileView.resolution.minHeight")));

        profileList.setItems(MOD_PROFILES);
        profileList.setCellFactory(param -> new ModProfileManagerCell(UI_SERVICE.getUSER_CONFIGURATION().getUserTheme()));
        profileList.setStyle("-fx-background-color: -color-bg-default;");

        stage.setScene(scene);
        stage.setOnCloseRequest(windowEvent -> Platform.exitNestedEventLoop(stage, null));

        UI_SERVICE.logPrivate("Successfully initialized mod profile manager.", MessageType.INFO);
    }

    @FXML
    private void createNewProfile() {
        ModImportUtility.createNewModProfile(UI_SERVICE, stage, PROFILE_INPUT_VIEW);
    }

    @FXML
    private void copyProfile() {
        ModlistProfile profileToCopy = profileList.getSelectionModel().getSelectedItem();
        if (profileToCopy != null) {
            int choice = Popup.displayYesNoDialog(String.format("Are you sure you want to copy the mod list \"%s\"", profileToCopy.getProfileName()), stage, MessageType.WARN);
            if (choice == 1) {
                boolean duplicateProfileName;
                int copyIndex = 1;
                String copyProfileName;

                //Prepare our copy string by removing any existing copy numbers.
                String endOfModlistName = profileToCopy.getProfileName().substring(profileToCopy.getProfileName().length() - 3);
                Pattern endOfModlistNameRegex = Pattern.compile("\\(([^d\\)]+)\\)");
                if (endOfModlistNameRegex.matcher(endOfModlistName).find()) { //Check if it ends with a (Number), so we can know if it was already a duplicate.
                    copyProfileName = profileToCopy.getProfileName();
                } else {
                    copyProfileName = String.format("%s (%d)", profileToCopy.getProfileName(), copyIndex);
                }

                do {
                    duplicateProfileName = profileNameExists(copyProfileName);
                    if (duplicateProfileName) {
                        copyIndex++;
                    }
                    int copyIndexStringLength = 2 + (String.valueOf(copyIndex).length());
                    copyProfileName = String.format("%s (%d)", copyProfileName.substring(0, copyProfileName.length() - copyIndexStringLength).trim(), copyIndex);
                } while (duplicateProfileName);

                ModlistProfile copyProfile = new ModlistProfile(profileList.getSelectionModel().getSelectedItem());
                copyProfile.setProfileName(copyProfileName);

                MOD_PROFILES.add(copyProfile);
                UI_SERVICE.saveUserData();

                Popup.displaySimpleAlert("Successfully copied mod list!", stage, MessageType.INFO);
            }
        } else {
            Popup.displaySimpleAlert("You have to select a profile first!", stage, MessageType.ERROR);
        }
    }

    @FXML
    private void removeProfile() {
        if (profileList.getSelectionModel().getSelectedItem() != null) {
            if (UI_SERVICE.getCurrentModlistProfile().equals(profileList.getSelectionModel().getSelectedItem())) {
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
    }

    @FXML
    private void renameProfile() {
        boolean duplicateProfileName;

        do {
            PROFILE_INPUT_VIEW.getInput().clear();
            PROFILE_INPUT_VIEW.getInput().requestFocus();
            if (profileList.getSelectionModel().getSelectedItem() != null) {
                PROFILE_INPUT_VIEW.show();

                duplicateProfileName = profileNameExists(PROFILE_INPUT_VIEW.getInput().getText());

                if (duplicateProfileName) {
                    Popup.displaySimpleAlert("Profile name already exists!", stage, MessageType.WARN);
                } else if (!PROFILE_INPUT_VIEW.getInput().getText().isBlank()) {
                    //We retrieve the index here instead of the item itself as an observable list only updates when you update it, not the list underlying it.
                    int profileIndex = profileList.getSelectionModel().getSelectedIndex();
                    MOD_PROFILES.get(profileIndex).setProfileName(PROFILE_INPUT_VIEW.getInput().getText());

                    //We manually refresh here because the original profile won't update its name while it's selected in the list
                    profileList.refresh();

                    //If we don't do this then the mod profile dropdown in the main window won't show the renamed profile if we rename the active profile
                    modTableContextBarView.getModProfileDropdown().getSelectionModel().selectNext();
                    modTableContextBarView.getModProfileDropdown().getSelectionModel().selectPrevious();

                    PROFILE_INPUT_VIEW.getInput().clear();
                    UI_SERVICE.log("Successfully renamed profile.", MessageType.INFO);
                    UI_SERVICE.saveUserData();
                }
            } else {
                duplicateProfileName = false;
            }
        } while (duplicateProfileName);
    }

    @FXML
    private void setActive() {
        if (profileList.getSelectionModel().getSelectedItem() != null) {
            ModlistProfile modlistProfile = profileList.getSelectionModel().getSelectedItem();
            UI_SERVICE.setCurrentModlistProfile(modlistProfile);
            modTableContextBarView.getModProfileDropdown().getSelectionModel().select(modlistProfile);
            UI_SERVICE.setLastActiveModlistProfile(modlistProfile.getID());
        }
    }

    @FXML
    private void closeProfileWindow() {
        stage.close();
        stage.setHeight(stage.getHeight() - 1);
        profileList.getSelectionModel().clearSelection();
        Platform.exitNestedEventLoop(stage, null);
    }

    //TODO: Refactor to being genercized later. This is basically duplicated in ModlistManagerView's version of this function.
    @FXML
    private void importModlistFile() {
        FileChooser importChooser = new FileChooser();
        importChooser.setTitle("Import Modlist");
        importChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        importChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SEMM Modlists", "*.semm"));

        File savePath = importChooser.showOpenDialog(stage);

        if (savePath != null) {
            Result<ModlistProfile> modlistProfileResult = UI_SERVICE.importModlist(savePath);
            if (modlistProfileResult.isSuccess()) {
                modTableContextBarView.getModProfileDropdown().getSelectionModel().select(modlistProfileResult.getPayload());
                UI_SERVICE.setLastActiveModlistProfile(modlistProfileResult.getPayload().getID());
            }
            Popup.displaySimpleAlert(modlistProfileResult, stage);
        }
    }

    @FXML
    private void exportModlistFile() {
        if (profileList.getSelectionModel().getSelectedItem() != null) {
            ModlistManagerHelper.exportModlistFile(stage, UI_SERVICE);
        }
    }

    private boolean profileNameExists(String profileName) {
        return MOD_PROFILES.stream()
                .anyMatch(modProfile -> modProfile.getProfileName().equals(profileName));
    }

    public void show() {
        stage.show();
        NativeWindowUtility.SetWindowsTitleBar(stage);
        Platform.enterNestedEventLoop(stage);
    }
}
