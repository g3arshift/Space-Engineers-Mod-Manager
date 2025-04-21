package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.*;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.LogCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.ModImportType;
import com.gearshiftgaming.se_mod_manager.frontend.models.ModNameCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.ModTableRowFactory;
import com.gearshiftgaming.se_mod_manager.frontend.models.utility.ModImportUtility;
import com.gearshiftgaming.se_mod_manager.frontend.view.helper.ModListManagerHelper;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.Popup;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.TutorialUtility;
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
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.List;
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
//TODO: At some point a bunch of logic needs to be rewritten with guard clause format. Especially for the mod scraping.
public class MasterManager {
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
    private StackPane modImportProgressPanel;

    @FXML
    private ProgressBar modImportProgressBar;

    @FXML
    private ProgressIndicator modImportProgressIndicator;

    @FXML
    private Label modImportProgressActionName;

    @FXML
    private Label modImportProgressNumerator;

    @FXML
    private Label modImportProgressDivider;

    @FXML
    private Label modImportProgressDenominator;

    @FXML
    private ProgressIndicator modImportProgressWheel;

    @FXML
    private Label modImportSteamCollectionName;

    @FXML
    private Label modIoUrlToIdName;


    //TODO: Organize the nightmare of variable declarations here.
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
    private final StatusBar STATUS_BAR_VIEW;

    private final ModListManagerHelper MODLIST_MANAGER_HELPER;

    @Getter
    private ScrollBar modTableVerticalScrollBar;

    private TableHeaderRow headerRow;

    private final ModListManager MOD_PROFILE_MANAGER_VIEW;

    private final SaveProfileManager SAVE_MANAGER_VIEW;

    @Getter
    private FilteredList<Mod> filteredModList;

    @Getter
    private final Stage STAGE;

    private final SimpleInput ID_AND_URL_MOD_IMPORT_INPUT;

    private final SaveInput MOD_FILE_SELECTION_VIEW;

    private final GeneralFileInput GENERAL_FILE_SELECT_VIEW;

    private final String STEAM_MOD_DATE_FORMAT;

    private final Pattern STEAM_WORKSHOP_MOD_ID;

    private final Pattern MOD_IO_URL;

    private final Pane[] TUTORIAL_HIGHLIGHT_PANES;

    //These three are here purely so we can enable and disable them when we add mods to prevent user interaction from breaking things.
    private ComboBox<MutableTriple<UUID, String, SpaceEngineersVersion>> modProfileDropdown;
    private ComboBox<SaveProfile> saveProfileDropdown;
    private TextField modTableSearchField;

    public MasterManager(@NotNull UiService uiService, Stage stage, @NotNull Properties properties, StatusBar statusBar,
                         ModListManager modListManager, SaveProfileManager saveProfileManager, SimpleInput modImportInputView, SaveInput saveInput,
                         GeneralFileInput generalFileInput) {
        this.UI_SERVICE = uiService;
        this.STAGE = stage;
        this.USER_LOG = uiService.getUSER_LOG();
        this.STATUS_BAR_VIEW = statusBar;
        this.MODLIST_MANAGER_HELPER = new ModListManagerHelper();
        this.ID_AND_URL_MOD_IMPORT_INPUT = modImportInputView;
        this.MOD_FILE_SELECTION_VIEW = saveInput;
        this.GENERAL_FILE_SELECT_VIEW = generalFileInput;

        this.MOD_PROFILE_MANAGER_VIEW = modListManager;
        this.SAVE_MANAGER_VIEW = saveProfileManager;

        this.STEAM_MOD_DATE_FORMAT = properties.getProperty("semm.steam.mod.dateFormat");

        SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");
        SELECTIONS = new ArrayList<>();

        filteredModList = new FilteredList<>(UI_SERVICE.getCurrentModList(), mod -> true);

        this.STEAM_WORKSHOP_MOD_ID = Pattern.compile(properties.getProperty("semm.steam.mod.id.pattern"));
        this.MOD_IO_URL = Pattern.compile(properties.getProperty("semm.modio.mod.name.pattern"));
        TUTORIAL_HIGHLIGHT_PANES = UI_SERVICE.getHighlightPanes();
    }

