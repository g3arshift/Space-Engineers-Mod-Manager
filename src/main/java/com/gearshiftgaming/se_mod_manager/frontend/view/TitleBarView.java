package com.gearshiftgaming.se_mod_manager.frontend.view;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import lombok.Getter;
import org.kordamp.ikonli.javafx.FontIcon;
/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class TitleBarView {
	@FXML
	@Getter
	private HBox titleBar;

	@FXML
	private Label appNameVersion;

	@FXML
	private Button minimizeButton;

	@FXML
	private Button maximizeRestoreButton;

	@FXML
	private FontIcon maximizeRestoreIcon;

	@FXML
	private Button closeButton;
	private final String TITLE_BUTTON_STYLING;
	private final Stage STAGE;
	public TitleBarView(Stage stage) {
		TITLE_BUTTON_STYLING = "-fx-background-radius: 0;" +
				"-fx-text-fill: -color-button-fg;" +
				"-color-button-bg: -color-bg-default;" +
				"-color-button-border-pressed: transparent;" +
				"-color-button-border: transparent;";
		this.STAGE = stage;
	}

	public void initView() {
		maximizeRestoreButton.setStyle(TITLE_BUTTON_STYLING);
		minimizeButton.setStyle(TITLE_BUTTON_STYLING);
		closeButton.setStyle(TITLE_BUTTON_STYLING);
	}
	@FXML
	private void minimize() {
		STAGE.setIconified(true);
	}
	//TODO: We also need to add the app icon and a title to it all
	@FXML
	private void maximizeOrRestore() {
		if(maximizeRestoreIcon.getIconLiteral().equals("cil-window-maximize")) {
			STAGE.setMaximized(false);
			maximizeRestoreIcon.setIconLiteral("cil-window-restore");
		} else {
			STAGE.setMaximized(true);
			maximizeRestoreIcon.setIconLiteral("cil-window-maximize");
		}
	}
	@FXML
	private void closeSemm() {
		Platform.exit();
	}
}