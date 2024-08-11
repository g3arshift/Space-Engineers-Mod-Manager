package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.controller.BackendController;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;

public class SaveListInputView {

    @FXML
    private Label saveName;

    @Getter
    @FXML
    private Button selectSave;

    @FXML
    private Button addSave;

    @FXML
    private Button cancelAddSave;

    @Getter
    private Stage stage;

    private BackendController backendController;

    public void initView(Parent root) {
        Scene scene = new Scene(root);
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        stage.setScene(scene);
        saveName.setText("No save selected");
    }


    @FXML
    private void selectSave() {
        //TODO: Implement
    }

    @FXML
    private void addSave() {
        //TODO: Implement
    }

    @FXML
    private void cancelAddSave() {
        //TODO: Implement
    }
}
