package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.*;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.SaveProfileManagerCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.utility.ModImportUtility;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.*;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;


/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */

public class SaveProfileManager {
    @FXML
    private ListView<SaveProfile> saveList;

    @FXML
    private Button addSave;

    @FXML
    private Button copySave;

    @FXML
    private Button removeSave;

    @FXML
    private Button renameProfile;

    @FXML
    private Button selectSave;

    @FXML
    private Label activeProfileName;

    @FXML
    private Button closeSaveWindow;

    @FXML
    private StackPane modImportProgressPanel;

    @FXML
    private ProgressBar modImportProgressBar;

    @FXML
    private Label modImportProgressDenominator;

    @FXML
    private Label modImportProgressDivider;

    @FXML
    private Label modImportProgressNumerator;

    @FXML
    private Label modImportProgressActionName;

    @FXML
    private Label saveCopyMessage;

    @FXML
    private ProgressIndicator modImportProgressWheel;

    private Stage stage;

    private final ObservableList<SaveProfile> SAVE_PROFILES;

    private final UiService UI_SERVICE;

    private final SaveInput SAVE_INPUT_VIEW;

    private final SimpleInput PROFILE_INPUT_VIEW;

    private ModTableContextBar modTableContextBar;

    public SaveProfileManager(UiService UI_SERVICE, SaveInput saveInput, SimpleInput simpleInput) {
        this.UI_SERVICE = UI_SERVICE;
        SAVE_PROFILES = UI_SERVICE.getSAVE_PROFILES();
        this.SAVE_INPUT_VIEW = saveInput;
        this.PROFILE_INPUT_VIEW = simpleInput;
    }

    public void initView(Parent root, Properties properties, ModTableContextBar modTableContextBar) {
        this.modTableContextBar = modTableContextBar;
        Scene scene = new Scene(root);

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        stage.setTitle("Save Manager");
        WindowDressingUtility.appendStageIcon(stage);

        stage.setMinWidth(Double.parseDouble(properties.getProperty("semm.profileView.resolution.minWidth")));
        stage.setMinHeight(Double.parseDouble(properties.getProperty("semm.profileView.resolution.minHeight")));

        saveList.setItems(SAVE_PROFILES);
        saveList.setCellFactory(param -> new SaveProfileManagerCell(UI_SERVICE));

        saveList.setStyle("-fx-background-color: -color-bg-default;");

        modImportProgressNumerator.textProperty().bind(UI_SERVICE.getModImportProgressNumeratorProperty().asString());
        modImportProgressDenominator.textProperty().bind(UI_SERVICE.getModImportProgressDenominatorProperty().asString());
        modImportProgressBar.progressProperty().bind(UI_SERVICE.getModImportProgressPercentageProperty());

        stage.setScene(scene);

        saveCopyMessage.setVisible(false);

        //stage.setOnCloseRequest(windowEvent -> Platform.exitNestedEventLoop(stage, null));

        UI_SERVICE.logPrivate("Successfully initialized save manager.", MessageType.INFO);
    }

    @FXML
    private void addSave() throws IOException {
        boolean duplicateSavePath = false;
        Result<SaveProfile> saveProfileResult;
        //Get our selected file from the user, check if its already being managed by SEMM by checking the save path, and then check if the save name already exists. If it does, append a number to the end of it.
        do {
            SAVE_INPUT_VIEW.setSaveProfileInputTitle("Add new SE save");
            SAVE_INPUT_VIEW.setAddSaveButtonText("Next");
            SAVE_INPUT_VIEW.show(stage);
            File selectedSave = SAVE_INPUT_VIEW.getSelectedSave();
            if (selectedSave != null && SAVE_INPUT_VIEW.getLastPressedButtonId().equals("addSave")) {
                saveProfileResult = UI_SERVICE.getSaveProfile(selectedSave);
            } else {
                saveProfileResult = new Result<>();
            }

            duplicateSavePath = addSave(duplicateSavePath, saveProfileResult, selectedSave);
        } while (saveProfileResult.isSuccess() && duplicateSavePath);

        //Cleanup our UI actions.
        PROFILE_INPUT_VIEW.getInput().clear();
    }

