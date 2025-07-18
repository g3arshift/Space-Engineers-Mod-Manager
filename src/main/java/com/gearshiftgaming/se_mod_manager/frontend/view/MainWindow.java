package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.domain.tool.ToolManagerService;
import com.gearshiftgaming.se_mod_manager.backend.models.save.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.user.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.view.popup.Popup;
import com.gearshiftgaming.se_mod_manager.frontend.view.popup.TwoButtonChoice;
import com.gearshiftgaming.se_mod_manager.frontend.view.window.WindowDressingUtility;
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

    private final Properties properties;

    private final UiService uiService;

    private final Stage stage;

    private Scene scene;

    private final UserConfiguration userConfiguration;

    //This is the reference to the controller for the bar located in the top section of the main borderpane
    private final ModTableContextBar contextBarView;

    //This is the reference to the meat and potatoes of the UI, the actual controls located in the center of the UI responsible for managing modlists
    private final MasterManager masterManagerView;

    //This is the reference to the controller for the bar located in the bottom section of the main borderpane
    private final StatusBar statusBarView;

    //This is the service that handles the logic behind setting up the various tools we need for SEMM to function.
    private final ToolManagerService toolManagerService;

    //This is the reference for the UI portion of the tool manager.
    private final ToolManager toolManagerView;

    //Initializes our controller while maintaining the empty constructor JavaFX expects
    public MainWindow(Properties properties, Stage stage, ModTableContextBar modTableContextBar, MasterManager masterManager, StatusBar statusBar, ToolManager toolManager, UiService uiService) {
        this.stage = stage;
        this.properties = properties;
        this.userConfiguration = uiService.getUserConfiguration();
        this.uiService = uiService;
        this.contextBarView = modTableContextBar;
        this.masterManagerView = masterManager;
        this.statusBarView = statusBar;
        this.toolManagerView = toolManager;

        toolManagerService = new ToolManagerService(this.uiService,
                this.properties.getProperty("semm.steam.cmd.localFolderPath"),
                this.properties.getProperty("semm.steam.cmd.download.source"),
                Integer.parseInt(this.properties.getProperty("semm.steam.cmd.download.retry.limit")),
                Integer.parseInt(this.properties.getProperty("semm.steam.cmd.download.connection.timeout")),
                Integer.parseInt(this.properties.getProperty("semm.steam.cmd.download.read.timeout")),
                Integer.parseInt(this.properties.getProperty("semm.steam.cmd.download.retry.delay")));
    }

    public void initView(Parent mainViewRoot, Parent menuBarRoot, Parent modlistManagerRoot, Parent statusBarRoot, SaveProfileManager saveProfileManager, ModListProfileManager modListProfileManager) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        //Prepare the UI
        setupWindow(mainViewRoot);
        contextBarView.initView();
        masterManagerView.initView(contextBarView.getLogToggle(), contextBarView.getModDescriptionToggle(), Integer.parseInt(properties.getProperty("semm.modTable.cellSize")),
                contextBarView.getModProfileDropdown(), contextBarView.getSaveProfileDropdown(), contextBarView.getModTableSearchField());
        statusBarView.initView();
        mainWindowLayout.setTop(menuBarRoot);
        mainWindowLayout.setCenter(modlistManagerRoot);
        mainWindowLayout.setBottom(statusBarRoot);

        final ObservableList<SaveProfile> saveProfiles = uiService.getSaveProfiles();

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

                    TwoButtonChoice choice = Popup.displayYesNoDialog(errorMessage, stage, MessageType.WARN);
                    if (choice == TwoButtonChoice.YES) {
                        uiService.log("Removing save " + saveProfiles.get(i).getSaveName() + ".", MessageType.INFO);
                        Result<Void> deleteResult = uiService.deleteSaveProfile(saveProfiles.get(i));
                        if (deleteResult.isFailure()) {
                            uiService.log(deleteResult);
                            Popup.displaySimpleAlert(deleteResult);
                            break;
                        }
                        saveProfiles.remove(i);
                        i--;
                    }
                } else {
                    saveProfiles.get(i).setSaveExists(true);
                }
            }
        }

        mainWindowLayout.setOnDragOver(masterManagerView::handleModTableDragOver);

        //TODO: REMOVE FOR FULL RELEASE
        //TODO: BE REALLY SURE TO REMOVE FOR FULL RELEASE
        //TODO: YOU BETTER REMOVE THIS FOR FULL RELEASE
        Popup.displayInfoMessageWithLink("This is a pre-release version of SEMM and you will likely encounter bugs. " +
                        "Make sure to backup your Space Engineers saves before use, and please report any bugs you find at the following link.",
                "https://bugreport.spaceengineersmodmanager.com", "ATTENTION!!!", MessageType.INFO);

        stage.setOnShown(event -> {
            //Add title and icon to the stage
            Properties versionProperties = new Properties();
            try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("version.properties")) {
                versionProperties.load(input);
            } catch (IOException | NullPointerException e) {
                uiService.log(e);
            }

            //Setup the window title and icon
            stage.setTitle("SEMM v" + versionProperties.getProperty("version"));
            WindowDressingUtility.appendStageIcon(stage);

            setupRequiredTools();

            if (uiService.getUserConfiguration().isRunFirstTimeSetup()) {
                TwoButtonChoice firstTimeSetupChoice = Popup.displayYesNoDialog("This seems to be your first time running SEMM. Do you want to take the tutorial?", stage, MessageType.INFO);
                if (firstTimeSetupChoice == TwoButtonChoice.YES) {
                    uiService.displayTutorial(stage, masterManagerView);
                } else if (firstTimeSetupChoice == TwoButtonChoice.NO) { //It seems like this branch doesn't matter, but it prevents the firstTimeSetup bool from being set if the application closes mid-tutorial.
                    uiService.getUserConfiguration().setRunFirstTimeSetup(false);
                    uiService.saveUserConfiguration();
                }
            }
        });
    }

    private void setupRequiredTools() {
        //TODO: Add some check for if the files already exist.
        // We want to skip the steps that exist. If they all exist, just skip this entirely.
        //Download all the required tools we need for SEMM to function
        uiService.log("Downloading required tools...", MessageType.INFO);

        //Download SteamCMD.
        //TODO: Genericize
        if(Files.exists(Path.of(properties.getProperty("semm.steam.cmd.localFolderPath")).getParent().resolve("steamcmd.exe"))) {
            uiService.log("SteamCMD already installed.", MessageType.INFO);
            return;
        }

        StackPane toolManagerWindow = toolManagerView.getToolDownloadProgressPanel();
        masterManagerView.getMainViewStack().getChildren().add(toolManagerWindow);

        //TODO: Wrap this in another task so we can just staple on additional tasks for additional tools.
        Task<Result<Void>> toolSetupTask = toolManagerService.setupSteamCmd();
        toolManagerView.setToolNameText("SteamCMD");

        toolSetupTask.setOnRunning(event1 -> {
            masterManagerView.disableUserInputElements(true);
            toolManagerView.bindProgressAndUpdateValues(toolSetupTask.messageProperty(), toolSetupTask.progressProperty());
        });
        //When the task is finished log our result, display the last message from it, and fade it out.
        toolSetupTask.setOnSucceeded(event1 -> {
            Result<Void> steamCmdSetupResult = toolSetupTask.getValue();
            uiService.log(steamCmdSetupResult);

            if (steamCmdSetupResult.isFailure()) {
                Popup.displayInfoMessageWithLink("Failed to download SteamCMD. SEMM requires SteamCMD to run. " +
                                "Please submit your log file at the following link.",
                        "https://bugreport.spaceengineersmodmanager.com", "ATTENTION!!!", stage, MessageType.ERROR);
                Platform.exit();
                return;
            }

            toolManagerView.setAllDownloadsCompleteState();
            FadeTransition fadeTransition = new FadeTransition(Duration.millis(1200), toolManagerWindow);
            fadeTransition.setFromValue(1d);
            fadeTransition.setToValue(0d);

            fadeTransition.setOnFinished(actionEvent -> {
                masterManagerView.disableUserInputElements(false);
                toolManagerView.setDefaultState();
                masterManagerView.getMainViewStack().getChildren().remove(toolManagerWindow);
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
        int minWidth = Integer.parseInt(properties.getProperty("semm.mainView.resolution.minWidth"));
        int minHeight = Integer.parseInt(properties.getProperty("semm.mainView.resolution.minHeight"));
        int prefWidth = Integer.parseInt(properties.getProperty("semm.mainView.resolution.prefWidth"));
        int prefHeight = Integer.parseInt(properties.getProperty("semm.mainView.resolution.prefHeight"));

        //Prepare the stage
        stage.setScene(scene);
        stage.setMinWidth(minWidth);
        stage.setMinHeight(minHeight);
        stage.setWidth(prefWidth);
        stage.setHeight(prefHeight);

        //Add a listener to make the slider on the split pane stay at the bottom of our window when resizing it when it shouldn't be visible
        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (!masterManagerView.isMainViewSplitDividerVisible()) {
                masterManagerView.getMainViewSplit().setDividerPosition(0, 1);
            }
        });
    }
}
