package com.gearshiftgaming.se_mod_manager.controller;

import atlantafx.base.theme.Theme;
import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import jakarta.xml.bind.JAXBException;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.

 */
public interface StorageController {
    Result<UserConfiguration> getUserData() throws JAXBException;

    Result<Void> applyModlist(List<Mod> modList, String sandboxConfigPath) throws IOException;

    Result<Void> saveUserData(UserConfiguration userConfiguration);

    Result<SaveProfile> getSaveProfile(File sandboxConfig) throws IOException;

    Result<SaveProfile> copySaveProfile(SaveProfile saveProfile) throws IOException;

    public Result<Void> createTestUserData(Theme theme);
}
