package com.gearshiftgaming.se_mod_manager.controller;

import atlantafx.base.theme.Theme;
import com.gearshiftgaming.se_mod_manager.backend.data.ModlistRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SaveRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.UserDataRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.ModlistService;
import com.gearshiftgaming.se_mod_manager.backend.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.backend.domain.SaveService;
import com.gearshiftgaming.se_mod_manager.backend.domain.UserDataService;
import com.gearshiftgaming.se_mod_manager.backend.models.*;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.ResultType;
import jakarta.xml.bind.JAXBException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class BackendFileController implements BackendController {

	private final SandboxService SANDBOX_SERVICE;

	private final ModlistService MODLIST_SERVICE;

	private final UserDataService USER_DATA_SERVICE;

	private final SaveService SAVE_SERVICE;

	private final File USER_CONFIGURATION_FILE;

	public BackendFileController(SandboxConfigRepository sandboxConfigRepository, ModlistRepository modlistRepository, UserDataRepository userDataRepository, SaveRepository saveRepository, Properties properties, File USER_CONFIGURATION_FILE) {
		this.SANDBOX_SERVICE = new SandboxService(sandboxConfigRepository);
		this.MODLIST_SERVICE = new ModlistService(modlistRepository, properties);
		this.USER_DATA_SERVICE = new UserDataService(userDataRepository);
		this.USER_CONFIGURATION_FILE = USER_CONFIGURATION_FILE;
		this.SAVE_SERVICE = new SaveService(saveRepository, SANDBOX_SERVICE);
	}

	public Result<UserConfiguration> getUserData() throws JAXBException {
		Result<UserConfiguration> userConfigurationResult = USER_DATA_SERVICE.getUserData(USER_CONFIGURATION_FILE);

		if(userConfigurationResult.isSuccess()) {
			for(ModProfile modProfile : userConfigurationResult.getPayload().getModProfiles()) {
				for(int i = 0; i < modProfile.getModList().size(); i ++) {
					modProfile.getModList().get(i).setLoadPriority(i + 1);
				}
			}
		}

		return userConfigurationResult;
	}

	//TODO: Sort the incoming mod list based on priority without modifying the actual list
	public Result<Void> applyModlist(List<Mod> modList, String sandboxConfigPath) throws IOException {
		Result<String> modifiedSandboxConfigResult = SANDBOX_SERVICE.injectModsIntoSandboxConfig(new File(sandboxConfigPath), modList);
		if (modifiedSandboxConfigResult.isSuccess()) {
			return SANDBOX_SERVICE.saveSandboxToFile(sandboxConfigPath, modifiedSandboxConfigResult.getPayload());
		} else {
			Result<Void> failedModification = new Result<>();
			failedModification.addMessage(modifiedSandboxConfigResult.getCurrentMessage(), ResultType.FAILED);
			return failedModification;
		}
	}

	//Sort our mod profile's mod list by loadPriority when we save so that load priority is preserved
	public Result<Void> saveUserData(UserConfiguration userConfiguration) {
		return USER_DATA_SERVICE.saveUserData(sortUserConfigurationModLists(userConfiguration), USER_CONFIGURATION_FILE);
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
	public Result<SaveProfile> copySaveProfile(SaveProfile sourceSaveProfile) throws IOException {
		Result<SaveProfile> copyResult = SAVE_SERVICE.copySaveFiles(sourceSaveProfile);
		if (copyResult.isSuccess()) {
			copyResult.addMessage("Successfully copied save " + sourceSaveProfile.getProfileName() + ".", ResultType.SUCCESS);
		}
		return copyResult;
	}

	//Only here for development purposes
	public Result<Void> createTestUserData(Theme theme) {

		SaveProfile testSaveProfile = new SaveProfile("Test Profile", "./Storage/fake.sbc");
		testSaveProfile.setSaveName("Test save");
		ModProfile testModProfile = new ModProfile("Test Profile");

		Mod testMod = new Mod("123456789", ModType.STEAM);
		List<String> testCategories = new ArrayList<>();
		testCategories.add("Test Category");
		testCategories.add("Three Category test");
		testMod.setFriendlyName("Test Mod");
		testMod.setCategories(testCategories);
		testModProfile.getModList().add(testMod);

		Mod secondTestMod = new Mod("0987654321", ModType.MOD_IO);
		secondTestMod.setFriendlyName("Second test mod");
		secondTestMod.setCategories(testCategories);
		testModProfile.getModList().add(secondTestMod);

		testSaveProfile.setLastUsedModProfile(testModProfile.getID());

		UserConfiguration userConfiguration = new UserConfiguration();
		userConfiguration.getSaveProfiles().removeFirst();
		userConfiguration.getSaveProfiles().add(testSaveProfile);
		userConfiguration.getModProfiles().add(testModProfile);
		userConfiguration.setUserTheme(theme.getName());

		return USER_DATA_SERVICE.saveUserData(userConfiguration, new File("./Storage/SEMM_TEST_Data.xml"));
	}

	private UserConfiguration sortUserConfigurationModLists(UserConfiguration userConfiguration) {
		UserConfiguration sortedUserConfiguration = new UserConfiguration(userConfiguration);
		for (ModProfile m : sortedUserConfiguration.getModProfiles()) {
			List<Mod> sortedModList = m.getModList().stream()
					.sorted(Comparator.comparing(Mod::getLoadPriority))
					.toList();
			m.setModList(sortedModList);
		}
		return sortedUserConfiguration;
	}
}
