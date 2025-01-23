package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.Popup;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.WindowDressingUtility;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

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
public class MainWindow {

    //TODO: Remove all menu options under file, except for "Close"

    //FXML Items
    @FXML
    private AnchorPane mainWindowRoot;

    @FXML
    private BorderPane mainWindowLayout;

    //TODO: We need to replace the window control bar for the window.

    private final Properties PROPERTIES;

    private final UiService UI_SERVICE;

    private final Stage STAGE;

    private Scene scene;

    private final UserConfiguration USER_CONFIGURATION;

    //This is the reference to the controller for the bar located in the top section of the main borderpane
    private final ModTableContextBar CONTEXT_BAR_VIEW;

    //This is the reference to the meat and potatoes of the UI, the actual controls located in the center of the UI responsible for managing modlists
    private final MasterManager MODLIST_MANAGER_VIEW;

    //This is the reference to the controller for the bar located in the bottom section of the main borderpane
    private final StatusBar STATUS_BAR_VIEW;

    //Initializes our controller while maintaining the empty constructor JavaFX expects
    public MainWindow(Properties properties, Stage stage, ModTableContextBar modTableContextBar, MasterManager masterManager, StatusBar statusBar, UiService uiService) {
        this.STAGE = stage;
        this.PROPERTIES = properties;
        this.USER_CONFIGURATION = uiService.getUSER_CONFIGURATION();
        this.UI_SERVICE = uiService;
        this.CONTEXT_BAR_VIEW = modTableContextBar;
        this.MODLIST_MANAGER_VIEW = masterManager;
        this.STATUS_BAR_VIEW = statusBar;
    }

    public void initView(Parent mainViewRoot, Parent menuBarRoot, Parent modlistManagerRoot, Parent statusBarRoot, SaveProfileManager saveProfileManager, ModListManager modListManager) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        //Prepare the UI
        setupWindow(mainViewRoot);
        CONTEXT_BAR_VIEW.initView();
        MODLIST_MANAGER_VIEW.initView(CONTEXT_BAR_VIEW.getLogToggle(), CONTEXT_BAR_VIEW.getModDescriptionToggle(), Integer.parseInt(PROPERTIES.getProperty("semm.modTable.cellSize")),
                CONTEXT_BAR_VIEW.getModProfileDropdown(), CONTEXT_BAR_VIEW.getSaveProfileDropdown(), CONTEXT_BAR_VIEW.getModTableSearchField());
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

                    int choice = Popup.displayYesNoDialog(errorMessage, STAGE, MessageType.WARN);
                    if (choice == 1) {
                        UI_SERVICE.log("Removing save " + SAVE_PROFILES.get(i).getSaveName() + ".", MessageType.INFO);
                        SAVE_PROFILES.remove(i);
                        i--;
                    }
                } else {
                    SAVE_PROFILES.get(i).setSaveExists(true);
                }
            }

            UI_SERVICE.saveUserData();
        }

        mainWindowLayout.setOnDragOver(MODLIST_MANAGER_VIEW::handleModTableDragOver);

        //TODO: REMOVE FOR FULL RELEASE
        //TODO: BE REALLY SURE TO REMOVE FOR FULL RELEASE
        //TODO: YOU BETTER REMOVE THIS FOR FULL RELEASE
        Popup.displayInfoMessageWithLink("This is a pre-release version of SEMM and you will likely encounter bugs. " +
                        "Make sure to backup your Space Engineers saves before use, and please report any bugs you find at the following link.",
                "https://bugreport.spaceengineersmodmanager.com", "ATTENTION!!!", MessageType.INFO);

        STAGE.setOnShown(event -> {
            //Add title and icon to the stage
            Properties versionProperties = new Properties();
            try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("version.properties")) {
                versionProperties.load(input);
            } catch (IOException | NullPointerException e) {
                UI_SERVICE.log(e);
            }

            STAGE.setTitle("SEMM v" + versionProperties.getProperty("version"));

            WindowDressingUtility.appendStageIcon(STAGE);
            if (UI_SERVICE.getUSER_CONFIGURATION().shouldRunFirstTimeSetup()) {
                int firstTimeSetupChoice = Popup.displayYesNoDialog("This seems to be your first time running SEMM. Do you want to take the tutorial?", STAGE, MessageType.INFO);
                if (firstTimeSetupChoice == 1) {
                    UI_SERVICE.displayTutorial(STAGE, MODLIST_MANAGER_VIEW, saveProfileManager, modListManager);
                } else if(firstTimeSetupChoice == 0){ //It seems like this branch doesn't matter, but it prevents the firstTimeSetup bool from being set if the application closes mid-tutorial.
                    UI_SERVICE.getUSER_CONFIGURATION().shouldRunFirstTimeSetup(false);
                    UI_SERVICE.saveUserData();
                }
            }
        });
    }

    /**
     * Sets the basic properties of the window for the application, including the title bar, minimum resolutions, and listeners.
     */
    private void setupWindow(Parent root) throws IOException {
        this.scene = new Scene(root);
        //Prepare the scene
        int minWidth = Integer.parseInt(PROPERTIES.getProperty("semm.mainView.resolution.minWidth"));
        int minHeight = Integer.parseInt(PROPERTIES.getProperty("semm.mainView.resolution.minHeight"));
        int prefWidth = Integer.parseInt(PROPERTIES.getProperty("semm.mainView.resolution.prefWidth"));
        int prefHeight = Integer.parseInt(PROPERTIES.getProperty("semm.mainView.resolution.prefHeight"));

        //Prepare the stage
        STAGE.setScene(scene);
        STAGE.setMinWidth(minWidth);
        STAGE.setMinHeight(minHeight);
        STAGE.setWidth(prefWidth);
        STAGE.setHeight(prefHeight);

        //Add a listener to make the slider on the split pane stay at the bottom of our window when resizing it when it shouldn't be visible
        STAGE.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (!MODLIST_MANAGER_VIEW.isMainViewSplitDividerVisible()) {
                MODLIST_MANAGER_VIEW.getMainViewSplit().setDividerPosition(0, 1);
            }
        });
    }
}
