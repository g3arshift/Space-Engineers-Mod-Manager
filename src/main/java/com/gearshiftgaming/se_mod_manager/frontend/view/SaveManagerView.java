package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
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


public class SaveManagerView
{
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
        //TODO: Set cell factory

        saveList.setStyle("-fx-background-color: -color-bg-default;");

        stage.setScene(scene);
        uiService.getLogger().info("Successfully initialized SaveManagerView.");
    }

    @FXML
    private void addSave() {
        //TODO: Implement
    }

    @FXML
    private void copySave() {
        //TODO: Implement
    }

    @FXML
    private void removeSave() {
        //TODO: Implement
    }

    @FXML
    private void selectSave() {
        //TODO: Implement
    }

    @FXML
    private void closeSaveWindow() {
        stage.close();
    }
}
