package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.UserDataRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.ModListProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.ResultType;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import jakarta.xml.bind.JAXBException;

import java.io.File;
import java.io.IOException;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public record UserDataService(UserDataRepository userDataFileRepository) {

    public Result<UserConfiguration> getUserData() {
        return userDataFileRepository.loadUserData();
    }

    public Result<Void> saveUserData(UserConfiguration userConfiguration) throws IOException {
        return userDataFileRepository().saveUserData(userConfiguration);
    }

    public Result<Void> exportModlist(ModListProfile modListProfile, File saveLocation) {
        return userDataFileRepository.exportModlist(modListProfile, saveLocation);
    }

    public Result<ModListProfile> importModlist(File saveLocation) {
        return userDataFileRepository.importModlist(saveLocation);
    }

    public Result<Void> resetUserConfig() {
        return userDataFileRepository.resetUserConfiguration();
    }
}
