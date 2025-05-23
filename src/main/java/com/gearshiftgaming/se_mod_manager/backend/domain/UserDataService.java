package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.UserDataRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.*;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public record UserDataService(UserDataRepository userDataRepository) {
    public Result<Void> saveCurrentData(UserConfiguration userConfiguration, ModListProfile modListProfile, SaveProfile saveProfile) {
        return userDataRepository.saveCurrentData(userConfiguration, modListProfile, saveProfile);
    }

    public Result<Void> initializeData() {
        return userDataRepository.initializeData();
    }

    public Result<UserConfiguration> loadStartupData() {
        return userDataRepository.loadStartupData();
    }

    public Result<Void> saveUserConfiguration(UserConfiguration userConfiguration) {
        return userDataRepository.saveUserConfiguration(userConfiguration);
    }

    public Result<ModListProfile> loadFirstModListProfile() {
        return userDataRepository.loadFirstModListProfile();
    }

    public Result<ModListProfile> loadModListProfileById(UUID modListProfileId) {
        return userDataRepository.loadModListProfileById(modListProfileId);
    }

    public Result<Void> saveModListProfileDetails(UUID modListProfileId, String modListProfileName, SpaceEngineersVersion spaceEngineersVersion) {
        return userDataRepository.saveModListProfileDetails(modListProfileId, modListProfileName, spaceEngineersVersion);
    }

    public Result<Void> saveModListProfile(ModListProfile modListProfile) {
        return userDataRepository.saveModListProfile(modListProfile);
    }

    public Result<Void> deleteModListProfile(UUID modListProfileId) {
        return userDataRepository.deleteModListProfile(modListProfileId);
    }

    public Result<Void> updateModListProfileModList(UUID modListProfileId, List<Mod> modList) {
        return userDataRepository.updateModListProfileModList(modListProfileId, modList);
    }

    public Result<Void> updateModListActiveMods(UUID modListProfileId, List<Mod> modList) {
        return userDataRepository.updateModListActiveMods(modListProfileId, modList);
    }

    public Result<Void> updateModListLoadPriority(UUID modListProfileId, List<Mod> modList) {
        return userDataRepository.updateModListLoadPriority(modListProfileId, modList);
    }

    public Result<Void> saveSaveProfile(SaveProfile saveProfile) {
        return userDataRepository.saveSaveProfile(saveProfile);
    }

    public Result<Void> resetData() {
        return userDataRepository.resetData();
    }

    public Result<Void> deleteSaveProfile(SaveProfile saveProfile) {
        return userDataRepository.deleteSaveProfile(saveProfile);
    }

    public Result<Void> updateModInformation(List<Mod> modList) {
        return userDataRepository.updateModInformation(modList);
    }

    public Result<Void> exportModlistProfile(ModListProfile modListProfile, File saveLocation) {
        return userDataRepository.exportModListProfile(modListProfile, saveLocation);
    }

    public Result<ModListProfile> importModlistProfile(File saveLocation) {
        return userDataRepository.importModListProfile(saveLocation);
    }
}
