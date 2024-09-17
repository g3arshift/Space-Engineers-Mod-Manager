package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;

import javafx.scene.control.TextField;

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
public class ProfileInputView {

    @Getter
    @FXML
    private TextField profileNameInput;

    @FXML
    private Button profileCreateAccept;

    @FXML
    private Button profileCreateCancel;

    @Getter
    private Stage stage;

    public void initView(Parent root) {
        Scene scene = new Scene(root);
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        stage.setTitle("Save Profile name");
        stage.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResourceAsStream("/icons/logo.png"))));

        stage.setScene(scene);
    }

    @FXML
    private void getProfileName() {
        if (profileNameInput.getText().isEmpty() || profileNameInput.getText().isBlank()) {
            Popup.displaySimpleAlert("Profile name cannot be empty!", stage, MessageType.WARN);
        } else {
            stage.close();
        }
    }

    @FXML
    private void closeWindow() {
        profileNameInput.clear();
        stage.close();
    }
}
