package com.gearshiftgaming.se_mod_manager.backend.data.user;

import com.gearshiftgaming.se_mod_manager.backend.models.mod.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.modlist.ModListProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.save.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.SpaceEngineersVersion;
import com.gearshiftgaming.se_mod_manager.backend.models.user.UserConfiguration;

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
    Result<Void> saveCurrentData(UserConfiguration userConfiguration, ModListProfile modListProfile, SaveProfile saveProfile);

    Result<Void> initializeData();

    Result<UserConfiguration> loadStartupData();

    Result<Void> saveUserConfiguration(UserConfiguration userConfiguration);

    Result<ModListProfile> loadFirstModListProfile();

    Result<ModListProfile> loadModListProfileByName(String profileName);

    Result<ModListProfile> loadModListProfileById(UUID modListProfileId);

    Result<Void> saveModListProfileDetails(UUID modListProfileId, String modListProfileName, SpaceEngineersVersion spaceEngineersVersion);

    Result<Void> saveModListProfile(ModListProfile modListProfile);

    Result<Void> deleteModListProfile(UUID modListProfileId);

    Result<Void> updateModListProfileModList(UUID modListProfileId, List<Mod> modList);

    Result<Void> updateModListActiveMods(UUID modListProfileId, List<Mod> modList);

    Result<Void> updateModListLoadPriority(UUID modListProfileId, List<Mod> modList);

    Result<Void> saveSaveProfile(SaveProfile saveProfile);

    Result<Void> deleteSaveProfile(SaveProfile saveProfile);

    Result<Void> updateModInformation(List<Mod> modList);

    Result<Void> exportModListProfile(ModListProfile modListProfile, File modlistLocation);

    Result<ModListProfile> importModListProfile(File modlistLocation);

    Result<Void> resetData();
}