    //TODO: This is kind of ugly. Needs a rewrite at some point, probably with guard clauses.
    //TODO: The function name kind of sucks too.
    private boolean addSave(boolean duplicateSavePath, Result<SaveProfile> saveProfileResult, File selectedSave) {
        if (saveProfileResult.isSuccess()) {
            SaveProfile saveProfile = saveProfileResult.getPayload();
            duplicateSavePath = saveAlreadyExists(saveProfile.getSavePath());

            if (duplicateSavePath) {
                Popup.displaySimpleAlert("Save is already being managed!", stage, MessageType.WARN);
                SAVE_INPUT_VIEW.resetSelectedSave();
            } else {
                //Remove the default save profile that isn't actually a profile if it's all that we have in the list.
                boolean duplicateProfileName;
                do {
                    PROFILE_INPUT_VIEW.getInput().clear();
                    PROFILE_INPUT_VIEW.getInput().requestFocus();
                    PROFILE_INPUT_VIEW.show(stage);
                    duplicateProfileName = isDuplicateProfileName(PROFILE_INPUT_VIEW.getInput().getText());

                    if (duplicateProfileName) {
                        Popup.displaySimpleAlert("Profile name already exists!", stage, MessageType.WARN);
                    } else if (!PROFILE_INPUT_VIEW.getInput().getText().isBlank()) {
                        addSaveProfileToList(saveProfileResult, saveProfile);
                        displayAddExistingModsDialog(selectedSave);

                        saveList.refresh();
                        modTableContextBar.getSaveProfileDropdown().getSelectionModel().select(saveProfile);
                        modTableContextBar.getSaveProfileDropdown().fireEvent(new ActionEvent());

                        UI_SERVICE.saveUserData();
                    }
                } while (duplicateProfileName);
            }
        }
        return duplicateSavePath;
    }

    private void addSaveProfileToList(Result<SaveProfile> saveProfileResult, SaveProfile saveProfile) {
        saveProfile.setProfileName(PROFILE_INPUT_VIEW.getInput().getText());
        if (SAVE_PROFILES.size() == 1 && SAVE_PROFILES.getFirst().getSaveName().equals("None") && SAVE_PROFILES.getFirst().getProfileName().equals("None") && SAVE_PROFILES.getFirst().getSavePath() == null) {
            saveProfile.setSaveExists(true);
            SAVE_PROFILES.set(0, saveProfile);
        } else {
            SAVE_PROFILES.add(saveProfile);
            saveProfileResult.addMessage("Successfully added profile " + saveProfile.getSaveName() + " to save list.", ResultType.SUCCESS);
            UI_SERVICE.log(saveProfileResult);

            PROFILE_INPUT_VIEW.getInput().clear();
            //TODO: Switch active profile to the new profile
        }
    }

    private void displayAddExistingModsDialog(File selectedSave) {
        int addExistingModsChoice = Popup.displayYesNoDialog("Do you want to add the mods in the save to a modlist?", stage, MessageType.INFO);
        if (addExistingModsChoice == 1) {
            int addExistingModsLocationChoice = Popup.displayThreeChoiceDialog("Which modlist do you want to add the mods in the save to?",
                    stage, MessageType.INFO, "Current Modlist", "New Modlist", "Cancel");
            if (addExistingModsLocationChoice != 0) {
                if (addExistingModsLocationChoice == 1) { //Create a new modlist and switch to it before we add mods
                    String newProfileName = ModImportUtility.createNewModProfile(UI_SERVICE, stage, PROFILE_INPUT_VIEW);
                    if (!newProfileName.isEmpty()) {
                        Optional<ModList> modlistProfile = UI_SERVICE.getMODLIST_PROFILES().stream()
                                .filter(modlistProfile1 -> modlistProfile1.getProfileName().equals(newProfileName))
                                .findFirst();
                        modlistProfile.ifPresent(profile -> modTableContextBar.getModProfileDropdown().getSelectionModel().select(profile));
                        importExistingModlist(selectedSave);
                    }
                } else {
                    importExistingModlist(selectedSave);
                }
            }
        }
    }

