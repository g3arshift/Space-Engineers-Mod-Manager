package com.gearshiftgaming.se_mod_manager.frontend.domain;

import atlantafx.base.theme.Theme;
import com.gearshiftgaming.se_mod_manager.backend.models.*;
import com.gearshiftgaming.se_mod_manager.controller.BackendStorageController;
import com.gearshiftgaming.se_mod_manager.controller.ModInfoController;
import com.gearshiftgaming.se_mod_manager.frontend.view.helper.ModlistManagerHelper;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.Popup;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.CheckMenuItem;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * All the UI logic passes through here, and is the endpoint that the UI uses to connect to the rest of the system.
 * It holds all the relevant variables that are actual logic, such as the observable lists for save and mod profiles, as well as the current profiles.
 * <p>
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class UiService {
	private final Logger LOGGER;

	private final BackendStorageController BACKEND_STORAGE_CONTROLLER;

	private final ModInfoController MOD_INFO_CONTROLLER;

	@Getter
	private final ObservableList<LogMessage> USER_LOG;

	@Getter
	private final ObservableList<ModProfile> MOD_PROFILES;

	@Getter
	private final ObservableList<SaveProfile> SAVE_PROFILES;

	@Getter
	private final UserConfiguration USER_CONFIGURATION;

	@Getter
	@Setter
	private SaveProfile currentSaveProfile;

	@Getter
	private ModProfile currentModProfile;

	@Getter
	private ObservableList<Mod> currentModList;

	@Getter
	private final IntegerProperty activeModCount;

	public UiService(Logger LOGGER, @NotNull ObservableList<LogMessage> USER_LOG,
					 @NotNull ObservableList<ModProfile> MOD_PROFILES, @NotNull ObservableList<SaveProfile> SAVE_PROFILES,
					 BackendStorageController backendStorageController, ModInfoController modInfoController, UserConfiguration USER_CONFIGURATION) {

		this.LOGGER = LOGGER;
		this.MOD_INFO_CONTROLLER = modInfoController;
		this.USER_LOG = USER_LOG;
		this.MOD_PROFILES = MOD_PROFILES;
		this.SAVE_PROFILES = SAVE_PROFILES;
		this.BACKEND_STORAGE_CONTROLLER = backendStorageController;
		this.USER_CONFIGURATION = USER_CONFIGURATION;

		//Initialize our current mod and save profiles
		Optional<SaveProfile> lastUsedSaveProfile = SAVE_PROFILES.stream()
				.filter(saveProfile -> saveProfile.getID().equals(USER_CONFIGURATION.getLastUsedSaveProfileId()))
				.findFirst();
		if (lastUsedSaveProfile.isPresent()) {
			currentSaveProfile = lastUsedSaveProfile.get();
			Optional<ModProfile> lastUsedModProfile = MOD_PROFILES.stream()
					.filter(modProfile -> modProfile.getID().equals(currentSaveProfile.getLastUsedModProfile()))
					.findFirst();
			currentModProfile = lastUsedModProfile.orElseGet(MOD_PROFILES::getFirst);
		} else {
			log("No previously applied save profile detected.", MessageType.INFO);
			currentSaveProfile = SAVE_PROFILES.getFirst();
			currentModProfile = MOD_PROFILES.getFirst();
		}

		//A little bit of duplication, but the order of construction is a big different than setCurrentModProfile
		//currentModProfile.getModList()
		currentModList = FXCollections.observableArrayList(currentModProfile.getModList());
		activeModCount = new SimpleIntegerProperty((int) currentModList.stream().filter(Mod::isActive).count());
	}

	public void log(String message, MessageType messageType) {
		LogMessage logMessage = new LogMessage(message, messageType, LOGGER);
		USER_LOG.add(logMessage);
	}

	public <T> void log(Result<T> result) {
		MessageType messageType;
		switch (result.getType()) {
			case INVALID -> messageType = MessageType.WARN;
			case CANCELLED, NOT_INITIALIZED, FAILED -> messageType = MessageType.ERROR;
			default -> messageType = MessageType.INFO;
		}
		log(result.getCurrentMessage(), messageType);
	}

	public void logPrivate(String message, MessageType messageType) {
		switch (messageType) {
			case INFO -> LOGGER.info(message);
			case WARN -> LOGGER.warn(message);
			case ERROR -> LOGGER.error(message);
			case UNKNOWN -> LOGGER.error("ERROR UNKNOWN - " + message);
		}
	}

	public Result<Void> saveUserData() {
		return BACKEND_STORAGE_CONTROLLER.saveUserData(USER_CONFIGURATION);
	}

	public Result<Void> applyModlist(List<Mod> modList, String sandboxConfigPath) throws IOException {
		return BACKEND_STORAGE_CONTROLLER.applyModlist(modList, sandboxConfigPath);
	}

	public Result<SaveProfile> copySaveProfile(SaveProfile saveProfile) throws IOException {
		return BACKEND_STORAGE_CONTROLLER.copySaveProfile(saveProfile);
	}

	public Result<SaveProfile> getSaveProfile(File sandboxConfigFile) throws IOException {
		return BACKEND_STORAGE_CONTROLLER.getSaveProfile(sandboxConfigFile);
	}

	public void firstTimeSetup() {
		//TODO: Setup users first modlist and save, and also ask if they want to try and automatically find ALL saves they have and add them to SEMM.
	}

	//Sets the theme for our application based on the users preferred theme using reflection.
	//It expects to receive a list of CheckMenuItems that represent the UI dropdown list for all the available system themes in the MenuBar. Not the *best* way to do this, but it works.
	public void setUserSavedApplicationTheme(List<CheckMenuItem> themeList) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
		for (CheckMenuItem c : themeList) {
			String currentTheme = StringUtils.removeEnd(c.getId(), "Theme");
			String themeName = currentTheme.substring(0, 1).toUpperCase() + currentTheme.substring(1);
			if (themeName.equals(StringUtils.deleteWhitespace(USER_CONFIGURATION.getUserTheme()))) {
				c.setSelected(true);
				Class<?> cls = Class.forName("atlantafx.base.theme." + StringUtils.deleteWhitespace(USER_CONFIGURATION.getUserTheme()));
				Theme theme = (Theme) cls.getDeclaredConstructor().newInstance();
				Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
			}
		}
	}

	public void setCurrentModProfile(ModProfile modProfile) {
		currentModProfile = modProfile;
		currentModList = FXCollections.observableArrayList(currentModProfile.getModList());
		activeModCount.set((int) currentModList.stream().filter(Mod::isActive).count());
	}

	public void modifyActiveModCount(Mod mod) {
		if (mod.isActive()) {
			activeModCount.set(activeModCount.get() + 1);
		} else {
			activeModCount.set(activeModCount.get() - 1);
		}
	}

	public void addModFromSteamId(String modId, Stage stage) {
		Mod mod = new Mod(modId, ModType.STEAM);
		try {
			Future<String> modInfoScrape = MOD_INFO_CONTROLLER.addModBySteamId(mod);
			Thread singleModThread = getSingleModAddThread(mod, modInfoScrape, stage);
			singleModThread.start();
		} catch (IOException | ExecutionException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public Result<List<Mod>> addModsFromSteamCollection() {
		//TODO: Implement
		return null;
	}

	public Result<Mod> addModFromModIoId() {
		//TODO: Implement
		return null;
	}

	public Result<List<Mod>> addModsFromFile() {
		//TODO: Implement
		return null;
	}

	//TODO: We need to debug when we give it bad input. It doesn't do anything.
	private Thread getSingleModAddThread(Mod mod, Future<String> scrapedModInfo, Stage stage) {
		final Task<Result<Void>> TASK = new Task<>() {
			@Override
			protected Result<Void> call() {
				Result<Void> modInfoResult = new Result<>();
				String[] modInfo;
				if (mod.getModType() == ModType.STEAM) {
					try {
						//Calling .get on a future is a blocking task which is why we're calling it in a thread that'll get run by Platform.runlater
						modInfo = scrapedModInfo.get().split(" Workshop::");
					} catch (InterruptedException | ExecutionException e) {
						throw new RuntimeException(e);
					}
				} else {
					//TODO: REMOVE. TEST SETUP.
					modInfo = new String[0];
					//TODO: Implement modIO stuff here.
				}

				MOD_INFO_CONTROLLER.setModInformation(mod, modInfo);
				if (mod.getFriendlyName().equals("_NOT_A_MOD")) {
					modInfoResult.addMessage("The supplied Mod ID is for either a workshop item that is not a mod, for the wrong game, or is not publicly available on the workshop.", ResultType.INVALID);
				} else {
					modInfoResult.addMessage("Mod \"" + mod.getFriendlyName() + "\" has been successfully added.", ResultType.SUCCESS);
				}
				//TODO: Update some UI element here to indicate progress. pass or fail, update it as complete.
				//TODO: The whole UI needs to get locked out with some half-opaque progress pane, or bar in the middle of a pane, because you can really fuck it up otherwise
				// Do it with Platform.run
				return modInfoResult;
			}
		};

		TASK.setOnSucceeded(workerStateEvent -> Platform.runLater(() -> {
			Result<Void> modScrapeResult = TASK.getValue();

			if (modScrapeResult.isSuccess()) {
				//TODO: Save the changes
				//TODO: Might need to do the modprofile trickery thing here. Like in the cell factories.
				//TODO: Add duplicate mod check for if it's already in our modlist.
				//TODO: Load priority is wrong. Need to fix that. ModListHelper might work, but we can also just set it to the size of the list for singles since it'll add at the end.
				currentModList.add(mod);
				currentModProfile.setModList(currentModList);
				saveUserData();
				//TODO: Popup success message and clear the UI progress bar/whatever we use
			} else {
				log(modScrapeResult);
				//TODO: We need to bring in the modlist manager object to get its stage and shit here.
				//TODO: When mods fail, first display a popup with the successful number of mods added, then display a popup with the summarized failures.
				// Then add the mod to our list, and save it. Might need a reference to sorted list, or maybe can just directly use observable list. Or filteredList.getSource().
				// Finally, select the very first of the added mods in the list
				//TODO: Still need to populate rest of mod info fields
			}
			log(modScrapeResult);
			Popup.displaySimpleAlert(modScrapeResult, stage);
		}));

		Thread thread = Thread.ofVirtual().unstarted(TASK);
		thread.setDaemon(true);
		return thread;
	}
}