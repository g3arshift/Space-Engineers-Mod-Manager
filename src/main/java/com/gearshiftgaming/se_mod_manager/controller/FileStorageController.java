package com.gearshiftgaming.se_mod_manager.controller;

import atlantafx.base.theme.Theme;
import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SaveRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.UserDataRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.backend.domain.SaveService;
import com.gearshiftgaming.se_mod_manager.backend.domain.UserDataService;
import com.gearshiftgaming.se_mod_manager.backend.models.*;
import jakarta.xml.bind.JAXBException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class FileStorageController implements StorageController {

	private final SandboxService SANDBOX_SERVICE;

	private final UserDataService USER_DATA_SERVICE;

	private final SaveService SAVE_SERVICE;

	private final File USER_CONFIGURATION_FILE;

	public FileStorageController(SandboxConfigRepository sandboxConfigRepository, UserDataRepository userDataRepository, SaveRepository saveRepository, Properties properties, File USER_CONFIGURATION_FILE) {
		this.SANDBOX_SERVICE = new SandboxService(sandboxConfigRepository);
		this.USER_DATA_SERVICE = new UserDataService(userDataRepository);
		this.USER_CONFIGURATION_FILE = USER_CONFIGURATION_FILE;
		this.SAVE_SERVICE = new SaveService(saveRepository, SANDBOX_SERVICE);
	}

	public Result<UserConfiguration> getUserData() throws JAXBException {
		Result<UserConfiguration> userConfigurationResult = USER_DATA_SERVICE.getUserData(USER_CONFIGURATION_FILE);

		if (userConfigurationResult.isSuccess()) {
			for (ModlistProfile modlistProfile : userConfigurationResult.getPayload().getModlistProfiles()) {
				for (int i = 0; i < modlistProfile.getModList().size(); i++) {
					modlistProfile.getModList().get(i).setLoadPriority(i + 1);
				}
			}
		}

		return userConfigurationResult;
	}

	public Result<Void> applyModlist(List<Mod> modList, SaveProfile saveProfile) throws IOException {
		File sandboxConfigFile = new File(saveProfile.getSavePath());
		Result<String> modifiedSandboxConfigResult = SANDBOX_SERVICE.injectModsIntoSandboxConfig(sandboxConfigFile, modList);
		if (modifiedSandboxConfigResult.isSuccess()) {
			return SANDBOX_SERVICE.saveSandboxToFile(saveProfile.getSavePath(), modifiedSandboxConfigResult.getPayload());
		} else {
			Result<Void> failedModification = new Result<>();
			failedModification.addMessage(modifiedSandboxConfigResult.getCurrentMessage(), ResultType.FAILED);
			return failedModification;
		}
	}

	//Sort our mod profile's mod list by loadPriority when we save so that load priority is preserved
	public Result<Void> saveUserData(UserConfiguration userConfiguration) {
		Result<Void> saveResult = new Result<>();
		try {
			saveResult = USER_DATA_SERVICE.saveUserData(sortUserConfigurationModLists(userConfiguration), USER_CONFIGURATION_FILE);
		} catch (IOException e) {
			saveResult.addMessage(e.toString(), ResultType.FAILED);
		}
		return saveResult;
	}

	@Override
	public Result<SaveProfile> getSaveProfile(File sandboxConfigFile) throws IOException {
		Result<SaveProfile> saveProfileResult = new Result<>();
		Result<String> sandboxFileResult = SANDBOX_SERVICE.getSandboxFromFile(sandboxConfigFile);
		if (!sandboxFileResult.isSuccess()) {
			saveProfileResult.addMessage(sandboxFileResult.getCurrentMessage(), sandboxFileResult.getType());
			return saveProfileResult;
		}

		String sandboxConfig = sandboxFileResult.getPayload();

		//Technically, the name of the save the game reads is in Sandbox.sbc, not Sandbox_config.sbc.
		// But when you rename a save in the game it changes both files, so this is probably fine.
		SaveProfile saveProfile = new SaveProfile(sandboxConfigFile);
		saveProfile.setSaveName(SAVE_SERVICE.getSessionName(sandboxConfig, sandboxConfigFile.getPath()));

		saveProfileResult.setPayload(saveProfile);
		saveProfileResult.addMessage("Successfully loaded save: " + saveProfile.getSaveName(), ResultType.SUCCESS);
		return saveProfileResult;
	}

	@Override
	public Result<String> getSaveName(File sandboxConfigFile) throws IOException {
		Result<String> sandboxfileResult = SANDBOX_SERVICE.getSandboxFromFile(sandboxConfigFile);
		if (sandboxfileResult.isSuccess()) {
			sandboxfileResult.setPayload(SAVE_SERVICE.getSessionName(sandboxfileResult.getPayload(), sandboxConfigFile.getPath()));
		}
		return sandboxfileResult;
	}

	@Override
	public Result<SaveProfile> copySaveProfile(SaveProfile sourceSaveProfile) throws IOException {
		Result<SaveProfile> copyResult = SAVE_SERVICE.copySaveFiles(sourceSaveProfile);
		if (copyResult.isSuccess()) {
			copyResult.addMessage("Successfully copied save " + sourceSaveProfile.getProfileName() + ".", ResultType.SUCCESS);
		}
		return copyResult;
	}

	@Override
	public Result<List<Mod>> getModlistFromSave(File sandboxConfigFile) {
		return SANDBOX_SERVICE.getModlistFromSandboxConfig(sandboxConfigFile);
	}

	@Override
	public Result<Void> exportModlist(ModlistProfile modlistProfile, File saveLocation) {
		return USER_DATA_SERVICE.exportModlist(modlistProfile, saveLocation);
	}

	@Override
	public Result<ModlistProfile> importModlist(File saveLocation) {
		return USER_DATA_SERVICE.importModlist(saveLocation);
	}

	//Only here for development purposes
	public Result<Void> createTestUserData(Theme theme) {

		SaveProfile testSaveProfile = new SaveProfile("Test Profile", "./Storage/fake.sbc");
		testSaveProfile.setSaveName("Test save");
		ModlistProfile testModlistProfile = new ModlistProfile("Test Profile");

		SteamMod testMod = new SteamMod("123456789");
		List<String> testCategories = new ArrayList<>();
		testCategories.add("Test Category");
		testCategories.add("Three Category test");
		testMod.setFriendlyName("Test Mod");
		testMod.setCategories(testCategories);
		testModlistProfile.getModList().add(testMod);

		SteamMod secondTestMod = new SteamMod("0987654321");
		secondTestMod.setFriendlyName("Second test mod");
		secondTestMod.setCategories(testCategories);
		testModlistProfile.getModList().add(secondTestMod);

		ModIoMod thirdTestMod = new ModIoMod("122122");
		thirdTestMod.setFriendlyName("Third test mod");
		thirdTestMod.setCategories(testCategories);
		testModlistProfile.getModList().add(thirdTestMod);

		testSaveProfile.setLastUsedModProfileId(testModlistProfile.getID());

		UserConfiguration userConfiguration = new UserConfiguration();
		userConfiguration.getSaveProfiles().removeFirst();
		userConfiguration.getSaveProfiles().add(testSaveProfile);
		userConfiguration.getModlistProfiles().add(testModlistProfile);
		userConfiguration.setUserTheme(theme.getName());

		System.out.println("Created test user data.");
		try {
			return USER_DATA_SERVICE.saveUserData(userConfiguration, new File("./Storage/SEMM_TEST_Data.xml"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private UserConfiguration sortUserConfigurationModLists(UserConfiguration userConfiguration) {
		UserConfiguration sortedUserConfiguration = new UserConfiguration(userConfiguration);
		for (ModlistProfile m : sortedUserConfiguration.getModlistProfiles()) {
			List<Mod> sortedModList = m.getModList().stream()
					.sorted(Comparator.comparing(Mod::getLoadPriority))
					.toList();
			m.setModList(sortedModList);
		}
		return sortedUserConfiguration;
	}
}
