package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.*;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import lombok.Getter;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * This represents the main window of the application, with a border pane at its core.
 * It contains the center section of the borderpane, but all other sections should be delegated to their own controllers and FXML files.
 * <p>
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
@Getter
public class MainWindowView {

	//TODO: Remove all menu options under file, except for "Close"

	//FXML Items
	@FXML
	private BorderPane mainWindowLayout;

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

	//TODO: We need to replace the window control bar for the window.

	private final Properties PROPERTIES;

	private final UiService UI_SERVICE;

	private final Stage STAGE;

	private Scene scene;

	private final UserConfiguration USER_CONFIGURATION;

	//This is the reference to the controller for the bar located in the top section of the main borderpane
	private final MenuBarView MENU_BAR_VIEW;

	//This is the reference to the meat and potatoes of the UI, the actual controls located in the center of the UI responsible for managing modlists
	private final ModlistManagerView MODLIST_MANAGER_VIEW;

	//This is the reference to the controller for the bar located in the bottom section of the main borderpane
	private final StatusBarView STATUS_BAR_VIEW;

	//Initializes our controller while maintaining the empty constructor JavaFX expects
	public MainWindowView(Properties properties, Stage stage, MenuBarView menuBarView, ModlistManagerView modlistManagerView, StatusBarView statusBarView, UiService uiService) {
		this.STAGE = stage;
		this.PROPERTIES = properties;
		this.USER_CONFIGURATION = uiService.getUSER_CONFIGURATION();
		this.UI_SERVICE = uiService;
		this.MENU_BAR_VIEW = menuBarView;
		this.MODLIST_MANAGER_VIEW = modlistManagerView;
		this.STATUS_BAR_VIEW = statusBarView;
	}

	public void initView(Parent mainViewRoot, Parent menuBarRoot, Parent modlistManagerRoot, Parent statusBarRoot) throws XmlPullParserException, IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
		//Prepare the UI
		setupWindow(mainViewRoot);
		MENU_BAR_VIEW.initView();
		MODLIST_MANAGER_VIEW.initView(MENU_BAR_VIEW.getLogToggle(), MENU_BAR_VIEW.getModDescriptionToggle());
		STATUS_BAR_VIEW.initView();
		mainWindowLayout.setTop(menuBarRoot);
		mainWindowLayout.setCenter(modlistManagerRoot);
		mainWindowLayout.setBottom(statusBarRoot);


		final ObservableList<SaveProfile> SAVE_PROFILES = UI_SERVICE.getSAVE_PROFILES();

		//Prompt the user to remove any saves that no longer exist on the file system.
		if (SAVE_PROFILES.size() != 1 &&
				!SAVE_PROFILES.getFirst().getSaveName().equals("None") &&
				!SAVE_PROFILES.getFirst().getProfileName().equals("None") &&
				SAVE_PROFILES.getFirst().getSavePath() != null) {
			for (int i = 0; i < SAVE_PROFILES.size(); i++) {
				if (Files.notExists(Path.of(SAVE_PROFILES.get(i).getSavePath()))) {
					SAVE_PROFILES.get(i).setSaveExists(false);
					String errorMessage = "The save associated with the profile \"" + SAVE_PROFILES.get(i).getProfileName() + "\" was not found. Do you want " +
							"to remove this profile from the managed saves?";
					UI_SERVICE.log("Save \"" + SAVE_PROFILES.get(i).getSaveName() + "\" is missing from the disk.", MessageType.ERROR);

					int choice = Popup.displayYesNoDialog(errorMessage, MessageType.WARN);
					if (choice == 1) {
						UI_SERVICE.log("Removing save " + SAVE_PROFILES.get(i).getSaveName() + ".", MessageType.INFO);
						SAVE_PROFILES.remove(i);
						i--;
					}
				} else {
					SAVE_PROFILES.get(i).setSaveExists(true);
				}
			}
		}
		mainWindowLayout.setOnDragOver(MODLIST_MANAGER_VIEW::handleModTableDragOver);

		minimizeButton.setStyle("-fx-background-radius: 0;" +
				"-fx-text-fill: -color-button-fg;" +
				"-color-button-bg: -color-bg-default;" +
				"-color-button-border-pressed: transparent;" +
				"-color-button-border: transparent;");

		maximizeRestoreButton.setStyle("-fx-background-radius: 0;" +
				"-fx-text-fill: -color-button-fg;" +
				"-color-button-bg: -color-bg-default;" +
				"-color-button-border-pressed: transparent;" +
				"-color-button-border: transparent;");

		closeButton.setStyle("-fx-background-radius: 0;" +
				"-fx-text-fill: -color-button-fg;" +
				"-color-button-bg: -color-bg-default;" +
				"-color-button-border-pressed: transparent;" +
				"-color-button-border: transparent;");
	}

	/**
	 * Sets the basic properties of the window for the application, including the title bar, minimum resolutions, and listeners.
	 */
	private void setupWindow(Parent root) throws IOException, XmlPullParserException {
		this.scene = new Scene(root);
		//Prepare the scene
		int minWidth = Integer.parseInt(PROPERTIES.getProperty("semm.mainView.resolution.minWidth"));
		int minHeight = Integer.parseInt(PROPERTIES.getProperty("semm.mainView.resolution.minHeight"));

		//Prepare the stage
		STAGE.setScene(scene);
		STAGE.setMinWidth(minWidth);
		STAGE.setMinHeight(minHeight);

		//Add title and icon to the stage
		MavenXpp3Reader reader = new MavenXpp3Reader();
		Model model = reader.read(new FileReader("pom.xml"));

		STAGE.setTitle("SEMM v" + model.getVersion());
		STAGE.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResourceAsStream("/icons/logo.png"))));

		//Add a listener to make the slider on the split pane stay at the bottom of our window when resizing it when it shouldn't be visible
		STAGE.heightProperty().addListener((obs, oldVal, newVal) -> {
			if (!MODLIST_MANAGER_VIEW.isMainViewSplitDividerVisible()) {
				MODLIST_MANAGER_VIEW.getMainViewSplit().setDividerPosition(0, 1);
			}
		});
	}

	@FXML
	private void minimize() {
		STAGE.setIconified(true);
	}

	//TODO: Implement functionality
	//TODO: Change the icon to a better one. It's blurry.
	//TODO: This bar stuff all needs to be made into its own class and attached at the top of every window. Make it an FXML file on its own.
	// We also need to add the app icon and a title to it all
	@FXML
	private void maximizeOrRestore() {
		if(maximizeRestoreIcon.getIconLiteral().equals("codicon-chrome-maximize")) {
			maximizeRestoreIcon.setIconLiteral("codicon-chrome-restore");
		} else {
			maximizeRestoreIcon.setIconLiteral("codicon-chrome-maximize");
		}

		System.out.println("Max or mini!");
	}

	@FXML
	private void closeSemm() {
		Platform.exit();
	}
}
