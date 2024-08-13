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
    private Button selectSave;

    @FXML
    private Button closeSaveWindow;

    @Getter
    private Stage stage;

    private ObservableList<SaveProfile> saveProfiles;

    private UiService uiService;

    private SaveListInputView saveListInputView;

    private MainWindowView mainWindowView;

    //TODO: Add a button to rename save profiles, that will also rename them in the file structure! MAke sure it renames the session name in Sandbox.sbc and Sandbox_config.sbc, as well as the folder name.

    public void initView(Parent root, UiService uiService, SaveListInputView saveListInputView, Properties properties, MainWindowView mainWindowView) {

        Scene scene = new Scene(root);
        this.uiService = uiService;
        saveProfiles = uiService.getSaveProfiles();

        this.saveListInputView = saveListInputView;
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
        //TODO: If the save list only contains one item and that one item = "None" with all its fields equal to null, replace it with our new item.
        boolean duplicateSaveName = false;
        Result<SaveProfile> result;
        do {
            saveListInputView.getStage().showAndWait();
            result = saveListInputView.getSaveProfileResult();
            if (result.isSuccess()) {
                SaveProfile saveProfile = result.getPayload();
                duplicateSaveName = saveNameExists(saveProfile.getSaveName());

                if (duplicateSaveName) {
                    Popup.displaySimpleAlert("Save is already being managed!", stage, MessageType.WARN);
                } else {
                    //Remove the default save profile that isn't actually a profile if it's all that we have in the list.
                    if (saveProfiles.size() == 1 && saveProfiles.getFirst().getSaveName().equals("None") && saveProfiles.getFirst().getSavePath() == null) {
                        saveProfiles.set(0, saveProfile);
                        mainWindowView.setCurrentSaveProfile(saveProfile);
                    } else {
                        saveProfiles.add(saveProfile);
                    }
                    result.addMessage("Successfully added profile " + saveProfile.getSaveName() + " to save list.", ResultType.SUCCESS);
                    uiService.log(result);
                }
                saveListInputView.getSaveName().setText("No save selected");
                saveListInputView.setSelectedSave(null);
            }
        } while (result.isSuccess() && duplicateSaveName);
    }

    @FXML
    private void copySave() {
        //TODO: Implement
        //TODO: Make an alert that "This will only copy the sandbox config, not the entire save folder. It will be stored at XYZ. Continue?
    }

    @FXML
    private void removeSave() {
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

    private boolean saveNameExists(String saveName) {
        return saveProfiles.stream()
                .anyMatch(saveProfile -> saveProfile.getSaveName().equals(saveName));
    }
}
