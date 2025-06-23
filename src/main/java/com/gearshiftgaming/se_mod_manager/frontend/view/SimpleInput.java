package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.MessageType;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.Popup;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.WindowDressingUtility;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.WindowPositionUtility;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.WindowTitleBarColorUtility;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class SimpleInput {

    @Getter
    @FXML
    private TextField input;

    @FXML
    private Label inputInstructions;

    @FXML
    private Button accept;

    @FXML
    private Button cancel;

    private Stage stage;

    @Setter
    private String emptyTextMessage;

    @Getter
    private String lastPressedButtonId;

    public void initView(Parent root) {
        Scene scene = new Scene(root);
        emptyTextMessage = "Profile name cannot be empty!";
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        stage.setTitle("Profile Naming");
        WindowDressingUtility.appendStageIcon(stage);

        stage.setScene(scene);

        accept.setOnAction(actionEvent -> {
            Button btn = (Button) actionEvent.getSource();
            lastPressedButtonId = btn.getId();
            acceptInput();
        });

        cancel.setOnAction(actionEvent -> {
            Button btn = (Button) actionEvent.getSource();
            lastPressedButtonId = btn.getId();
            closeWindow();
        });

        stage.setResizable(false);
        stage.setOnCloseRequest(windowEvent -> {
            stage.close();
            Platform.exitNestedEventLoop(stage, null);
        });
    }

    @FXML
    private void acceptInput() {
        if (input.getText().isEmpty() || input.getText().isBlank()) {
            Popup.displaySimpleAlert(emptyTextMessage, stage, MessageType.WARN);
        } else {
            stage.close();
            Platform.exitNestedEventLoop(stage, null);
        }
    }

    @FXML
    private void closeWindow() {
        input.clear();
        stage.close();
        Platform.exitNestedEventLoop(stage, null);
    }

    public void show(Stage parentStage) {
        stage.show();
        WindowPositionUtility.centerStageOnStage(stage, parentStage);
        input.requestFocus();
        WindowTitleBarColorUtility.setWindowsTitleBar(stage);
        Platform.enterNestedEventLoop(stage);
    }

    public void setTitle(String title) {
        stage.setTitle(title);
    }

    public void setPromptText(String promptText) {
        input.setPromptText(promptText);
    }

    public void setInputInstructions(String inputInstructions) {
        this.inputInstructions.setText(inputInstructions);
    }
}
