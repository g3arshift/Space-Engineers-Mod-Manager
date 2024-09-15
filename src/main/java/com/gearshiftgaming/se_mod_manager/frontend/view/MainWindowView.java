package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.ModProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.LogMessage;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.LogCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.ModCell;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import lombok.Getter;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * This represents the main window of the application, with a border pane at its core.
 * It contains the center section of the borderpane, but all other sections should be delegated to their own controllers and FXML files.
 * <p>
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 * @author Gear Shift
 */
@Getter
public class MainWindowView {

	//FXML Items
	@FXML
	private BorderPane mainWindowLayout;

	@FXML
	private ComboBox<String> modImportDropdown;

	@FXML
	private Button importModlist;

	@FXML
	private Button exportModlist;

	@FXML
	private Button saveModlist;

	@FXML
	private Button resetModlist;

	@FXML
	private Button injectModlist;

	@FXML
	private Button launchSpaceEngineers;

	@FXML
	private SplitPane mainViewSplit;

	//TODO: Low priority: Fix the alignment of the table headers to be centered, not center left.
	@FXML
	private TableView<ModCell> modTable; //TODO: Create a new object for the mod items that contains a check box and all the context menu stuff

	//TODO: The modcell needs properties for each of these https://stackoverflow.com/questions/53751455/how-to-create-a-javafx-tableview-without-warnings
	@FXML
	private TableColumn<ModCell, String> modName;

	@FXML
	private TableColumn<ModCell, String> modType;

	@FXML
	private TableColumn<ModCell, String> modVersion;

	@FXML
	private TableColumn<ModCell, String> modLastUpdated;

	@FXML
	private TableColumn<ModCell, Integer> loadPriority;

	@FXML
	private TableColumn<ModCell, String> modSource;

	@FXML
	private TableColumn<ModCell, String> modCategory;

	@FXML
	private TabPane informationPane;

	@FXML
	private Tab logTab;

	@FXML
	private Tab modDescriptionTab;

	@FXML
	private ListView<LogMessage> viewableLog;

	//TODO: We need to replace the window control bar for the window.
	private ObservableList<LogMessage> userLog;

	private Properties properties;

	private UiService uiService;

	private Stage stage;

	private Scene scene;

	private boolean mainViewSplitDividerVisible = true;

	private UserConfiguration userConfiguration;

	//This is just a wrapper for the userConfiguration modProfiles list. Any changes made to this will propagate back to it, but not the other way around.
	private ObservableList<ModProfile> modProfiles;

	//This is just a wrapper for the userConfiguration saveProfiles list. Any changes made to this will propagate back to it, but not the other way around.
	private ObservableList<SaveProfile> saveProfiles;

	//This is the reference to the controller for the bar located in the top section of the main borderpane
	private MenuBarView menuBarView;

	//This is the reference to the controller for the bar located in the bottom section of the main borderpane
	private StatusBarView statusBarView;

	//Initializes our controller while maintaining the empty constructor JavaFX expects
	public void initView(Properties properties,
						 Stage stage, Parent root,
						 ModProfileManagerView modProfileManagerView, SaveManagerView saveManagerView,
						 MenuBarView menuBarView, Parent menuBarRoot,
						 StatusBarView statusBarView, Parent statusBarRoot,
						 UiService uiService) throws IOException, XmlPullParserException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
		this.scene = new Scene(root);
		this.stage = stage;
		this.properties = properties;
		this.userConfiguration = uiService.getUserConfiguration();
		this.uiService = uiService;
		this.userLog = this.uiService.getUserLog();
		this.menuBarView = menuBarView;
		this.statusBarView = statusBarView;

		modProfiles = uiService.getModProfiles();
		saveProfiles = uiService.getSaveProfiles();

		//Prepare the UI
		setupWindow();
		menuBarView.initView(this, uiService, modProfileManagerView, saveManagerView);
		statusBarView.initView(this, uiService);
		mainWindowLayout.setTop(menuBarRoot);
		mainWindowLayout.setBottom(statusBarRoot);
		setupMainViewItems();

		uiService.saveUserData();

