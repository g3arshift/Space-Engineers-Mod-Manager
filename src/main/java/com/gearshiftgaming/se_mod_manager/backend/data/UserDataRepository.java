package com.gearshiftgaming.se_mod_manager.backend.data;

import com.gearshiftgaming.se_mod_manager.backend.models.ModListProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
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
public interface UserDataRepository {
    Result<UserConfiguration> loadUserData();

    Result<Void> saveUserData(UserConfiguration userConfiguration);

    Result<Void> exportModlist(ModListProfile modListProfile, File modlistLocation);

    Result<ModListProfile> importModlist(File modlistLocation);

    Result<Void> resetUserConfiguration();
}
