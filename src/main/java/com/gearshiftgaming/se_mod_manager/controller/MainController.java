package com.gearshiftgaming.se_mod_manager.controller;

import com.gearshiftgaming.se_mod_manager.backend.models.utility.LogMessage;
import com.gearshiftgaming.se_mod_manager.frontend.models.LogCell;
import com.gearshiftgaming.se_mod_manager.frontend.models.MessageType;
import com.gearshiftgaming.se_mod_manager.frontend.models.ModCell;
import com.gearshiftgaming.se_mod_manager.backend.models.ModType;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import lombok.Getter;
import org.apache.logging.log4j.Logger;

@Getter
public class MainController {

    //TODO: We need to replace the window control bar for the window.
    private ObservableList<LogMessage> viewableLogList;
    private Logger logger;

    //FXML Items
    @FXML
    private MenuItem settings;

    @FXML
    private MenuItem saveModlistAs;

    @FXML
    private CheckMenuItem logToggle;

    @FXML
    private CheckMenuItem modDescriptionToggle;

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

    //TODO: Set the text
    @FXML
    private Label lastSaved;

    //TODO: Set the color of this with the AtlantaFX global colors
    //TODO: Set the text
    @FXML
    private Label saveStatus;

    //TODO: Set the color of this with the AtlantaFX global colors
    //TODO: Set the text
    @FXML
    private Label lastModifiedBy;

    @FXML
    private SplitPane mainViewSplit;

    //TODO: Low priority: Fix the alignment of the table headers to be centered, not center left.
    @FXML
    private TableView<ModCell> modTable; //TODO: Create a new object for the mod items that contains a check box and all the context menu stuff

    //TODO: The modcell needs properties for each of these https://stackoverflow.com/questions/53751455/how-to-create-a-javafx-tableview-without-warnings
    @FXML
    private TableColumn<ModCell, String> modName;

    @FXML
    private TableColumn<ModCell, ModType> modType;

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
    private TextField modTableSearchBox;

    @FXML
    private TabPane informationPane;

    @FXML
    private Tab logTab;

    @FXML
    private Tab modDescriptionTab;

    private boolean mainViewSplitDividerVisible = true;

    @FXML
    private ListView<LogMessage> viewableLog;

    @FXML
    private Button clearSearchBox;


    public void initController(ObservableList<LogMessage> viewableLogList, Logger logger) {
        this.viewableLogList = viewableLogList;
        this.logger = logger;

        //TODO: Remove and replace with a debug log message.
        viewableLogList.add(new LogMessage("Finished MainViewController setup", MessageType.INFO, logger));
        setupMainViewItems();
    }

    //TODO: Move to service
    public void setupMainViewItems() {
        viewableLog.setItems(viewableLogList);
        viewableLog.setCellFactory(param -> new LogCell());

        modImportDropdown.getItems().addAll("Add mods by Steam Workshop ID", "Add mods from Steam Collection", "Add mods from Mod.io", "Add mods from modlist file");

        selectedSaveDropdown.getItems().add("Manage...");
        //TODO: Setup a service to read a created user config that has all their modprofile information saved in it
        //TODO: Setup a service to get options for the user created mod profiles
        //TODO: Setup an activeModList var in ModList service to track active mods
        //TODO: Setup a function in ModList service to track conflicts.
        //TODO: Populate modtable
    }
    //TODO: Setup control stuff for the split pane and log resizing/closing.
    //TODO: Hookup all the buttons to everything
    //TODO: Hookup connections for the last save date and status

    @FXML
    private void printTest() {
        System.out.println("Button pressed!");
    }

    @FXML
    private void clearSearchBox() {
        modTableSearchBox.clear();
    }

    @FXML
    private void closeLogTab() {
        logToggle.setSelected(false);
        if(informationPane.getTabs().isEmpty()) {
            disableSplitPaneDivider();
        }
    }

    @FXML
    private void toggleLog() {
        if(!logToggle.isSelected()) {
            informationPane.getTabs().remove(logTab);
        }else informationPane.getTabs().add(logTab);

        if(informationPane.getTabs().isEmpty()) {
            disableSplitPaneDivider();
        } else if(!mainViewSplitDividerVisible) {
            enableSplitPaneDivider();
        }
    }

    @FXML
    private void closeModDescriptionTab() {
        modDescriptionToggle.setSelected(false);
        if(informationPane.getTabs().isEmpty()) {
            disableSplitPaneDivider();
        }
    }

    @FXML
    private void toggleModDescription() {
        if(!modDescriptionToggle.isSelected()) {
            informationPane.getTabs().remove(modDescriptionTab);
        } else informationPane.getTabs().add(modDescriptionTab);

        if(informationPane.getTabs().isEmpty()) {
            disableSplitPaneDivider();
        } else if(!mainViewSplitDividerVisible) {
            enableSplitPaneDivider();
        }
    }

    private void disableSplitPaneDivider() {
        for (Node node : mainViewSplit.lookupAll(".split-pane-divider")) {
            node.setVisible(false);
            mainViewSplitDividerVisible = false;
        }
        mainViewSplit.setDividerPosition(0, 1);
    }

    private void enableSplitPaneDivider() {
        for (Node node : mainViewSplit.lookupAll(".split-pane-divider")) {
            node.setVisible(true);
            mainViewSplitDividerVisible = true;
        }
        mainViewSplit.setDividerPosition(0, 0.7);
    }
}
