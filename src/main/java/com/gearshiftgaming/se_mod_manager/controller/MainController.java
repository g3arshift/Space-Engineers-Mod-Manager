package com.gearshiftgaming.se_mod_manager.controller;

import com.gearshiftgaming.se_mod_manager.backend.domain.ModlistService;
import com.gearshiftgaming.se_mod_manager.backend.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.LogMessage;
import com.gearshiftgaming.se_mod_manager.frontend.models.LogCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.MessageType;
import com.gearshiftgaming.se_mod_manager.frontend.models.ModTableCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.apache.logging.log4j.Logger;

import java.io.IOException;


public class MainController {

    private Logger logger;

    private final String DESKTOP_PATH = System.getProperty("user.home") + "/Desktop";
    private final String APP_DATA_PATH = System.getenv("APPDATA") + "/SpaceEngineers/Saves";

    private SandboxService sandboxService;
    private ModlistService modlistService;

    private ObservableList<LogMessage> viewableLogList;

    //FXML Items
    @FXML
    private MenuItem settings;

    @FXML
    private MenuItem saveModlistAs;

    @FXML
    private CheckMenuItem logToggle;

    @FXML
    private MenuItem themes;

    @FXML
    private MenuItem close;

    @FXML
    private MenuItem about;

    @FXML
    private MenuItem guide;

    @FXML
    private MenuItem faq;

    @FXML
    private ComboBox<String> modProfileDropdown;

    @FXML
    private ComboBox<String> selectedSaveDropdown;

    @FXML
    private Text activeModCount;

    @FXML
    private Text modConflicts;

    @FXML
    private ChoiceBox<String> modImportDropdown;

    @FXML
    private Button saveModlist;

    @FXML
    private Button resetModlist;

    @FXML
    private Button injectModlist;

    @FXML
    private Button launchSpaceEngineers;

    @FXML
    private Label lastSaved;

    @FXML
    private Rectangle saveStatusColor;

    @FXML
    private Label saveStatus;

    @FXML
    private SplitPane mainViewSplit;

    @FXML
    private TableView<ModTableCell> modTable; //TODO: Create a new object for the mod items that contains a check box and all the context menu stuff

    @FXML
    private TextField modTableSearchBox;

    @FXML
    private ListView<LogMessage> viewableLog;

    public void initController(Logger logger, ObservableList<LogMessage> viewableLogList, int minWidth, int minHeight, SandboxService sandboxService, ModlistService modlistService) throws IOException, XmlPullParserException {
        this.logger = logger;
        this.viewableLogList = viewableLogList;
        this.sandboxService = sandboxService;
        this.modlistService = modlistService;

        setupSceneItems();
        viewableLogList.add(new LogMessage("Finished MainViewController setup.", MessageType.INFO, logger));
    }

    public void setupSceneItems() {
        viewableLog.setItems(viewableLogList);
        viewableLog.setCellFactory(param -> new LogCell());

        modImportDropdown.getItems().addAll("Steam", "Mod.io", "Steam Collection", "Mod List File");
        modImportDropdown.getSelectionModel().selectFirst();

        selectedSaveDropdown.getItems().add("Manage..."); //TODO: Setup a service to read a created user config that has all their modprofile information saved in it
        //TODO: Setup a service to get options for the user created mod profiles
        //TODO: Setup an activeModList var in ModList service to track active mods
        //TODO: Setup a function in ModList service to track conflicts.
        //TODO: Populate modtable
    }
    //TODO: Setup control stuff for the split pane and log resizing/closing.
    //TODO: Hookup all the buttons to everything
    //TODO: Hookup connections for the last save date and status
}
