package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.*;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.LogCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.ModImportType;
import com.gearshiftgaming.se_mod_manager.frontend.models.ModNameCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.ModTableRowFactory;
import com.gearshiftgaming.se_mod_manager.frontend.view.helper.ModlistManagerHelper;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.Popup;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ModlistManagerView {
	@FXML
	private ComboBox<String> modImportDropdown;

	@FXML
	private Button manageModProfiles;

	@FXML
	private Button manageSaveProfiles;

	@FXML
	private Button importModlist;

	@FXML
	private Button exportModlist;

	@FXML
	private Button applyModlist;

	@FXML
	private Button launchSpaceEngineers;

	@FXML
	@Getter
	private SplitPane mainViewSplit;

	@FXML
	@Getter
	private TableView<Mod> modTable;

	@FXML
	private TableColumn<Mod, Mod> modName;

	@FXML
	private TableColumn<Mod, String> modType;


	@FXML
	private TableColumn<Mod, String> modLastUpdated;

	@FXML
	private TableColumn<Mod, Integer> loadPriority;

	@FXML
	private TableColumn<Mod, String> modSource;

	@FXML
	private TableColumn<Mod, String> modCategory;

	@FXML
	private HBox actions;

	@FXML
	@Getter
	private TabPane informationPane;

	@FXML
	@Getter
	private Tab logTab;

	@FXML
	@Getter
	private Tab modDescriptionTab;

	@FXML
	private StackPane modDescriptionBackground;

	@FXML
	@Getter
	private WebView modDescription;

	@FXML
	private ListView<LogMessage> viewableLog;

	@FXML
	private StackPane modAdditionProgressPanel;

	@FXML
	private ProgressBar modAdditionProgressBar;

	@FXML
	private ProgressIndicator modAdditionProgressIndicator;

	@FXML
	private Label modAdditionProgressActionName;

	@FXML
	private Label modAdditionProgressNumerator;

	@FXML
	private Label modAdditionProgressDivider;

	@FXML
	private Label modAdditionProgressDenominator;

	@FXML
	private ProgressIndicator modAdditionProgressWheel;

	@FXML
	private Label modAdditionSteamCollectionName;

	private final UiService UI_SERVICE;

	private final ObservableList<LogMessage> USER_LOG;

	@Getter
	private boolean mainViewSplitDividerVisible = true;

	private final DataFormat SERIALIZED_MIME_TYPE;

	private ListChangeListener<TableColumn<Mod, ?>> sortListener;

	@Getter
	private Timeline scrollTimeline;

	private final List<Mod> SELECTIONS;

	@Getter
	@Setter
	//This is a really dumb hack that we have to use to actually get a row as it is styled in the application.
	private TableRow<Mod> singleTableRow;

	@Getter
	@Setter
	private TableRow<Mod> previousRow;
	private CheckMenuItem logToggle;

	private CheckMenuItem modDescriptionToggle;

	//This is the reference to the controller for the bar located in the bottom section of the main borderpane. We need everything in it so might as well get the whole reference.
	//TODO: Hook this up
	private final StatusBarView STATUS_BAR_VIEW;

	private final ModlistManagerHelper MODLIST_MANAGER_HELPER;

	@Getter
	private ScrollBar modTableVerticalScrollBar;

	private TableHeaderRow headerRow;

	private final ModProfileManagerView MOD_PROFILE_MANAGER_VIEW;

	private final SaveManagerView SAVE_MANAGER_VIEW;

	@Getter
	private FilteredList<Mod> filteredModList;

	private final Stage STAGE;

	private final SimpleInputView ID_AND_URL_MOD_ADDITION_INPUT;

	private final String MOD_DATE_FORMAT;

	private final Pattern STEAM_WORKSHOP_ID_REGEX_PATTERN;

	//These three are here purely so we can enable and disable them when we add mods to prevent user interaction from breaking things.
	private ComboBox<ModProfile> modProfileDropdown;
	private ComboBox<SaveProfile> saveProfileDropdown;
	private TextField modTableSearchField;


	public ModlistManagerView(UiService uiService, Stage stage, Properties properties, StatusBarView statusBarView,
							  ModProfileManagerView modProfileManagerView, SaveManagerView saveManagerView, SimpleInputView modAdditionInputView) {
		this.UI_SERVICE = uiService;
		this.STAGE = stage;
		this.USER_LOG = uiService.getUSER_LOG();
		this.STATUS_BAR_VIEW = statusBarView;
		this.MODLIST_MANAGER_HELPER = new ModlistManagerHelper();
		this.ID_AND_URL_MOD_ADDITION_INPUT = modAdditionInputView;
		this.STEAM_WORKSHOP_ID_REGEX_PATTERN = Pattern.compile("(id=[0-9])\\d*");

		this.MOD_PROFILE_MANAGER_VIEW = modProfileManagerView;
		this.SAVE_MANAGER_VIEW = saveManagerView;

		this.MOD_DATE_FORMAT = properties.getProperty("semm.mod.dateFormat");

		SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");
		SELECTIONS = new ArrayList<>();

		filteredModList = new FilteredList<>(UI_SERVICE.getCurrentModList(), mod -> true);
	}

	public void initView(CheckMenuItem logToggle, CheckMenuItem modDescriptionToggle, int modTableCellSize,
						 ComboBox<ModProfile> modProfileDropdown, ComboBox<SaveProfile> saveProfileDropdown, TextField modTableSearchField) {
		this.logToggle = logToggle;
		this.modDescriptionToggle = modDescriptionToggle;

		this.modProfileDropdown = modProfileDropdown;
		this.saveProfileDropdown = saveProfileDropdown;
		this.modTableSearchField = modTableSearchField;

		sortListener = change -> {
			if (modTable.getSortOrder().isEmpty()) {
				applyDefaultSort();
			}
		};

		setupMainViewItems();
		setupModTable(modTableCellSize);
		actions.setOnDragDropped(this::handleTableActionsOnDragDrop);
		actions.setOnDragOver(this::handleTableActionsOnDragOver);
		actions.setOnDragExited(this::handleTableActionsOnDragExit);

		//Setup the mod description handlers
		modTable.getSelectionModel().selectedItemProperty().addListener((observableValue, mod, t1) -> {
			Mod selectedMod = modTable.getSelectionModel().getSelectedItem();
			if (selectedMod != null) {
				modDescription.getEngine().loadContent(selectedMod.getDescription());
			} else {
				modDescription.getEngine().loadContent("");
			}
		});

		modDescription.getEngine().getLoadWorker().stateProperty().addListener((observableValue, oldState, newState) -> {
			if (newState == Worker.State.SUCCEEDED) {
				redirectHyperlinks();
			}
		});

		modDescription.getEngine().setUserStyleSheetLocation("file:src/main/resources/styles/mod-description_primer-light.css");
		modDescriptionBackground.setStyle("-fx-border-color: -color-border-default; -fx-border-width:1px");
		modDescription.setContextMenuEnabled(false);

		String activeThemeName = StringUtils.substringAfter(Application.getUserAgentStylesheet(), "theme/");
		modDescription.getEngine().setUserStyleSheetLocation("file:src/main/resources/styles/mod-description_" + activeThemeName);

		//TODO: These aren't updating properly.
		modAdditionProgressNumerator.textProperty().bind(UI_SERVICE.getModAdditionProgressNumeratorProperty().asString());
		modAdditionProgressDenominator.textProperty().bind(UI_SERVICE.getModAdditionProgressDenominatorProperty().asString());
		modAdditionProgressBar.progressProperty().bind(UI_SERVICE.getModAdditionProgressPercentageProperty());

		modAdditionSteamCollectionName.setVisible(false);
		viewableLog.setFixedCellSize(35);

		//This is a dumb hack, but it swallows the drag events otherwise when we drag rows over it.
		modDescription.setOnDragOver(dragEvent -> {
		});

		UI_SERVICE.logPrivate("Successfully initialized modlist manager.", MessageType.INFO);
	}

	//TODO: If our mod profile is null but we make a save, popup mod profile UI too. And vice versa for save profile.
	//TODO: Allow for adding/removing columns. Add a context menu to the column header.
	private void setupModTable(int modTableCellSize) {
		//Format the appearance, styling, and menu`s of our table cells, rows, and columns
		modTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		modTable.setRowFactory(new ModTableRowFactory(UI_SERVICE, SERIALIZED_MIME_TYPE, SELECTIONS, this, MODLIST_MANAGER_HELPER));

		modName.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
		modName.setCellFactory(param -> new ModNameCell(UI_SERVICE));
		modName.setComparator(Comparator.comparing(Mod::getFriendlyName));

		//Create a comparator for the date column so it sorts properly.
		modLastUpdated.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLastUpdated() != null ?
				cellData.getValue().getLastUpdated().format(DateTimeFormatter.ofPattern(MOD_DATE_FORMAT)) : "Unknown"));
		modLastUpdated.setComparator((date1, date2) -> LocalDateTime.parse(date1, DateTimeFormatter.ofPattern(MOD_DATE_FORMAT))
				.compareTo(LocalDateTime.parse(date2, DateTimeFormatter.ofPattern(MOD_DATE_FORMAT))));

		loadPriority.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getLoadPriority()).asObject());

		modType.setCellValueFactory(cellData -> new SimpleStringProperty((cellData.getValue().getModType().equals(ModType.STEAM) ? "Steam" : "Mod.io")));

		//Perform some stylistic formatting for the tags/category column
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

		//Add a listener so that we can have some custom behavior on sorting
		modTable.getSortOrder().addListener(sortListener);

		//Update the mod contents by wrapping the observable list with a filtered list and then that filtered list with a sorted list.
		//This gives us both sorting and searching.
		updateModTableContents();

		modTableVerticalScrollBar = (ScrollBar) modTable.lookup(".scroll-bar:vertical");
		headerRow = (TableHeaderRow) modTable.lookup("TableHeaderRow");

		modTable.setFixedCellSize(modTableCellSize);
	}


	//TODO: Make it so that when we change the modlist but don't inject it, the status becomes "Modified since last injection". Will have to happen in the modnamecell and row factory.
	public void setupMainViewItems() {
		viewableLog.setItems(USER_LOG);
		viewableLog.setCellFactory(param -> new LogCell());
		//Disable selecting rows in the log.
		viewableLog.setSelectionModel(null);

		// Just do this by manually setting the selected item after we select an item. To actually call code, call one function on selection/action in the dropdown, that determines which function to call and do stuff in the rest of the code, then reset the selected item.
		modImportDropdown.getItems().addAll("Add mods from...",
				ModImportType.STEAM_ID.getName(),
				ModImportType.STEAM_COLLECTION.getName(),
				ModImportType.MOD_IO.getName(),
				ModImportType.FILE.getName());


		//TODO: Setup a function in ModList service to track conflicts.
	}

	//TODO: Hookup all the buttons to everything
	@FXML
	private void addMod() {
		ModImportType selectedImportOption = ModImportType.fromString(modImportDropdown.getSelectionModel().getSelectedItem());
		modImportDropdown.getSelectionModel().selectFirst();
		modImportDropdown.setValue(modImportDropdown.getSelectionModel().getSelectedItem());

		//TODO: Popup based on result if bad. If good, no popup. For a collection import, only ONE POPUP with all the details of the error, with some window size limits and a scrollpane.

		if (selectedImportOption != null) {
			switch (selectedImportOption) {
				case STEAM_ID -> addModFromSteamId();
				case STEAM_COLLECTION -> addModsFromSteamCollection();
				case MOD_IO -> addModFromModIoId();
				case FILE -> addModsFromFile();
			}
		}
	}

	//TODO: We need a cancel button for adding mods so if the user decides they want to stop, they can.
	// In it, when they cancel, ask them "Do you want to add the already processed mods?", and if yes, add the ones we've already scraped.
	// Probably need to clear the rest of the futures/kill their threads when that happens. Also need to pause those threads when we hit cancel.

	private void addModFromSteamId() {
		setModAddingInputViewText("Steam Workshop Mod URL/ID",
				"Enter the Steam Workshop URL/ID",
				"Workshop URL/Mod ID",
				"URL/ID cannot be blank!");


		String modId = getModLocationFromUser(false);
		if (!modId.isBlank()) {
			Mod mod = new Mod(modId, ModType.STEAM);

			//This is a bit hacky, but it makes a LOT less code we need to maintain.
			final Mod[] modList = new Mod[1];
			modList[0] = mod;
			getModAdditionThread(List.of(modList)).start();
		}
	}

	private void addModsFromSteamCollection() {
		//TODO: Check it's from the right game before anything else. Gonna have to scrape the page.
		setModAddingInputViewText("Steam Workshop Collection URL/ID",
				"Enter the URL/ID for the Steam Workshop Collection",
				"Collection URL/ID",
				"URL/ID cannot be blank!");

		String modId = getModLocationFromUser(true);
		if (!modId.isBlank()) {
			try {
				getSteamModCollectionThread(modId).start();
			} catch (RuntimeException e) {
				UI_SERVICE.log(e);
				Popup.displaySimpleAlert(String.valueOf(e), STAGE, MessageType.ERROR);
			}
		}
	}

	private void addModFromModIoId() {
		//TODO: Check it's from the right game before anything else. Gonna have to scrape the page.
		//TODO: The actual adding to the modlist should happen here
		setModAddingInputViewText("Mod.io Mod URL/ID",
				"Enter the Mod.io URL or ID",
				"Mod.io URL/ID",
				"URL/ID cannot be blank!");

		String modId = getModLocationFromUser(false);
		if (!modId.isBlank()) {
			Mod mod = new Mod(modId, ModType.MOD_IO);

			//This is a bit hacky, but it makes a LOT less code we need to maintain.
			final Mod[] modList = new Mod[1];
			modList[0] = mod;
			getModAdditionThread(List.of(modList)).start();
		}
	}

	private void addModsFromFile() {
		//TODO: Check it's from the right game before anything else. Gonna have to scrape the page.
		//TODO: The actual adding to the modlist should happen here
		//Result<List<Mod>> modImportResult = UI_SERVICE.addModsFromFile();
	}

	private String getModLocationFromUser(boolean steamCollection) {
		boolean goodModId = false;
		String chosenModId = "";

		//This starts a loop that will continuously get user input until they choose any option that isn't accept.
		do {
			String userInputModId = getUserModIdInput();
			String lastPressedButtonId = ID_AND_URL_MOD_ADDITION_INPUT.getLastPressedButtonId();
			//Checks to make sure the button pressed was accept, then it checks to make sure it is NOT only letters. URL's will pass this.
			if (lastPressedButtonId != null && lastPressedButtonId.equals("accept")) {
				if (!StringUtils.isAlpha(userInputModId)) {
					String modId;
					if (StringUtils.isNumeric(userInputModId)) {
						modId = userInputModId;
					} else {
						modId = STEAM_WORKSHOP_ID_REGEX_PATTERN.matcher(userInputModId)
								.results()
								.map(MatchResult::group)
								.collect(Collectors.joining(""));

						//Certain strings will sometimes return empty strings after the regex.
						if (!modId.isBlank()) {
							modId = modId.substring(3);
						}
					}

					//It next checks the input, after passing it through a regex that will strip anything but numbers, to make sure it isn't empty. URL's with only letters or no ID will not pass this.
					if (!modId.isEmpty()) {
						Optional<Mod> duplicateMod = Optional.empty();
						//We have this check so we don't try and compare single mod ID's to a collection URL.
						if (!steamCollection) {
							String finalModId = modId;
							duplicateMod = UI_SERVICE.getCurrentModList().stream()
									.filter(mod -> finalModId.equals(mod.getId()))
									.findFirst();
						}
						//Last it checks to make sure the provided ID doesn't match a mod ID already in the list.
						if (duplicateMod.isPresent()) {
							Popup.displaySimpleAlert("\"" + duplicateMod.get().getFriendlyName() + "\" is already in the modlist!", MessageType.WARN);
						} else {
							chosenModId = modId;
							goodModId = true;
						}
					} else {
						Popup.displaySimpleAlert("Invalid Mod ID or URL entered.", MessageType.WARN);
					}
				} else {
					Popup.displaySimpleAlert("Mod ID must contain a number!", MessageType.WARN);
				}
			} else {
				goodModId = true;
			}
		} while (!goodModId);

		ID_AND_URL_MOD_ADDITION_INPUT.getInput().clear();

		return chosenModId;
	}

	@FXML
	private void manageModProfiles() {
		MOD_PROFILE_MANAGER_VIEW.show();
	}

	@FXML
	private void manageSaveProfiles() {
		SAVE_MANAGER_VIEW.show();
	}

	@FXML
	private void importModlist() {
		//TODO: Implement. Allow importing modlists from either sandbox file or exported list.
		// For our own applications lists, aka exported ones, create a custom file extension. Like, .SEMM. Then just marshall the modlist only.
	}


	@FXML
	private void exportModlist() {
		//TODO: Implement. Export in our own format (use XML). Make our file end in .SEMM
	}

	//Apply the modlist the user is currently using to the save profile they're currently using.
	//TODO: This whole thing likely need rewritten
	@FXML
	private void applyModlist() throws IOException {
		//TODO: Disable this button when our save profile save is not found
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

	@FXML
	private void closeLogTab() {
		logToggle.setSelected(false);
		if (informationPane.getTabs().isEmpty()) {
			disableSplitPaneDivider();
		}
	}

	@FXML
	private void closeModDescriptionTab() {
		modDescriptionToggle.setSelected(false);
		if (informationPane.getTabs().isEmpty()) {
			disableSplitPaneDivider();
		}
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

	private void applyDefaultSort() {
		if (loadPriority != null) {
			modTable.getSortOrder().removeListener(sortListener);

			loadPriority.setSortType(TableColumn.SortType.ASCENDING);
			modTable.getSortOrder().add(loadPriority);
			modTable.sort();
			modTable.getSortOrder().clear();

			modTable.getSortOrder().addListener(sortListener);
		}
	}

	/**
	 * Enables edge-scrolling on the table. When you drag a row above or below the visible rows, the table will automatically start to scroll.
	 */
	protected void handleModTableDragOver(DragEvent dragEvent) {
		//This normalizes our scroll speed so small and large tables all scroll at the same speed.
		final double TOTAL_ROW_HEIGHT = UI_SERVICE.getCurrentModList().size() * singleTableRow.getHeight();
		final double SCROLL_SPEED_CONSTANT = 0.035;
		final double SCROLL_SPEED = SCROLL_SPEED_CONSTANT / (TOTAL_ROW_HEIGHT / 100) * (modTable.getHeight() / 100);

		double y = dragEvent.getY();
		double modTableTop = modTable.localToScene(modTable.getBoundsInLocal()).getMinY();
		double modTableBottom = modTable.localToScene(modTable.getBoundsInLocal()).getMaxY();

		//These two if statements are here to reduce the actual amount of lookups we're doing since they're relatively expensive
		if (modTableVerticalScrollBar == null) {
			modTableVerticalScrollBar = (ScrollBar) modTable.lookup(".scroll-bar:vertical");
		}

		if (headerRow == null) {
			headerRow = (TableHeaderRow) modTable.lookup("TableHeaderRow");
		}

		double currentScrollValue = modTableVerticalScrollBar.getValue();
		double minScrollValue = modTableVerticalScrollBar.getMin();
		double maxScrollValue = modTableVerticalScrollBar.getMax();
		double scrollAmount;

		//Scroll up
		if (y < modTableTop && currentScrollValue > minScrollValue && TOTAL_ROW_HEIGHT > modTable.getHeight()) {
			scrollAmount = -SCROLL_SPEED;
		} else if (y > modTableBottom + actions.getHeight() && currentScrollValue < maxScrollValue && TOTAL_ROW_HEIGHT > modTable.getHeight()) { //Scroll down
			scrollAmount = SCROLL_SPEED;
		} else {
			scrollAmount = 0;
		}

		if (scrollAmount != 0) {
			if (scrollTimeline == null || !scrollTimeline.getStatus().equals(Animation.Status.RUNNING)) {
				//We disable the transfer mode here since we know that if we're scrolling we're outside the valid drop zone
				dragEvent.acceptTransferModes(TransferMode.NONE);
				scrollTimeline = new Timeline(
						new KeyFrame(Duration.millis(16), actionEvent -> { // 1000ms in a second, so we need 16ms here for a 60fps animation
							double newValue = modTableVerticalScrollBar.getValue() + scrollAmount;
							newValue = Math.max(minScrollValue, Math.min(maxScrollValue, newValue)); // Clamp the value
							modTableVerticalScrollBar.setValue(newValue);
						})
				);
				scrollTimeline.setCycleCount(60); //One second of animation is 60 cycles, so set this to 60 so we don't end up with infinite animations.
				scrollTimeline.play(); // Start the scrolling animation
			}
		} else {
			if (scrollTimeline != null) {
				scrollTimeline.stop();
			}
		}

		if (y > modTableTop + headerRow.getHeight() && y < modTableBottom + actions.getHeight()) {
			dragEvent.acceptTransferModes(TransferMode.MOVE);
		}

		dragEvent.consume();
	}

	//This ensures that we properly allow dragging items to the bottom of the table even when we have a scrollable table.
	private void handleTableActionsOnDragDrop(DragEvent dragEvent) {
		Dragboard dragboard = dragEvent.getDragboard();

		actions.setBorder(null);

		if (dragboard.hasContent(SERIALIZED_MIME_TYPE)) {
			//I'd love to get a class level reference of this, but we need to progressively get it as the view changes

			if (modTableVerticalScrollBar == null) {
				modTableVerticalScrollBar = (ScrollBar) modTable.lookup(".scroll-bar:vertical");
			}

			if (modTableVerticalScrollBar.getValue() == modTableVerticalScrollBar.getMax()) {
				for (Mod m : SELECTIONS) {
					UI_SERVICE.getCurrentModList().remove(m);
				}

				modTable.getSelectionModel().clearSelection();

				for (Mod m : SELECTIONS) {
					UI_SERVICE.getCurrentModList().add(m);
					modTable.getSelectionModel().select(UI_SERVICE.getCurrentModList().size() - 1);
				}

				MODLIST_MANAGER_HELPER.setCurrentModListLoadPriority(modTable, UI_SERVICE);

				//Redo our sort since our row order has changed
				modTable.sort();

			/*
				We shouldn't need this since currentModList which backs our table is an observable list backed by the currentModProfile.getModList,
				but for whatever reason the changes aren't propagating without this.
			*/
				//TODO: Look into why the changes don't propagate without setting it here. Indicative of a deeper issue or misunderstanding.
				UI_SERVICE.getCurrentModProfile().setModList(UI_SERVICE.getCurrentModList());
				UI_SERVICE.saveUserData();
			}

			dragEvent.consume();
		}
	}

	private void handleTableActionsOnDragOver(DragEvent dragEvent) {
		ScrollBar verticalScrollBar = (ScrollBar) modTable.lookup(".scroll-bar:vertical");

		if (verticalScrollBar.isVisible() && verticalScrollBar.getValue() == verticalScrollBar.getMax()) {
			Color indicatorColor = Color.web(MODLIST_MANAGER_HELPER.getSelectedCellBorderColor(UI_SERVICE));
			Border dropIndicator;
			dropIndicator = new Border(new BorderStroke(indicatorColor, indicatorColor, indicatorColor, indicatorColor,
					BorderStrokeStyle.SOLID, BorderStrokeStyle.NONE, BorderStrokeStyle.NONE, BorderStrokeStyle.NONE,
					CornerRadii.EMPTY, new BorderWidths(2, 0, 0, 0), new Insets(-2, verticalScrollBar.getWidth(), 0, 0)));
			actions.setBorder(dropIndicator);
		}
	}

	private void handleTableActionsOnDragExit(DragEvent dragEvent) {
		actions.setBorder(null);
	}

	//This is where we update the actual contents of the mod table when we want to set it, such as if we switch mod profiles.
	//This wraps our filtered list in a sorted list so that we can properly use the column sorts in the UI, while also maintaining searchability
	//We leave the filteredList as an attribute as we need to access it in the ModTableContextBarView to set a listener on it
	public void updateModTableContents() {
		filteredModList = new FilteredList<>(UI_SERVICE.getCurrentModList(), mod -> true);
		SortedList<Mod> sortedList = new SortedList<>(filteredModList);
		sortedList.comparatorProperty().bind(modTable.comparatorProperty());
		modTable.setItems(sortedList);
	}

	private String getUserModIdInput() {
		ID_AND_URL_MOD_ADDITION_INPUT.show();
		return ID_AND_URL_MOD_ADDITION_INPUT.getInput().getText();
	}

	private void setModAddingInputViewText(String title, String instructions, String promptText, String emptyTextMessage) {
		ID_AND_URL_MOD_ADDITION_INPUT.setTitle(title);
		ID_AND_URL_MOD_ADDITION_INPUT.setInputInstructions(instructions);
		ID_AND_URL_MOD_ADDITION_INPUT.setPromptText(promptText);
		ID_AND_URL_MOD_ADDITION_INPUT.setEmptyTextMessage(emptyTextMessage);
	}

	private void redirectHyperlinks() {
		NodeList nodeList = modDescription.getEngine().getDocument().getElementsByTagName("a");

		for (int i = 0; i < nodeList.getLength(); i++) {
			org.w3c.dom.Node node = nodeList.item(i);
			EventTarget eventTarget = (EventTarget) node;
			eventTarget.addEventListener("click", evt -> {
				EventTarget target = evt.getCurrentTarget();
				HTMLAnchorElement anchorElement = (HTMLAnchorElement) target;
				String href = anchorElement.getHref();
				try {
					Desktop.getDesktop().browse(new URI(href));
				} catch (IOException | URISyntaxException e) {
					UI_SERVICE.log(e);
				}
				evt.preventDefault();
			}, false);
		}
	}

	private Thread getSteamModCollectionThread(String collectionId) {
		final Task<List<Result<String>>> TASK;

		TASK = new Task<>() {
			@Override
			protected List<Result<String>> call() throws IOException {
				return UI_SERVICE.scrapeSteamModCollectionModList(collectionId);
			}
		};

		TASK.setOnRunning(workerStateEvent -> {
			//We lockout the user input here to prevent any problems from the user doing things while the modlist is modified.
			disableUserInputElements(true);
			modAdditionProgressPanel.setVisible(true);
			disableModAdditionUiText(true);
			modAdditionSteamCollectionName.setVisible(true);
		});

		TASK.setOnSucceeded(workerStateEvent -> {
			int modIdsSuccessfullyFound = 0;
			int duplicateModIds = 0;
			List<Mod> successfullyFoundMods = new ArrayList<>();

			List<Result<String>> steamCollectionModIds = TASK.getValue();
			//This should only be false if we get a collection for the wrong game
			if (steamCollectionModIds.size() != 1 && !steamCollectionModIds.getFirst().getCurrentMessage().equals("The collection must be a Space Engineers collection!")) {
				for (Result<String> steamCollectionModId : steamCollectionModIds) {
					if (steamCollectionModId.isSuccess()) {
						modIdsSuccessfullyFound++;
						Mod mod = new Mod(steamCollectionModId.getPayload(), ModType.STEAM);
						successfullyFoundMods.add(mod);
					} else {
						if (steamCollectionModId.getType() == ResultType.INVALID) duplicateModIds++;
					}
				}

				if (duplicateModIds == steamCollectionModIds.size()) {
					Popup.displaySimpleAlert("All the mods in the collection are already in the modlist!", STAGE, MessageType.INFO);
					Platform.runLater(() -> {
						FadeTransition fadeTransition = new FadeTransition(Duration.millis(1000), modAdditionProgressPanel);
						fadeTransition.setFromValue(1d);
						fadeTransition.setToValue(0d);

						fadeTransition.setOnFinished(actionEvent -> {
							modAdditionSteamCollectionName.setVisible(false);
							disableModAdditionUiText(false);
							disableUserInputElements(false);
							resetModAdditionProgressUi();
						});

						fadeTransition.play();
					});
				} else {
					int totalNumberOfMods = modIdsSuccessfullyFound + duplicateModIds;
					String postCollectionScrapeMessage = totalNumberOfMods +
							" mods had their ID's successfully pulled. " + duplicateModIds + " were duplicates. Add the remaining " +
							(totalNumberOfMods - duplicateModIds) + "?";

					int userChoice = Popup.displayYesNoDialog(postCollectionScrapeMessage, STAGE, MessageType.INFO);

					if (userChoice == 1) {
						getModAdditionThread(successfullyFoundMods).start();
					}

					Platform.runLater(() -> {
						modAdditionSteamCollectionName.setVisible(false);
						disableModAdditionUiText(false);

						if (userChoice != 1) {
							disableUserInputElements(false);
							resetModAdditionProgressUi();
						}
					});
				}
			} else {
				Popup.displaySimpleAlert(steamCollectionModIds.getFirst(), STAGE);
				Platform.runLater(() -> {
					modAdditionSteamCollectionName.setVisible(false);
					disableModAdditionUiText(false);

					disableUserInputElements(false);
					resetModAdditionProgressUi();

				});
			}
		});


		Thread thread = Thread.ofVirtual().unstarted(TASK);
		thread.setDaemon(true);
		return thread;
	}

	private Thread getModAdditionThread(List<Mod> modList) {
		final Task<List<Result<Mod>>> TASK;
		List<Result<Mod>> modInfoFillOutResults = new ArrayList<>();

		TASK = new Task<>() {
			@Override
			protected List<Result<Mod>> call() throws ExecutionException, InterruptedException {
				Platform.runLater(() -> UI_SERVICE.getModAdditionProgressDenominatorProperty().setValue(modList.size()));

				List<Future<Result<Mod>>> futures = new ArrayList<>(modList.size());
				try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
					for (Mod m : modList) {

						// Submit the task without waiting for it to finish
						Future<Result<Mod>> future = executorService.submit(() -> {
							try {
								return UI_SERVICE.fillOutModInformation(m);
							} catch (IOException e) {
								Result<Mod> failedResult = new Result<>();
								failedResult.addMessage(e.toString(), ResultType.FAILED);
								return failedResult;
							}
						});
						// Store the Future if you need to track the task later
						futures.add(future);
					}
					try {
						for (Future<Result<Mod>> f : futures) {
							modInfoFillOutResults.add(f.get());
						}
					} catch (RuntimeException e) {
						Result<Mod> failedResult = new Result<>();
						failedResult.addMessage(e.toString(), ResultType.FAILED);
						modInfoFillOutResults.add(failedResult);
					}
				}
				return modInfoFillOutResults;
			}
		};

		TASK.setOnRunning(workerStateEvent -> {
			//We lockout the user input here to prevent any problems from the user doing things while the modlist is modified.
			disableUserInputElements(true);
			modAdditionProgressPanel.setVisible(true);
		});

		TASK.setOnSucceeded(workerStateEvent -> Platform.runLater(() -> {
			modAdditionProgressWheel.setVisible(false);


			int successfulScrapes = 0;
			int failedScrapes = 0;

			for (Result<Mod> currentModInfoFillOutResult : modInfoFillOutResults) {
				if (currentModInfoFillOutResult.isSuccess()) {
					currentModInfoFillOutResult.getPayload().setLoadPriority(UI_SERVICE.getCurrentModList().size() + 1);
					UI_SERVICE.getCurrentModList().add(currentModInfoFillOutResult.getPayload());
					successfulScrapes++;
					modTable.sort();
					UI_SERVICE.logPrivate(currentModInfoFillOutResult);
				} else {
					failedScrapes++;
					UI_SERVICE.log(currentModInfoFillOutResult);
				}
			}


			//TODO: This might be unwanted behavior from users. Requires actual experience testing.
			for (Result<Mod> modResult : modInfoFillOutResults) {
				if (modResult.isSuccess()) {
					modTable.getSelectionModel().clearSelection();
					modTable.getSelectionModel().select(modResult.getPayload());
					modTable.scrollTo(modTable.getSelectionModel().getSelectedIndex());
					break;
				}
			}

			if (modList.size() == 1) {
				Popup.displaySimpleAlert(modInfoFillOutResults.getFirst(), STAGE);
			} else {
				String modFillOutResultMessage = successfulScrapes + " mods were successfully added. " +
						failedScrapes + " failed to be added. Check the log for more information for each specific mod.";
				Popup.displaySimpleAlert(modFillOutResultMessage, STAGE, MessageType.INFO);
				UI_SERVICE.log(modFillOutResultMessage, MessageType.INFO);
			}

			UI_SERVICE.getCurrentModProfile().setModList(UI_SERVICE.getCurrentModList());
			UI_SERVICE.saveUserData();

			//TODO: We might just want to disable the progress pane stuff entirely. Needs user testing. UX question.
			//Reset our UI settings for the mod progress
			FadeTransition fadeTransition = new FadeTransition(Duration.millis(1000), modAdditionProgressPanel);
			fadeTransition.setFromValue(1d);
			fadeTransition.setToValue(0d);

			fadeTransition.setOnFinished(actionEvent -> {
				disableUserInputElements(false);
				resetModAdditionProgressUi();
			});

			fadeTransition.play();
		}));

		Thread thread = Thread.ofVirtual().unstarted(TASK);
		thread.setDaemon(true);
		return thread;
	}

	//These function names suck
	private void disableUserInputElements(boolean shouldDisable) {
		modImportDropdown.setDisable(shouldDisable);
		manageModProfiles.setDisable(shouldDisable);
		manageSaveProfiles.setDisable(shouldDisable);
		importModlist.setDisable(shouldDisable);
		exportModlist.setDisable(shouldDisable);
		applyModlist.setDisable(shouldDisable);
		launchSpaceEngineers.setDisable(shouldDisable);

		modProfileDropdown.setDisable(shouldDisable);
		saveProfileDropdown.setDisable(shouldDisable);
		modTableSearchField.setDisable(shouldDisable);
	}

	private void disableModAdditionUiText(boolean shouldDisable) {
		modAdditionProgressActionName.setVisible(!shouldDisable);
		modAdditionProgressNumerator.setVisible(!shouldDisable);
		modAdditionProgressDivider.setVisible(!shouldDisable);
		modAdditionProgressDenominator.setVisible(!shouldDisable);
	}

	private void resetModAdditionProgressUi() {
		modAdditionProgressPanel.setVisible(false);
		modAdditionProgressPanel.setOpacity(1d);
		UI_SERVICE.getModAdditionProgressNumeratorProperty().setValue(0);
		UI_SERVICE.getModAdditionProgressDenominatorProperty().setValue(0);
		UI_SERVICE.getModAdditionProgressPercentageProperty().setValue(0d);
		modAdditionProgressWheel.setVisible(true);
	}
}
