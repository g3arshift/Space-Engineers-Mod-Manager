package com.gearshiftgaming.se_mod_manager.frontend.domain;

import atlantafx.base.theme.Theme;
import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.ModProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.backend.models.LogMessage;
import com.gearshiftgaming.se_mod_manager.backend.models.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.controller.BackendStorageController;
import com.gearshiftgaming.se_mod_manager.controller.ModInfoController;
import javafx.application.Application;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckMenuItem;
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
		if(mod.isActive()) {
			activeModCount.set(activeModCount.get() + 1);
		} else {
			activeModCount.set(activeModCount.get() - 1);
		}
	}

	public Result<Mod> addModFromSteamId(String modId) {
		try {
			MOD_INFO_CONTROLLER.addModBySteamId(modId);
			//TODO: Actually add it to the current list
		} catch (IOException e) {
			//TODO: Return a bad result
			throw new RuntimeException(e);
		}
		return null;
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
}