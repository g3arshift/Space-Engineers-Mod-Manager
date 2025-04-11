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

    public Result<UserConfiguration> loadStartupData() {
        return userDataRepository.loadStartupData();
    }

    public Result<Void> saveUserConfiguration(UserConfiguration userConfiguration) {
        return userDataRepository.saveUserConfiguration(userConfiguration);
    }

    public Result<ModListProfile> loadModListProfileByName(String profileName) {
        return userDataRepository.loadModListProfileByName(profileName);
    }

    public Result<Void> saveCurrentData(UserConfiguration userConfiguration, ModListProfile modListProfile, SaveProfile saveProfile) {
        return userDataRepository.saveCurrentData(userConfiguration, modListProfile, saveProfile);
    }

    public Result<ModListProfile> loadFirstModListProfile() {
        return userDataRepository.loadFirstModListProfile();
    }

    public Result<ModListProfile> loadModListProfileById(UUID modListProfileId) {
        return userDataRepository.loadModListProfileById(modListProfileId);
    }

    public Result<Void> saveModListProfileDetails(ModListProfile modListProfile) {
        return userDataRepository.saveModListProfileDetails(, modListProfile, , );
    }

    public Result<Void> saveModListProfile(ModListProfile modListProfile) {
        return userDataRepository.saveModListProfile(modListProfile);
    }

    public Result<Void> deleteModListProfile(ModListProfile modListProfile) {
        return userDataRepository.deleteModListProfile(modListProfile);
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

    public Result<ModListProfile> importModListProfile(File modlistLocation) {
        return userDataRepository.importModListProfile(modlistLocation);
    }

    public Result<Void> exportModListProfile(ModListProfile modListProfile, File modlistLocation) {
        return userDataRepository.exportModListProfile(modListProfile, modlistLocation);
    }

    public Result<Void> exportModlist(ModListProfile modListProfile, File saveLocation) {
        return userDataRepository.exportModListProfile(modListProfile, saveLocation);
    }

    public Result<ModListProfile> importModlist(File saveLocation) {
        return userDataRepository.importModListProfile(saveLocation);
    }
}
