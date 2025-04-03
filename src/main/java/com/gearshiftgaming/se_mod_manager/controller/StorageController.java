package com.gearshiftgaming.se_mod_manager.controller;

import atlantafx.base.theme.Theme;
import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SaveRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.UserDataRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.backend.domain.SaveService;
import com.gearshiftgaming.se_mod_manager.backend.domain.UserDataService;
import com.gearshiftgaming.se_mod_manager.backend.models.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class StorageController {

	private final SandboxService SANDBOX_SERVICE;

	private final UserDataService USER_DATA_SERVICE;

	private final SaveService SAVE_SERVICE;

    public StorageController(SandboxConfigRepository sandboxConfigRepository, UserDataRepository userDataRepository, SaveRepository saveRepository) {
		this.SANDBOX_SERVICE = new SandboxService(sandboxConfigRepository);
		this.USER_DATA_SERVICE = new UserDataService(userDataRepository);
        this.SAVE_SERVICE = new SaveService(saveRepository, SANDBOX_SERVICE);
	}

	public Result<UserConfiguration> loadStartupData() {

		//No longer necessary for the database implementation.
//		if (userConfigurationResult.isSuccess()) {
//			for (ModListProfile modListProfile : userConfigurationResult.getPayload().getModListProfiles()) {
//				for (int i = 0; i < modListProfile.getModList().size(); i++) {
//					modListProfile.getModList().get(i).setLoadPriority(i + 1);
//				}
//			}
//		}

		return USER_DATA_SERVICE.loadStartupData();
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
			saveResult = USER_DATA_SERVICE.saveUserData(sortUserConfigurationModLists(userConfiguration));
		} catch (IOException e) {
			saveResult.addMessage(e.toString(), ResultType.FAILED);
		}
		return saveResult;
	}
	
	//TODO: Space Engineers version checking. Right now "getSessionName" is setup for only SE1.
	public Result<SaveProfile> getSpaceEngineersOneSaveProfile(File sandboxConfigFile) throws IOException {
		Result<SaveProfile> saveProfileResult = new Result<>();
		Result<String> sandboxFileResult = SANDBOX_SERVICE.getSandboxFromFile(sandboxConfigFile);
		if (!sandboxFileResult.isSuccess()) {
			saveProfileResult.addMessage(sandboxFileResult.getCurrentMessage(), sandboxFileResult.getType());
			return saveProfileResult;
		}

		String sandboxConfig = sandboxFileResult.getPayload();

		//Technically, the name of the save the game reads is in Sandbox.sbc, not Sandbox_config.sbc.
		// But when you rename a save in the game it changes both files, so this is probably fine since you can't rename it any other way than through manual modification.
		SaveProfile saveProfile = new SaveProfile(sandboxConfigFile, SpaceEngineersVersion.SPACE_ENGINEERS_ONE);
		saveProfile.setSaveName(SAVE_SERVICE.getSessionName(sandboxConfig, sandboxConfigFile.getPath()));

		saveProfileResult.setPayload(saveProfile);
		saveProfileResult.addMessage("Successfully loaded save: " + saveProfile.getSaveName(), ResultType.SUCCESS);
		return saveProfileResult;
	}

	public Result<String> getSaveName(File sandboxConfigFile) throws IOException {
		Result<String> sandboxfileResult = SANDBOX_SERVICE.getSandboxFromFile(sandboxConfigFile);
		if (sandboxfileResult.isSuccess()) {
			sandboxfileResult.setPayload(SAVE_SERVICE.getSessionName(sandboxfileResult.getPayload(), sandboxConfigFile.getPath()));
		}
		return sandboxfileResult;
	}
	
	public Result<SaveProfile> copySaveProfile(SaveProfile sourceSaveProfile) throws IOException {
		Result<SaveProfile> copyResult = SAVE_SERVICE.copySaveFiles(sourceSaveProfile);
		if (copyResult.isSuccess()) {
			copyResult.addMessage("Successfully copied save \"" + sourceSaveProfile.getProfileName() + "\".", ResultType.SUCCESS);
		}
		return copyResult;
	}

	public Result<List<Mod>> getModlistFromSave(File sandboxConfigFile) {
		return SANDBOX_SERVICE.getModListFromSandboxConfig(sandboxConfigFile);
	}

	
	public Result<Void> exportModlist(ModListProfile modListProfile, File saveLocation) {
		return USER_DATA_SERVICE.exportModlist(modListProfile, saveLocation);
	}

	
	public Result<ModListProfile> importModlist(File saveLocation) {
		return USER_DATA_SERVICE.importModlist(saveLocation);
	}

	//Only here for development purposes
	public Result<Void> createTestUserData(Theme theme) {

		SaveProfile testSaveProfile = new SaveProfile("Test Profile", "./Storage/fake.sbc", SpaceEngineersVersion.SPACE_ENGINEERS_ONE);
		testSaveProfile.setSaveName("Test save");
		ModListProfile testModListProfile = new ModListProfile("Test Profile", SpaceEngineersVersion.SPACE_ENGINEERS_ONE);

		SteamMod testMod = new SteamMod("123456789");
		List<String> testCategories = new ArrayList<>();
		testCategories.add("Test Category");
		testCategories.add("Three Category test");
		testMod.setFriendlyName("Test Mod");
		testMod.setCategories(testCategories);
		testModListProfile.getModList().add(testMod);

		SteamMod secondTestMod = new SteamMod("0987654321");
		secondTestMod.setFriendlyName("Second test mod");
		secondTestMod.setCategories(testCategories);
		testModListProfile.getModList().add(secondTestMod);

		ModIoMod thirdTestMod = new ModIoMod("122122");
		thirdTestMod.setFriendlyName("Third test mod");
		thirdTestMod.setCategories(testCategories);
		testModListProfile.getModList().add(thirdTestMod);

		testSaveProfile.setLastUsedModProfileId(testModListProfile.getID());

		UserConfiguration userConfiguration = new UserConfiguration();
		userConfiguration.getSaveProfiles().removeFirst();
		userConfiguration.getSaveProfiles().add(testSaveProfile);
		userConfiguration.getModListProfilesBasicInfo().add(testModListProfile);
		userConfiguration.setUserTheme(theme.getName());

		System.out.println("Created test user data.");
		try {
			return USER_DATA_SERVICE.saveUserData(userConfiguration);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Result<Void> resetUserConfig() {
		Result<Void> userConfigResetResult = USER_DATA_SERVICE.resetUserConfig();
		if(userConfigResetResult.isSuccess()) {
            try {
                USER_DATA_SERVICE.saveUserData(new UserConfiguration());
				userConfigResetResult.addMessage("Successfully deleted existing user configuration and saved new one.", ResultType.SUCCESS);
            } catch (IOException e) {
                userConfigResetResult.addMessage(e.toString(), ResultType.FAILED);
            }
        }

		return userConfigResetResult;
	}

	private UserConfiguration sortUserConfigurationModLists(UserConfiguration userConfiguration) {
		UserConfiguration sortedUserConfiguration = new UserConfiguration(userConfiguration);
		for (ModListProfile m : sortedUserConfiguration.getModListProfilesBasicInfo()) {
			List<Mod> sortedModList = m.getModList().stream()
					.sorted(Comparator.comparing(Mod::getLoadPriority))
					.toList();
			m.setModList(sortedModList);
		}
		return sortedUserConfiguration;
	}
}
