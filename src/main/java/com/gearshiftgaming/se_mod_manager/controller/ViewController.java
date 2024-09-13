package com.gearshiftgaming.se_mod_manager.controller;

import com.gearshiftgaming.se_mod_manager.backend.data.ModlistFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SaveFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.UserDataFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.ModProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.LogMessage;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
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
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 *
 * @author Gear Shift
 */
public class ViewController {
	private final String DESKTOP_PATH = System.getProperty("user.home") + "/Desktop";

	private final Properties properties;

	private final UiService uiService;

	private final Logger logger;

	public ViewController(Stage stage, Logger logger) throws IOException, JAXBException, XmlPullParserException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
		this.logger = logger;
		logger.info("Started application");

		properties = new Properties();
		try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("SEMM.properties")) {
			properties.load(input);
		} catch (IOException | NullPointerException e) {
			logger.error("Could not load SEMM.properties. " + e.getMessage());
			throw (e);
		}

		BackendController backendController = new BackendFileController(new SandboxConfigFileRepository(),
				new ModlistFileRepository(),
				new UserDataFileRepository(),
				new SaveFileRepository(),
				properties,
				new File(properties.getProperty("semm.userData.default.location")));

		Result<UserConfiguration> userConfigurationResult = backendController.getUserData();
		UserConfiguration userConfiguration;

		if (userConfigurationResult.isSuccess()) {
			userConfiguration = userConfigurationResult.getPayload();
		} else {
			userConfiguration = new UserConfiguration();
			backendController.saveUserData(userConfiguration);
		}

		ObservableList<ModProfile> modProfiles = FXCollections.observableList(userConfiguration.getModProfiles());
		ObservableList<SaveProfile> saveProfiles = FXCollections.observableList(userConfiguration.getSaveProfiles());

		//Initialize the list we use to store log messages shown to the user
		ObservableList<LogMessage> userLog = FXCollections.observableArrayList(logMessage ->
				new Observable[]{
						logMessage.viewableLogMessageProperty(),
						logMessage.messageTypeProperty()
				});

		uiService = new UiService(logger, userLog, modProfiles, saveProfiles, backendController, userConfiguration);
		uiService.log(userConfigurationResult);

		setupInterface(stage);

		//TODO: Actually implement this. Function is empty at the moment.
		//TODO: When we launch the app for the first time, walk the user through first making a save profile, then renaming the default mod profile, then IMMEDIATELY save to file.
		if (!userConfigurationResult.isSuccess()) {
			uiService.firstTimeSetup();
		}
	}

	private void setupInterface(Stage stage) throws IOException, XmlPullParserException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
		//Manually inject our controllers into our FXML so we can reuse the FXML for the profile creation elsewhere, and have greater flexibility in controller injection and FXML initialization.
		//View for adding a new Save Profile
		FXMLLoader saveListInputLoader = new FXMLLoader(getClass().getResource("/view/save-list-input.fxml"));
		SaveListInput saveListInputFirstStepView = new SaveListInput();
		saveListInputLoader.setController(saveListInputFirstStepView);
		Parent saveListInputRoot = saveListInputLoader.load();
		saveListInputFirstStepView.initView(saveListInputRoot, uiService);

		//View for text input when creating a new save profile.
		FXMLLoader saveProfileManagerLoader = new FXMLLoader(getClass().getResource("/view/profile-input.fxml"));
		ProfileInputView saveListInputSecondStepView = new ProfileInputView();
		saveProfileManagerLoader.setController(saveListInputSecondStepView);
		Parent saveListInputSecondStepRoot = saveProfileManagerLoader.load();
		saveListInputSecondStepView.initView(saveListInputSecondStepRoot);

		//View for text input when adding a new Mod Profile
		FXMLLoader modProfileManagerLoader = new FXMLLoader(getClass().getResource("/view/profile-input.fxml"));
		ProfileInputView profileInputView = new ProfileInputView();
		modProfileManagerLoader.setController(profileInputView);
		Parent modProfileCreateRoot = modProfileManagerLoader.load();
		profileInputView.initView(modProfileCreateRoot);

		//View for managing Save Profiles
		FXMLLoader saveManagerLoader = new FXMLLoader(getClass().getResource("/view/save-profile-manager.fxml"));
		SaveManagerView saveManagerView = new SaveManagerView();
		saveManagerLoader.setController(saveManagerView);
		Parent saveManagerRoot = saveManagerLoader.load();

		//View for managing Mod Profiles
		FXMLLoader modProfilerManagerLoader = new FXMLLoader(getClass().getResource("/view/mod-profile-manager.fxml"));
		ModProfileManagerView modProfileManagerView = new ModProfileManagerView();
		modProfilerManagerLoader.setController(modProfileManagerView);
		Parent modProfileRoot = modProfilerManagerLoader.load();

		//View for the menubar section of the main window
		FXMLLoader menuBarLoader = new FXMLLoader(getClass().getResource("/view/menubar.fxml"));
		MenuBarView menuBarView = new MenuBarView();
		menuBarLoader.setController(menuBarView);
		Parent menuBarRoot = menuBarLoader.load();

		//View for the statusbar section of the main window
		FXMLLoader statusBarLoader = new FXMLLoader(getClass().getResource("/view/statusbar.fxml"));
		StatusBarView statusBarView = new StatusBarView();
		statusBarLoader.setController(statusBarView);
		Parent statusBarRoot = statusBarLoader.load();

		//View for the primary application window
		FXMLLoader mainViewLoader = new FXMLLoader(getClass().getResource("/view/main-window.fxml"));
		Parent mainViewRoot = mainViewLoader.load();
		MainWindowView mainWindowView = mainViewLoader.getController();
		mainWindowView.initView(properties,
				stage, mainViewRoot,
				modProfileManagerView, saveManagerView,
				menuBarView, menuBarRoot,
				statusBarView, statusBarRoot,
				uiService);

		modProfileManagerView.initView(modProfileRoot, uiService, profileInputView, properties, mainWindowView.getMenuBarView());
		saveManagerView.initView(saveManagerRoot, uiService, saveListInputFirstStepView, saveListInputSecondStepView, properties, mainWindowView.getMenuBarView());
	}
}