		//Prompt the user to remove any saves that no longer exist on the file system.
		if (saveProfiles.size() != 1 &&
				!saveProfiles.getFirst().getSaveName().equals("None") &&
				!saveProfiles.getFirst().getProfileName().equals("None") &&
				saveProfiles.getFirst().getSavePath() != null) {
			for (int i = 0; i < saveProfiles.size(); i++) {
				if (Files.notExists(Path.of(saveProfiles.get(i).getSavePath()))) {
					saveProfiles.get(i).setSaveExists(false);
					String errorMessage = "The save associated with the profile \"" + saveProfiles.get(i).getProfileName() + "\" was not found. Do you want " +
							"to remove this profile from the managed saves?";
					uiService.log("Save \"" + saveProfiles.get(i).getSaveName() + "\" is missing from the disk.", MessageType.ERROR);

					int choice = Popup.displayYesNoDialog(errorMessage, MessageType.WARN);
					if (choice == 1) {
						uiService.log("Removing save " + saveProfiles.get(i).getSaveName() + ".", MessageType.INFO);
						saveProfiles.remove(i);
						i--;
					}
				} else {
					saveProfiles.get(i).setSaveExists(true);
				}
			}
			uiService.saveUserData();
		}
	}

	//TODO: If our mod profile is null but we make a save, popup mod profile UI too. And vice versa for save profile.

	/**
	 * Sets the basic properties of the window for the application, including the title bar, minimum resolutions, and listeners.
	 */
	private void setupWindow() throws IOException, XmlPullParserException {
		//Prepare the scene
		int minWidth = Integer.parseInt(properties.getProperty("semm.mainView.resolution.minWidth"));
		int minHeight = Integer.parseInt(properties.getProperty("semm.mainView.resolution.minHeight"));

		//Prepare the stage
		stage.setScene(scene);
		stage.setMinWidth(minWidth);
		stage.setMinHeight(minHeight);

		//Add title and icon to the stage
		MavenXpp3Reader reader = new MavenXpp3Reader();
		Model model = reader.read(new FileReader("pom.xml"));
		stage.setTitle("SEMM v" + model.getVersion());
		stage.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResourceAsStream("/icons/logo.png"))));

		//Add a listener to make the slider on the split pane stay at the bottom of our window when resizing it when it shouldn't be visible
		stage.heightProperty().addListener((obs, oldVal, newVal) -> {
			if (!this.isMainViewSplitDividerVisible()) {
				this.getMainViewSplit().setDividerPosition(0, 1);
			}
		});
	}

	//TODO: Make it so that when we change the modlist and save it, but don't inject it, the status becomes "Modified since last injection"
	//TODO: Set a limit on the modprofile and saveprofile maximum sizes that's reasonable. If they're too large they messup the appearance of the prompt text for the search bar.
	public void setupMainViewItems() {
		viewableLog.setItems(userLog);
		viewableLog.setCellFactory(param -> new LogCell());
		//Disable selecting rows in the log.
		viewableLog.setSelectionModel(null);

		modImportDropdown.getItems().addAll("Add mods by Steam Workshop ID", "Add mods from Steam Collection", "Add mods from Mod.io", "Add mods from modlist file");

		//TODO: Much of this needs to happen down in the service layer
		//TODO: Setup a function in ModList service to track conflicts.
		//TODO: Populate mod table
	}
	//TODO: Hookup all the buttons to everything

	@FXML
	private void closeLogTab() {
		menuBarView.getLogToggle().setSelected(false);
		if (informationPane.getTabs().isEmpty()) {
			disableSplitPaneDivider();
		}
	}

	@FXML
	private void closeModDescriptionTab() {
		menuBarView.getModDescriptionToggle().setSelected(false);
		if (informationPane.getTabs().isEmpty()) {
			disableSplitPaneDivider();
		}
	}


	private void importModlist() {
		//TODO: Implement. Allow importing modlists from either sandbox file or exported list.
	}

	@FXML
	private void exportModlist() {
		//TODO: Implement. Export in our own format (use XML).
	}

	@FXML
	private void saveModlist() {
		//TODO: Implement
	}

	@FXML
	private void resetModlist() {
		//TODO: Implement by setting the current modprofile modlist to whatever it is in our user configuration.
		// Make a popup asking if they're sure they want to reset the modlist.
		// Also make a popup if the list was never saved and can't be found in the user configuration.
	}

	//Apply the modlist the user is currently using to the save profile they're currently using.
	//TODO: This whole thing likely need rewritten
	@FXML
	private void applyModlist() throws IOException {
//		SaveProfile currentSaveProfile = uiService.getCurrentSaveProfile();
//		ModProfile currentModProfile = uiService.getCurrentModProfile();
//		//This should only return null when the SEMM has been run for the first time and the user hasn't made and modlists or save profiles.
//		if (currentSaveProfile != null && currentModProfile != null && currentSaveProfile.getSavePath() != null) {
//			//TODO: Have a warning popup asking the user if they want to continue IF they have a mod profile that contains no mods.
//			Result<Void> modlistResult = uiService.applyModlist(currentModProfile.getModList(), currentSaveProfile.getSavePath());
//			uiService.log(modlistResult);
//			if (!modlistResult.isSuccess()) {
//				currentSaveProfile.setLastSaveStatus(SaveStatus.FAILED);
//			} else {
//				currentSaveProfile.setLastAppliedModProfileId(currentModProfile.getId());
//
//				//TODO: This and the currentSave profile are both null, but they aren't actually. Why? This logic probably needs all looked over and rewritten.
//				int modProfileIndex = modProfiles.indexOf(currentModProfile);
//				modProfiles.set(modProfileIndex, currentModProfile);
//
//				int saveProfileIndex = saveProfiles.indexOf(currentSaveProfile);
//				saveProfiles.set(saveProfileIndex, currentSaveProfile);
//
//				uiService.log(uiService.saveUserData(userConfiguration));
//				currentSaveProfile.setLastSaveStatus(SaveStatus.SAVED);
//			}
//			statusBarView.update(currentSaveProfile);
//		} else {
//			//Save or Mod profile not setup yet. Create both a Save and Mod profile to be able to apply a modlist.
//			String errorMessage = "Save profile not setup yet. Create a save profile to apply a modlist.";
//			uiService.log(errorMessage, MessageType.ERROR);
//			Popup.displaySimpleAlert(errorMessage, stage, MessageType.ERROR);
//		}
	}

	@FXML
	private void launchSpaceEngineers() throws URISyntaxException, IOException {
		//TODO: Check this works on systems with no previous steam url association
		Desktop.getDesktop().browse(new URI("steam://rungameid/244850"));
	}

	protected void disableSplitPaneDivider() {
		for (Node node : mainViewSplit.lookupAll(".split-pane-divider")) {
			node.setVisible(false);
			mainViewSplitDividerVisible = false;
		}
		mainViewSplit.setDividerPosition(0, 1);
	}

	protected void enableSplitPaneDivider() {
		for (Node node : mainViewSplit.lookupAll(".split-pane-divider")) {
			node.setVisible(true);
			mainViewSplitDividerVisible = true;
		}
		mainViewSplit.setDividerPosition(0, 0.7);
	}
}
