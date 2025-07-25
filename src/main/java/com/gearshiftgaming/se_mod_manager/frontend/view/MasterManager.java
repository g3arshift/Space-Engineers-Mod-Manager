package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.AppContext;
import com.gearshiftgaming.se_mod_manager.backend.domain.archive.TarballArchiveTool;
import com.gearshiftgaming.se_mod_manager.backend.domain.archive.ZipArchiveTool;
import com.gearshiftgaming.se_mod_manager.backend.domain.tool.ToolManagerService;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.ModIoMod;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.ModType;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.SteamMod;
import com.gearshiftgaming.se_mod_manager.backend.models.save.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.save.SaveStatus;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.*;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.models.mastermanager.LogCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.mastermanager.ModImportType;
import com.gearshiftgaming.se_mod_manager.frontend.models.mastermanager.ModNameCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.mastermanager.ModTableRowFactory;
import com.gearshiftgaming.se_mod_manager.frontend.models.utility.ModImportUtility;
import com.gearshiftgaming.se_mod_manager.frontend.view.helper.ModListManagerHelper;
import com.gearshiftgaming.se_mod_manager.frontend.view.input.GeneralFileInput;
import com.gearshiftgaming.se_mod_manager.frontend.view.input.SaveInput;
import com.gearshiftgaming.se_mod_manager.frontend.view.input.SimpleInput;
import com.gearshiftgaming.se_mod_manager.frontend.view.popup.ThreeButtonChoice;
import com.gearshiftgaming.se_mod_manager.frontend.view.popup.Popup;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.TutorialUtility;
import com.gearshiftgaming.se_mod_manager.frontend.view.popup.TwoButtonChoice;
import com.gearshiftgaming.se_mod_manager.operatingsystem.OperatingSystemVersionUtility;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
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
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
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
public class MasterManager {

    //TODO: We need to clean this up and look at separting it into different classes. This is a LOT of variables in one class.
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
    private StackPane mainViewStack;

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
    @Getter
    private Tab conflictsTab;

    @FXML
    private StackPane modDescriptionBackground;

    @FXML
    @Getter
    private WebView modDescription;

    @FXML
    private ListView<LogMessage> viewableLog;


    private final UiService uiService;

    private final ObservableList<LogMessage> userLog;

    @Getter
    private boolean mainViewSplitDividerVisible = true;

    private final DataFormat serializedMimeType;

    private ListChangeListener<TableColumn<Mod, ?>> sortListener;

    @Getter
    private Timeline scrollTimeline;

    private final List<Mod> selections;

    @Getter
    @Setter
    //This is a really dumb hack that we have to use to actually get a row as it is styled in the application.
    private TableRow<Mod> singleTableRow;

    @Getter
    @Setter
    private TableRow<Mod> previousRow;
    private CheckMenuItem logToggle;

    private CheckMenuItem modDescriptionToggle;

    private CheckMenuItem conflictsToggle;

    //This is the reference to the controller for the bar located in the bottom section of the main borderpane. We need everything in it so might as well get the whole reference.
    private final StatusBar statusBarView;

    private final ModListManagerHelper modListManagerHelper;

    @Getter
    private ScrollBar modTableVerticalScrollBar;

    private TableHeaderRow headerRow;

    private final ModListProfileManager modProfileManagerView;

    private final SaveProfileManager saveManagerView;

    @Getter
    private FilteredList<Mod> filteredModList;

    @Getter
    private final Stage stage;

    private final SimpleInput idAndUrlModImportInput;

    private final SaveInput modFileSelectionView;

    private final GeneralFileInput generalFileSelectView;

    private final String steamModDateFormat;

    private final Pattern steamWorkshopModId;

    private final Pattern modIoUrl;

    private final Pane[] tutorialHighlightPanes;

    private final Properties columnFlags = new Properties();

    private final File columnFlagsFile;

    //These three are here purely so we can enable and disable them when we add mods to prevent user interaction from breaking things.
    private ComboBox<MutableTriple<UUID, String, SpaceEngineersVersion>> modProfileDropdown;
    private ComboBox<SaveProfile> saveProfileDropdown;
    private TextField modTableSearchField;

    private final ProgressDisplay progressDisplay;

