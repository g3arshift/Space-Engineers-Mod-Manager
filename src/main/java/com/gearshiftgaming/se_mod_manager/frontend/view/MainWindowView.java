package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.*;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.LogMessage;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.LogCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.ModNameCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.ModTableRowFactory;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Getter;
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
import java.util.List;

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
 */
@Getter
public class MainWindowView {

	//TODO: Remove all menu options under file, except for "Close"

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
	private Button resetModlist;

	@FXML
	private Button injectModlist;

	@FXML
	private Button launchSpaceEngineers;

	@FXML
	private SplitPane mainViewSplit;

	//TODO: Low priority: Fix the alignment of the table headers to be centered, not center left.
	@FXML
	private TableView<Mod> modTable;

	@FXML
	private TableColumn<Mod, Mod> modName;

	@FXML
	private TableColumn<Mod, String> modType;

	@FXML
	private TableColumn<Mod, String> modVersion;

	@FXML
	private TableColumn<Mod, String> modLastUpdated;

	@FXML
	private TableColumn<Mod, String> loadPriority;

	@FXML
	private TableColumn<Mod, String> modSource;

	@FXML
	private TableColumn<Mod, String> modCategory;

	@FXML
	private HBox tableActions;

	@FXML
	private TabPane informationPane;

	@FXML
	private Tab logTab;

	@FXML
	private Tab modDescriptionTab;

	@FXML
	private ListView<LogMessage> viewableLog;

	//TODO: We need to replace the window control bar for the window.
	private final ObservableList<LogMessage> USER_LOG;

	private final Properties PROPERTIES;

	private final UiService UI_SERVICE;

	private final Stage STAGE;

	private Scene scene;

	private boolean mainViewSplitDividerVisible = true;

	private final UserConfiguration USER_CONFIGURATION;

	//This is just a wrapper for the userConfiguration modProfiles list. Any changes made to this will propagate back to it, but not the other way around.
	private final ObservableList<ModProfile> MOD_PROFILES;

	//This is just a wrapper for the userConfiguration saveProfiles list. Any changes made to this will propagate back to it, but not the other way around.
	private final ObservableList<SaveProfile> SAVE_PROFILES;

	//This is the reference to the controller for the bar located in the top section of the main borderpane
	private final MenuBarView MENU_BAR_VIEW;

	//This is the reference to the controller for the bar located in the bottom section of the main borderpane
	private final StatusBarView STATUS_BAR_VIEW;

	private final DataFormat SERIALIZED_MIME_TYPE;

	private final ListChangeListener<TableColumn<Mod, ?>> SORT_LISTENER;

	private final double SCROLL_THRESHOLD;

	private final double SCROLL_SPEED;

	private Timeline scrollTimeline;

	//Initializes our controller while maintaining the empty constructor JavaFX expects
	public MainWindowView(Properties properties, Stage stage, MenuBarView menuBarView, StatusBarView statusBarView, UiService uiService) {
		this.STAGE = stage;
		this.PROPERTIES = properties;
		this.USER_CONFIGURATION = uiService.getUSER_CONFIGURATION();
		this.UI_SERVICE = uiService;
		this.USER_LOG = this.UI_SERVICE.getUSER_LOG();
		this.MENU_BAR_VIEW = menuBarView;
		this.STATUS_BAR_VIEW = statusBarView;

		MOD_PROFILES = uiService.getMOD_PROFILES();
		SAVE_PROFILES = uiService.getSAVE_PROFILES();

		SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");

		SCROLL_THRESHOLD = 30.0;
		SCROLL_SPEED = 0.6;

		SORT_LISTENER = change -> {
			if (modTable.getSortOrder().isEmpty()) {
				applyDefaultSort();
			}
		};
	}

