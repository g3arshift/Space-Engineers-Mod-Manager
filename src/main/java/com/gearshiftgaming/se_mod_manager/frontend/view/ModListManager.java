package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.ModListProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.SpaceEngineersVersion;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.ModListManagerCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.utility.ModImportUtility;
import com.gearshiftgaming.se_mod_manager.frontend.view.helper.ModListManagerHelper;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
//TODO: This needs refactored to share a common class with the save profile manager. They really share a LOT of stuff.
public class ModListManager {
    @FXML
    private ListView<ModListProfile> profileList;

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
    private Label activeProfileName;

    @FXML
    private Button closeProfileWindow;

    @Getter
    private Stage stage;

    private final UiService UI_SERVICE;

    private final SimpleInput PROFILE_INPUT_VIEW;

    private ModTableContextBar modTableContextBar;

    private final ObservableList<ModListProfile> MOD_PROFILES;

    private final Pane[] TUTORIAL_HIGHLIGHT_PANES;

    public ModListManager(UiService UI_SERVICE, SimpleInput PROFILE_INPUT_VIEW) {
        this.UI_SERVICE = UI_SERVICE;
        MOD_PROFILES = UI_SERVICE.getMODLIST_PROFILES();
        this.PROFILE_INPUT_VIEW = PROFILE_INPUT_VIEW;
        PROFILE_INPUT_VIEW.setTitle("Create Mod List");
        PROFILE_INPUT_VIEW.setInputInstructions("Mod list name");
        PROFILE_INPUT_VIEW.setEmptyTextMessage("Mod list name cannot be empty!");
        TUTORIAL_HIGHLIGHT_PANES = UI_SERVICE.getHighlightPanes();
    }


    public void initView(Parent root, Properties properties, ModTableContextBar modTableContextBar) {
        Scene scene = new Scene(root);
        this.modTableContextBar = modTableContextBar;
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        stage.setTitle("Mod List Manager");
        WindowDressingUtility.appendStageIcon(stage);

        stage.setMinWidth(Double.parseDouble(properties.getProperty("semm.profileView.resolution.minWidth")));
        stage.setMinHeight(Double.parseDouble(properties.getProperty("semm.profileView.resolution.minHeight")));

        profileList.setItems(MOD_PROFILES);
        profileList.setCellFactory(param -> new ModListManagerCell(UI_SERVICE));
        profileList.setStyle("-fx-background-color: -color-bg-default;");

        stage.setScene(scene);
        stage.setOnCloseRequest(windowEvent -> Platform.exitNestedEventLoop(stage, null));

        UI_SERVICE.logPrivate("Successfully initialized mod profile manager.", MessageType.INFO);
    }

    @FXML
    private void createNewProfile() {
        if (!UI_SERVICE.getUSER_CONFIGURATION().isRunFirstTimeSetup()) {
            ModImportUtility.createNewModProfile(UI_SERVICE, stage, PROFILE_INPUT_VIEW);
            profileList.getSelectionModel().selectLast();
            setActive();
        } else {
            String newProfileName;
            do {
                newProfileName = ModImportUtility.createNewModProfile(UI_SERVICE, stage, PROFILE_INPUT_VIEW);
                if (newProfileName.isBlank()) {
                    Popup.displaySimpleAlert("You have to create a new mod list for the tutorial!", stage, MessageType.WARN);
                } else {
                    profileList.getSelectionModel().selectLast();
                    setActive();
                }
            } while (newProfileName.isBlank());
            List<String> tutorialMessages = new ArrayList<>();
            profileList.getSelectionModel().selectLast();
            tutorialMessages.add("When a profile is created it's automatically set as the active profile, but if you need to change the active profile click the \"Set Active\" button. Try this now.");
            Popup.displayNavigationDialog(tutorialMessages, stage, MessageType.INFO, "Managing Mod Lists");
            TutorialUtility.tutorialElementHighlight(TUTORIAL_HIGHLIGHT_PANES, stage.getWidth(), stage.getHeight(), selectProfile);
            selectProfile.requestFocus();
        }

        //TODO: Switch active profile to the new profile
    }

