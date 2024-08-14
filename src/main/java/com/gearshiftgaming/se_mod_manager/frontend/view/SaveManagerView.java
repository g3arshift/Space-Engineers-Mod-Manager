package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.ResultType;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.SaveProfileCell;
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

import java.io.IOException;
import java.util.Objects;
import java.util.Properties;


public class SaveManagerView {
    @FXML
    private ListView<SaveProfile> saveList;

    @FXML
    private Button addSave;

    @FXML
    private Button copySave;

    @FXML
    private Button removeSave;

    @FXML
    private Button renameSave;

    @FXML
    private Button selectSave;

    @FXML
    private Button closeSaveWindow;

    @Getter
    private Stage stage;

    private ObservableList<SaveProfile> saveProfiles;

    private UiService uiService;

    private SaveListInput saveListInputFirstStepView;

    private ProfileInputView saveListInputSecondStepView;

    private MainWindowView mainWindowView;

    //TODO: Add a button to rename save profiles, that will also rename them in the file structure! MAke sure it renames the session name in Sandbox.sbc and Sandbox_config.sbc, as well as the folder name.

    public void initView(Parent root, UiService uiService,
                         SaveListInput saveListInputFirstStepView, ProfileInputView saveListInputSecondStepView,
                         Properties properties, MainWindowView mainWindowView) {

        Scene scene = new Scene(root);
        this.uiService = uiService;
        saveProfiles = uiService.getSaveProfiles();

        this.saveListInputFirstStepView = saveListInputFirstStepView;
        this.saveListInputSecondStepView = saveListInputSecondStepView;
        this.mainWindowView = mainWindowView;

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        stage.setTitle("Save Manager");
        stage.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResourceAsStream("/icons/logo.png"))));

        stage.setMinWidth(Double.parseDouble(properties.getProperty("semm.profileView.resolution.minWidth")));
        stage.setMinHeight(Double.parseDouble(properties.getProperty("semm.profileView.resolution.minHeight")));

        saveList.setItems(saveProfiles);
        saveList.setCellFactory(param -> new SaveProfileCell());

        saveList.setStyle("-fx-background-color: -color-bg-default;");

        stage.setScene(scene);
        uiService.getLogger().info("Successfully initialized SaveManagerView.");
    }

    @FXML
    private void addSave() throws IOException {
        //TODO: We need to implement profile names as well. This also means we need to change the cell factory so it displays the profile name, not save name!
        // When implemented, change duplicate checking for saveName to profileName.
        // Implement this with two screens. The first selects a save, the second adds a name for the profile.
        boolean duplicateSavePath = false;
        Result<SaveProfile> result;
        //Get our selected file from the user, check if its already being managed by SEMM by checking the save path, and then check if the save name already exists. If it does, append a number to the end of it.
        do {
            saveListInputFirstStepView.getStage().showAndWait();
            result = saveListInputFirstStepView.getSaveProfileResult();
            if (result.isSuccess()) {
                SaveProfile saveProfile = result.getPayload();
                duplicateSavePath = saveAlreadyExists(saveProfile.getSavePath());

                if (duplicateSavePath) {
                    Popup.displaySimpleAlert("Save is already being managed!", stage, MessageType.WARN);
                } else {
                    //Remove the default save profile that isn't actually a profile if it's all that we have in the list.
                    //Technically a user could enter this by mistake, but it's extremely unlikely.
                    boolean duplicateProfileName;
                    do {
                        saveListInputSecondStepView.getProfileNameInput().clear();
                        saveListInputSecondStepView.getProfileNameInput().requestFocus();
                        saveListInputSecondStepView.getStage().showAndWait();
                        duplicateProfileName = profileNameAlreadyExists(saveListInputSecondStepView.getProfileNameInput().getText());

                        if (duplicateProfileName) {
                            Popup.displaySimpleAlert("Profile name already exists!", stage, MessageType.WARN);
                        } else if (!saveListInputSecondStepView.getProfileNameInput().getText().isBlank()) {
                            saveProfile.setProfileName(saveListInputSecondStepView.getProfileNameInput().getText());
                            if (saveProfiles.size() == 1 &&
                                    saveProfiles.getFirst().getSaveName().equals("None") &&
                                    saveProfiles.getFirst().getProfileName().equals("None") &&
                                    saveProfiles.getFirst().getSavePath() == null) {
                                saveProfiles.set(0, saveProfile);
                                mainWindowView.setCurrentSaveProfile(saveProfile);
                            } else {
                                saveProfiles.add(saveProfile);
                                result.addMessage("Successfully added profile " + saveProfile.getSaveName() + " to save list.", ResultType.SUCCESS);
                                uiService.log(result);

                                saveListInputSecondStepView.getProfileNameInput().clear();
                            }
                        }
                    } while (duplicateProfileName);
                }
            }
        } while (result.isSuccess() && duplicateSavePath);

        //Cleanup our UI actions.
        saveListInputFirstStepView.getSaveName().setText("No save selected.");
        saveListInputFirstStepView.setSelectedSave(null);
        saveListInputSecondStepView.getProfileNameInput().clear();
    }

    @FXML
    private void copySave() throws IOException {
        Result<SaveProfile> profileCopyResult = uiService.getBackendController().copySaveProfile(saveList.getSelectionModel().getSelectedItem());
        if(profileCopyResult.isSuccess()) {
            saveProfiles.add(profileCopyResult.getPayload());
        }
        uiService.log(profileCopyResult);
    }

    @FXML
    private void removeSave() {
        //TODO: Implement
    }

    @FXML
    private void renameSave() {
        //TODO: Implement
    }

    @FXML
    private void selectSave() {
        mainWindowView.setCurrentSaveProfile(saveList.getSelectionModel().getSelectedItem());
        mainWindowView.getSaveProfileDropdown().getSelectionModel().select(saveList.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void closeSaveWindow() {
        stage.close();
    }

    private boolean saveAlreadyExists(String savePath) {
        return saveProfiles.stream()
                .anyMatch(saveProfile -> saveProfile.getSavePath() != null && saveProfile.getSavePath().equals(savePath));
    }

    private boolean saveNameAlreadyExists(String saveName) {
        return saveProfiles.stream()
                .anyMatch(saveProfile -> saveProfile.getSaveName().equals(saveName));
    }

    private boolean profileNameAlreadyExists(String profileName) {
        return saveProfiles.stream()
                .anyMatch(saveProfile -> saveProfile.getProfileName().equals(profileName));
    }
}
