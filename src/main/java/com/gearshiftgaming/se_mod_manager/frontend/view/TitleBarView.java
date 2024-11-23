package com.gearshiftgaming.se_mod_manager.frontend.view;
import com.gearshiftgaming.se_mod_manager.frontend.models.WindowType;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
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

	@Getter
	private final HBox titleBar;

	@Getter
	private final Label APP_NAME_VERSION;

	private final Button MINIMIZE_BUTTON;

	private final Button MAXIMIZE_RESTORE_BUTTON;
	private final FontIcon MAXIMIZE_RESTORE_ICON;

	private final Button CLOSE_BUTTON;

	private final String TITLE_BUTTON_STYLING;
	private final Stage STAGE;

	private final WindowType WINDOW_TYPE;

	public TitleBarView(Stage stage, WindowType windowType) {

		//TODO: We also need to add the app icon and a title to it all

		TITLE_BUTTON_STYLING = "-fx-background-radius: 0;" +
				"-fx-text-fill: -color-button-fg;" +
				"-color-button-bg: -color-bg-default;" +
				"-color-button-border-pressed: transparent;" +
				"-color-button-border: transparent;";

		this.WINDOW_TYPE = windowType;
		this.STAGE = stage;

		double width = 40d;
		double height = 30d;

		APP_NAME_VERSION = new Label();

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		spacer.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);

		MINIMIZE_BUTTON = new Button();
		MINIMIZE_BUTTON.setPrefSize(width, height);
		MINIMIZE_BUTTON.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		MINIMIZE_BUTTON.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		MINIMIZE_BUTTON.setFocusTraversable(false);
		MINIMIZE_BUTTON.setOnAction(this::minimize);
		FontIcon minimizeIcon = new FontIcon();
		minimizeIcon.setIconLiteral("cil-window-minimize");
		minimizeIcon.setMouseTransparent(true);
		StackPane minimizeLayout = new StackPane(MINIMIZE_BUTTON, minimizeIcon);

		MAXIMIZE_RESTORE_BUTTON = new Button();
		MAXIMIZE_RESTORE_BUTTON.setPrefSize(width, height);
		MAXIMIZE_RESTORE_BUTTON.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		MAXIMIZE_RESTORE_BUTTON.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		MAXIMIZE_RESTORE_BUTTON.setFocusTraversable(false);
		MAXIMIZE_RESTORE_BUTTON.setOnAction(this::maximizeOrRestore);
		MAXIMIZE_RESTORE_ICON = new FontIcon();
		MAXIMIZE_RESTORE_ICON.setIconLiteral("cil-window-maximize");
		MAXIMIZE_RESTORE_ICON.setMouseTransparent(true);
		StackPane maximizeRestoreLayout = new StackPane(MAXIMIZE_RESTORE_BUTTON, MAXIMIZE_RESTORE_ICON);

		CLOSE_BUTTON = new Button();
		CLOSE_BUTTON.setPrefSize(width, height);
		CLOSE_BUTTON.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		CLOSE_BUTTON.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		CLOSE_BUTTON.setFocusTraversable(false);
		CLOSE_BUTTON.setOnAction(this::closeSemm);
		FontIcon CLOSE_ICON = new FontIcon();
		CLOSE_ICON.setIconLiteral("cil-x");
		CLOSE_ICON.setMouseTransparent(true);
		StackPane CLOSE_LAYOUT = new StackPane(CLOSE_BUTTON, CLOSE_ICON);


		titleBar = new HBox(APP_NAME_VERSION, spacer, minimizeLayout, maximizeRestoreLayout, CLOSE_LAYOUT);
	}

	public void initView() {
		MAXIMIZE_RESTORE_BUTTON.setStyle(TITLE_BUTTON_STYLING);
		MINIMIZE_BUTTON.setStyle(TITLE_BUTTON_STYLING);
		CLOSE_BUTTON.setStyle(TITLE_BUTTON_STYLING);
	}
	private void minimize(Event event) {
		STAGE.setIconified(true);
		event.consume();
	}

	private void maximizeOrRestore(Event event) {
		if(MAXIMIZE_RESTORE_ICON.getIconLiteral().equals("cil-window-maximize")) {
			STAGE.setMaximized(true);
			MAXIMIZE_RESTORE_ICON.setIconLiteral("cil-window-restore");
		} else {
			STAGE.setMaximized(false);
			MAXIMIZE_RESTORE_ICON.setIconLiteral("cil-window-maximize");
		}
		event.consume();
	}

	private void closeSemm(Event event) {
		if(WINDOW_TYPE == WindowType.MODAL) {
			STAGE.close();
			event.consume();
		} else Platform.exit();
	}
}