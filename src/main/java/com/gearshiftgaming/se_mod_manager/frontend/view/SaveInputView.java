package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.Popup;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.WindowTitleBarColorUtility;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.WindowDressingUtility;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.WindowPositionUtility;
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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class SaveInputView {

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

	private Stage stage;

	private final String APP_DATA_PATH = System.getenv("APPDATA") + "/SpaceEngineers/Saves";

	String noSaveSelectedMessage = "No save selected";

	@Getter
	private File selectedSave;

	@Getter
	private String lastPressedButtonId;

	private final UiService UI_SERVICE;

	public SaveInputView(UiService UI_SERVICE) {
		this.UI_SERVICE = UI_SERVICE;
	}

	public void initView(Parent root) {
		Scene scene = new Scene(root);
		stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);

		stage.setScene(scene);

		//Just a default. Usually gets overriden.
		stage.setTitle("Save Selection");
		WindowDressingUtility.appendStageIcon(stage);

		saveName.setText(noSaveSelectedMessage);

		addSave.setOnAction(actionEvent -> {
			Button btn = (Button) actionEvent.getSource();
			lastPressedButtonId = btn.getId();
			addSave();
		});

		cancelAddSave.setOnAction(actionEvent -> {
			Button btn = (Button) actionEvent.getSource();
			lastPressedButtonId = btn.getId();
			cancelAddSave();
		});

		stage.setOnCloseRequest(windowEvent -> {
			Platform.exitNestedEventLoop(stage, null);
			saveName.setText(noSaveSelectedMessage);
			selectedSave = null;
		});

		stage.setResizable(false);
	}


	@FXML
	private void chooseSave() {
		FileChooser fileChooser = getFileChooser();
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Sandbox_config Files", "*_config.sbc"));
		selectedSave = fileChooser.showOpenDialog(stage);
		if(selectedSave != null) {
			try {
				Result<String> sandboxNameResult = UI_SERVICE.getSaveName(selectedSave);
				if (sandboxNameResult.isSuccess()) {
					saveName.setText(sandboxNameResult.getPayload());
				} else {
					UI_SERVICE.log(sandboxNameResult);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			saveName.setText(noSaveSelectedMessage);
		}
	}

	private @NotNull FileChooser getFileChooser() {
		String[] directoryContents = new File(APP_DATA_PATH).list();
		FileChooser fileChooser = new FileChooser();

		//If there's only one save folder in our save directory, which there should be, set the path to that folder.
		if (directoryContents != null && directoryContents.length == 1) {
			fileChooser.setInitialDirectory(new File(APP_DATA_PATH + "/" + directoryContents[0]));
		} else {
			fileChooser.setInitialDirectory(new File(APP_DATA_PATH));
		}

		fileChooser.setTitle("Save Selection");
		return fileChooser;
	}

	private void addSave() {
		if (selectedSave == null) {
			Popup.displaySimpleAlert("You must select a save!", stage, MessageType.ERROR);
		} else {
			stage.close();
			stage.setHeight(stage.getHeight() - 1);
			saveName.setText(noSaveSelectedMessage);
			Platform.exitNestedEventLoop(stage, null);
		}
	}

	private void cancelAddSave() {
		stage.close();
		stage.setHeight(stage.getHeight() - 1);
		saveName.setText(noSaveSelectedMessage);
		selectedSave = null;
		Platform.exitNestedEventLoop(stage, null);
	}

	public void show(Stage parentStage) {
		stage.show();
		WindowPositionUtility.centerStageOnStage(stage, parentStage);
		WindowTitleBarColorUtility.SetWindowsTitleBar(stage);
		Platform.enterNestedEventLoop(stage);
	}

	public void setSaveProfileInputTitle(String title) {
		this.stage.setTitle(title);
	}

	public void setAddSaveButtonText(String text) {
		this.addSave.setText(text);
	}

	public void resetSelectedSave() {
		this.selectedSave = null;
	}
}
