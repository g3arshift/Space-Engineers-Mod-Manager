package com.gearshiftgaming.se_mod_manager.controller;

import com.gearshiftgaming.se_mod_manager.backend.data.ModlistRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.UserDataRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.ModlistService;
import com.gearshiftgaming.se_mod_manager.backend.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.backend.domain.UserDataService;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import jakarta.xml.bind.JAXBException;

import java.io.File;
import java.util.Properties;

public class BackendFileController implements BackendController{

    private final SandboxService sandboxService;

    private final ModlistService modlistService;

    private final UserDataService userDataService;

    public BackendFileController(SandboxConfigRepository sandboxConfigRepository, ModlistRepository modlistRepository, Properties properties, UserDataRepository userDataRepository) {
        this.sandboxService = new SandboxService(sandboxConfigRepository);
        this.modlistService = new ModlistService(modlistRepository, properties);
        this.userDataService = new UserDataService(userDataRepository);
    }

    public Result<UserConfiguration> getUserData(File userConfigurationFile) throws JAXBException {
        return userDataService.getUserData(userConfigurationFile);
    }
}
