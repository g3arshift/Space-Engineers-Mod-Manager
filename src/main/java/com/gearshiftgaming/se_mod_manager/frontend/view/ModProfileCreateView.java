package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;

import javafx.scene.control.TextField;

public class ModProfileCreateView {

    @Getter
    @FXML
    private TextField profileCreateInput;

    @FXML
    private Button profileCreateAccept;

    @FXML
    private Button profileCreateCancel;

    @Getter
    private Stage stage;
    private Scene scene;

    public void initController(Parent root) {
        this.scene = new Scene(root);
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        stage.setScene(scene);
    }

    @FXML
    private void getProfileName() {
        if (profileCreateInput.getText().isEmpty() || profileCreateInput.getText().isBlank()) {
            Alert.display("Profile name cannot be empty!", stage, MessageType.WARN);
        } else {
            stage.close();
        }
    }

    @FXML
    private void closeWindow() {
        profileCreateInput.clear();
        stage.close();
    }
}
