package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.UserDataRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.ResultType;
import jakarta.xml.bind.JAXBException;

import java.io.File;

public record UserDataService(UserDataRepository userDataFileRepository) {

    public Result<UserConfiguration> getUserData(File userConfigurationFile) throws JAXBException {
        Result<UserConfiguration> result = new Result<>();

        if(!userConfigurationFile.exists()) {
            result.addMessage("Could not load user backend.data. Defaulting to new user configuration.", ResultType.FAILED);
            result.setPayload(new UserConfiguration());
        } else {
            result.setPayload(userDataFileRepository.loadUserData(userConfigurationFile));
            result.addMessage("Successfully loaded user backend.data.", ResultType.SUCCESS);
        }
        return result;
    }

    private Result<Boolean> saveUserData(UserConfiguration userConfiguration, File userConfigurationFile) {
        Result<Boolean> result = new Result<>();
        if(userDataFileRepository().saveUserData(userConfiguration, userConfigurationFile)) {
            result.addMessage("Successfully saved user backend.data.", ResultType.SUCCESS);
        } else {
            result.addMessage("Failed to save user backend.data.", ResultType.FAILED);
        }
        return result;
    }
}
