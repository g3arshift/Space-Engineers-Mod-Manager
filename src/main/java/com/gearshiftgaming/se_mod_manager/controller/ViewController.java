package com.gearshiftgaming.se_mod_manager.controller;

import atlantafx.base.theme.PrimerLight;
import com.gearshiftgaming.se_mod_manager.backend.data.ModlistFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SaveFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.UserDataSqliteRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.*;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.view.*;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.Popup;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;


/**
 * Sets up the basic environment for the application.
 * Each FXML file is loaded individually here as it enables control over when specific files are launched.
 * This helps to solve issues such as loading certain FXML files before the application theme has been set, as well as more easily injecting references to each sub-window into the parent controllers.
 * <p>
 * Used to prepare basic environment setup for the application.
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ViewController {
    private final Properties PROPERTIES;

    private UiService uiService;

    //TODO: Check for file locks to prevent two copies of the app from running simultaneously
    public ViewController(Stage stage, Logger logger) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        logger.info("Started application");

        PROPERTIES = new Properties();
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("SEMM.properties")) {
            PROPERTIES.load(input);
        } catch (IOException | NullPointerException e) {
            logger.error("Could not load SEMM.properties. {}", e.getMessage());
            throw (e);
        }

        StorageController storageController = new StorageController(new SandboxConfigFileRepository(),
                new UserDataSqliteRepository(PROPERTIES.getProperty("semm.userData.default.path") + ".db"),
                new SaveFileRepository());

        Result<UserConfiguration> userConfigurationResult = storageController.loadStartupData();
        UserConfiguration userConfiguration = new UserConfiguration();

        if (userConfigurationResult.isSuccess()) {
            userConfiguration = userConfigurationResult.getPayload();
        } else {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
            for (String message : userConfigurationResult.getMessages()) {
                logger.error(message);
            }
            try (Stream<Path> stream = Files.list(Path.of("./logs"))) {
                if (stream.anyMatch(Files::isDirectory)) { //This is a hack, but it's the only way to check for a true first time setup versus a deleted config.
                    int choice = Popup.displayYesNoDialog("Failed to load user configuration, see log for details. " +
                            "Would you like to create a new user configuration and continue?", MessageType.WARN);
                    if (choice == 1) {
                        Result<Void> dataInitializaitonResult = storageController.initializeData();
                        if (dataInitializaitonResult.isFailure()) {
                            uiService.log(dataInitializaitonResult);
                            Popup.displaySimpleAlert(dataInitializaitonResult);
                            throw new RuntimeException(dataInitializaitonResult.getCurrentMessage());
                        }
                    } else {
                        Platform.exit();
                        return;
                    }
                    //TODO: What is this really for? I THINK it's for first time startup, but I think the earlier phases capture that... Check it later.
                } else {
                    Result<Void> dataInitializaitonResult = storageController.initializeData();
                    if (dataInitializaitonResult.isFailure()) {
                        uiService.log(dataInitializaitonResult);
                        Popup.displaySimpleAlert(dataInitializaitonResult);
                        throw new RuntimeException(dataInitializaitonResult.getCurrentMessage());
                    }
                }
            }
        }

        ObservableList<MutableTriple<UUID, String, SpaceEngineersVersion>> modListProfileDetails = FXCollections.observableList(userConfiguration.getModListProfilesBasicInfo());
        ObservableList<SaveProfile> saveProfiles = FXCollections.observableList(userConfiguration.getSaveProfiles());

        //Initialize the list we use to store log messages shown to the user
        ObservableList<LogMessage> userLog = FXCollections.observableArrayList(logMessage ->
                new Observable[]{
                        logMessage.VIEWABLE_LOG_MESSAGEProperty(),
                        logMessage.MESSAGE_TYPEProperty()
                });

        ModInfoController modInfoController = new ModInfoController(new ModlistFileRepository(), PROPERTIES);

        uiService = new UiService(logger, userLog, Integer.parseInt(PROPERTIES.getProperty("semm.ui.maxUserLogSize")), modListProfileDetails, saveProfiles, storageController, modInfoController, userConfiguration, PROPERTIES);
        uiService.log(userConfigurationResult);

        setupInterface(stage);
    }

    private void setupInterface(Stage stage) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        //Manually inject our controllers into our FXML so we can reuse the FXML for the profile creation elsewhere, and have greater flexibility in controller injection and FXML initialization.
        //This method also allows us to properly define constructors for the view objects which is otherwise not feasible with JavaFX.
        //The reason we have the initView function however is because @FXML tagged variables are only injected *after* the constructor runs, so we initialize any FXML dependent items in initView.
        //For the constructors for each view, they need to have a value for whatever views that will be the "child" of that view, ie, they are only accessible in the UI through that view. Think of it as a hierarchical structure.

        //View for adding a new Save Profile
        final FXMLLoader SAVE_LIST_INPUT_LOADER = new FXMLLoader(getClass().getResource("/view/sandbox-save-input.fxml"));
        final SaveInput SAVE_INPUT_VIEW = new SaveInput(uiService);
        SAVE_LIST_INPUT_LOADER.setController(SAVE_INPUT_VIEW);
        final Parent SAVE_LIST_INPUT_ROOT = SAVE_LIST_INPUT_LOADER.load();
        SAVE_INPUT_VIEW.initView(SAVE_LIST_INPUT_ROOT);

        //View for text input when creating a new save profile.
        final FXMLLoader SAVE_PROFILE_MANAGER_LOADER = new FXMLLoader(getClass().getResource("/view/simple-input.fxml"));
        final SimpleInput SAVE_PROFILE_INPUT_VIEW = new SimpleInput();
        SAVE_PROFILE_MANAGER_LOADER.setController(SAVE_PROFILE_INPUT_VIEW);
        final Parent SAVE_PROFILE_MANAGER_ROOT = SAVE_PROFILE_MANAGER_LOADER.load();
        SAVE_PROFILE_INPUT_VIEW.initView(SAVE_PROFILE_MANAGER_ROOT);

        //View for text input when adding a new Mod Profile
        final FXMLLoader MOD_PROFILE_INPUT_LOADER = new FXMLLoader(getClass().getResource("/view/simple-input.fxml"));
        final SimpleInput MOD_PROFILE_INPUT_VIEW = new SimpleInput();
        MOD_PROFILE_INPUT_LOADER.setController(MOD_PROFILE_INPUT_VIEW);
        final Parent MOD_PROFILE_INPUT_ROOT = MOD_PROFILE_INPUT_LOADER.load();
        MOD_PROFILE_INPUT_VIEW.initView(MOD_PROFILE_INPUT_ROOT);

        //View for managing Save Profiles
        final FXMLLoader SAVE_MANAGER_LOADER = new FXMLLoader(getClass().getResource("/view/save-profile-manager.fxml"));
        final SaveProfileManager SAVE_MANAGER_VIEW = new SaveProfileManager(uiService, SAVE_INPUT_VIEW, SAVE_PROFILE_INPUT_VIEW);
        SAVE_MANAGER_LOADER.setController(SAVE_MANAGER_VIEW);
        final Parent SAVE_MANAGER_ROOT = SAVE_MANAGER_LOADER.load();

        //View for managing Mod Profiles
        final FXMLLoader MOD_LIST_MANAGER_LOADER = new FXMLLoader(getClass().getResource("/view/mod-list-manager.fxml"));
        final ModListProfileManager MOD_LIST_MANAGER_VIEW = new ModListProfileManager(uiService, MOD_PROFILE_INPUT_VIEW);
        MOD_LIST_MANAGER_LOADER.setController(MOD_LIST_MANAGER_VIEW);
        final Parent MOD_LIST_MANAGER_ROOT = MOD_LIST_MANAGER_LOADER.load();

        //View for the statusbar section of the main window
        final FXMLLoader STATUS_BAR_LOADER = new FXMLLoader(getClass().getResource("/view/statusbar.fxml"));
        final StatusBar STATUS_BAR_VIEW = new StatusBar(uiService);
        STATUS_BAR_LOADER.setController(STATUS_BAR_VIEW);
        final Parent STATUS_BAR_ROOT = STATUS_BAR_LOADER.load();

        //View for text input when adding a new Mod either by ID or URL, but not for files.
        final FXMLLoader ID_AND_URL_MOD_IMPORT_INPUT_LOADER = new FXMLLoader(getClass().getResource("/view/simple-input.fxml"));
        final SimpleInput ID_AND_URL_MOD_IMPORT_INPUT_VIEW = new SimpleInput();
        ID_AND_URL_MOD_IMPORT_INPUT_LOADER.setController(ID_AND_URL_MOD_IMPORT_INPUT_VIEW);
        final Parent ID_AND_URL_MOD_IMPORT_INPUT_ROOT = ID_AND_URL_MOD_IMPORT_INPUT_LOADER.load();
        ID_AND_URL_MOD_IMPORT_INPUT_VIEW.initView(ID_AND_URL_MOD_IMPORT_INPUT_ROOT);

        //View for handling general file input, but primarily for ingesting modlist files.
        final FXMLLoader GENERAL_FILE_INPUT_LOADER = new FXMLLoader(getClass().getResource("/view/general-file-input.fxml"));
        final GeneralFileInput GENERAL_FILE_INPUT_VIEW = new GeneralFileInput();
        GENERAL_FILE_INPUT_LOADER.setController(GENERAL_FILE_INPUT_VIEW);
        final Parent GENERAL_FILE_LOADER_ROOT = GENERAL_FILE_INPUT_LOADER.load();
        GENERAL_FILE_INPUT_VIEW.initView(GENERAL_FILE_LOADER_ROOT);

        //View for managing the actual mod lists. This is the center section of the main window
        final FXMLLoader MASTER_MANAGER_LOADER = new FXMLLoader(getClass().getResource("/view/master-manager.fxml"));
        final MasterManager MASTER_MANAGER_VIEW = new MasterManager(uiService, stage, PROPERTIES, STATUS_BAR_VIEW, MOD_LIST_MANAGER_VIEW, SAVE_MANAGER_VIEW, ID_AND_URL_MOD_IMPORT_INPUT_VIEW, SAVE_INPUT_VIEW, GENERAL_FILE_INPUT_VIEW);
        MASTER_MANAGER_LOADER.setController(MASTER_MANAGER_VIEW);
        final Parent MASTER_MANAGER_ROOT = MASTER_MANAGER_LOADER.load();

        //View for the menubar section of the main window
        final FXMLLoader MOD_TABLE_CONTEXT_BAR_LOADER = new FXMLLoader(getClass().getResource("/view/mod-table-context-bar.fxml"));
        final ModTableContextBar MOD_TABLE_CONTEXT_BAR_VIEW = new ModTableContextBar(uiService, MASTER_MANAGER_VIEW, STATUS_BAR_VIEW, stage);
        MOD_TABLE_CONTEXT_BAR_LOADER.setController(MOD_TABLE_CONTEXT_BAR_VIEW);
        final Parent MENU_BAR_ROOT = MOD_TABLE_CONTEXT_BAR_LOADER.load();

        //The mod and save manager are fully initialized down here as we only have all the references we need at this stage
        MOD_LIST_MANAGER_VIEW.initView(MOD_LIST_MANAGER_ROOT, Double.parseDouble(PROPERTIES.getProperty("semm.profileView.resolution.minWidth")), Double.parseDouble(PROPERTIES.getProperty("semm.profileView.resolution.minHeight")), MOD_TABLE_CONTEXT_BAR_VIEW);
        SAVE_MANAGER_VIEW.initView(SAVE_MANAGER_ROOT, Double.parseDouble(PROPERTIES.getProperty("semm.profileView.resolution.minWidth")), Double.parseDouble(PROPERTIES.getProperty("semm.profileView.resolution.minHeight")), MOD_TABLE_CONTEXT_BAR_VIEW);

        //View for the primary application window
        final FXMLLoader MAIN_VIEW_LOADER = new FXMLLoader(getClass().getResource("/view/main-window.fxml"));
        final MainWindow MAIN_WINDOW_VIEW = new MainWindow(PROPERTIES, stage,
                MOD_TABLE_CONTEXT_BAR_VIEW, MASTER_MANAGER_VIEW, STATUS_BAR_VIEW, uiService);
        MAIN_VIEW_LOADER.setController(MAIN_WINDOW_VIEW);
        final Parent MAIN_VIEW_ROOT = MAIN_VIEW_LOADER.load();
        MAIN_WINDOW_VIEW.initView(MAIN_VIEW_ROOT, MENU_BAR_ROOT, MASTER_MANAGER_ROOT, STATUS_BAR_ROOT, SAVE_MANAGER_VIEW, MOD_LIST_MANAGER_VIEW);
    }
}