    @FXML
    private void copyProfile() {
        ModListProfile profileToCopy = profileList.getSelectionModel().getSelectedItem();
        if (profileToCopy != null) {
            int copyChoice = Popup.displayYesNoDialog(String.format("Are you sure you want to copy the mod list \"%s\"", profileToCopy.getProfileName()), stage, MessageType.WARN);
            if (copyChoice == 1) {
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

                int renameChoice = Popup.displayThreeChoiceDialog("Do you want to rename the copied profile?", stage, MessageType.INFO, "Yes", "No", "Cancel");

                if (renameChoice == 0) {
                    return;
                }

                if (renameChoice == 2) {
                    do {
                        PROFILE_INPUT_VIEW.getInput().setText(copyProfileName);
                        PROFILE_INPUT_VIEW.getInput().requestFocus();
                        PROFILE_INPUT_VIEW.getInput().selectAll();
                        PROFILE_INPUT_VIEW.show(stage);

                        copyProfileName = PROFILE_INPUT_VIEW.getInput().getText();
                        duplicateProfileName = profileNameExists(copyProfileName);
                        if (duplicateProfileName) {
                            Popup.displaySimpleAlert("Profile name already exists!", stage, MessageType.WARN);
                        } //There's an implicit else here that if you hit the cancel button on the rename the entire process will cancel
                    } while (duplicateProfileName);
                } else {
                    do {
                        duplicateProfileName = profileNameExists(copyProfileName);
                        if (duplicateProfileName) {
                            copyIndex++;
                        }
                        int copyIndexStringLength = 2 + (String.valueOf(copyIndex).length());
                        copyProfileName = String.format("%s (%d)", copyProfileName.substring(0, copyProfileName.length() - copyIndexStringLength).trim(), copyIndex);
                    } while (duplicateProfileName);
                }

                if (!copyProfileName.isBlank()) {
                    ModListProfile copyProfile = new ModListProfile(profileList.getSelectionModel().getSelectedItem());
                    copyProfile.setProfileName(copyProfileName);

                    MOD_PROFILES.add(copyProfile);
                    UI_SERVICE.saveUserData();

                    Popup.displaySimpleAlert("Successfully copied mod list!", stage, MessageType.INFO);
                }
            }
        } else {
            Popup.displaySimpleAlert("You have to select a profile first!", stage, MessageType.ERROR);
        }
    }

    @FXML
    private void removeProfile() {
        if (profileList.getSelectionModel().getSelectedItem() != null) {
            if (UI_SERVICE.getCurrentModListProfileProfile().equals(profileList.getSelectionModel().getSelectedItem())) {
                Popup.displaySimpleAlert("You cannot remove the active profile.", stage, MessageType.WARN);
            } else {
                int choice = Popup.displayYesNoDialog("Are you sure you want to delete this profile?", stage, MessageType.WARN);
                if (choice == 1) {
                    int profileIndex = profileList.getSelectionModel().getSelectedIndex();
                    MOD_PROFILES.remove(profileIndex);

                    UI_SERVICE.saveUserData();
                    if (profileIndex > MOD_PROFILES.size())
                        profileList.getSelectionModel().select(MOD_PROFILES.size() - 1);
                    else
                        profileList.getSelectionModel().select(profileIndex);
                }
            }
        }
    }

