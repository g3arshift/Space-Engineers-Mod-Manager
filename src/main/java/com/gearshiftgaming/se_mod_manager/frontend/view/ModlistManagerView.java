package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.ModProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.ModType;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.LogMessage;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.LogCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.ModNameCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.ModTableRowFactory;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ModlistManagerView {
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
	@Getter
	private SplitPane mainViewSplit;

	//TODO: Low priority: Fix the alignment of the table headers to be centered, not center left.
	@FXML
	@Getter
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
	private TableColumn<Mod, Integer> loadPriority;

	@FXML
	private TableColumn<Mod, String> modSource;

	@FXML
	private TableColumn<Mod, String> modCategory;

	@FXML
	private HBox tableActions;

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
	private ListView<LogMessage> viewableLog;

	private final UiService UI_SERVICE;

	private final ObservableList<LogMessage> USER_LOG;

	@Getter
	private boolean mainViewSplitDividerVisible = true;

	//This is just a wrapper for the userConfiguration modProfiles list. Any changes made to this will propagate back to it, but not the other way around.
	private final ObservableList<ModProfile> MOD_PROFILES;

	//This is just a wrapper for the userConfiguration saveProfiles list. Any changes made to this will propagate back to it, but not the other way around.
	private final ObservableList<SaveProfile> SAVE_PROFILES;

	private final DataFormat SERIALIZED_MIME_TYPE;

	private ListChangeListener<TableColumn<Mod, ?>> sortListener;

	@Getter
	private Timeline scrollTimeline;

	private final List<Mod> SELECTIONS;

	@Getter
	@Setter
	//This is a really dumb hack that we have to use to actually get a row as it is styled in the application.
	private TableRow<Mod> singleTableRow;

	//TODO: We might not need this if we setup a listener properly on the active mod count, but I'm not sure how we can since it has to be done in ModNameCell.
	private Text activeModCount;

	//TODO: We might not need this if we setup a listener properly on the active mod count, but I'm not sure how we can since it has to be done in ModNameCell.
	private Text modConflicts;

	private CheckMenuItem logToggle;

	private CheckMenuItem modDescriptionToggle;

	//This is the reference to the controller for the bar located in the bottom section of the main borderpane. We need everything in it so might as well get the whole reference.
	private final StatusBarView STATUS_BAR_VIEW;

	public ModlistManagerView(UiService uiService, StatusBarView statusBarView) {
		this.UI_SERVICE = uiService;
		this.MOD_PROFILES = uiService.getMOD_PROFILES();
		this.SAVE_PROFILES = uiService.getSAVE_PROFILES();
		this.USER_LOG = uiService.getUSER_LOG();
		this.STATUS_BAR_VIEW = statusBarView;

		SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");
		SELECTIONS = new ArrayList<>();
	}

	public void initView(Text activeModCount, Text modConflicts, CheckMenuItem logToggle, CheckMenuItem modDescriptionToggle) {
		this.activeModCount = activeModCount;
		this.modConflicts = modConflicts;
		this.logToggle = logToggle;
		this.modDescriptionToggle = modDescriptionToggle;

		sortListener = change -> {
			if (modTable.getSortOrder().isEmpty()) {
				applyDefaultSort();
			}
		};

		setupMainViewItems();
		setupModTable();
		tableActions.setOnDragDropped(this::handleTableActionsDragOver);
	}

	//TODO: If our mod profile is null but we make a save, popup mod profile UI too. And vice versa for save profile.
	//TODO: Allow for adding/removing columns. Add a context menu to the column header.
	private void setupModTable() {

		modTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		modTable.setRowFactory(new ModTableRowFactory(UI_SERVICE, SERIALIZED_MIME_TYPE, SELECTIONS, this));

		modName.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
		modName.setCellFactory(param -> new ModNameCell(UI_SERVICE));
		modName.setComparator(Comparator.comparing(Mod::getFriendlyName));

		//Format the appearance, styling, and menu`s of our table cells, rows, and columns
		modVersion.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getModVersion()));
		modLastUpdated.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLastUpdated() != null ?
				cellData.getValue().getLastUpdated().toString() : "Unknown"));

		loadPriority.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getLoadPriority()).asObject());

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

		modTable.getSortOrder().addListener(sortListener);
		modTable.setItems(UI_SERVICE.getCurrentModList());
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

	//TODO: Increase speed based on distance from the edge
	protected void handleModTableDragOver(DragEvent event) {
		//Enables auto-scrolling on the table. When you drag a row above or below the visible rows, the table will automatically start to scroll
		final double SCROLL_THRESHOLD = 20.0;
		final double SCROLL_SPEED = 0.3;

		double y = event.getY();
		double modTableTop = modTable.localToScene(modTable.getBoundsInLocal()).getMinY();
		double modTableBottom = modTable.localToScene(modTable.getBoundsInLocal()).getMaxY();

		ScrollBar verticalScrollBar = (ScrollBar) modTable.lookup(".scroll-bar:vertical");

		double currentScrollValue = verticalScrollBar.getValue();
		double minScrollValue = verticalScrollBar.getMin();
		double maxScrollValue = verticalScrollBar.getMax();
		double scrollAmount;


		//Scroll up
		if (y < modTableTop - SCROLL_THRESHOLD && currentScrollValue > minScrollValue && modTable.getItems().size() * singleTableRow.getHeight() > modTable.getHeight()) {
			scrollAmount = -SCROLL_SPEED * 0.1;
		} else if (y > modTableBottom + SCROLL_THRESHOLD && currentScrollValue < maxScrollValue && modTable.getItems().size() * singleTableRow.getHeight() > modTable.getHeight()) {
			scrollAmount = SCROLL_SPEED * 0.1;
		} else {
			scrollAmount = 0;
		}

		if (scrollAmount != 0) {
			if (scrollTimeline == null || !scrollTimeline.getStatus().equals(Animation.Status.RUNNING)) {
				scrollTimeline = new Timeline(
						new KeyFrame(Duration.millis(16), e -> { // 1000ms in a second, so we need 16ms here for a 60fps animation
							double newValue = verticalScrollBar.getValue() + scrollAmount;
							newValue = Math.max(minScrollValue, Math.min(maxScrollValue, newValue)); // Clamp the value
							verticalScrollBar.setValue(newValue);
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

		event.acceptTransferModes(TransferMode.MOVE);
		event.consume();
	}

	//This ensures that we properly allow dragging items to the bottom of the table even when we have a scrollable table.
	private void handleTableActionsDragOver(DragEvent dragEvent) {
		Dragboard dragboard = dragEvent.getDragboard();

		if (dragboard.hasContent(SERIALIZED_MIME_TYPE)) {

			for (Mod m : SELECTIONS) {
				modTable.getItems().remove(m);
			}

			modTable.getSelectionModel().clearSelection();

			for (Mod m : SELECTIONS) {
				modTable.getItems().add(m);
				modTable.getSelectionModel().select(modTable.getItems().size() - 1);
			}

			//TODO: Need to move the duplicates to a shared helper class. Call it "tableHelper" or something.
			if (modTable.getSortOrder().isEmpty() || modTable.getSortOrder().getFirst().getSortType().equals(TableColumn.SortType.ASCENDING)) {
				for (int i = 0; i < UI_SERVICE.getCurrentModProfile().getModList().size(); i++) {
					UI_SERVICE.getCurrentModList().get(i).setLoadPriority(i + 1);
				}
			} else {
				for (int i = 0; i < UI_SERVICE.getCurrentModProfile().getModList().size(); i++) {
					UI_SERVICE.getCurrentModList().get(i).setLoadPriority(getIntendedLoadPriority(modTable, i));
				}
			}

			//Redo our sort since our row order has changed
			modTable.sort();

			/*
				We shouldn't need this since currentModList which backs our table is an observable list backed by the currentModProfile.getModList,
				but for whatever reason the changes aren't propagating without this.
			*/
			//TODO: Look into why the changes don't propagate without setting it here. Indicative of a deeper issue or misunderstanding.
			UI_SERVICE.getCurrentModProfile().setModList(UI_SERVICE.getCurrentModList());
			UI_SERVICE.saveUserData();

			dragEvent.consume();
		}
	}

	private int getIntendedLoadPriority(TableView<Mod> modTable, int index) {
		int intendedLoadPriority;
		//Check if we are in ascending/default order, else we're in descending order
		if (modTable.getSortOrder().isEmpty() || modTable.getSortOrder().getFirst().getSortType().equals(TableColumn.SortType.ASCENDING)) {
			return index;
		} else {
			intendedLoadPriority = UI_SERVICE.getCurrentModList().size() - index;
		}
		return intendedLoadPriority;
	}
}