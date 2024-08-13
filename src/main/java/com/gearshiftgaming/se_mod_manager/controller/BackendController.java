package com.gearshiftgaming.se_mod_manager.controller;

import atlantafx.base.theme.Theme;
import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import jakarta.xml.bind.JAXBException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface BackendController {
    Result<UserConfiguration> getUserData() throws JAXBException;

    Result<Boolean> applyModlist(List<Mod> modList, String sandboxConfigPath) throws IOException;

    Result<Boolean> saveUserData(UserConfiguration userConfiguration);

    Result<SaveProfile> getSaveProfile(File sandboxConfig) throws IOException;

    public Result<Boolean> createTestUserData(Theme theme);
}