    public MasterManager(@NotNull UiService uiService, Stage stage, @NotNull Properties properties, StatusBar statusBar,
                         ModListProfileManager modListProfileManager, SaveProfileManager saveProfileManager, SimpleInput modImportInputView, SaveInput saveInput,
                         GeneralFileInput generalFileInput) throws IOException {
        this.uiService = uiService;
        this.stage = stage;
        this.userLog = uiService.getUserLog();
        this.statusBarView = statusBar;
        this.modListManagerHelper = new ModListManagerHelper();
        this.idAndUrlModImportInput = modImportInputView;
        this.modFileSelectionView = saveInput;
        this.generalFileSelectView = generalFileInput;

        this.modProfileManagerView = modListProfileManager;
        this.saveManagerView = saveProfileManager;
        this.progressDisplay = new ProgressDisplay();

        this.steamModDateFormat = properties.getProperty("semm.steam.mod.dateFormat");
        columnFlagsFile = new File(properties.getProperty("semm.userData.trivialData.path"));

        if (columnFlagsFile.exists()) {
            try (FileInputStream in = new FileInputStream(columnFlagsFile)) {
                columnFlags.load(in);
            }
        }

        serializedMimeType = new DataFormat("application/x-java-serialized-object");
        selections = new ArrayList<>();

        filteredModList = new FilteredList<>(this.uiService.getCurrentModList(), mod -> true);

        this.steamWorkshopModId = Pattern.compile(properties.getProperty("semm.steam.mod.id.pattern"));
        this.modIoUrl = Pattern.compile(properties.getProperty("semm.modio.mod.name.pattern"));
        tutorialHighlightPanes = this.uiService.getHighlightPanes();
    }

    public void initView(CheckMenuItem logToggle,
                         CheckMenuItem modDescriptionToggle,
                         CheckMenuItem conflictsToggle,
                         int modTableCellSize,
                         ComboBox<MutableTriple<UUID, String, SpaceEngineersVersion>> modProfileDropdown,
                         ComboBox<SaveProfile> saveProfileDropdown,
                         TextField modTableSearchField,
                         Properties properties) throws IOException, InterruptedException {
        this.logToggle = logToggle;
        this.modDescriptionToggle = modDescriptionToggle;
        this.conflictsToggle = conflictsToggle;

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

        viewableLog.setFixedCellSize(35);

        /* This is a dumb hack, but it swallows the drag events otherwise when we drag rows over it,
         * so just sit there and do nothing when you're dragged over.*/
        modDescription.setOnDragOver(dragEvent -> {
        });

        //TODO: We probably don't need this default hiding, but let's check.
        //progressDisplay.hide();
        mainViewStack.getChildren().add(progressDisplay);

        //Download all the required tools we need for SEMM to function
        uiService.log("Downloading required tools...", MessageType.INFO);

        //Download SteamCMD.
        if (Files.exists(Path.of(properties.getProperty("semm.steam.cmd.windows.localFolderPath")).getParent().resolve("steamcmd.exe")) ||
                Files.exists(Path.of(properties.getProperty("semm.steam.cmd.linux.localFolderPath")).getParent().resolve("steamcmd.sh"))) {
            uiService.log("SteamCMD already installed.", MessageType.INFO);
        } else
            setupTools(properties);

        uiService.logPrivate("Successfully initialized modlist manager.", MessageType.INFO);
    }

    private void setupTools(Properties properties) throws IOException, InterruptedException {
        AppContext appContext = new AppContext(OperatingSystemVersionUtility.getOperatingSystemVersion());
        ToolManagerService toolManagerService = new ToolManagerService(this.uiService,
                appContext.isWindows() ? properties.getProperty("semm.steam.cmd.windows.localFolderPath") : properties.getProperty("semm.steam.cmd.linux.localFolderPath"),
                appContext.isWindows() ? properties.getProperty("semm.steam.cmd.windows.download.source") : properties.getProperty("semm.steam.cmd.linux.download.source"),
                Integer.parseInt(properties.getProperty("semm.steam.cmd.download.retry.limit")),
                Integer.parseInt(properties.getProperty("semm.steam.cmd.download.connection.timeout")),
                Integer.parseInt(properties.getProperty("semm.steam.cmd.download.read.timeout")),
                Integer.parseInt(properties.getProperty("semm.steam.cmd.download.retry.delay")),
                appContext.isWindows() ? new ZipArchiveTool() : new TarballArchiveTool());

        Task<Result<Void>> steamCmdSetupTask = toolManagerService.setupSteamCmd();
        steamCmdSetupTask.setOnRunning(event -> {
            disableUserInputElements(true);
            progressDisplay.setProgressTitleName("Downloading SteamCMD");
            progressDisplay.setProgressTitleVisible(true);
            progressDisplay.showWithMessageAndProgressBinding(steamCmdSetupTask.messageProperty(), steamCmdSetupTask.progressProperty());
        });

        //When the task is finished log our result, display the last message from it, and fade it out.
        steamCmdSetupTask.setOnSucceeded(event -> {
            Result<Void> steamCmdSetupResult = steamCmdSetupTask.getValue();
            uiService.log(steamCmdSetupResult);

            if (steamCmdSetupResult.isFailure()) {
                Popup.displayInfoMessageWithLink("Failed to download SteamCMD. SEMM requires SteamCMD to run. " +
                                "Please submit your log file at the following link.",
                        "https://bugreport.spaceengineersmodmanager.com", "ATTENTION!!!", stage, MessageType.ERROR);
                Platform.exit();
                return;
            }

            progressDisplay.setAllOperationsCompleteState();
            PauseTransition pauseTransition = new PauseTransition(Duration.millis(450));
            pauseTransition.setOnFinished(event1 -> {
                disableUserInputElements(false);
                progressDisplay.close();
            });

            pauseTransition.play();
        });

        Thread.ofVirtual().start(steamCmdSetupTask);
    }

