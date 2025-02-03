package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.UserDataRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.ModListProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.ResultType;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import jakarta.xml.bind.JAXBException;

import java.io.File;
import java.io.IOException;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.

 */
public record UserDataService(UserDataRepository userDataFileRepository) {

	public Result<UserConfiguration> getUserData(File userConfigurationFile) throws JAXBException {
		Result<UserConfiguration> result = new Result<>();

		if (!userConfigurationFile.exists()) {
			result.addMessage("User data was not found. Defaulting to new user configuration.", ResultType.FAILED);
			result.setPayload(new UserConfiguration());
		} else {
            result = userDataFileRepository.loadUserData(userConfigurationFile);
		}
		return result;
	}

	public Result<Void> saveUserData(UserConfiguration userConfiguration, File userConfigurationFile) throws IOException {
		Result<Void> result = new Result<>();
		if (userDataFileRepository().saveUserData(userConfiguration, userConfigurationFile)) {
			result.addMessage("Successfully saved user data.", ResultType.SUCCESS);
		} else {
			result.addMessage("Failed to save user data.", ResultType.FAILED);
		}
		return result;
	}

	public Result<Void> exportModlist(ModListProfile modListProfile, File saveLocation) {
		return userDataFileRepository.exportModlist(modListProfile, saveLocation);
	}

	public Result<ModListProfile> importModlist(File saveLocation) {
		return userDataFileRepository.importModlist(saveLocation);
	}

	public Result<Void> resetUserConfig(File userConfigFile) {
		return userDataFileRepository.resetUserConfiguration(userConfigFile);
	}
}
