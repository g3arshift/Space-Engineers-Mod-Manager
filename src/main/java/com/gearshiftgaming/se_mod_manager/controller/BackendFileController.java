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
import java.util.List;
import java.util.Properties;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 * @author Gear Shift
 */
public class BackendFileController implements BackendController {

	private final SandboxService sandboxService;

	private final ModlistService modlistService;

	private final UserDataService userDataService;

	private final SaveService saveService;

	private final File userConfigurationFile;

	public BackendFileController(SandboxConfigRepository sandboxConfigRepository, ModlistRepository modlistRepository, UserDataRepository userDataRepository, SaveRepository saveRepository, Properties properties, File userConfigurationFile) {
		this.sandboxService = new SandboxService(sandboxConfigRepository);
		this.modlistService = new ModlistService(modlistRepository, properties);
		this.userDataService = new UserDataService(userDataRepository);
		this.userConfigurationFile = userConfigurationFile;
		this.saveService = new SaveService(saveRepository, sandboxService);
	}

	public Result<UserConfiguration> getUserData() throws JAXBException {
		return userDataService.getUserData(userConfigurationFile);
	}

	public Result<Void> applyModlist(List<Mod> modList, String sandboxConfigPath) throws IOException {
		Result<String> modifiedSandboxConfigResult = sandboxService.injectModsIntoSandboxConfig(new File(sandboxConfigPath), modList);
		if (modifiedSandboxConfigResult.isSuccess()) {
			return sandboxService.saveSandboxToFile(sandboxConfigPath, modifiedSandboxConfigResult.getPayload());
		} else {
			Result<Void> failedModification = new Result<>();
			failedModification.addMessage(modifiedSandboxConfigResult.getCurrentMessage(), ResultType.FAILED);
			return failedModification;
		}
	}

	public Result<Void> saveUserData(UserConfiguration userConfiguration) {
		return userDataService.saveUserData(userConfiguration, userConfigurationFile);
	}

	@Override
	public Result<SaveProfile> getSaveProfile(File sandboxConfigFile) throws IOException {
		Result<SaveProfile> saveProfileResult = new Result<>();
		Result<String> sandboxFileResult = sandboxService.getSandboxFromFile(sandboxConfigFile);
		if (!sandboxFileResult.isSuccess()) {
			saveProfileResult.addMessage(sandboxFileResult.getCurrentMessage(), sandboxFileResult.getType());
			return saveProfileResult;
		}

		String sandboxConfig = sandboxFileResult.getPayload();

		//Technically, the name of the save the game reads is in Sandbox.sbc, not Sandbox_config.sbc.
		// But when you rename a save in the game it changes both files, so this is probably fine.
		SaveProfile saveProfile = new SaveProfile(sandboxConfigFile);
		saveProfile.setSaveName(saveService.getSessionName(sandboxConfig, sandboxConfigFile.getPath()));

		saveProfileResult.setPayload(saveProfile);
		saveProfileResult.addMessage("Successfully loaded save: " + saveProfile.getSaveName(), ResultType.SUCCESS);
		return saveProfileResult;
	}

	@Override
	public Result<SaveProfile> copySaveProfile(SaveProfile sourceSaveProfile) throws IOException {
		Result<SaveProfile> copyResult = saveService.copySaveFiles(sourceSaveProfile);
		if (copyResult.isSuccess()) {
			copyResult.addMessage("Successfully copied save " + sourceSaveProfile.getProfileName() + ".", ResultType.SUCCESS);
		}
		return copyResult;
	}

	//Only here so we
	public Result<Void> createTestUserData(Theme theme) {

		ModProfile testModProfile = new ModProfile();
		Mod testMod = new SteamMod("123456789");
		List<String> testCategories = new ArrayList<>();
		testCategories.add("Test Category");
		testCategories.add("Three Category test");
		testMod.setCategories(testCategories);
		testModProfile.getModList().add(testMod);
		testModProfile.getModList().add(new SteamMod("4444444"));

		SaveProfile testSaveProfile = new SaveProfile();
		testSaveProfile.setSaveName("Test Save");
		testSaveProfile.setSavePath("Fake/Path");
		testSaveProfile.setLastAppliedModProfileId(testModProfile.getId());

		UserConfiguration userConfiguration = new UserConfiguration();
		userConfiguration.getSaveProfiles().add(testSaveProfile);
		userConfiguration.getModProfiles().add(testModProfile);
		userConfiguration.setUserTheme(theme.getName());

		return userDataService.saveUserData(userConfiguration, new File("./Storage/SEMM_TEST_Data.xml"));
	}
}