    private void setupModTable(int modTableCellSize) {
        //Format the appearance, styling, and menu`s of our table cells, rows, and columns
        modTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        modTable.setRowFactory(new ModTableRowFactory(uiService, serializedMimeType, selections, this, modListManagerHelper));

        modName.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        modName.setCellFactory(param -> new ModNameCell(uiService));
        modName.setComparator(Comparator.comparing(Mod::getFriendlyName));

        modLastUpdated.setCellValueFactory(cellData -> {
            if (cellData.getValue() instanceof SteamMod steamMod) {
                if (steamMod.getLastUpdated() != null) {
                    return new SimpleStringProperty(steamMod.getLastUpdated().format(DateTimeFormatter.ofPattern(steamModDateFormat)));
                } else {
                    return new SimpleStringProperty("Unknown");
                }
            } else if (cellData.getValue() instanceof ModIoMod modIoMod) {
                String lastUpdated = modIoMod.getLastUpdatedMonthDay().format(DateTimeFormatter.ofPattern("MMM d")) + ", " +
                        modIoMod.getLastUpdatedYear() +
                        " @ " + modIoMod.getLastUpdatedHour().format(DateTimeFormatter.ofPattern("hh:mm a"));

                return new SimpleStringProperty(lastUpdated);
            } else {
                return new SimpleStringProperty("Unknown");
            }
        });

        modLastUpdated.setComparator((date1, date2) -> {
            LocalDateTime firstDateNormalized;
            if (date1.length() <= 19) { //Only steam mods will be greater than 19.
                firstDateNormalized = getModIoLastUpdatedComparatorDate(date1);
            } else {
                firstDateNormalized = LocalDateTime.parse(date1, DateTimeFormatter.ofPattern(steamModDateFormat));
            }

            LocalDateTime secondDateNormalized;
            if (date2.length() <= 19) {
                secondDateNormalized = getModIoLastUpdatedComparatorDate(date2);
            } else {
                secondDateNormalized = LocalDateTime.parse(date2, DateTimeFormatter.ofPattern(steamModDateFormat));
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
        setupColumnToggleMenu();
    }

    private void setupColumnToggleMenu() {
        ContextMenu columnToggleMenu = new ContextMenu();
        for (TableColumn<?, ?> column : modTable.getColumns()) {
            if (!column.getText().equals("Mod Name") && !column.getText().equals("Priority")) {
                CheckMenuItem menuItem = new CheckMenuItem(column.getText());
                boolean isVisible = Boolean.parseBoolean(columnFlags.getProperty(column.getText(), "true"));
                menuItem.setSelected(isVisible);
                column.setVisible(isVisible);

                menuItem.selectedProperty().addListener((observable, wasSelected, isNowSelected) -> {
                    column.setVisible(isNowSelected);
                    columnFlags.setProperty(column.getText(), Boolean.toString(isNowSelected));
                    try {
                        saveColumnFlags();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                columnToggleMenu.getItems().add(menuItem);
            }
        }
        modTable.setOnContextMenuRequested(event -> columnToggleMenu.show(modTable, event.getScreenX(), event.getScreenY()));
    }

    private void saveColumnFlags() throws IOException {
        try (FileOutputStream out = new FileOutputStream(columnFlagsFile)) {
            columnFlags.store(out, "Mod table column visibility flags");
        }
    }

    //TODO: Localize formatting.
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
        viewableLog.setItems(userLog);
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
                uiService.log(e);
                Popup.displaySimpleAlert(String.valueOf(e), stage, MessageType.ERROR);
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
                ModIoMod duplicateModIoMod = ModListManagerHelper.findDuplicateModIoMod(modId, uiService.getCurrentModList());
                if (duplicateModIoMod == null) {
                    ModIoMod mod = new ModIoMod(modId);

                    //This is a bit hacky, but it makes a LOT less code we need to maintain.
                    final Mod[] modList = new Mod[1];
                    modList[0] = mod;
                    importModsFromList(List.of(modList)).start();
                } else {
                    String errorMessage = String.format("\"%s\" is already in the mod list!", duplicateModIoMod.getFriendlyName());
                    uiService.log(errorMessage, MessageType.WARN);
                    Popup.displaySimpleAlert(errorMessage, stage, MessageType.WARN);
                }
            }
        }
    }

    private void addModsFromExistingSave() {
        // Popup the same save chooser we use for save profiles for this and get the file path that way. Look at how the save manager handles it.
        modFileSelectionView.setSaveProfileInputTitle("Import Save Modlist");
        modFileSelectionView.setAddSaveButtonText("Import Mods");
        modFileSelectionView.show(stage);
        File selectedSave = modFileSelectionView.getSelectedSave();
        if (selectedSave != null && modFileSelectionView.getLastPressedButtonId().equals("addSave")) {
            Result<List<Mod>> existingModlistResult = ModImportUtility.getModlistFromSandboxConfig(uiService, selectedSave, stage);

            if (existingModlistResult.isSuccess()) {

                importModsFromList(existingModlistResult.getPayload()).start();
            }
        }
    }

    private void addModsFromFile() {
        ThreeButtonChoice choice = Popup.displayThreeChoiceDialog("Are the mods in the file for Mod.io, or Steam? Modlist files should only contain mods from either Steam or Mod.io, but not both.", stage, MessageType.INFO,
                "Steam", "Mod.io", "Cancel");
        if (choice != ThreeButtonChoice.CANCEL) {
            //TODO: Remove this once we add back mod.io functionality.
            //Popup.displaySimpleAlert("Select a file containing Steam Workshop mod ID's or URL's. Make sure each ID or URL is on its own line by itself.", STAGE, MessageType.INFO);
            generalFileSelectView.resetSelectedSave();
            generalFileSelectView.setSaveProfileInputTitle("Import Modlist from File");
            generalFileSelectView.setNextButtonText("Import Mods");
            generalFileSelectView.setExtensionFilter(new FileChooser.ExtensionFilter("Modlist Files", "*.txt", "*.doc"));
            generalFileSelectView.show(stage);
            File selectedModlistFile = generalFileSelectView.getSelectedFile();
            if (selectedModlistFile != null && generalFileSelectView.getLastPressedButtonId().equals("next")) {
                List<String> modIds = new ArrayList<>();
                ModType selectedModType;

                if (choice == ThreeButtonChoice.LEFT) { //Steam modlist file
                    selectedModType = ModType.STEAM;
                    try {
                        modIds = uiService.getModlistFromFile(selectedModlistFile, ModType.STEAM);
                    } catch (IOException e) {
                        uiService.log(e.toString(), MessageType.ERROR);
                    }
                } else { //Mod.io modlist file
                    try {
                        modIds = uiService.getModlistFromFile(selectedModlistFile, ModType.MOD_IO);
                        for (int i = 0; i < modIds.size(); i++) {
                            String modName;
                            if (!StringUtils.isNumeric(modIds.get(i))) {
                                modName = modIoUrl.matcher(modIds.get(i))
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
                        uiService.log(e.toString(), MessageType.ERROR);
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
                            SteamMod duplicateMod = ModListManagerHelper.findDuplicateSteamMod(s, uiService.getCurrentModList());
                            if (duplicateMod != null) {
                                numDuplicateMods++;
                            } else
                                modList.add(new SteamMod(s));
                        }
                        if (numDuplicateMods > 0) {
                            if (numDuplicateMods == modIds.size()) {
                                Popup.displaySimpleAlert("All the mods in the mod list file are already in the modlist!", stage, MessageType.INFO);
                                return;
                            } else {
                                TwoButtonChoice modFileImportChoice = Popup.displayYesNoDialog(String.format("%d mods in the file were duplicates. Add the remaining %d?", numDuplicateMods, modIds.size() - numDuplicateMods), stage, MessageType.INFO);
                                if (modFileImportChoice == TwoButtonChoice.NO) {
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
        final Task<List<Result<String>>> TASK = uiService.convertModIoUrlListToIds(modUrls);

        TASK.setOnRunning(workerStateEvent -> {
            disableUserInputElements(true);
            progressDisplay.showWithMessageAndProgressBinding(TASK.messageProperty(), TASK.progressProperty());
        });

        TASK.setOnSucceeded(workerStateEvent -> {
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
                    uiService.log(r);
                }
            }

            if (!modList.isEmpty()) {
                if (duplicateMods > 0) {
                    Popup.displaySimpleAlert(duplicateMods + " mods already in the modlist were found.", MessageType.INFO);
                }
                importModsFromList(modList).start();
            } else {
                if (duplicateMods > 0) {
                    Popup.displaySimpleAlert("All the mods in the modlist file are already in the modlist!", stage, MessageType.INFO);
                } else {
                    Popup.displaySimpleAlert("Could not add any of the mods in the modlist file. See the log for more information.", stage, MessageType.WARN);
                }

                progressDisplay.close();
            }
        });

        return Thread.ofVirtual().unstarted(TASK);
    }

    private String getSteamModLocationFromUser(boolean steamCollection) {
        String chosenModId = "";

        do {
            String userInputModId = getUserModIdInput();
            String lastPressedButtonId = idAndUrlModImportInput.getLastPressedButtonId();
            if (lastPressedButtonId == null || !lastPressedButtonId.equals("accept")) {
                break;
            }

            if (StringUtils.isAlpha(userInputModId)) {
                Popup.displaySimpleAlert("Mod ID must contain a number!", stage, MessageType.WARN);
                continue;
            }

            String modId = StringUtils.isNumeric(userInputModId) ? userInputModId :
                    steamWorkshopModId.matcher(userInputModId)
                            .results()
                            .map(MatchResult::group)
                            .collect(Collectors.joining(""));

            if (modId.isBlank()) {
                Popup.displaySimpleAlert("Invalid Mod ID or URL entered.", stage, MessageType.WARN);
                continue;
            }
            if (!StringUtils.isNumeric(modId)) {
                modId = modId.substring(3);
            }

            if (!steamCollection) {
                SteamMod duplicateSteamMod = ModListManagerHelper.findDuplicateSteamMod(modId, uiService.getCurrentModList());
                if (duplicateSteamMod != null) {
                    Popup.displaySimpleAlert(String.format("\"%s\" is already in the modlist!", duplicateSteamMod.getFriendlyName()), MessageType.WARN);
                    continue;
                }
            }

            chosenModId = modId;
            break;
        } while (true);

        idAndUrlModImportInput.getInput().clear();

        return chosenModId;
    }

    private String getModIoModLocationFromUser() {
        boolean goodModId = false;
        String chosenModId = "";

        do {
            String userInputModId = getUserModIdInput();
            String lastPressedButtonId = idAndUrlModImportInput.getLastPressedButtonId();
            if (lastPressedButtonId != null && lastPressedButtonId.equals("accept")) {
                String modUrlName;

                if (StringUtils.isNumeric(userInputModId)) {
                    modUrlName = userInputModId;
                } else {
                    modUrlName = modIoUrl.matcher(userInputModId)
                            .results()
                            .map(MatchResult::group)
                            .collect(Collectors.joining());
                }

                if (!modUrlName.isEmpty()) {
                    //Unlike the steam check, the duplicate check has to happen down in the scraping layer since we don't know the mod ID yet.
                    chosenModId = modUrlName;
                    goodModId = true;
                } else {
                    Popup.displaySimpleAlert("Invalid Mod ID or URL entered.", stage, MessageType.WARN);
                }
            } else {
                goodModId = true;
            }
        } while (!goodModId);

        idAndUrlModImportInput.getInput().clear();

        //This will return either the name of a mod as it appears in the url, or the actual mod ID. We can have the controller handle parsing these out.
        return chosenModId;
    }

    @FXML
    public void manageModProfiles() {
        if (!uiService.getUserConfiguration().isRunFirstTimeSetup()) {
            modProfileManagerView.show(stage);
        } else {
            TutorialUtility.tutorialCoverStage(tutorialHighlightPanes, stage);
            modProfileManagerView.runTutorial(stage);
            Popup.displaySimpleAlert("Now let's select a Space Engineers save file by pressing the \"Manage SE Saves\" button.", stage, MessageType.INFO);
            TutorialUtility.tutorialElementHighlight(tutorialHighlightPanes, stage.getWidth(), stage.getHeight(), manageSaveProfiles);
            manageSaveProfiles.requestFocus();
        }
    }

    @FXML
    public void manageSaveProfiles() {
        if (!uiService.getUserConfiguration().isRunFirstTimeSetup()) {
            saveManagerView.show(stage);
        } else {
            TutorialUtility.tutorialCoverStage(tutorialHighlightPanes, stage);
            saveManagerView.runTutorial(stage);
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

        File savePath = importChooser.showOpenDialog(stage);

        if (savePath != null) {
            Result<Void> modlistProfileResult = uiService.importModlistProfile(savePath);
            if (modlistProfileResult.isSuccess()) {
                modProfileDropdown.getSelectionModel().selectLast();
            }
            Popup.displaySimpleAlert(modlistProfileResult, stage);
        }
    }


    @FXML
    private void exportModlistFile() {
        ModListManagerHelper.exportModlistFile(stage, uiService);
    }

    //Apply the modlist the user is currently using to the save profile they're currently using.
    @FXML
    private void applyModlist() throws IOException {
        if (!uiService.getUserConfiguration().isRunFirstTimeSetup()) {
            SaveProfile currentSaveProfile = uiService.getCurrentSaveProfile();
            if (currentSaveProfile.isSaveExists()) {
                TwoButtonChoice overwriteChoice = Popup.displayYesNoDialog("Are you sure you want to apply this modlist to the current save? The modlist in the save will be overwritten.", stage, MessageType.WARN);
                if (overwriteChoice == TwoButtonChoice.YES) {
                    sortAndApplyModList();
                }
            } else {
                Popup.displaySimpleAlert("The current save cannot be found.", stage, MessageType.ERROR);
            }
        } else {
            sortAndApplyModList();
            List<String> tutorialMessages = new ArrayList<>();
            tutorialMessages.add("You have successfully applied your mod list to a save. " +
                    "You can optionally launch Space Engineers from SEMM by clicking on the \"Launch SE\" button however the mods you've added will still be loaded if you launch the game through steam.");
            tutorialMessages.add("Now get out there and start modding, Engineers!");
            Popup.displayNavigationDialog(tutorialMessages, stage, MessageType.INFO, "Congratulations!");

            uiService.getUserConfiguration().setRunFirstTimeSetup(false);

            Result<Void> saveConfigResult = uiService.saveUserConfiguration();
            if (saveConfigResult.isFailure()) {
                uiService.log(saveConfigResult);
                Popup.displaySimpleAlert(saveConfigResult);
            }
            runTutorialCleanup();
        }
    }

    // Deep copy list and sort by priority.
    private void sortAndApplyModList() throws IOException {
        List<Mod> copiedModList = uiService.getCurrentModList().stream()
                .filter(Mod::isActive)
                .sorted(Comparator.comparing(Mod::getLoadPriority))
                .collect(Collectors.toList())
                .reversed();

        if (copiedModList.isEmpty()) {
            TwoButtonChoice emptyWriteChoice = Popup.displayYesNoDialog("The modlist contains no mods. Do you still want to apply it?", stage, MessageType.WARN);
            if (emptyWriteChoice != TwoButtonChoice.YES) {
                return;
            }
        }

        Result<Void> modApplyResult = uiService.applyModlist(copiedModList, uiService.getCurrentSaveProfile());
        if (modApplyResult.isSuccess()) {
            Popup.displaySimpleAlert("Mod list successfully applied!", stage, MessageType.INFO);
        } else {
            Popup.displaySimpleAlert(modApplyResult, stage);
        }

        if (modApplyResult.isSuccess())
            uiService.setSaveProfileInformationAfterSuccessfullyApplyingModlist();
        else
            uiService.getCurrentSaveProfile().setLastSaveStatus(SaveStatus.FAILED);

        statusBarView.update();
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

    @FXML
    private void closeConflictsTab() {
        conflictsToggle.setSelected(false);
        if(informationPane.getTabs().isEmpty())
            disableSplitPaneDivider();
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
        final double TOTAL_ROW_HEIGHT = uiService.getCurrentModList().size() * singleTableRow.getHeight();
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

        if (dragboard.hasContent(serializedMimeType)) {
            //I'd love to get a class level reference of this, but we need to progressively get it as the view changes

            if (modTableVerticalScrollBar == null) {
                modTableVerticalScrollBar = (ScrollBar) modTable.lookup(".scroll-bar:vertical");
            }

            if (modTableVerticalScrollBar.getValue() == modTableVerticalScrollBar.getMax()) {
                for (Mod m : selections) {
                    uiService.getCurrentModList().remove(m);
                }

                modTable.getSelectionModel().clearSelection();

                for (Mod m : selections) {
                    uiService.getCurrentModList().add(m);
                    modTable.getSelectionModel().select(uiService.getCurrentModList().size() - 1);
                }

                modListManagerHelper.setCurrentModListLoadPriority(modTable, uiService);

                //Redo our sort since our row order has changed
                modTable.sort();

			/*
				We shouldn't need this since currentModList which backs our table is an observable list backed by the currentModProfile.getModList,
				but for whatever reason the changes aren't propagating without this.
			*/
                //TODO: Look into why the changes don't propagate without setting it here. Indicative of a deeper issue or misunderstanding.
                //TODO: NEw memory model might fix. check.
                uiService.getCurrentModListProfile().setModList(uiService.getCurrentModList());

                uiService.updateModListLoadPriority();
            }

            dragEvent.consume();
        }
    }

    private void handleTableActionsOnDragOver(DragEvent dragEvent) {
        ScrollBar verticalScrollBar = (ScrollBar) modTable.lookup(".scroll-bar:vertical");

        if (verticalScrollBar.isVisible() && verticalScrollBar.getValue() == verticalScrollBar.getMax()) {
            Color indicatorColor = Color.web(modListManagerHelper.getSelectedCellBorderColor(uiService));
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
        filteredModList = new FilteredList<>(uiService.getCurrentModList(), mod -> true);
        SortedList<Mod> sortedList = new SortedList<>(filteredModList);
        sortedList.comparatorProperty().bind(modTable.comparatorProperty());
        modTable.setItems(sortedList);
    }

    private String getUserModIdInput() {
        idAndUrlModImportInput.show(stage);
        return idAndUrlModImportInput.getInput().getText().trim();
    }

    private void setModAddingInputViewText(String title, String instructions, String promptText) {
        idAndUrlModImportInput.setTitle(title);
        idAndUrlModImportInput.setInputInstructions(instructions);
        idAndUrlModImportInput.setPromptText(promptText);
        idAndUrlModImportInput.setEmptyTextMessage("URL/ID cannot be blank!");
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
                    uiService.log(e);
                }
                evt.preventDefault();
            }, false);
        }
    }

    private @NotNull Thread importSteamCollection(String collectionId) {
        final Task<List<Result<String>>> TASK = uiService.importSteamCollection(collectionId);

        TASK.setOnRunning(workerStateEvent -> {
            //We lockout the user input here to prevent any problems from the user doing things while the modlist is modified.
            disableUserInputElements(true);
            progressDisplay.showWithMessageBinding(TASK.messageProperty());
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


                if (duplicateModIds == steamCollectionModIds.size())
                    Popup.displaySimpleAlert("All the mods in the collection are already in the modlist!", stage, MessageType.INFO);
                else if (modIdsSuccessfullyFound == 0)
                    Popup.displaySimpleAlert("Collection contained no mods. Items like scripts, blueprints, worlds, and other non-mod objects are not able to be imported.", stage, MessageType.WARN);
                else {
                    int totalNumberOfMods = modIdsSuccessfullyFound + duplicateModIds;
                    String postCollectionScrapeMessage = totalNumberOfMods +
                            " mods had their ID's successfully pulled. " + duplicateModIds + " were duplicates. Add the remaining " +
                            (totalNumberOfMods - duplicateModIds) + "?";

                    TwoButtonChoice userChoice = Popup.displayYesNoDialog(postCollectionScrapeMessage, stage, MessageType.INFO);

                    if (userChoice == TwoButtonChoice.YES) {
                        importModsFromList(successfullyFoundMods).start();
                    }
                }
            } else {
                Popup.displaySimpleAlert(steamCollectionModIds.getFirst(), stage);
                progressDisplay.close();
            }

        });

        return Thread.ofVirtual().unstarted(TASK);
    }

    private @NotNull Thread addSingleModIoModFromId(String modUrl) {
        final Task<Result<String>> TASK = uiService.convertModIoUrlToId(modUrl);

        TASK.setOnRunning(workerStateEvent -> Platform.runLater(() -> {
            disableUserInputElements(true);
            progressDisplay.showWithMessageBinding(TASK.messageProperty());
        }));

        TASK.setOnSucceeded(workerStateEvent -> {

            Result<String> modIdResult = TASK.getValue();
            if (modIdResult.isSuccess()) {
                ModIoMod mod = new ModIoMod(modIdResult.getPayload());
                final Mod[] modList = new Mod[1];
                modList[0] = mod;
                importModsFromList(List.of(modList)).start();
            } else {
                uiService.log(modIdResult);
                Popup.displaySimpleAlert(modIdResult, stage);

                //This gets done down in the mod addition thread too, but that won't ever get hit if we fail.
                progressDisplay.close();
            }
        });

        return Thread.ofVirtual().unstarted(TASK);
    }

    /**
     * This is the main function called to import mods into SEMM.
     *
     * @param modList The list of mods to import
     */
    public @NotNull Thread importModsFromList(List<Mod> modList) {
        final Task<List<Result<Mod>>> TASK = uiService.importModsFromList(modList);

        TASK.setOnRunning(workerStateEvent -> {
            //We lockout the user input here to prevent any problems from the user doing things while the modlist is modified.
            disableUserInputElements(true);
            progressDisplay.showWithMessageAndProgressBinding(TASK.messageProperty(), TASK.progressProperty());
            progressDisplay.setProgressWheelVisible(true);
        });

        TASK.setOnSucceeded(workerStateEvent -> Platform.runLater(() -> {
            List<Result<Mod>> results = TASK.getValue();
            Thread.startVirtualThread(() -> ModImportUtility.finishImportingMods(results, uiService));
            Mod topMostMod = ModImportUtility.addModScrapeResultsToModlist(uiService, stage, results, modList.size());

            modTable.sort();

            if (topMostMod != null) {
                modTable.getSelectionModel().clearSelection();
                modTable.getSelectionModel().select(topMostMod);
                modTable.scrollTo(modTable.getSelectionModel().getSelectedIndex());
            }

            progressDisplay.setProgressWheelVisible(false);

            if (uiService.getUserConfiguration().isRunFirstTimeSetup()) {
                progressDisplay.closeWithCustomPostProcessing(() -> {
                    disableUserInputElements(false);
                    List<String> tutorialMessages = new ArrayList<>();
                    TutorialUtility.tutorialElementHighlight(tutorialHighlightPanes, stage.getWidth(), stage.getHeight(), modTable);
                    tutorialMessages.add("Mods that you import to a mod list will be active by default and applied to the save when you hit the \"Apply Mod List\" button. " +
                            "If you don't want to apply a mod to a save without removing it from the list click on the blue checkmark next to an item to deactivate it.");
                    tutorialMessages.add("To apply the mods imported to your mod list to a save you need to press the \"Apply Mod List\" button. " +
                            "This will overwrite any mods currently on that save, and if you apply a mod list that doesn't contain any active mods to a save it will remove all mods on a save.");
                    Popup.displayNavigationDialog(tutorialMessages, stage, MessageType.INFO, "Applying the Mod List");
                    TutorialUtility.tutorialElementHighlight(tutorialHighlightPanes, stage.getWidth(), stage.getHeight(), applyModlist);
                });
            }

            progressDisplay.close();
            disableUserInputElements(false);


            //We call this here because it keeps far too many unnecessary references in memory without it right after the web scraping. So we give it a hint to collect garbage.
            //It really, truly is, not cleaning up when it should at this point. Trust me.
            //We've just finished scraping, the UI isn't doing anything other than having just finished a transition, and there's really nothing happening. It's a good time.
            System.gc();
        }));

        return Thread.ofVirtual().unstarted(TASK);
    }

    protected void disableUserInputElements(boolean shouldDisable) {
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

    /*TODO: All the mod import/tool downloading stuff is gonna need to be looked at by Reaper again.
    *  I probably missed something when replacing the UI elements, so we need to make sure it transitions properly and looks as it should. */

    public void runTutorialModListManagementStep() {
        stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, uiService.getKeyboardButtonNavigationDisabler());
        TutorialUtility.tutorialElementHighlight(tutorialHighlightPanes, stage.getWidth(), stage.getHeight(), manageModProfiles);
        ((Pane) stage.getScene().getRoot()).getChildren().addAll(tutorialHighlightPanes);
        manageModProfiles.requestFocus();
    }

    public void runTutorialAddModStep() {
        stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, uiService.getKeyboardButtonNavigationDisabler());

        if (modImportDropdown.getItems().size() > 2) {
            modImportDropdown.getItems().subList(2, modImportDropdown.getItems().size()).clear();
        }

        modImportDropdown.requestFocus();
        modImportDropdown.layout();

        TutorialUtility.tutorialElementHighlight(tutorialHighlightPanes, stage.getWidth(), stage.getHeight(), modImportDropdown);
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
        Popup.displayNavigationDialog(tutorialMessages, stage, MessageType.INFO, "Adding Mods");
    }

    public void runTutorialCleanup() {
        stage.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, uiService.getKeyboardButtonNavigationDisabler());
        ((Pane) stage.getScene().getRoot()).getChildren().removeAll(tutorialHighlightPanes);
        modImportDropdown.getItems().addAll(
                ModImportType.STEAM_COLLECTION.getName(),
                ModImportType.MOD_IO.getName(),
                ModImportType.EXISTING_SAVE.getName(),
                ModImportType.FILE.getName());
        stage.setResizable(true);
    }
}