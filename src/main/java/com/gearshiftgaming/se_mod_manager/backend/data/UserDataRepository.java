package com.gearshiftgaming.se_mod_manager.backend.data;

import com.gearshiftgaming.se_mod_manager.backend.models.ModlistProfile;
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
    Result<UserConfiguration> loadUserData(File userConfigurationFile) throws JAXBException;

    boolean saveUserData(UserConfiguration userConfiguration, File userConfigurationFile) throws IOException;

    Result<Void> exportModlist(ModlistProfile modlistProfile, File modlistLocation);

    Result<ModlistProfile> importModlist(File modlistLocation);
}