    @FXML
    private void renameProfile() {
        boolean duplicateProfileName;

        do {
            ModListProfile selectedProfile = profileList.getSelectionModel().getSelectedItem();
            if (selectedProfile != null) {
                PROFILE_INPUT_VIEW.getInput().setText(selectedProfile.getProfileName());
                PROFILE_INPUT_VIEW.getInput().requestFocus();
                PROFILE_INPUT_VIEW.getInput().selectAll();
                PROFILE_INPUT_VIEW.show(stage);

                String newProfileName = PROFILE_INPUT_VIEW.getInput().getText();
                duplicateProfileName = profileNameExists(newProfileName);

                if (duplicateProfileName) {
                    Popup.displaySimpleAlert("Profile name already exists!", stage, MessageType.WARN);
                } else if (!newProfileName.isBlank()) {
                    //We retrieve the index here instead of the item itself as an observable list only updates when you update it, not the list underlying it.
                    int profileIndex = profileList.getSelectionModel().getSelectedIndex();
                    String originalProfileName = MOD_PROFILES.get(profileIndex).getProfileName();
                    ModListProfile profileToModify = MOD_PROFILES.get(profileIndex);
                    profileToModify.setProfileName(newProfileName);

                    //We manually refresh here because the original profile won't update its name while it's selected in the list
                    profileList.refresh();

                    //If we don't do this then the mod profile dropdown in the main window won't show the renamed profile if we rename the active profile
                    int modProfileDropdownSelectedIndex = modTableContextBar.getModProfileDropdown().getSelectionModel().getSelectedIndex();
                    if (modProfileDropdownSelectedIndex != MOD_PROFILES.size() - 1) {
                        modTableContextBar.getModProfileDropdown().getSelectionModel().selectNext();
                        modTableContextBar.getModProfileDropdown().getSelectionModel().selectPrevious();
                    } else if (MOD_PROFILES.size() == 1) {
                        MOD_PROFILES.add(new ModListProfile(SpaceEngineersVersion.SPACE_ENGINEERS_ONE));
                        modTableContextBar.getModProfileDropdown().getSelectionModel().selectNext();
                        modTableContextBar.getModProfileDropdown().getSelectionModel().selectPrevious();
                        MOD_PROFILES.removeLast();
                    } else {
                        modTableContextBar.getModProfileDropdown().getSelectionModel().selectPrevious();
                        modTableContextBar.getModProfileDropdown().getSelectionModel().selectNext();
                    }

                    if (profileToModify.equals(UI_SERVICE.getCurrentModListProfileProfile())) {
                        activeProfileName.setText(profileToModify.getProfileName());
                    }

                    UI_SERVICE.log(String.format("Successfully renamed mod profile \"%s\" to \"%s\".", originalProfileName, newProfileName), MessageType.INFO);
                    PROFILE_INPUT_VIEW.getInput().clear();
                    UI_SERVICE.saveUserData();
                }
            } else {
                duplicateProfileName = false;
            }
        } while (duplicateProfileName);
    }

    @FXML
    private void setActive() {
        if(!UI_SERVICE.getUSER_CONFIGURATION().isRunFirstTimeSetup()) {
            setModListActive();
        } else {
            setModListActive();
            List<String> tutorialMessages = new ArrayList<>();
            profileList.getSelectionModel().selectFirst();
            tutorialMessages.add("Now that we have a new mod list profile let's close the mod list manager so we can add a save profile. Press the \"Close\" button.");
            Popup.displayNavigationDialog(tutorialMessages, stage, MessageType.INFO, "Managing Mod Lists");
            TutorialUtility.tutorialElementHighlight(TUTORIAL_HIGHLIGHT_PANES, stage.getWidth(), stage.getHeight(), closeProfileWindow);
            closeProfileWindow.requestFocus();
        }
    }

    private void setModListActive() {
        if (profileList.getSelectionModel().getSelectedItem() != null) {
            ModListProfile modListProfile = profileList.getSelectionModel().getSelectedItem();
            modTableContextBar.getModProfileDropdown().getSelectionModel().select(modListProfile);
            activeProfileName.setText(modListProfile.getProfileName());
            profileList.refresh();
        }
    }