    private void importExistingModlist(final File selectedSave) {
        Result<List<Mod>> existingModlistResult = ModImportUtility.getModlistFromSandboxConfig(UI_SERVICE, selectedSave, stage);
        if (existingModlistResult.isSuccess()) {
            importModlist(existingModlistResult.getPayload()).start();
        }
    }

    private @NotNull Thread importModlist(List<Mod> modList) {
        final Task<List<Result<Mod>>> TASK = UI_SERVICE.importModlist(modList);

        TASK.setOnRunning(workerStateEvent -> {
            disableUserInput(true);
            modImportProgressPanel.setVisible(true);
        });

        TASK.setOnSucceeded(workerStateEvent -> {
            ModImportUtility.addModScrapeResultsToModlist(UI_SERVICE, stage, TASK.getValue(), modList.size());
            UI_SERVICE.getCurrentModListProfile().setModList(UI_SERVICE.getCurrentModList());
            UI_SERVICE.saveUserData();

            Platform.runLater(() -> {
                FadeTransition fadeTransition = new FadeTransition(Duration.millis(1200), modImportProgressPanel);
                fadeTransition.setFromValue(1d);
                fadeTransition.setToValue(0d);

                fadeTransition.setOnFinished(actionEvent -> resetProgressUi());

                fadeTransition.play();
            });
        });

        Thread thread = Thread.ofVirtual().unstarted(TASK);
        thread.setDaemon(true);
        return thread;
    }

    @FXML
    private void copySave() {
        SaveProfile profileToCopy = saveList.getSelectionModel().getSelectedItem();
        if (profileToCopy != null) {
            if (profileToCopy.isSaveExists()) {
                int choice = Popup.displayYesNoDialog(String.format("Are you sure you want to copy the save \"%s\"", profileToCopy.getProfileName()), stage, MessageType.WARN);
                if (choice == 1)
                    getCopyThread().start();
            } else {
                Popup.displaySimpleAlert("You cannot copy a profile that is missing its save!", stage, MessageType.ERROR);
            }
        } else {
            Popup.displaySimpleAlert("You have to select a profile first!", stage, MessageType.ERROR);
        }
    }

    //Create a new thread that runs the code for copying profiles
    private Thread getCopyThread() {
        final Task<Result<SaveProfile>> TASK = new Task<>() {
            @Override
            protected Result<SaveProfile> call() throws Exception {
                return UI_SERVICE.copySaveProfile(saveList.getSelectionModel().getSelectedItem());
            }
        };

        TASK.setOnRunning(workerStateEvent -> {
            disableModImportBar(true);
            disableSaveCopyBar(false);
            modImportProgressPanel.setVisible(true);
            disableUserInput(true);
        });

        TASK.setOnSucceeded(event -> Platform.runLater(() -> {
            saveCopyMessage.setText("Finished!");
            UI_SERVICE.getModImportProgressPercentageProperty().setValue(1d);
            modImportProgressWheel.setVisible(false);
            Result<SaveProfile> profileCopyResult = TASK.getValue();

            if (profileCopyResult.isSuccess()) {
                SAVE_PROFILES.add(profileCopyResult.getPayload());
            }

            Popup.displaySimpleAlert(profileCopyResult, stage);

            UI_SERVICE.log(profileCopyResult);
            UI_SERVICE.saveUserData();

            FadeTransition fadeTransition = new FadeTransition(Duration.millis(1200), modImportProgressPanel);
            fadeTransition.setFromValue(1d);
            fadeTransition.setToValue(0d);

            fadeTransition.setOnFinished(actionEvent -> Platform.runLater(this::resetProgressUi));
            fadeTransition.play();
        }));

        Thread thread = Thread.ofVirtual().unstarted(TASK);
        thread.setDaemon(true);
        return thread;
    }

