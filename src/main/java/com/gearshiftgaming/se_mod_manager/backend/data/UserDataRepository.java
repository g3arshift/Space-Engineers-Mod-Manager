package com.gearshiftgaming.se_mod_manager.backend.data;

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
public interface UserDataRepository {
    public Result<Void> saveCurrentData(UserConfiguration userConfiguration, ModListProfile modListProfile, SaveProfile saveProfile);
    
    public Result<Void> initializeData();

    public Result<UserConfiguration> loadStartupData();

    public Result<Void> saveUserConfiguration(UserConfiguration userConfiguration);

    public Result<ModListProfile> loadFirstModListProfile();

    public Result<ModListProfile> loadModListProfileByName(String profileName);

    public Result<ModListProfile> loadModListProfileById(UUID modListProfileId);

    public Result<Void> saveModListProfileDetails(UUID modListProfileId, String modListProfileName, SpaceEngineersVersion spaceEngineersVersion);

    public Result<Void> saveModListProfile(ModListProfile modListProfile);

    public Result<Void> deleteModListProfile(UUID modListProfileId);

    public Result<Void> updateModListProfileModList(UUID modListProfileId, List<Mod> modList);

    public Result<Void> updateModListActiveMods(UUID modListProfileId, List<Mod> modList);

    public Result<Void> updateModListLoadPriority(UUID modListProfileId, List<Mod> modList);

    public Result<Void> saveSaveProfile(SaveProfile saveProfile);

    public Result<Void> deleteSaveProfile(SaveProfile saveProfile);

    public Result<Void> updateModInformation(List<Mod> modList);

    public Result<Void> exportModListProfile(ModListProfile modListProfile, File modlistLocation);

    public Result<ModListProfile> importModListProfile(File modlistLocation);

    Result<Void> resetData();
}