    @FXML
    private void closeProfileWindow() {
        if(!UI_SERVICE.getUSER_CONFIGURATION().isRunFirstTimeSetup()) {
            stage.close();
            stage.setHeight(stage.getHeight() - 1);
            profileList.getSelectionModel().clearSelection();
            Platform.exitNestedEventLoop(stage, null);
        } else {
            stage.close();
            Platform.exitNestedEventLoop(stage, null);
            stage.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, UI_SERVICE.getKEYBOARD_BUTTON_NAVIGATION_DISABLER());
            ((Pane) stage.getScene().getRoot()).getChildren().removeAll(TUTORIAL_HIGHLIGHT_PANES);

            //Reset the stage
            Parent root = stage.getScene().getRoot();
            stage.getScene().setRoot(new Group());
            stage = new Stage();
            stage.setScene(new Scene(root));
        }
    }

    //TODO: Refactor to being genericized  later. This is basically duplicated in ModlistManagerView's version of this function.
    @FXML
    private void importModlistFile() {
        FileChooser importChooser = new FileChooser();
        importChooser.setTitle("Import Modlist");
        importChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        importChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SEMM Modlists", "*.semm"));

        File savePath = importChooser.showOpenDialog(stage);

        if (savePath != null) {
            Result<ModListProfile> modlistProfileResult = UI_SERVICE.importModlist(savePath);
            if (modlistProfileResult.isSuccess()) {
                modTableContextBar.getModProfileDropdown().getSelectionModel().select(modlistProfileResult.getPayload());
                UI_SERVICE.setLastActiveModlistProfile(modlistProfileResult.getPayload().getID());
                profileList.refresh();
            }
            Popup.displaySimpleAlert(modlistProfileResult, stage);
        }
    }

    @FXML
    private void exportModlistFile() {
        if (profileList.getSelectionModel().getSelectedItem() != null) {
            ModListManagerHelper.exportModlistFile(stage, UI_SERVICE);
        }
    }

    //TODO: Modify this to exclude the profile we're renaming.
    private boolean profileNameExists(String profileName) {
        return MOD_PROFILES.stream()
                .anyMatch(modProfile -> modProfile.getProfileName().equals(profileName));
    }

    public void show(Stage parentStage) {
        profileList.refresh();
        stage.show();
        WindowPositionUtility.centerStageOnStage(stage, parentStage);
        WindowTitleBarColorUtility.SetWindowsTitleBar(stage);
        activeProfileName.setText(UI_SERVICE.getCurrentModListProfileProfile().getProfileName());
        Platform.enterNestedEventLoop(stage);
    }

    public void runTutorial(Stage parentStage) {
        //Reset the scene so we can undecorate the stage.
        Parent root = stage.getScene().getRoot();
        stage.getScene().setRoot(new Group());
        stage = new Stage();
        stage.setScene(new Scene(root));
        stage.initStyle(StageStyle.UNDECORATED);
        stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, UI_SERVICE.getKEYBOARD_BUTTON_NAVIGATION_DISABLER());

        ((Pane) stage.getScene().getRoot()).getChildren().addAll(TUTORIAL_HIGHLIGHT_PANES);
        createNewProfile.requestFocus();

        profileList.refresh();
        stage.show();
        WindowPositionUtility.centerStageOnStage(stage, parentStage);
        WindowTitleBarColorUtility.SetWindowsTitleBar(stage);
        activeProfileName.setText(UI_SERVICE.getCurrentModListProfileProfile().getProfileName());

        List<String> tutorialMessages = new ArrayList<>();
        tutorialMessages.add("This is the Mod List Manager. Here you can manage the mod lists that you can apply to saves.");
        tutorialMessages.add("You can copy, rename, and remove mod lists here. You can also export mod lists to a file for backup or sharing purposes and import those files here.");
        tutorialMessages.add("The mod list that is currently active will be highlighted by blue bars above and below it. While a mod list is active it cannot be removed.");
        tutorialMessages.add("Let's create a new mod list. Press the \"Create New\" button then put in any profile name you'd like.");
        Popup.displayNavigationDialog(tutorialMessages, stage, MessageType.INFO, "Managing Mod Lists");
        TutorialUtility.tutorialElementHighlight(TUTORIAL_HIGHLIGHT_PANES, stage.getWidth(), stage.getHeight(), createNewProfile);

        Platform.enterNestedEventLoop(stage);
    }
}