    @FXML
    private void removeSave() {
        if (saveList.getSelectionModel().getSelectedItem() != null) {
            if (UI_SERVICE.getCurrentSaveProfile().equals(saveList.getSelectionModel().getSelectedItem())) {
                Popup.displaySimpleAlert("You cannot remove the active profile.", stage, MessageType.WARN);
            } else {
                int choice = Popup.displayYesNoDialog("Are you sure you want to delete this profile? It will not delete the save itself from the saves folder, ONLY the profile used by SEMM.", stage, MessageType.WARN);
                if (choice == 1) {
                    int profileIndex = saveList.getSelectionModel().getSelectedIndex();
                    SAVE_PROFILES.remove(profileIndex);

                    UI_SERVICE.saveUserData();
                    if (profileIndex > SAVE_PROFILES.size())
                        saveList.getSelectionModel().select(profileIndex - 1);
                    else
                        saveList.getSelectionModel().select(profileIndex);
                }
            }
        }
    }

    @FXML
    private void renameProfile() {
        boolean duplicateProfileName;

        do {
            SaveProfile selectedProfile = saveList.getSelectionModel().getSelectedItem();
            if (selectedProfile != null) {
                PROFILE_INPUT_VIEW.getInput().setText(selectedProfile.getProfileName());
                PROFILE_INPUT_VIEW.getInput().requestFocus();
                PROFILE_INPUT_VIEW.getInput().selectAll();
                PROFILE_INPUT_VIEW.show(stage);

                String newProfileName = PROFILE_INPUT_VIEW.getInput().getText();
                duplicateProfileName = isDuplicateProfileName(newProfileName);
                if (duplicateProfileName) {
                    Popup.displaySimpleAlert("Profile name already exists!", stage, MessageType.WARN);
                } else if (!newProfileName.isBlank()) {
                    String originalProfileName = saveList.getSelectionModel().getSelectedItem().getProfileName();
                    SaveProfile profileToModify = saveList.getSelectionModel().getSelectedItem();
                    profileToModify.setProfileName(newProfileName);
                    saveList.refresh();

                    int saveProfileDropdownSelectedIndex = modTableContextBar.getSaveProfileDropdown().getSelectionModel().getSelectedIndex();
                    if (saveProfileDropdownSelectedIndex != SAVE_PROFILES.size() - 1) {
                        modTableContextBar.getSaveProfileDropdown().getSelectionModel().selectNext();
                        modTableContextBar.getSaveProfileDropdown().getSelectionModel().selectPrevious();
                    } else if (SAVE_PROFILES.size() == 1) {
                        SAVE_PROFILES.add(new SaveProfile());
                        modTableContextBar.getSaveProfileDropdown().getSelectionModel().selectNext();
                        modTableContextBar.getSaveProfileDropdown().getSelectionModel().selectPrevious();
                        SAVE_PROFILES.removeLast();
                    } else {
                        modTableContextBar.getSaveProfileDropdown().getSelectionModel().selectPrevious();
                        modTableContextBar.getSaveProfileDropdown().getSelectionModel().selectNext();
                    }

                    if(profileToModify.equals(UI_SERVICE.getCurrentSaveProfile())) {
                        activeProfileName.setText(profileToModify.getProfileName());
                    }

                    UI_SERVICE.log(String.format("Successfully renamed save profile \"%s\" to \"%s\".", originalProfileName, newProfileName), MessageType.INFO);
                    PROFILE_INPUT_VIEW.getInput().clear();
                    UI_SERVICE.saveUserData();
                }
            } else {
                duplicateProfileName = false;
            }
        } while(duplicateProfileName);
    }

    @FXML
    private void setActive() {
        if (saveList.getSelectionModel().getSelectedItem() != null) {
            UI_SERVICE.setCurrentSaveProfile(saveList.getSelectionModel().getSelectedItem());
            modTableContextBar.getSaveProfileDropdown().getSelectionModel().select(saveList.getSelectionModel().getSelectedItem());
            activeProfileName.setText(UI_SERVICE.getCurrentSaveProfile().getProfileName());
            saveList.refresh();
        }
    }

    @FXML
    private void closeSaveWindow() {
        stage.close();
        stage.setHeight(stage.getHeight() - 1);
        saveList.getSelectionModel().clearSelection();
        //Platform.exitNestedEventLoop(stage, null);
    }

    private boolean saveAlreadyExists(String savePath) {
        return SAVE_PROFILES.stream()
                .anyMatch(saveProfile -> saveProfile.getSavePath() != null && saveProfile.getSavePath().equals(savePath));
    }

