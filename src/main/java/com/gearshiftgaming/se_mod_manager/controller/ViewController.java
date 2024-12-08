package com.gearshiftgaming.se_mod_manager.controller;

import com.gearshiftgaming.se_mod_manager.backend.data.ModlistFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SaveFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.UserDataFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.ModProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.backend.models.LogMessage;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.view.*;
import jakarta.xml.bind.JAXBException;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;


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
	private final String DESKTOP_PATH = System.getProperty("user.home") + "/Desktop";

	private final Properties PROPERTIES;

	private final UiService UI_SERVICE;

	//TODO: Check for file locks to prevent two copies of the app from running simultaneously
	public ViewController(Stage stage, Logger logger) throws IOException, JAXBException, XmlPullParserException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
		logger.info("Started application");

		PROPERTIES = new Properties();
		try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("SEMM.properties")) {
			PROPERTIES.load(input);
		} catch (IOException | NullPointerException e) {
			logger.error("Could not load SEMM.properties. " + e.getMessage());
			throw (e);
		}

		BackendStorageController backendStorageController = new BackendFileStorageController(new SandboxConfigFileRepository(),
				new UserDataFileRepository(),
				new SaveFileRepository(),
				PROPERTIES,
				new File(PROPERTIES.getProperty("semm.userData.default.location")));


		Result<UserConfiguration> userConfigurationResult = backendStorageController.getUserData();
		UserConfiguration userConfiguration;

		if (userConfigurationResult.isSuccess()) {
			userConfiguration = userConfigurationResult.getPayload();
		} else {
			userConfiguration = new UserConfiguration();
			backendStorageController.saveUserData(userConfiguration);
		}

		ObservableList<ModProfile> modProfiles = FXCollections.observableList(userConfiguration.getModProfiles());
		ObservableList<SaveProfile> saveProfiles = FXCollections.observableList(userConfiguration.getSaveProfiles());

		//Initialize the list we use to store log messages shown to the user
		ObservableList<LogMessage> userLog = FXCollections.observableArrayList(logMessage ->
				new Observable[]{
						logMessage.VIEWABLE_LOG_MESSAGEProperty(),
						logMessage.MESSAGE_TYPEProperty()
				});

		ModInfoController modInfoController = new ModInfoController(new ModlistFileRepository(), PROPERTIES);

		UI_SERVICE = new UiService(logger, userLog, modProfiles, saveProfiles, backendStorageController, modInfoController, userConfiguration, PROPERTIES);
		UI_SERVICE.log(userConfigurationResult);

		setupInterface(stage);

		//TODO: Actually implement this. Function is empty at the moment.
		//TODO: When we launch the app for the first time, walk the user through first making a save profile, then renaming the default mod profile, then IMMEDIATELY save to file.
		if (!userConfigurationResult.isSuccess()) {
			UI_SERVICE.firstTimeSetup();
		}
	}

	private void setupInterface(Stage stage) throws IOException, XmlPullParserException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
		//Manually inject our controllers into our FXML so we can reuse the FXML for the profile creation elsewhere, and have greater flexibility in controller injection and FXML initialization.
		//This method also allows us to properly define constructors for the view objects which is otherwise not feasible with JavaFX.
		//The reason we have the initView function however is because @FXML tagged variables are only injected *after* the constructor runs, so we initialize any FXML dependent items in initView.
		//For the constructors for each view, they need to have a value for whatever views that will be the "child" of that view, ie, they are only accessible in the UI through that view. Think of it as a hierarchical structure.

		//View for adding a new Save Profile
		final FXMLLoader SAVE_LIST_INPUT_LOADER = new FXMLLoader(getClass().getResource("/view/save-list-input.fxml"));
		final SaveInputView SAVE_INPUT_VIEW = new SaveInputView(UI_SERVICE);
		SAVE_LIST_INPUT_LOADER.setController(SAVE_INPUT_VIEW);
		final Parent SAVE_LIST_INPUT_ROOT = SAVE_LIST_INPUT_LOADER.load();
		SAVE_INPUT_VIEW.initView(SAVE_LIST_INPUT_ROOT);

		//View for text input when creating a new save profile.
		final FXMLLoader SAVE_PROFILE_MANAGER_LOADER = new FXMLLoader(getClass().getResource("/view/simple-input.fxml"));
		final SimpleInputView SAVE_PROFILE_INPUT_VIEW = new SimpleInputView();
		SAVE_PROFILE_MANAGER_LOADER.setController(SAVE_PROFILE_INPUT_VIEW);
		final Parent SAVE_PROFILE_MANAGER_ROOT = SAVE_PROFILE_MANAGER_LOADER.load();
		SAVE_PROFILE_INPUT_VIEW.initView(SAVE_PROFILE_MANAGER_ROOT);

		//View for text input when adding a new Mod Profile
		final FXMLLoader MOD_PROFILE_INPUT_LOADER = new FXMLLoader(getClass().getResource("/view/simple-input.fxml"));
		final SimpleInputView MOD_PROFILE_INPUT_VIEW = new SimpleInputView();
		MOD_PROFILE_INPUT_LOADER.setController(MOD_PROFILE_INPUT_VIEW);
		final Parent MOD_PROFILE_INPUT_ROOT = MOD_PROFILE_INPUT_LOADER.load();
		MOD_PROFILE_INPUT_VIEW.initView(MOD_PROFILE_INPUT_ROOT);

		//View for managing Save Profiles
		final FXMLLoader SAVE_MANAGER_LOADER = new FXMLLoader(getClass().getResource("/view/save-profile-manager.fxml"));
		final SaveManagerView SAVE_MANAGER_VIEW = new SaveManagerView(UI_SERVICE, SAVE_INPUT_VIEW, SAVE_PROFILE_INPUT_VIEW);
		SAVE_MANAGER_LOADER.setController(SAVE_MANAGER_VIEW);
		final Parent SAVE_MANAGER_ROOT = SAVE_MANAGER_LOADER.load();

		//View for managing Mod Profiles
		final FXMLLoader MOD_PROFILE_MANAGER_LOADER = new FXMLLoader(getClass().getResource("/view/mod-profile-manager.fxml"));
		final ModProfileManagerView MOD_PROFILE_MANAGER_VIEW = new ModProfileManagerView(UI_SERVICE, MOD_PROFILE_INPUT_VIEW);
		MOD_PROFILE_MANAGER_LOADER.setController(MOD_PROFILE_MANAGER_VIEW);
		final Parent MOD_PROFILE_MANAGER_ROOT = MOD_PROFILE_MANAGER_LOADER.load();

		//View for the statusbar section of the main window
		final FXMLLoader STATUS_BAR_LOADER = new FXMLLoader(getClass().getResource("/view/statusbar.fxml"));
		final StatusBarView STATUS_BAR_VIEW = new StatusBarView(UI_SERVICE);
		STATUS_BAR_LOADER.setController(STATUS_BAR_VIEW);
		final Parent STATUS_BAR_ROOT = STATUS_BAR_LOADER.load();

		//View for text input when adding a new Mod either by ID or URL, but not for files.
		final FXMLLoader ID_AND_URL_MOD_IMPORT_INPUT_LOADER = new FXMLLoader(getClass().getResource("/view/simple-input.fxml"));
		final SimpleInputView ID_AND_URL_MOD_IMPORT_INPUT_VIEW = new SimpleInputView();
		ID_AND_URL_MOD_IMPORT_INPUT_LOADER.setController(ID_AND_URL_MOD_IMPORT_INPUT_VIEW);
		final Parent ID_AND_URL_MOD_IMPORT_INPUT_ROOT = ID_AND_URL_MOD_IMPORT_INPUT_LOADER.load();
		ID_AND_URL_MOD_IMPORT_INPUT_VIEW.initView(ID_AND_URL_MOD_IMPORT_INPUT_ROOT);

		//View for managing the actual mod lists. This is the center section of the main window
		final FXMLLoader MODLIST_MANAGER_LOADER = new FXMLLoader(getClass().getResource("/view/modlist-manager.fxml"));
		final ModlistManagerView MODLIST_MANAGER_VIEW = new ModlistManagerView(UI_SERVICE, stage, PROPERTIES, STATUS_BAR_VIEW, MOD_PROFILE_MANAGER_VIEW, SAVE_MANAGER_VIEW, ID_AND_URL_MOD_IMPORT_INPUT_VIEW);
		MODLIST_MANAGER_LOADER.setController(MODLIST_MANAGER_VIEW);
		final Parent MODLIST_MANAGER_ROOT = MODLIST_MANAGER_LOADER.load();

		//View for the menubar section of the main window
		final FXMLLoader MOD_TABLE_CONTEXT_BAR_LOADER = new FXMLLoader(getClass().getResource("/view/mod-table-context-bar.fxml"));
		final ModTableContextBarView MOD_TABLE_CONTEXT_BAR_VIEW = new ModTableContextBarView(UI_SERVICE, MODLIST_MANAGER_VIEW, stage);
		MOD_TABLE_CONTEXT_BAR_LOADER.setController(MOD_TABLE_CONTEXT_BAR_VIEW);
		final Parent MENU_BAR_ROOT = MOD_TABLE_CONTEXT_BAR_LOADER.load();

		//The mod and save manager are fully initialized down here as we only have all the references we need at this stage
		MOD_PROFILE_MANAGER_VIEW.initView(MOD_PROFILE_MANAGER_ROOT, PROPERTIES, MOD_TABLE_CONTEXT_BAR_VIEW);
		SAVE_MANAGER_VIEW.initView(SAVE_MANAGER_ROOT, PROPERTIES, MOD_TABLE_CONTEXT_BAR_VIEW);

		//View for the primary application window
		final FXMLLoader MAIN_VIEW_LOADER = new FXMLLoader(getClass().getResource("/view/main-window.fxml"));
		final MainWindowView MAIN_WINDOW_VIEW = new MainWindowView(PROPERTIES, stage,
				MOD_TABLE_CONTEXT_BAR_VIEW, MODLIST_MANAGER_VIEW, STATUS_BAR_VIEW, UI_SERVICE);
		MAIN_VIEW_LOADER.setController(MAIN_WINDOW_VIEW);
		final Parent MAIN_VIEW_ROOT = MAIN_VIEW_LOADER.load();
		MAIN_WINDOW_VIEW.initView(MAIN_VIEW_ROOT, MENU_BAR_ROOT, MODLIST_MANAGER_ROOT, STATUS_BAR_ROOT);

		//UI_SERVICE.saveUserData();
	}
}