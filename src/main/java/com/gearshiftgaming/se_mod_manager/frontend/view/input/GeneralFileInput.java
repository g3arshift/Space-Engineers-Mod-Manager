package com.gearshiftgaming.se_mod_manager.frontend.view.input;

import com.gearshiftgaming.se_mod_manager.backend.models.shared.MessageType;
import com.gearshiftgaming.se_mod_manager.frontend.view.popup.Popup;
import com.gearshiftgaming.se_mod_manager.frontend.view.window.WindowDressingUtility;
import com.gearshiftgaming.se_mod_manager.frontend.view.window.WindowPositionUtility;
import com.gearshiftgaming.se_mod_manager.frontend.view.window.WindowTitleBarColorUtility;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class GeneralFileInput {

	@FXML
	@Getter
	private Label fileName;

	@Getter
	@FXML
	private Button chooseFile;

	@FXML
	private Button next;

	@FXML
	private Button cancel;

	private Stage stage;

	private final String BASE_DIR = System.getProperty("user.home");

	String noSaveSelectedMessage = "No file selected";

	@Getter
	private File selectedFile;

	@Getter
	private String lastPressedButtonId;

	@Setter
	private FileChooser.ExtensionFilter extensionFilter;

	public GeneralFileInput() {
		extensionFilter = new FileChooser.ExtensionFilter("Files", "*.*");
	}

	public void initView(Parent root) {
		Scene scene = new Scene(root);
		stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);

		stage.setScene(scene);

		//Just a default. Usually gets overriden.
		stage.setTitle("File Select");
		WindowDressingUtility.appendStageIcon(stage);

		fileName.setText(noSaveSelectedMessage);

		next.setOnAction(actionEvent -> {
			Button btn = (Button) actionEvent.getSource();
			lastPressedButtonId = btn.getId();
			next();
		});

		cancel.setOnAction(actionEvent -> {
			Button btn = (Button) actionEvent.getSource();
			lastPressedButtonId = btn.getId();
			cancel();
		});

		stage.setOnCloseRequest(windowEvent -> {
			Platform.exitNestedEventLoop(stage, null);
			fileName.setText(noSaveSelectedMessage);
			selectedFile = null;
		});

		stage.setResizable(false);
	}


	@FXML
	private void chooseFile() {
		FileChooser fileChooser = getFileChooser();
		fileChooser.getExtensionFilters().add(extensionFilter);
		selectedFile = fileChooser.showOpenDialog(stage);
		if (selectedFile != null) {
			fileName.setText(selectedFile.getName());
		} else {
			fileName.setText(noSaveSelectedMessage);
		}
	}

	private @NotNull FileChooser getFileChooser() {;
		FileChooser fileChooser = new FileChooser();

		fileChooser.setInitialDirectory(new File(BASE_DIR));

		return fileChooser;
	}

	private void next() {
		if (selectedFile == null) {
			Popup.displaySimpleAlert("You must select a file!", stage, MessageType.ERROR);
		} else {
			stage.close();
			fileName.setText(noSaveSelectedMessage);
			Platform.exitNestedEventLoop(stage, null);
		}
	}

	private void cancel() {
		stage.close();
		fileName.setText(noSaveSelectedMessage);
		selectedFile = null;
		Platform.exitNestedEventLoop(stage, null);
	}

	public void show(Stage parentStage) {
		stage.show();
		WindowPositionUtility.centerStageOnStage(stage, parentStage);
		WindowTitleBarColorUtility.setWindowsTitleBar(stage);
		Platform.enterNestedEventLoop(stage);
	}

	public void setSaveProfileInputTitle(String title) {
		this.stage.setTitle(title);
	}

	public void setNextButtonText(String text) {
		this.next.setText(text);
	}

	public void resetSelectedSave() {
		this.selectedFile = null;
	}
}
