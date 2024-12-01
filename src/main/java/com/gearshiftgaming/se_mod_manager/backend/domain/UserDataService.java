package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.UserDataRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.ResultType;
import jakarta.xml.bind.JAXBException;

import java.io.File;

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
			result.addMessage("Could not load user data. Defaulting to new user configuration.", ResultType.FAILED);
			result.setPayload(new UserConfiguration());
		} else {
            result.setPayload(userDataFileRepository.loadUserData(userConfigurationFile));
			result.addMessage("Successfully loaded user data.", ResultType.SUCCESS);
		}
		return result;
	}

	public Result<Void> saveUserData(UserConfiguration userConfiguration, File userConfigurationFile) {
		Result<Void> result = new Result<>();
		if (userDataFileRepository().saveUserData(userConfiguration, userConfigurationFile)) {
			result.addMessage("Successfully saved user data.", ResultType.SUCCESS);
		} else {
			result.addMessage("Failed to save user data.", ResultType.FAILED);
		}
		return result;
	}


}