    private boolean isDuplicateProfileName(String profileName) {
        return SAVE_PROFILES.stream()
                .anyMatch(saveProfile -> saveProfile.getProfileName().toLowerCase().trim().equals(profileName.toLowerCase().trim()));
    }

    public void show(Stage parentStage) {
        saveList.refresh();
        stage.show();
        WindowPositionUtility.centerStageOnStage(stage, parentStage);
        WindowTitleBarColorUtility.SetWindowsTitleBar(stage);
        activeProfileName.setText(UI_SERVICE.getCurrentSaveProfile().getProfileName());
    }

    private void disableModImportBar(boolean shouldDisable) {
        modImportProgressActionName.setVisible(!shouldDisable);
        modImportProgressNumerator.setVisible(!shouldDisable);
        modImportProgressDivider.setVisible(!shouldDisable);
        modImportProgressDenominator.setVisible(!shouldDisable);
    }

    private void disableSaveCopyBar(boolean shouldDisable) {
        saveCopyMessage.setVisible(!shouldDisable);
    }

    private void disableUserInput(boolean shouldDisable) {
        addSave.setDisable(shouldDisable);
        copySave.setDisable(shouldDisable);
        removeSave.setDisable(shouldDisable);
        renameProfile.setDisable(shouldDisable);
        selectSave.setDisable(shouldDisable);
    }

    private void resetProgressUi() {
        saveCopyMessage.setText("Copying save...");
        modImportProgressWheel.setVisible(true);
        disableModImportBar(false);
        disableSaveCopyBar(true);
        modImportProgressPanel.setVisible(false);
        disableUserInput(false);
        modImportProgressPanel.setOpacity(1d);
        UI_SERVICE.getModImportProgressNumeratorProperty().setValue(0);
        UI_SERVICE.getModImportProgressDenominatorProperty().setValue(0);
        UI_SERVICE.getModImportProgressPercentageProperty().setValue(0d);
    }

    public Stage displayTutorial(EventHandler<KeyEvent> arrowKeyDisabler) {
        stage.initStyle(StageStyle.UNDECORATED);
        Pane[] panes = UI_SERVICE.getHighlightPanes();
        ((Pane) stage.getScene().getRoot()).getChildren().addAll(panes);
        addSave.requestFocus();

        stage.setOnShown(event -> {
            TutorialUtility.tutorialElementHighlight(panes, stage.getWidth(), stage.getHeight(), saveList);
            List<String> tutorialMessages = new ArrayList<>();
            tutorialMessages.add("This is the SEMM Save Profile Manager. Here you manage the actual Space Engineers saves you apply modlists to.");
            tutorialMessages.add("SEMM uses Save Profiles to store the information of a save you want to manage the modlist of. " +
                    "A save profile has two names. The name of the profile, and the name of the actual save it contains. The name of the profile is what is displayed in SEMM, but the save name can be shown by hovering your cursor over a save profile in the manager. ");
            tutorialMessages.add("Like with mod lists, the currently active save profile will have a pair of bars surrounding the active save profile.");
            tutorialMessages.add("If a save no longer exists on your computer however, such as the default \"None\" profile that you will see here if it is your first time running this tutorial, its text will change to red and have a line through it.");
            Popup.displayNavigationDialog(tutorialMessages, stage, MessageType.INFO, "Managing Saves");

            tutorialMessages.clear();

            tutorialMessages.add("Let's start by adding an existing save to SEMM. " +
                    "When you press the \"Add Save\" button, SEMM will automatically take you to your Space Engineers Save folder.");
            tutorialMessages.add("Only Sandbox_config.sbc files, which store the mod list for a save, are valid. Press the \"Add Save\" button and select a Sandbox_config.sbc file inside a save folder");
            Popup.displayNavigationDialog(tutorialMessages, stage, MessageType.INFO, "Managing Save Profiles");

            TutorialUtility.tutorialElementHighlight(panes, stage.getWidth(), stage.getHeight(), addSave);
            stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, arrowKeyDisabler);
        });

