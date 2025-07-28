package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.AppContext;
import com.gearshiftgaming.se_mod_manager.backend.domain.archive.TarballArchiveTool;
import com.gearshiftgaming.se_mod_manager.backend.domain.archive.ZipArchiveTool;
import com.gearshiftgaming.se_mod_manager.backend.domain.tool.ToolManagerService;
import com.gearshiftgaming.se_mod_manager.backend.models.save.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.user.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.view.popup.Popup;
import com.gearshiftgaming.se_mod_manager.frontend.view.popup.TwoButtonChoice;
import com.gearshiftgaming.se_mod_manager.frontend.view.window.WindowDressingUtility;
import com.gearshiftgaming.se_mod_manager.operatingsystem.OperatingSystemVersionUtility;
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

    private final AppContext appContext;

    //Initializes our controller while maintaining the empty constructor JavaFX expects
    public MainWindow(Properties properties, Stage stage, ModTableContextBar modTableContextBar, MasterManager masterManager, StatusBar statusBar, UiService uiService) throws IOException, InterruptedException {
        this.stage = stage;
        this.properties = properties;
        this.userConfiguration = uiService.getUserConfiguration();
        this.uiService = uiService;
        this.contextBarView = modTableContextBar;
        this.masterManagerView = masterManager;
        this.statusBarView = statusBar;

        appContext = new AppContext(OperatingSystemVersionUtility.getOperatingSystemVersion());
    }

    public void initView(Parent mainViewRoot, Parent menuBarRoot, Parent modlistManagerRoot, Parent statusBarRoot) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException, InterruptedException {
        //Prepare the UI
        setupWindow(mainViewRoot);
        contextBarView.initView();
        masterManagerView.initView(contextBarView.getLogToggle(),
                contextBarView.getModDescriptionToggle(),
                contextBarView.getConflictsToggle(),
                Integer.parseInt(properties.getProperty("semm.modTable.cellSize")),
                contextBarView.getModProfileDropdown(),
                contextBarView.getSaveProfileDropdown(),
                contextBarView.getModTableSearchField(),
                properties);
        statusBarView.initView();
        mainWindowLayout.setTop(menuBarRoot);
        mainWindowLayout.setCenter(modlistManagerRoot);
        mainWindowLayout.setBottom(statusBarRoot);

        ObservableList<SaveProfile> saveProfiles = uiService.getSaveProfiles();

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

            if (uiService.getUserConfiguration().isRunFirstTimeSetup()) {
                TwoButtonChoice firstTimeSetupChoice = Popup.displayYesNoDialog("This seems to be your first time running SEMM. Do you want to take the tutorial?", stage, MessageType.INFO);
                if (firstTimeSetupChoice == TwoButtonChoice.YES) {
                    uiService.displayTutorial(stage, masterManagerView);
                } else if (firstTimeSetupChoice == TwoButtonChoice.NO) { //It seems like this branch doesn't matter, but it prevents the firstTimeSetup bool from being set if the application closes mid-tutorial.
                    uiService.getUserConfiguration().setRunFirstTimeSetup(false);
                    uiService.saveUserConfiguration();
                }
            }

            //TODO: Start the download of any mods in our current mod list that are set to "in progress".
        });
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
