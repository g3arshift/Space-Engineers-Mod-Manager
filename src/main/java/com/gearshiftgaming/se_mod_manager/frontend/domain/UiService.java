package com.gearshiftgaming.se_mod_manager.frontend.domain;

import atlantafx.base.theme.Theme;
import com.gearshiftgaming.se_mod_manager.backend.models.*;
import com.gearshiftgaming.se_mod_manager.controller.StorageController;
import com.gearshiftgaming.se_mod_manager.controller.ModInfoController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

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

	private final StorageController STORAGE_CONTROLLER;

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


	@Setter
	private IntegerProperty modImportProgressNumerator;


	@Setter
	private IntegerProperty modImportProgressDenominator;


	@Setter
	private DoubleProperty modImportProgressPercentage;

	private final String MOD_DATE_FORMAT;

	public UiService(Logger LOGGER, @NotNull ObservableList<LogMessage> USER_LOG,
					 @NotNull ObservableList<ModProfile> MOD_PROFILES, @NotNull ObservableList<SaveProfile> SAVE_PROFILES,
					 StorageController storageController, ModInfoController modInfoController, UserConfiguration USER_CONFIGURATION, @NotNull Properties properties) {

		this.LOGGER = LOGGER;
		this.MOD_INFO_CONTROLLER = modInfoController;
		this.USER_LOG = USER_LOG;
		this.MOD_PROFILES = MOD_PROFILES;
		this.SAVE_PROFILES = SAVE_PROFILES;
		this.STORAGE_CONTROLLER = storageController;
		this.USER_CONFIGURATION = USER_CONFIGURATION;

		this.MOD_DATE_FORMAT = properties.getProperty("semm.steam.mod.dateFormat");

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

		//A little bit of duplication, but the order of construction is a big different from setCurrentModProfile
		//currentModProfile.getModList()
		currentModList = FXCollections.observableArrayList(currentModProfile.getModList());
		activeModCount = new SimpleIntegerProperty((int) currentModList.stream().filter(Mod::isActive).count());
	}

	public void log(String message, MessageType messageType) {
		LogMessage logMessage = new LogMessage(message, messageType, LOGGER);
		USER_LOG.add(logMessage);
	}

	public <T> void log(@NotNull Result<T> result) {
		MessageType messageType;
		switch (result.getType()) {
			case INVALID -> messageType = MessageType.WARN;
			case CANCELLED, NOT_INITIALIZED, FAILED -> messageType = MessageType.ERROR;
			default -> messageType = MessageType.INFO;
		}
		log(result.getCurrentMessage(), messageType);
	}

	public void log(Exception e) {
		log(String.valueOf(e), MessageType.ERROR);
	}

	public void logPrivate(String message, @NotNull MessageType messageType) {
		switch (messageType) {
			case INFO -> LOGGER.info(message);
			case WARN -> LOGGER.warn(message);
			case ERROR -> LOGGER.error(message);
			case DEBUG -> LOGGER.debug(message);
			case UNKNOWN -> LOGGER.error("ERROR UNKNOWN - {}", message);
		}
	}

	public <T> void logPrivate(@NotNull Result<T> result) {
		switch (result.getType()) {
			case SUCCESS, CANCELLED -> LOGGER.info(result.getCurrentMessage());
			case INVALID -> LOGGER.warn(result.getCurrentMessage());
			case FAILED -> LOGGER.error(result.getCurrentMessage());
			default -> LOGGER.error("ERROR UNKNOWN - {}",  result.getCurrentMessage());
		}
	}

	public Result<Void> saveUserData() {
		return STORAGE_CONTROLLER.saveUserData(USER_CONFIGURATION);
	}

	public Result<Void> applyModlist(List<Mod> modList, String sandboxConfigPath) throws IOException {
		return STORAGE_CONTROLLER.applyModlist(modList, sandboxConfigPath);
	}

	public Result<SaveProfile> copySaveProfile(SaveProfile saveProfile) throws IOException {
		return STORAGE_CONTROLLER.copySaveProfile(saveProfile);
	}

	public Result<SaveProfile> getSaveProfile(File sandboxConfigFile) throws IOException {
		return STORAGE_CONTROLLER.getSaveProfile(sandboxConfigFile);
	}

	public Result<String> getSaveName(File sandboxConfigFile) throws IOException {
		return STORAGE_CONTROLLER.getSaveName(sandboxConfigFile);
	}

	public void firstTimeSetup() {
		//TODO: Setup users first modlist and save, and also ask if they want to try and automatically find ALL saves they have and add them to SEMM.
	}

	//Sets the theme for our application based on the users preferred theme using reflection.
	//It expects to receive a list of CheckMenuItems that represent the UI dropdown list for all the available system themes in the MenuBar. Not the *best* way to do this, but it works.
	public void setUserSavedApplicationTheme(@NotNull List<CheckMenuItem> themeList) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
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

	public void modifyActiveModCount(@NotNull Mod mod) {
		if (mod.isActive()) {
			activeModCount.set(activeModCount.get() + 1);
		} else {
			activeModCount.set(activeModCount.get() - 1);
		}
	}

	public void modifyActiveModCount(int numMods) {
		activeModCount.set(activeModCount.get() + numMods);
	}

	//This isn't down in the ModlistService because we need to actually update the numerator on each and every single completed get call for the UI progress
	// bars to work properly.
	public List<Result<String>> scrapeSteamModCollectionModList(String collectionId) throws IOException {

		List<Result<String>> steamCollectionModIds = MOD_INFO_CONTROLLER.scrapeSteamModCollectionModList(collectionId);

		//Process the returned ID's and check for duplicates in our current mod list.
		for (Result<String> modIdResult : steamCollectionModIds) {
			if (modIdResult.isSuccess()) {
				Optional<Mod> duplicateMod = currentModList.stream()
						.filter(mod -> modIdResult.getPayload().equals(mod.getId()))
						.findFirst();
				duplicateMod.ifPresent(mod -> modIdResult.addMessage("\"" + mod.getFriendlyName() + "\" already exists in modlist.", ResultType.INVALID));
			}
		}

		return steamCollectionModIds;
	}

	public Result<String> getModIoModIdFromUrlName(String modName) throws IOException {
		Result<String> idFromUrlResult = MOD_INFO_CONTROLLER.getModIoIdFromUrlName(modName);
		if (idFromUrlResult.isSuccess()) {
			for (Mod mod : currentModList) {
				if (mod.getId().equals(idFromUrlResult.getPayload())) {
					idFromUrlResult.addMessage("\"" + mod.getFriendlyName() + "\" already exists in the modlist!", ResultType.INVALID);
					break;
				}
			}
		}
		return idFromUrlResult;
	}

	public Result<Mod> fillOutModInformation(Mod mod) throws IOException {
		Result<String[]> modScrapeResult = MOD_INFO_CONTROLLER.fillOutModInformation(mod);
		Result<Mod> modInfoResult = new Result<>();

		if (modScrapeResult.isSuccess()) {
			String[] modInfo = modScrapeResult.getPayload();

			mod.setFriendlyName(modInfo[0]);

			List<String> modTags = List.of(modInfo[1].split(","));
			mod.setCategories(modTags);

			mod.setDescription(modInfo[2]);

			modInfoResult.setPayload(mod);

			boolean duplicateModFound = false;
			String duplicateModName = "";
			for (Mod m : currentModList) {
				//We only want to do this comparison when we are comparing different mod types, as we can otherwise assume the ID check has handled duplicates.
				if (!m.getClass().equals(mod.getClass())) {
					String shorterModName;
					String longerModName;
					if (m.getFriendlyName().length() < mod.getFriendlyName().length()) {
						shorterModName = m.getFriendlyName();
						longerModName = mod.getFriendlyName();
					} else {
						shorterModName = mod.getFriendlyName();
						longerModName = m.getFriendlyName();
					}

					if (longerModName.contains(shorterModName)) {
						duplicateModFound = true;
						duplicateModName = m.getFriendlyName();
						break;
					}
				}
			}

			DateTimeFormatter formatter;
			if (mod instanceof SteamMod) {
				formatter = new DateTimeFormatterBuilder()
						.parseCaseInsensitive()
						.appendPattern(MOD_DATE_FORMAT)
						.toFormatter();
				((SteamMod) mod).setLastUpdated(LocalDateTime.parse(modInfo[3], formatter));
			} else {
				((ModIoMod) mod).setLastUpdatedYear(Year.parse(modInfo[3]));

				if (modInfo[4] != null) {
					((ModIoMod) mod).setLastUpdatedMonthDay(MonthDay.parse(modInfo[4]));
				}

				if (modInfo[5] != null) {
					((ModIoMod) mod).setLastUpdatedHour(LocalTime.parse(modInfo[5]));
				}
			}

			if (duplicateModFound) {
				modInfoResult.addMessage(String.format("Mod \"%s\" may be the same as \"%s\". Do you still want to add it?", mod.getFriendlyName(), duplicateModName), ResultType.REQUIRES_ADJUDICATION);
			} else {
				modInfoResult.addMessage("Mod \"" + mod.getFriendlyName() + "\" has been successfully scraped.", ResultType.SUCCESS);
			}
		} else {
			modInfoResult.addMessage(modScrapeResult.getCurrentMessage(), modScrapeResult.getType());
		}

		Platform.runLater(() -> {
			modImportProgressNumerator.setValue(modImportProgressNumerator.get() + 1);
			modImportProgressPercentage.setValue((double) modImportProgressNumerator.get() / (double) modImportProgressDenominator.get());
		});
		return modInfoResult;
	}

	public Result<List<Mod>> addModsFromFile() {
		//TODO: Implement
		return null;
	}

	public IntegerProperty getModImportProgressNumeratorProperty() {
		if (this.modImportProgressNumerator == null) {
			this.modImportProgressNumerator = new SimpleIntegerProperty(0);
		}
		return this.modImportProgressNumerator;
	}

	public int getModImportProgressNumerator() {
		return modImportProgressNumerator.get();
	}

	public IntegerProperty getModImportProgressDenominatorProperty() {
		if (this.modImportProgressDenominator == null) {
			this.modImportProgressDenominator = new SimpleIntegerProperty(0);
		}
		return this.modImportProgressDenominator;
	}

	public int getModImportProgressDenominator() {
		return modImportProgressDenominator.get();
	}

	public DoubleProperty getModImportProgressPercentageProperty() {
		if (modImportProgressPercentage == null) {
			modImportProgressPercentage = new SimpleDoubleProperty(0d);
		}
		return modImportProgressPercentage;
	}

	public double getModImportProgressPercentage() {
		return modImportProgressPercentage.get();
	}
}