	public void initView(Parent mainViewRoot, Parent menuBarRoot, Parent statusBarRoot) throws XmlPullParserException, IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
		//Prepare the UI
		setupWindow(mainViewRoot);
		MENU_BAR_VIEW.initView(this);
		STATUS_BAR_VIEW.initView();
		mainWindowLayout.setTop(menuBarRoot);
		mainWindowLayout.setBottom(statusBarRoot);
		setupMainViewItems();

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
		setupModTable();
		mainWindowLayout.setOnDragOver(this::handleDragOver);
	}

	@FXML
	private void closeLogTab() {
		MENU_BAR_VIEW.getLogToggle().setSelected(false);
		if (informationPane.getTabs().isEmpty()) {
			disableSplitPaneDivider();
		}
	}

	@FXML
	private void closeModDescriptionTab() {
		MENU_BAR_VIEW.getModDescriptionToggle().setSelected(false);
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

	//TODO: Allow for adding/removing columns. Add a context menu to the column header.
	private void setupModTable() {

		modTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		modTable.setRowFactory(new ModTableRowFactory(UI_SERVICE, SERIALIZED_MIME_TYPE));

		modName.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
		modName.setCellFactory(param -> new ModNameCell(UI_SERVICE));
		modName.setComparator(Comparator.comparing(Mod::getFriendlyName));

		//Format the appearance, styling, and menu`s of our table cells, rows, and columns
		modVersion.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getModVersion()));
		modLastUpdated.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLastUpdated() != null ?
				cellData.getValue().getLastUpdated().toString() : "Unknown"));

		loadPriority.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().getLoadPriority())));

		modType.setCellValueFactory(cellData -> new SimpleStringProperty((cellData.getValue().getModType().equals(ModType.STEAM) ? "Steam" : "Mod.io")));
		modCategory.setCellValueFactory(cellData -> {
			StringBuilder sb = new StringBuilder();
			List<String> categories = cellData.getValue().getCategories();
			for (int i = 0; i < cellData.getValue().getCategories().size(); i++) {
				if (i + 1 < cellData.getValue().getCategories().size()) {
					sb.append(categories.get(i)).append(", ");
				} else {
					sb.append(categories.get(i));
				}
			}
			return new SimpleStringProperty(sb.toString());
		});

		modTable.getSortOrder().addListener(SORT_LISTENER);

		modTable.setItems(UI_SERVICE.getCurrentModList());
	}

	//TODO: If our mod profile is null but we make a save, popup mod profile UI too. And vice versa for save profile.

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
			if (!this.isMainViewSplitDividerVisible()) {
				this.getMainViewSplit().setDividerPosition(0, 1);
			}
		});
	}

	//TODO: Make it so that when we change the modlist and save it, but don't inject it, the status becomes "Modified since last injection"
	//TODO: Set a limit on the modprofile and saveprofile maximum sizes that's reasonable. If they're too large they messup the appearance of the prompt text for the search bar.
	public void setupMainViewItems() {
		viewableLog.setItems(USER_LOG);
		viewableLog.setCellFactory(param -> new LogCell());
		//Disable selecting rows in the log.
		viewableLog.setSelectionModel(null);

		modImportDropdown.getItems().addAll("Add mods by Steam Workshop ID", "Add mods from Steam Collection", "Add mods from Mod.io", "Add mods from modlist file");

		//TODO: Setup a function in ModList service to track conflicts.
	}
	//TODO: Hookup all the buttons to everything

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

	private void applyDefaultSort() {
		if (loadPriority != null) {
			modTable.getSortOrder().removeListener(SORT_LISTENER);

			loadPriority.setSortType(TableColumn.SortType.ASCENDING);
			modTable.getSortOrder().add(loadPriority);
			modTable.sort();
			modTable.getSortOrder().clear();

			modTable.getSortOrder().addListener(SORT_LISTENER);
		}
	}

	//TODO: Increase speed based on distance from the edge
	private void handleDragOver(DragEvent event) {
		double y = event.getY();
		double modTableTop = modTable.localToScene(modTable.getBoundsInLocal()).getMinY();
		double modTableBottom = modTable.localToScene(modTable.getBoundsInLocal()).getMaxY();

		ScrollBar verticalScrollBar = (ScrollBar) modTable.lookup(".scroll-bar:vertical");

		double currentScrollValue = verticalScrollBar.getValue();
		double minScrollValue = verticalScrollBar.getMin();
		double maxScrollValue = verticalScrollBar.getMax();
		double scrollAmount;

		//Scroll up
		if (y < modTableTop + SCROLL_THRESHOLD && currentScrollValue > minScrollValue) {
			scrollAmount = -SCROLL_SPEED * 0.1; // Increase speed
		}
		//Scroll down
		else if (y > modTableBottom - SCROLL_THRESHOLD && currentScrollValue < maxScrollValue) {
			scrollAmount = SCROLL_SPEED * 0.1; // Increase speed
		} else {
			scrollAmount = 0;
		}

		//Stop our timeline if it exists. If we don't call this then the timeline runs forever and the scroll wheel won't work, and probably a bunch of other things.
		if (scrollTimeline != null) {
			scrollTimeline.stop();
		}

		if (scrollAmount != 0) {
			// Create a new Timeline to update the scroll position over time
			scrollTimeline = new Timeline(
					new KeyFrame(Duration.millis(30), e -> {  // Increase the duration for smoother transitions. Too high will break it though.
						double newValue = currentScrollValue + scrollAmount;

						// Interpolate the new scroll value to create smooth transitions
						newValue = Math.max(minScrollValue, Math.min(maxScrollValue, newValue));

						verticalScrollBar.setValue(newValue);
					})
			);
			scrollTimeline.setCycleCount(Animation.INDEFINITE); // Keep updating until stopped
			scrollTimeline.play(); // Start the scrolling animation
		}

		event.acceptTransferModes(TransferMode.MOVE);
		event.consume();
	}
}
