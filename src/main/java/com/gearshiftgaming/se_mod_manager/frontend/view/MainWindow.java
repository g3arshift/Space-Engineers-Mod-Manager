package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.domain.ToolManagerService;
import com.gearshiftgaming.se_mod_manager.backend.models.*;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.Popup;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.WindowDressingUtility;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.Future;

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
    private final MasterManager MASTER_MANAGER_VIEW;

    //This is the reference to the controller for the bar located in the bottom section of the main borderpane
    private final StatusBar STATUS_BAR_VIEW;

    //This is the service that handles the logic behind setting up the various tools we need for SEMM to function.
    private final ToolManagerService TOOL_MANAGER_SERVICE;

    //This is the reference for the UI portion of the tool manager.
    private final ToolManager TOOL_MANAGER_VIEW;

    //Initializes our controller while maintaining the empty constructor JavaFX expects
    public MainWindow(Properties properties, Stage stage, ModTableContextBar modTableContextBar, MasterManager masterManager, StatusBar statusBar, ToolManager toolManager, UiService uiService) {
        this.STAGE = stage;
        this.PROPERTIES = properties;
        this.USER_CONFIGURATION = uiService.getUSER_CONFIGURATION();
        this.UI_SERVICE = uiService;
        this.CONTEXT_BAR_VIEW = modTableContextBar;
        this.MASTER_MANAGER_VIEW = masterManager;
        this.STATUS_BAR_VIEW = statusBar;
        this.TOOL_MANAGER_VIEW = toolManager;

        TOOL_MANAGER_SERVICE = new ToolManagerService(UI_SERVICE,
                PROPERTIES.getProperty("semm.steam.cmd.localFolderPath"),
                PROPERTIES.getProperty("semm.steam.cmd.download.source"),
                Integer.parseInt(PROPERTIES.getProperty("semm.steam.cmd.download.retry.limit")),
                Integer.parseInt(PROPERTIES.getProperty("semm.steam.cmd.download.connection.timeout")),
                Integer.parseInt(PROPERTIES.getProperty("semm.steam.cmd.download.read.timeout")),
                Integer.parseInt(PROPERTIES.getProperty("semm.steam.cmd.download.retry.delay")));
    }

    public void initView(Parent mainViewRoot, Parent menuBarRoot, Parent modlistManagerRoot, Parent statusBarRoot, SaveProfileManager saveProfileManager, ModListProfileManager modListProfileManager) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        //Prepare the UI
        setupWindow(mainViewRoot);
        CONTEXT_BAR_VIEW.initView();
        MASTER_MANAGER_VIEW.initView(CONTEXT_BAR_VIEW.getLogToggle(), CONTEXT_BAR_VIEW.getModDescriptionToggle(), Integer.parseInt(PROPERTIES.getProperty("semm.modTable.cellSize")),
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
                        Result<Void> deleteResult = UI_SERVICE.deleteSaveProfile(SAVE_PROFILES.get(i));
                        if (deleteResult.isFailure()) {
                            UI_SERVICE.log(deleteResult);
                            Popup.displaySimpleAlert(deleteResult);
                            break;
                        }
                        SAVE_PROFILES.remove(i);
                        i--;
                    }
                } else {
                    SAVE_PROFILES.get(i).setSaveExists(true);
                }
            }
        }

        mainWindowLayout.setOnDragOver(MASTER_MANAGER_VIEW::handleModTableDragOver);

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

            //Setup the window title and icon
            STAGE.setTitle("SEMM v" + versionProperties.getProperty("version"));
            WindowDressingUtility.appendStageIcon(STAGE);

            setupRequiredTools();

            if (UI_SERVICE.getUSER_CONFIGURATION().isRunFirstTimeSetup()) {
                int firstTimeSetupChoice = Popup.displayYesNoDialog("This seems to be your first time running SEMM. Do you want to take the tutorial?", STAGE, MessageType.INFO);
                if (firstTimeSetupChoice == 1) {
                    UI_SERVICE.displayTutorial(STAGE, MASTER_MANAGER_VIEW);
                } else if (firstTimeSetupChoice == 0) { //It seems like this branch doesn't matter, but it prevents the firstTimeSetup bool from being set if the application closes mid-tutorial.
                    UI_SERVICE.getUSER_CONFIGURATION().setRunFirstTimeSetup(false);
                    UI_SERVICE.saveUserConfiguration();
                }
            }
        });
    }

    private void setupRequiredTools() {
        //TODO: Add some check for if the files already exist.
        // We want to skip the steps that exist. If they all exist, just skip this entirely.
        //Download all the required tools we need for SEMM to function
        UI_SERVICE.log("Downloading required tools...", MessageType.INFO);
        StackPane toolManagerWindow = TOOL_MANAGER_VIEW.getToolDownloadProgressPanel();
        MASTER_MANAGER_VIEW.getMainViewStack().getChildren().add(toolManagerWindow);

        //Download SteamCMD.
        //TODO: Wrap this in another task so we can just staple on additional tasks for additional tools.
        Task<Result<Void>> toolSetupTask = TOOL_MANAGER_SERVICE.setupSteamCmd();
        TOOL_MANAGER_VIEW.setToolNameText("SteamCMD");

        toolSetupTask.setOnRunning(event1 -> {
            MASTER_MANAGER_VIEW.disableUserInputElements(true);
            TOOL_MANAGER_VIEW.bindProgressAndUpdateValues(toolSetupTask.messageProperty(), toolSetupTask.progressProperty());
        });
        //When the task is finished log our result, display the last message from it, and fade it out.
        toolSetupTask.setOnSucceeded(event1 -> {
            Result<Void> steamCmdSetupResult = toolSetupTask.getValue();
            UI_SERVICE.log(steamCmdSetupResult);

            if (steamCmdSetupResult.isFailure()) {
                Popup.displayInfoMessageWithLink("Failed to download SteamCMD. SEMM requires SteamCMD to run. " +
                                "Please submit your log file at the following link.",
                        "https://bugreport.spaceengineersmodmanager.com", "ATTENTION!!!", STAGE, MessageType.ERROR);
                Platform.exit();
                return;
            }

            TOOL_MANAGER_VIEW.setAllDownloadsCompleteState();
            FadeTransition fadeTransition = new FadeTransition(Duration.millis(1200), toolManagerWindow);
            fadeTransition.setFromValue(1d);
            fadeTransition.setToValue(0d);

            fadeTransition.setOnFinished(actionEvent -> {
                MASTER_MANAGER_VIEW.disableUserInputElements(false);
                TOOL_MANAGER_VIEW.setDefaultState();
                MASTER_MANAGER_VIEW.getMainViewStack().getChildren().remove(toolManagerWindow);
            });

            PauseTransition pauseTransition = new PauseTransition(Duration.millis(450));
            pauseTransition.setOnFinished(event -> fadeTransition.play());

            pauseTransition.play();
        });

        //Start the download on a background thread
        Thread.ofVirtual().start(toolSetupTask);
    }

    /**
     * Sets the basic properties of the window for the application, including the title bar, minimum resolutions, and listeners.
     */
    private void setupWindow(Parent root) {
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
            if (!MASTER_MANAGER_VIEW.isMainViewSplitDividerVisible()) {
                MASTER_MANAGER_VIEW.getMainViewSplit().setDividerPosition(0, 1);
            }
        });
    }
}
