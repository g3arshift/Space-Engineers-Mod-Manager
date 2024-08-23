package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 * @author Gear Shift
 */
public class SaveListInput {

    @FXML
    @Getter
    private Label saveName;

    @Getter
    @FXML
    private Button chooseSave;

    @FXML
    private Button addSave;

    @FXML
    private Button cancelAddSave;

    @Getter
    private Stage stage;

    private final String APP_DATA_PATH = System.getenv("APPDATA") + "/SpaceEngineers/Saves";

    @Getter
    private Result<SaveProfile> saveProfileResult = new Result<>();

    @Setter
    private File selectedSave;

    private UiService uiService;

    public void initView(Parent root, UiService uiService) throws IOException {
        this.uiService = uiService;
        Scene scene = new Scene(root);
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        stage.setScene(scene);

        stage.setTitle("Add new SE save");
        stage.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResourceAsStream("/icons/logo.png"))));

        saveName.setText("No save selected");
    }


    @FXML
    private void chooseSave() throws IOException {
        FileChooser fileChooser = getFileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Sandbox_config Files", "*_config.sbc"));
        selectedSave = fileChooser.showOpenDialog(stage);

        if (selectedSave != null) {
            saveProfileResult = uiService.getBackendController().getSaveProfile(selectedSave);
            if (saveProfileResult.isSuccess()) {
                saveName.setText(saveProfileResult.getPayload().getSaveName());
            }
        }
    }

    private FileChooser getFileChooser() {
        String[] directoryContents = new File(APP_DATA_PATH).list();
        FileChooser fileChooser = new FileChooser();

        //If there's only one save folder in our save directory, which there should be, set the path to that folder.
        if (directoryContents != null && directoryContents.length == 1) {
            fileChooser.setInitialDirectory(new File(APP_DATA_PATH + "/" + directoryContents[0]));
        } else {
            fileChooser.setInitialDirectory(new File(APP_DATA_PATH));
        }

        fileChooser.setTitle("Select SE Save");
        return fileChooser;
    }

    @FXML
    private void addSave() {
        if (selectedSave == null) {
            Popup.displaySimpleAlert("You must select a save!", stage, MessageType.ERROR);
        } else {
            stage.close();
        }
    }

    @FXML
    private void cancelAddSave() {
        saveProfileResult = new Result<>();
        stage.close();
    }
}