    public void initView(CheckMenuItem logToggle, CheckMenuItem modDescriptionToggle, int modTableCellSize,
                         ComboBox<MutableTriple<UUID, String, SpaceEngineersVersion>> modProfileDropdown, ComboBox<SaveProfile> saveProfileDropdown, TextField modTableSearchField) {
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

        //Set up the mod description handlers
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

        String activeThemeName = StringUtils.substringAfter(Application.getUserAgentStylesheet(), "theme/");
        modDescription.getEngine().setUserStyleSheetLocation(Objects.requireNonNull(getClass().getResource("/styles/mod-description_primer-light.css")).toExternalForm());
        modDescriptionBackground.setStyle("-fx-border-color: -color-border-default; -fx-border-width:1px");
        modDescription.setContextMenuEnabled(false);

        modDescription.getEngine().setUserStyleSheetLocation(Objects.requireNonNull(getClass().getResource("/styles/mod-description_" + activeThemeName)).toExternalForm());

        //This is here to make it so we can prevent users from clicking on the purely display option "Add mods from...", while also making it clear it's not a valid option.
        modImportDropdown.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setStyle(null);
                } else {
                    this.setText(item);
                    if (item.equals("Add mods from...")) {
                        this.setOpacity(0.6);
                        this.setDisable(true);
                    }
                }
            }
        });

        modImportProgressNumerator.textProperty().bind(UI_SERVICE.getModImportProgressNumeratorProperty().asString());
        modImportProgressDenominator.textProperty().bind(UI_SERVICE.getModImportProgressDenominatorProperty().asString());
        modImportProgressBar.progressProperty().bind(UI_SERVICE.getModImportProgressPercentageProperty());

        modImportSteamCollectionName.setVisible(false);

        modIoUrlToIdName.setVisible(false);

        viewableLog.setFixedCellSize(35);

        //This is a dumb hack, but it swallows the drag events otherwise when we drag rows over it.
        modDescription.setOnDragOver(dragEvent -> {
        });

        UI_SERVICE.logPrivate("Successfully initialized modlist manager.", MessageType.INFO);
    }

    private void setupModTable(int modTableCellSize) {
        //Format the appearance, styling, and menu`s of our table cells, rows, and columns
        modTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        modTable.setRowFactory(new ModTableRowFactory(UI_SERVICE, SERIALIZED_MIME_TYPE, SELECTIONS, this, MODLIST_MANAGER_HELPER));

        modName.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        modName.setCellFactory(param -> new ModNameCell(UI_SERVICE));
        modName.setComparator(Comparator.comparing(Mod::getFriendlyName));

        modLastUpdated.setCellValueFactory(cellData -> {
            if (cellData.getValue() instanceof SteamMod steamMod) {
                if (steamMod.getLastUpdated() != null) {
                    return new SimpleStringProperty(steamMod.getLastUpdated().format(DateTimeFormatter.ofPattern(STEAM_MOD_DATE_FORMAT)));
                } else {
                    return new SimpleStringProperty("Unknown");
                }
            } else if (cellData.getValue() instanceof ModIoMod modIoMod) {
                StringBuilder lastUpdated = new StringBuilder();
                if (modIoMod.getLastUpdatedMonthDay() != null) {
                    lastUpdated.append(modIoMod.getLastUpdatedMonthDay().format(DateTimeFormatter.ofPattern("MMM d"))).append(", ");
                }

                lastUpdated.append(modIoMod.getLastUpdatedYear());

                if (modIoMod.getLastUpdatedHour() != null) {
                    lastUpdated.append(" @ ").append(modIoMod.getLastUpdatedHour().format(DateTimeFormatter.ofPattern("ha")));
                }
                return new SimpleStringProperty(lastUpdated.toString());
            } else {
                return new SimpleStringProperty("Unknown");
            }
        });

        modLastUpdated.setComparator((date1, date2) -> {
            LocalDateTime firstDateNormalized;
            if (date1.length() <= 19) { //Only steam mods will be greater than 19.
                firstDateNormalized = getModIoLastUpdatedComparatorDate(date1);
            } else {
                firstDateNormalized = LocalDateTime.parse(date1, DateTimeFormatter.ofPattern(STEAM_MOD_DATE_FORMAT));
            }

            LocalDateTime secondDateNormalized;
            if (date2.length() <= 19) {
                secondDateNormalized = getModIoLastUpdatedComparatorDate(date2);
            } else {
                secondDateNormalized = LocalDateTime.parse(date2, DateTimeFormatter.ofPattern(STEAM_MOD_DATE_FORMAT));
            }

            return firstDateNormalized.compareTo(secondDateNormalized);
        });

        loadPriority.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getLoadPriority()).asObject());

        modType.setCellValueFactory(cellData -> new SimpleStringProperty((cellData.getValue() instanceof SteamMod ? "Steam" : "Mod.io")));

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

    private @NotNull LocalDateTime getModIoLastUpdatedComparatorDate(@NotNull String dateString) {
        DateTimeFormatter formatter;
        return switch (dateString.length()) {
            case 18, 19: //Mod IO hour format
                yield LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern("MMM d',' yyyy '@' ha"));
            case 11, 12: //Mod IO day format
                formatter = new DateTimeFormatterBuilder()
                        .appendPattern("MMM d',' yyyy")
                        .parseDefaulting(ChronoField.CLOCK_HOUR_OF_AMPM, 0)
                        .parseDefaulting(ChronoField.AMPM_OF_DAY, 0) //AM
                        .toFormatter();
                yield LocalDateTime.parse(dateString, formatter);
            default: //Mod IO year format
                formatter = new DateTimeFormatterBuilder()
                        .appendPattern("yyyy")
                        .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
                        .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                        .parseDefaulting(ChronoField.CLOCK_HOUR_OF_AMPM, 0)
                        .parseDefaulting(ChronoField.AMPM_OF_DAY, 0) //AM
                        .toFormatter();
                yield LocalDateTime.parse(dateString, formatter);
        };
    }

    //TODO: Make it so that when we change the modlist but don't inject it, the status becomes "Modified since last injection". Will have to happen in the modnamecell and row factory.
    public void setupMainViewItems() {
        viewableLog.setItems(USER_LOG);
        viewableLog.setCellFactory(param -> new LogCell());

        // Just do this by manually setting the selected item after we select an item. To actually call code, call one function on selection/action in the dropdown, that determines which function to call and do stuff in the rest of the code, then reset the selected item.
        modImportDropdown.getItems().addAll("Add mods from...",
                ModImportType.STEAM_ID.getName(),
                ModImportType.STEAM_COLLECTION.getName(),
                ModImportType.MOD_IO.getName(),
                ModImportType.EXISTING_SAVE.getName(),
                ModImportType.FILE.getName());

        //TODO: Setup a function in ModList service to track conflicts.
    }

    //TODO: This gets called twice every single time because of the .selectFirst stuff, but it's the only way to actually reset the damn selection.
    @FXML
    private void addMod() {
        ModImportType selectedImportOption = ModImportType.fromString(modImportDropdown.getSelectionModel().getSelectedItem());
        modImportDropdown.getSelectionModel().selectFirst();
        modImportDropdown.setValue(modImportDropdown.getSelectionModel().getSelectedItem());
        if (selectedImportOption != null) {
            switch (selectedImportOption) {
                case STEAM_ID -> addModFromSteamId();
                case STEAM_COLLECTION -> addModsFromSteamCollection();
                case MOD_IO -> addModFromModIoId();
                case EXISTING_SAVE -> addModsFromExistingSave();
                case FILE -> addModsFromFile();
            }
        }
    }

    //TODO: To start moving duplicate check, follow the path of each option to find where they are currently. Note the location, but don't change code yet.
    // We are trying to assemble a plan first.
    private void addModFromSteamId() {
        setModAddingInputViewText("Steam Workshop Mod URL/ID",
                "Enter the Steam Workshop URL/ID",
                "Workshop URL/Mod ID"
        );

        String modId = getSteamModLocationFromUser(false);
        if (!modId.isBlank()) {
            SteamMod mod = new SteamMod(modId);

            //This is a bit hacky, but it makes a LOT less code we need to maintain.
            final Mod[] modList = new Mod[1];
            modList[0] = mod;
            importModsFromList(List.of(modList)).start();
        }
    }

    private void addModsFromSteamCollection() {
        setModAddingInputViewText("Steam Workshop Collection URL/ID",
                "Enter the URL/ID for the Steam Workshop Collection",
                "Collection URL/ID"
        );

        String modId = getSteamModLocationFromUser(true);
        if (!modId.isBlank()) {
            try {
                importSteamCollection(modId).start();
            } catch (RuntimeException e) {
                UI_SERVICE.log(e);
                Popup.displaySimpleAlert(String.valueOf(e), STAGE, MessageType.ERROR);
            }
        }
    }

    private void addModFromModIoId() {
        setModAddingInputViewText("Mod.io Mod URL/ID",
                "Enter the Mod.io URL or ID",
                "Mod.io URL/ID"
        );

        String modId = getModIoModLocationFromUser();

        if (!modId.isBlank()) {
            if (!StringUtils.isNumeric(modId)) {
                addSingleModIoModFromId(modId).start();
            } else {
                ModIoMod duplicateModIoMod = ModListManagerHelper.findDuplicateModIoMod(modId, UI_SERVICE.getCurrentModList());
                if (duplicateModIoMod == null) {
                    ModIoMod mod = new ModIoMod(modId);

                    //This is a bit hacky, but it makes a LOT less code we need to maintain.
                    final Mod[] modList = new Mod[1];
                    modList[0] = mod;
                    importModsFromList(List.of(modList)).start();
                } else {
                    String errorMessage = String.format("\"%s\" is already in the mod list!", duplicateModIoMod.getFriendlyName());
                    UI_SERVICE.log(errorMessage, MessageType.WARN);
                    Popup.displaySimpleAlert(errorMessage, STAGE, MessageType.WARN);
                }
            }
        }
    }

    private void addModsFromExistingSave() {
        // Popup the same save chooser we use for save profiles for this and get the file path that way. Look at how the save manager handles it.
        MOD_FILE_SELECTION_VIEW.setSaveProfileInputTitle("Import Save Modlist");
        MOD_FILE_SELECTION_VIEW.setAddSaveButtonText("Import Mods");
        MOD_FILE_SELECTION_VIEW.show(STAGE);
        File selectedSave = MOD_FILE_SELECTION_VIEW.getSelectedSave();
        if (selectedSave != null && MOD_FILE_SELECTION_VIEW.getLastPressedButtonId().equals("addSave")) {
            Result<List<Mod>> existingModlistResult = ModImportUtility.getModlistFromSandboxConfig(UI_SERVICE, selectedSave, STAGE);

            if (existingModlistResult.isSuccess()) {

                importModsFromList(existingModlistResult.getPayload()).start();
            }
        }
    }

    private void addModsFromFile() {
        int choice = Popup.displayThreeChoiceDialog("Are the mods in the file for Mod.io, or Steam? Modlist files should only contain mods from either Steam or Mod.io, but not both.", STAGE, MessageType.INFO,
                "Steam", "Mod.io", "Cancel");
        if (choice != 0) {
            //TODO: Remove this once we add back mod.io functionality.
            //Popup.displaySimpleAlert("Select a file containing Steam Workshop mod ID's or URL's. Make sure each ID or URL is on its own line by itself.", STAGE, MessageType.INFO);
            GENERAL_FILE_SELECT_VIEW.resetSelectedSave();
            GENERAL_FILE_SELECT_VIEW.setSaveProfileInputTitle("Import Modlist from File");
            GENERAL_FILE_SELECT_VIEW.setNextButtonText("Import Mods");
            GENERAL_FILE_SELECT_VIEW.setExtensionFilter(new FileChooser.ExtensionFilter("Modlist Files", "*.txt", "*.doc"));
            GENERAL_FILE_SELECT_VIEW.show(STAGE);
            File selectedModlistFile = GENERAL_FILE_SELECT_VIEW.getSelectedFile();
            if (selectedModlistFile != null && GENERAL_FILE_SELECT_VIEW.getLastPressedButtonId().equals("next")) {
                List<String> modIds = new ArrayList<>();
                ModType selectedModType;

                if (choice == 2) { //Steam modlist file
                    selectedModType = ModType.STEAM;
                    try {
                        modIds = UI_SERVICE.getModlistFromFile(selectedModlistFile, ModType.STEAM);
                    } catch (IOException e) {
                        UI_SERVICE.log(e.toString(), MessageType.ERROR);
                    }
                } else { //Mod.io modlist file
                    try {
                        modIds = UI_SERVICE.getModlistFromFile(selectedModlistFile, ModType.MOD_IO);
                        for (int i = 0; i < modIds.size(); i++) {
                            String modName;
                            if (!StringUtils.isNumeric(modIds.get(i))) {
                                modName = MOD_IO_URL.matcher(modIds.get(i))
                                        .results()
                                        .map(MatchResult::group)
                                        .collect(Collectors.joining());
                            } else {
                                modName = modIds.get(i);
                            }
                            if (modName.isBlank()) {
                                modIds.remove(i);
                                i--;
                            } else {
                                modIds.set(i, modName);
                            }
                        }
                    } catch (IOException e) {
                        UI_SERVICE.log(e.toString(), MessageType.ERROR);
                    }
                    selectedModType = ModType.MOD_IO;
                }

                if (modIds.isEmpty()) {
                    Popup.displaySimpleAlert(String.format("No valid %s mods found in \"%s\".",
                            (selectedModType == ModType.STEAM ? "Steam" : "Mod.io"),
                            selectedModlistFile.getName()), MessageType.ERROR);
                } else {
                    List<Mod> modList = new ArrayList<>();
                    if (selectedModType == ModType.STEAM) {
                        int numDuplicateMods = 0;
                        for (String s : modIds) {
                            SteamMod duplicateMod = ModListManagerHelper.findDuplicateSteamMod(s, UI_SERVICE.getCurrentModList());
                            if (duplicateMod != null) {
                                numDuplicateMods++;
                            } else
                                modList.add(new SteamMod(s));
                        }
                        if (numDuplicateMods > 0) {
                            if (numDuplicateMods == modIds.size()) {
                                Popup.displaySimpleAlert("All the mods in the mod list file are already in the modlist!", STAGE, MessageType.INFO);
                                return;
                            } else {
                                int modFileImportChoice = Popup.displayYesNoDialog(String.format("%d mods in the file were duplicates. Add the remaining %d?", numDuplicateMods, modIds.size() - numDuplicateMods), STAGE, MessageType.INFO);
                                if (modFileImportChoice == 0) {
                                    return;
                                }
                            }
                        }
                        importModsFromList(modList).start();
                    } else {
                        importModIoListFile(modIds).start();
                    }
                }
            }
        }
    }

    private @NotNull Thread importModIoListFile(List<String> modUrls) {
        final Task<List<Result<String>>> TASK = UI_SERVICE.convertModIoUrlListToIds(modUrls);

        TASK.setOnRunning(workerStateEvent -> {
            modIoUrlToIdName.setVisible(true);
            disableUserInputElements(true);
            modImportProgressDenominator.setVisible(false);
            modImportProgressPanel.setVisible(true);
        });

        TASK.setOnSucceeded(workerStateEvent -> {
            Platform.runLater(() -> {
                modIoUrlToIdName.setVisible(false);
                modImportProgressDenominator.setVisible(true);
            });

            List<Result<String>> modIdResults = TASK.getValue();
            List<Mod> modList = new ArrayList<>();

            int duplicateMods = 0;
            for (Result<String> r : modIdResults) {
                if (r.isSuccess()) {
                    modList.add(new ModIoMod(r.getPayload()));
                } else {
                    if (r.getCurrentMessage() != null) {
                        if (r.getCurrentMessage().endsWith("already exists in the mod list!")) {
                            duplicateMods++;
                        }
                    }
                    UI_SERVICE.log(r);
                }
            }

            if (!modList.isEmpty()) {
                if (duplicateMods > 0) {
                    Popup.displaySimpleAlert(duplicateMods + " mods already in the modlist were found.", MessageType.INFO);
                }
                importModsFromList(modList).start();
            } else {
                if (duplicateMods > 0) {
                    Popup.displaySimpleAlert("All the mods in the modlist file are already in the modlist!", STAGE, MessageType.INFO);
                } else {
                    Popup.displaySimpleAlert("Could not add any of the mods in the modlist file. See the log for more information.", STAGE, MessageType.WARN);
                }

                //Reset our UI settings for the mod progress
                modImportProgressWheel.setVisible(false);
                FadeTransition fadeTransition = new FadeTransition(Duration.millis(1200), modImportProgressPanel);
                fadeTransition.setFromValue(1d);
                fadeTransition.setToValue(0d);

                fadeTransition.setOnFinished(actionEvent -> {
                    disableUserInputElements(false);
                    modImportProgressWheel.setVisible(true);
                    resetModImportProgressUi();
                });

                fadeTransition.play();
            }
        });

        Thread thread = Thread.ofVirtual().unstarted(TASK);
        thread.setDaemon(true);
        return thread;
    }

    private String getSteamModLocationFromUser(boolean steamCollection) {
        String chosenModId = "";

        do {
            String userInputModId = getUserModIdInput();
            String lastPressedButtonId = ID_AND_URL_MOD_IMPORT_INPUT.getLastPressedButtonId();
            if (lastPressedButtonId == null || !lastPressedButtonId.equals("accept")) {
                break;
            }

            if (StringUtils.isAlpha(userInputModId)) {
                Popup.displaySimpleAlert("Mod ID must contain a number!", STAGE, MessageType.WARN);
                continue;
            }

            String modId = StringUtils.isNumeric(userInputModId) ? userInputModId :
                    STEAM_WORKSHOP_MOD_ID.matcher(userInputModId)
                            .results()
                            .map(MatchResult::group)
                            .collect(Collectors.joining(""));

            if (modId.isBlank()) {
                Popup.displaySimpleAlert("Invalid Mod ID or URL entered.", STAGE, MessageType.WARN);
                continue;
            }
            if (!StringUtils.isNumeric(modId)) {
                modId = modId.substring(3);
            }

            if (!steamCollection) {
                SteamMod duplicateSteamMod = ModListManagerHelper.findDuplicateSteamMod(modId, UI_SERVICE.getCurrentModList());
                if (duplicateSteamMod != null) {
                    Popup.displaySimpleAlert(String.format("\"%s\" is already in the modlist!", duplicateSteamMod.getFriendlyName()), MessageType.WARN);
                    continue;
                }
            }

            chosenModId = modId;
            break;
        } while (true);

        ID_AND_URL_MOD_IMPORT_INPUT.getInput().clear();

        return chosenModId;
    }

    private String getModIoModLocationFromUser() {
        boolean goodModId = false;
        String chosenModId = "";

        do {
            String userInputModId = getUserModIdInput();
            String lastPressedButtonId = ID_AND_URL_MOD_IMPORT_INPUT.getLastPressedButtonId();
            if (lastPressedButtonId != null && lastPressedButtonId.equals("accept")) {
                String modUrlName;

                if (StringUtils.isNumeric(userInputModId)) {
                    modUrlName = userInputModId;
                } else {
                    modUrlName = MOD_IO_URL.matcher(userInputModId)
                            .results()
                            .map(MatchResult::group)
                            .collect(Collectors.joining());
                }

                if (!modUrlName.isEmpty()) {
                    //Unlike the steam check, the duplicate check has to happen down in the scraping layer since we don't know the mod ID yet.
                    chosenModId = modUrlName;
                    goodModId = true;
                } else {
                    Popup.displaySimpleAlert("Invalid Mod ID or URL entered.", STAGE, MessageType.WARN);
                }
            } else {
                goodModId = true;
            }
        } while (!goodModId);

        ID_AND_URL_MOD_IMPORT_INPUT.getInput().clear();

        //This will return either the name of a mod as it appears in the url, or the actual mod ID. We can have the controller handle parsing these out.
        return chosenModId;
    }

    @FXML
    public void manageModProfiles() {
        if (!UI_SERVICE.getUSER_CONFIGURATION().isRunFirstTimeSetup()) {
            MOD_PROFILE_MANAGER_VIEW.show(STAGE);
        } else {
            TutorialUtility.tutorialCoverStage(TUTORIAL_HIGHLIGHT_PANES, STAGE);
            MOD_PROFILE_MANAGER_VIEW.runTutorial(STAGE);
            Popup.displaySimpleAlert("Now let's select a Space Engineers save file by pressing the \"Manage SE Saves\" button.", STAGE, MessageType.INFO);
            TutorialUtility.tutorialElementHighlight(TUTORIAL_HIGHLIGHT_PANES, STAGE.getWidth(), STAGE.getHeight(), manageSaveProfiles);
            manageSaveProfiles.requestFocus();
        }
    }

    @FXML
    public void manageSaveProfiles() {
        if (!UI_SERVICE.getUSER_CONFIGURATION().isRunFirstTimeSetup()) {
            SAVE_MANAGER_VIEW.show(STAGE);
        } else {
            TutorialUtility.tutorialCoverStage(TUTORIAL_HIGHLIGHT_PANES, STAGE);
            SAVE_MANAGER_VIEW.runTutorial(STAGE);
            runTutorialAddModStep();
        }
        modTable.sort();
    }

    @FXML
    private void importModlistFile() {
        FileChooser importChooser = new FileChooser();
        importChooser.setTitle("Import Modlist");
        importChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        importChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SEMM Modlists", "*.semm"));

        File savePath = importChooser.showOpenDialog(STAGE);

        if (savePath != null) {
            Result<Void> modlistProfileResult = UI_SERVICE.importModlistProfile(savePath);
            if (modlistProfileResult.isSuccess()) {
                modProfileDropdown.getSelectionModel().selectLast();
            }
            Popup.displaySimpleAlert(modlistProfileResult, STAGE);
        }
    }


    @FXML
    private void exportModlistFile() {
        ModListManagerHelper.exportModlistFile(STAGE, UI_SERVICE);
    }

    //Apply the modlist the user is currently using to the save profile they're currently using.
    @FXML
    private void applyModlist() throws IOException {
        if (!UI_SERVICE.getUSER_CONFIGURATION().isRunFirstTimeSetup()) {
            SaveProfile currentSaveProfile = UI_SERVICE.getCurrentSaveProfile();
            if (currentSaveProfile.isSaveExists()) {
                int overwriteChoice = Popup.displayYesNoDialog("Are you sure you want to apply this modlist to the current save? The modlist in the save will be overwritten.", STAGE, MessageType.WARN);
                if (overwriteChoice == 1) {
                    sortAndApplyModList();
                }
            } else {
                Popup.displaySimpleAlert("The current save cannot be found.", STAGE, MessageType.ERROR);
            }
        } else {
            sortAndApplyModList();
            List<String> tutorialMessages = new ArrayList<>();
            tutorialMessages.add("You have successfully applied your mod list to a save. " +
                    "You can optionally launch Space Engineers from SEMM by clicking on the \"Launch SE\" button however the mods you've added will still be loaded if you launch the game through steam.");
            tutorialMessages.add("Now get out there and start modding, Engineers!");
            Popup.displayNavigationDialog(tutorialMessages, STAGE, MessageType.INFO, "Congratulations!");

            UI_SERVICE.getUSER_CONFIGURATION().setRunFirstTimeSetup(false);

            Result<Void> saveConfigResult = UI_SERVICE.saveUserConfiguration();
            if (!saveConfigResult.isSuccess()) {
                UI_SERVICE.log(saveConfigResult);
                Popup.displaySimpleAlert(saveConfigResult);
            }
            runTutorialCleanup();
        }
    }

    // Deep copy list and sort by priority.
    private void sortAndApplyModList() throws IOException {
        List<Mod> copiedModList = UI_SERVICE.getCurrentModList().stream()
                .filter(Mod::isActive)
                .sorted(Comparator.comparing(Mod::getLoadPriority))
                .collect(Collectors.toList());

        if (copiedModList.isEmpty()) {
            int emptyWriteChoice = Popup.displayYesNoDialog("The modlist contains no mods. Do you still want to apply it?", STAGE, MessageType.WARN);
            if (emptyWriteChoice != 1) {
                return;
            }
        }

        Result<Void> modApplyResult = UI_SERVICE.applyModlist(copiedModList, UI_SERVICE.getCurrentSaveProfile());
        if (modApplyResult.isSuccess()) {
            Popup.displaySimpleAlert("Mod list successfully applied!", STAGE, MessageType.INFO);
        } else {
            Popup.displaySimpleAlert(modApplyResult, STAGE);
        }

        if (modApplyResult.isSuccess())
            UI_SERVICE.setSaveProfileInformationAfterSuccessfullyApplyingModlist();
        else
            UI_SERVICE.getCurrentSaveProfile().setLastSaveStatus(SaveStatus.FAILED);

        STATUS_BAR_VIEW.update(UI_SERVICE.getCurrentSaveProfile(), UI_SERVICE.getCurrentModListProfile());
    }

    @FXML
    private void launchSpaceEngineers() throws URISyntaxException, IOException {
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
    protected void handleModTableDragOver(@NotNull DragEvent dragEvent) {
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
    private void handleTableActionsOnDragDrop(@NotNull DragEvent dragEvent) {
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
                //TODO: NEw memory model might fix. check.
                UI_SERVICE.getCurrentModListProfile().setModList(UI_SERVICE.getCurrentModList());

                UI_SERVICE.updateModListLoadPriority();
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
        ID_AND_URL_MOD_IMPORT_INPUT.show(STAGE);
        return ID_AND_URL_MOD_IMPORT_INPUT.getInput().getText().trim();
    }

    private void setModAddingInputViewText(String title, String instructions, String promptText) {
        ID_AND_URL_MOD_IMPORT_INPUT.setTitle(title);
        ID_AND_URL_MOD_IMPORT_INPUT.setInputInstructions(instructions);
        ID_AND_URL_MOD_IMPORT_INPUT.setPromptText(promptText);
        ID_AND_URL_MOD_IMPORT_INPUT.setEmptyTextMessage("URL/ID cannot be blank!");
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

    private @NotNull Thread importSteamCollection(String collectionId) {
        final Task<List<Result<String>>> TASK = UI_SERVICE.importSteamCollection(collectionId);

        TASK.setOnRunning(workerStateEvent -> {
            //We lockout the user input here to prevent any problems from the user doing things while the modlist is modified.
            disableUserInputElements(true);
            modImportProgressPanel.setVisible(true);
            disableModImportUiText(true);
            modImportSteamCollectionName.setVisible(true);
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
                        SteamMod mod = new SteamMod(steamCollectionModId.getPayload());
                        successfullyFoundMods.add(mod);
                    } else {
                        if (steamCollectionModId.getType() == ResultType.INVALID) duplicateModIds++;
                    }
                }


                if (duplicateModIds == steamCollectionModIds.size()) {
                    Popup.displaySimpleAlert("All the mods in the collection are already in the modlist!", STAGE, MessageType.INFO);
                    resetUiOnInvalidCollectionModImportCount();
                } else if (modIdsSuccessfullyFound == 0) {
                    Popup.displaySimpleAlert("Collection contained no mods. Items like scripts, blueprints, worlds, and other non-mod objects are not able to be imported.", STAGE, MessageType.WARN);
                    resetUiOnInvalidCollectionModImportCount();
                } else {
                    int totalNumberOfMods = modIdsSuccessfullyFound + duplicateModIds;
                    String postCollectionScrapeMessage = totalNumberOfMods +
                            " mods had their ID's successfully pulled. " + duplicateModIds + " were duplicates. Add the remaining " +
                            (totalNumberOfMods - duplicateModIds) + "?";

                    int userChoice = Popup.displayYesNoDialog(postCollectionScrapeMessage, STAGE, MessageType.INFO);

                    if (userChoice == 1) {
                        importModsFromList(successfullyFoundMods).start();
                    }

                    Platform.runLater(() -> {
                        modImportSteamCollectionName.setVisible(false);
                        disableModImportUiText(false);

                        if (userChoice != 1) {
                            disableUserInputElements(false);
                            resetModImportProgressUi();
                        }
                    });
                }
            } else {
                Popup.displaySimpleAlert(steamCollectionModIds.getFirst(), STAGE);
                Platform.runLater(() -> {
                    modImportSteamCollectionName.setVisible(false);
                    disableModImportUiText(false);

                    disableUserInputElements(false);
                    resetModImportProgressUi();

                });
            }

        });

        Thread thread = Thread.ofVirtual().unstarted(TASK);
        thread.setDaemon(true);
        return thread;
    }

    private void resetUiOnInvalidCollectionModImportCount() {
        Platform.runLater(() -> {
            FadeTransition fadeTransition = new FadeTransition(Duration.millis(1200), modImportProgressPanel);
            fadeTransition.setFromValue(1d);
            fadeTransition.setToValue(0d);

            fadeTransition.setOnFinished(actionEvent -> {
                modImportSteamCollectionName.setVisible(false);
                disableModImportUiText(false);
                disableUserInputElements(false);
                resetModImportProgressUi();
            });

            fadeTransition.play();
        });
    }

    private @NotNull Thread addSingleModIoModFromId(String modUrl) {
        final Task<Result<String>> TASK = UI_SERVICE.convertModIoUrlToId(modUrl);

        TASK.setOnRunning(workerStateEvent -> Platform.runLater(() -> {
            modIoUrlToIdName.setVisible(true);
            disableUserInputElements(true);
            modImportProgressDenominator.setVisible(false);
            modImportProgressPanel.setVisible(true);
        }));

        TASK.setOnSucceeded(workerStateEvent -> {
            Platform.runLater(() -> {
                modIoUrlToIdName.setVisible(false);
                modImportProgressDenominator.setVisible(true);
            });

            Result<String> modIdResult = TASK.getValue();
            if (modIdResult.isSuccess()) {
                ModIoMod mod = new ModIoMod(modIdResult.getPayload());
                final Mod[] modList = new Mod[1];
                modList[0] = mod;
                importModsFromList(List.of(modList)).start();
            } else {
                //This gets set down in the mod addition thread too, but that won't ever get hit if we fail.
                Platform.runLater(() -> {
                    modImportProgressWheel.setVisible(false);
                    UI_SERVICE.getModImportProgressDenominatorProperty().setValue(1);
                });
                UI_SERVICE.log(modIdResult);
                Popup.displaySimpleAlert(modIdResult, STAGE);

                Platform.runLater(() -> {
                    //Fadeout the UI if it doesn't succeed.
                    FadeTransition fadeTransition = new FadeTransition(Duration.millis(1000), modImportProgressPanel);
                    fadeTransition.setFromValue(1d);
                    fadeTransition.setToValue(0d);

                    fadeTransition.setOnFinished(actionEvent -> {
                        disableUserInputElements(false);
                        resetModImportProgressUi();
                    });
                    fadeTransition.play();
                });
            }
        });

        Thread thread = Thread.ofVirtual().unstarted(TASK);
        thread.setDaemon(true);
        return thread;
    }

    /**
     * This is the main function called to import mods into SEMM.
     *
     * @param modList The list of mods to import
     */
    public @NotNull Thread importModsFromList(List<Mod> modList) {
        final Task<List<Result<Mod>>> TASK = UI_SERVICE.importModsFromList(modList);

        TASK.setOnRunning(workerStateEvent -> {
            //We lockout the user input here to prevent any problems from the user doing things while the modlist is modified.
            disableUserInputElements(true);
            modImportProgressPanel.setVisible(true);
        });

        TASK.setOnSucceeded(workerStateEvent -> Platform.runLater(() -> {
            modImportProgressWheel.setVisible(false);

            Mod topMostMod = ModImportUtility.addModScrapeResultsToModlist(UI_SERVICE, STAGE, TASK.getValue(), modList.size());

            modTable.sort();

            if (topMostMod != null) {
                modTable.getSelectionModel().clearSelection();
                modTable.getSelectionModel().select(topMostMod);
                modTable.scrollTo(modTable.getSelectionModel().getSelectedIndex());
            }

            ModImportUtility.finishImportingMods(TASK.getValue(), UI_SERVICE);
            cleanupModImportUi();
            //We call this here because it keeps far too many unnecessary references in memory without it right after the web scraping.
            //It really, truly is, not cleaning up when it should at this point. Trust me.
            System.gc();
        }));

        Thread thread = Thread.ofVirtual().unstarted(TASK);
        thread.setDaemon(true);
        return thread;
    }

    private void cleanupModImportUi() {
        //Reset our UI settings for the mod progress
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(1200), modImportProgressPanel);
        fadeTransition.setFromValue(1d);
        fadeTransition.setToValue(0d);

        fadeTransition.setOnFinished(actionEvent -> {
            disableUserInputElements(false);
            resetModImportProgressUi();
            Platform.runLater(() -> {
                if (UI_SERVICE.getUSER_CONFIGURATION().isRunFirstTimeSetup()) {
                    List<String> tutorialMessages = new ArrayList<>();
                    TutorialUtility.tutorialElementHighlight(TUTORIAL_HIGHLIGHT_PANES, STAGE.getWidth(), STAGE.getHeight(), modTable);
                    tutorialMessages.add("Mods that you import to a mod list will be active by default and applied to the save when you hit the \"Apply Mod List\" button. " +
                            "If you don't want to apply a mod to a save without removing it from the list click on the blue checkmark next to an item to deactivate it.");
                    tutorialMessages.add("To apply the mods imported to your mod list to a save you need to press the \"Apply Mod List\" button. " +
                            "This will overwrite any mods currently on that save, and if you apply a mod list that doesn't contain any active mods to a save it will remove all mods on a save.");
                    Popup.displayNavigationDialog(tutorialMessages, STAGE, MessageType.INFO, "Applying the Mod List");
                    TutorialUtility.tutorialElementHighlight(TUTORIAL_HIGHLIGHT_PANES, STAGE.getWidth(), STAGE.getHeight(), applyModlist);
                }
            });
        });

        fadeTransition.play();
    }

    //TODO: These function names suck. Make them something like "show{name of what the UI is. Like "steam collection check panel.}.
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

    private void disableModImportUiText(boolean shouldDisable) {
        modImportProgressActionName.setVisible(!shouldDisable);
        modImportProgressNumerator.setVisible(!shouldDisable);
        modImportProgressDivider.setVisible(!shouldDisable);
        modImportProgressDenominator.setVisible(!shouldDisable);
    }

    public void resetModImportProgressUi() {
        modImportProgressPanel.setVisible(false);
        modImportProgressPanel.setOpacity(1d);
        UI_SERVICE.getModImportProgressNumeratorProperty().setValue(0);
        UI_SERVICE.getModImportProgressDenominatorProperty().setValue(0);
        UI_SERVICE.getModImportProgressPercentageProperty().setValue(0d);
        modImportProgressWheel.setVisible(true);
    }

    public void runTutorialModListManagementStep() {
        STAGE.getScene().addEventFilter(KeyEvent.KEY_PRESSED, UI_SERVICE.getKEYBOARD_BUTTON_NAVIGATION_DISABLER());
        TutorialUtility.tutorialElementHighlight(TUTORIAL_HIGHLIGHT_PANES, STAGE.getWidth(), STAGE.getHeight(), manageModProfiles);
        ((Pane) STAGE.getScene().getRoot()).getChildren().addAll(TUTORIAL_HIGHLIGHT_PANES);
        manageModProfiles.requestFocus();
    }

    public void runTutorialAddModStep() {
        STAGE.getScene().addEventFilter(KeyEvent.KEY_PRESSED, UI_SERVICE.getKEYBOARD_BUTTON_NAVIGATION_DISABLER());

        if (modImportDropdown.getItems().size() > 2) {
            modImportDropdown.getItems().subList(2, modImportDropdown.getItems().size()).clear();
        }

        modImportDropdown.requestFocus();
        modImportDropdown.layout();

        TutorialUtility.tutorialElementHighlight(TUTORIAL_HIGHLIGHT_PANES, STAGE.getWidth(), STAGE.getHeight(), modImportDropdown);
        List<String> tutorialMessages = new ArrayList<>();
        tutorialMessages.add("Now that we have a mod list and a save profile we can add some new mods.");
        tutorialMessages.add("""
                SEMM supports four different ways of adding mods:
                    1. Via Steam Workshop URL or ID.
                    2. Via a Mod.io URL or ID.
                    3. Via Steam Workshop collection URL or ID.
                    4. Via a text file containing a list of URLs or IDs for items either on the Steam Workshop or on Mod.io, however you cannot include the URLs for Steam Workshop collections and/or mix URLs from the Steam Workshop and Mod.io in the same text file.
                    5. Via importing the list of mods by selecting an existing Space Engineers save file.""");
        tutorialMessages.add("SEMM can only import and manage mods. Blueprints, worlds, scripts, and similar items are not mods and cannot be imported.");
        tutorialMessages.add("For this tutorial let's import a mod from the Steam Workshop using a URL. " +
                "Browse the workshop for a mod you want to import and copy the URL to your clipboard (if using the steam desktop app after you open the item right click anywhere on the page and select \"Copy Page URL\"). Once you've done that click on the button that says \"Add mod from...\" to open the Import Mods drop-down menu and select the \"Steam Workshop\" option");
        Popup.displayNavigationDialog(tutorialMessages, STAGE, MessageType.INFO, "Adding Mods");
    }

    public void runTutorialCleanup() {
        STAGE.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, UI_SERVICE.getKEYBOARD_BUTTON_NAVIGATION_DISABLER());
        ((Pane) STAGE.getScene().getRoot()).getChildren().removeAll(TUTORIAL_HIGHLIGHT_PANES);
        modImportDropdown.getItems().addAll(
                ModImportType.STEAM_COLLECTION.getName(),
                ModImportType.MOD_IO.getName(),
                ModImportType.EXISTING_SAVE.getName(),
                ModImportType.FILE.getName());
        STAGE.setResizable(true);
    }
}