        addSave.setOnAction(event -> {
            Result<SaveProfile> saveProfileResult;
            do {
                boolean duplicateSavePath = false;
                //Get our selected file from the user, check if its already being managed by SEMM by checking the save path, and then check if the save name already exists. If it does, append a number to the end of it.
                do {
                    SAVE_INPUT_VIEW.setSaveProfileInputTitle("Add new SE save");
                    SAVE_INPUT_VIEW.setAddSaveButtonText("Next");
                    SAVE_INPUT_VIEW.show(stage);
                    File selectedSave = SAVE_INPUT_VIEW.getSelectedSave();
                    if (selectedSave != null && SAVE_INPUT_VIEW.getLastPressedButtonId().equals("addSave")) {
                        try {
                            saveProfileResult = UI_SERVICE.getSaveProfile(selectedSave);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        saveProfileResult = new Result<>();
                        Popup.displaySimpleAlert("You HAVE to add a save or you cannot apply mod lists!", stage, MessageType.WARN);
                    }

                    //TODO: Break out the "import existing mods" stuff. We want a message before saying we'll import the existing mods for now.
                    if (saveProfileResult.isSuccess()) {
                        SaveProfile saveProfile = saveProfileResult.getPayload();
                        duplicateSavePath = saveAlreadyExists(saveProfile.getSavePath());

                        if (duplicateSavePath) {
                            Popup.displaySimpleAlert("Save is already being managed!", stage, MessageType.WARN);
                            SAVE_INPUT_VIEW.resetSelectedSave();
                        } else {
                            //Remove the default save profile that isn't actually a profile if it's all that we have in the list.
                            boolean duplicateProfileName;
                            do {
                                PROFILE_INPUT_VIEW.getInput().clear();
                                PROFILE_INPUT_VIEW.getInput().requestFocus();
                                PROFILE_INPUT_VIEW.show(stage);
                                duplicateProfileName = isDuplicateProfileName(PROFILE_INPUT_VIEW.getInput().getText());

                                if (duplicateProfileName) {
                                    Popup.displaySimpleAlert("Profile name already exists!", stage, MessageType.WARN);
                                } else if (!PROFILE_INPUT_VIEW.getInput().getText().isBlank()) {
                                    addSaveProfileToList(saveProfileResult, saveProfile);

                                    saveList.refresh();
                                    modTableContextBar.getSaveProfileDropdown().getSelectionModel().select(saveProfile);
                                    modTableContextBar.getSaveProfileDropdown().fireEvent(new ActionEvent());

                                    UI_SERVICE.saveUserData();
                                }
                            } while (duplicateProfileName);
                        }
                    }
                    if(UI_SERVICE.getCurrentSaveProfile().getProfileName().equals("None")) {
                        Popup.displaySimpleAlert("You HAVE to add a save or you cannot apply mod lists!", stage, MessageType.WARN);
                        duplicateSavePath = true;
                    }
                } while (saveProfileResult.isSuccess() && duplicateSavePath);

                //Cleanup our UI actions.
                PROFILE_INPUT_VIEW.getInput().clear();
            } while(saveProfileResult.getType() == ResultType.NOT_INITIALIZED);

            List<String> tutorialMessages = new ArrayList<>();
            tutorialMessages.add("When you're adding a save profile to SEMM you can actually automatically import the mods contained in that save to your current mod list or a new mod list. " +
                    "For now we will skip this step so you can see how to manually add mods.");
            tutorialMessages.add("Now that you've added a save profile let's head back to the mod list manager. Press the \"Close\" button.");
            Popup.displayNavigationDialog(tutorialMessages, stage, MessageType.INFO, "Managing Save Profiles");
            TutorialUtility.tutorialElementHighlight(panes, stage.getWidth(), stage.getHeight(), closeSaveWindow);
            closeSaveWindow.requestFocus();
        });

        stage.setOnCloseRequest(event -> {
            stage.initStyle(StageStyle.DECORATED);
            stage.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, arrowKeyDisabler);
            //Clean up the tutorial actions
            ((Pane) stage.getScene().getRoot()).getChildren().removeAll(panes);
            stage.setOnShown(event1 -> {});
            addSave.setOnAction(event1 -> {
                try {
                    addSave();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            stage.setOnCloseRequest(event1 -> {});
        });

        //This is fucking awful
        return stage;
    }
}
