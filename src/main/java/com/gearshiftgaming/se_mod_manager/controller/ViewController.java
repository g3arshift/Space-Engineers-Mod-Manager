package com.gearshiftgaming.se_mod_manager.controller;

import atlantafx.base.theme.PrimerLight;
import com.gearshiftgaming.se_mod_manager.backend.data.modlist.ModlistFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.sandbox.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.save.SaveFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.user.UserDataSqliteRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.save.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.LogMessage;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.SpaceEngineersVersion;
import com.gearshiftgaming.se_mod_manager.backend.models.user.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.view.*;
import com.gearshiftgaming.se_mod_manager.frontend.view.input.GeneralFileInput;
import com.gearshiftgaming.se_mod_manager.frontend.view.input.SaveInput;
import com.gearshiftgaming.se_mod_manager.frontend.view.input.SimpleInput;
import com.gearshiftgaming.se_mod_manager.frontend.view.popup.Popup;
import com.gearshiftgaming.se_mod_manager.frontend.view.popup.TwoButtonChoice;
import com.gearshiftgaming.se_mod_manager.operatingsystem.OperatingSystemVersion;
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
    public ViewController(Stage stage, Logger logger) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, InterruptedException {
        logger.info("Started application");

        PROPERTIES = new Properties();
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("SEMM.properties")) {
            PROPERTIES.load(input);
        } catch (IOException | NullPointerException e) {
            logger.error("Could not load SEMM.properties. {}", e.getMessage());
            throw (e);
        }

        //TODO: Something is bugging me about how this is all setup... It feels brittle.
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
                    TwoButtonChoice choice = Popup.displayYesNoDialog("Failed to load user configuration, see log for details. " +
                            "Would you like to create a new user configuration and continue?", MessageType.WARN);
                    if (choice == TwoButtonChoice.YES) {
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

    private void setupInterface(Stage stage) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, InterruptedException {
        //Manually inject our controllers into our FXML so we can reuse the FXML for the profile creation elsewhere, and have greater flexibility in controller injection and FXML initialization.
        //This method also allows us to properly define constructors for the view objects which is otherwise not feasible with JavaFX.
        //The reason we have the initView function however is because @FXML tagged variables are only injected *after* the constructor runs, so we initialize any FXML dependent items in initView.
        //For the constructors for each view, they need to have a value for whatever views that will be the "child" of that view, ie, they are only accessible in the UI through that view. Think of it as a hierarchical structure.

        //View for adding a new Save Profile
        final FXMLLoader saveListInputLoader = new FXMLLoader(getClass().getResource("/view/sandbox-save-input.fxml"));
        final SaveInput saveInputView = new SaveInput(uiService);
        saveListInputLoader.setController(saveInputView);
        final Parent saveListInputRoot = saveListInputLoader.load();
        saveInputView.initView(saveListInputRoot);

        //View for text input when creating a new save profile.
        final FXMLLoader saveProfileManagerLoader = new FXMLLoader(getClass().getResource("/view/simple-input.fxml"));
        final SimpleInput saveProfileInputView = new SimpleInput();
        saveProfileManagerLoader.setController(saveProfileInputView);
        final Parent saveProfileManagerRoot = saveProfileManagerLoader.load();
        saveProfileInputView.initView(saveProfileManagerRoot);

        //View for text input when adding a new Mod Profile
        final FXMLLoader modProfileInputLoader = new FXMLLoader(getClass().getResource("/view/simple-input.fxml"));
        final SimpleInput modProfileInputView = new SimpleInput();
        modProfileInputLoader.setController(modProfileInputView);
        final Parent modProfileInputRoot = modProfileInputLoader.load();
        modProfileInputView.initView(modProfileInputRoot);

        //View for managing Save Profiles
        final FXMLLoader saveManagerLoader = new FXMLLoader(getClass().getResource("/view/save-profile-manager.fxml"));
        final SaveProfileManager saveManagerView = new SaveProfileManager(uiService, saveInputView, saveProfileInputView);
        saveManagerLoader.setController(saveManagerView);
        final Parent saveManagerRoot = saveManagerLoader.load();

        //View for managing Mod Profiles
        final FXMLLoader modListManagerLoader = new FXMLLoader(getClass().getResource("/view/mod-list-manager.fxml"));
        final ModListProfileManager modListManagerView = new ModListProfileManager(uiService, modProfileInputView);
        modListManagerLoader.setController(modListManagerView);
        final Parent modListManagerRoot = modListManagerLoader.load();

        //View for the statusbar section of the main window
        final FXMLLoader statusBarLoader = new FXMLLoader(getClass().getResource("/view/statusbar.fxml"));
        final StatusBar statusBarView = new StatusBar(uiService);
        statusBarLoader.setController(statusBarView);
        final Parent statusBarRoot = statusBarLoader.load();

        //View for text input when adding a new Mod either by ID or URL, but not for files.
        final FXMLLoader idAndUrlModImportInputLoader = new FXMLLoader(getClass().getResource("/view/simple-input.fxml"));
        final SimpleInput idAndUrlModImportInputView = new SimpleInput();
        idAndUrlModImportInputLoader.setController(idAndUrlModImportInputView);
        final Parent idAndUrlModImportInputRoot = idAndUrlModImportInputLoader.load();
        idAndUrlModImportInputView.initView(idAndUrlModImportInputRoot);

        //View for handling general file input, but primarily for ingesting modlist files.
        final FXMLLoader generalFileInputLoader = new FXMLLoader(getClass().getResource("/view/general-file-input.fxml"));
        final GeneralFileInput generalFileInputView = new GeneralFileInput();
        generalFileInputLoader.setController(generalFileInputView);
        final Parent generalFileLoaderRoot = generalFileInputLoader.load();
        generalFileInputView.initView(generalFileLoaderRoot);

        //View for managing the actual mod lists. This is the center section of the main window
        final FXMLLoader masterManagerLoader = new FXMLLoader(getClass().getResource("/view/master-manager.fxml"));
        final MasterManager masterManagerView = new MasterManager(uiService, stage, PROPERTIES, statusBarView, modListManagerView, saveManagerView, idAndUrlModImportInputView, saveInputView, generalFileInputView);
        masterManagerLoader.setController(masterManagerView);
        final Parent masterManagerRoot = masterManagerLoader.load();

        //View for the menubar section of the main window
        final FXMLLoader modTableContextBarLoader = new FXMLLoader(getClass().getResource("/view/mod-table-context-bar.fxml"));
        final ModTableContextBar modTableContextBarView = new ModTableContextBar(uiService, masterManagerView, statusBarView, stage);
        modTableContextBarLoader.setController(modTableContextBarView);
        final Parent menuBarRoot = modTableContextBarLoader.load();

        //The mod and save manager are fully initialized down here as we only have all the references we need at this stage
        modListManagerView.initView(modListManagerRoot, Double.parseDouble(PROPERTIES.getProperty("semm.profileView.resolution.minWidth")), Double.parseDouble(PROPERTIES.getProperty("semm.profileView.resolution.minHeight")), modTableContextBarView);
        saveManagerView.initView(saveManagerRoot, Double.parseDouble(PROPERTIES.getProperty("semm.profileView.resolution.minWidth")), Double.parseDouble(PROPERTIES.getProperty("semm.profileView.resolution.minHeight")), modTableContextBarView);

        //View for the tool manager that gets attached to the primary app window
        final FXMLLoader toolManagerLoader = new FXMLLoader(getClass().getResource("/view/tool-manager.fxml"));
        final ToolManager toolManagerView = new ToolManager();
        toolManagerLoader.setController(toolManagerView);
        toolManagerLoader.load();

        //View for the primary application window
        final FXMLLoader mainViewLoader = new FXMLLoader(getClass().getResource("/view/main-window.fxml"));
        final MainWindow mainWindowView = new MainWindow(PROPERTIES, stage,
                modTableContextBarView, masterManagerView, statusBarView, toolManagerView, uiService);
        mainViewLoader.setController(mainWindowView);
        final Parent mainViewRoot = mainViewLoader.load();
        mainWindowView.initView(mainViewRoot, menuBarRoot, masterManagerRoot, statusBarRoot, saveManagerView, modListManagerView);
    }
}