package com.gearshiftgaming.se_mod_manager.controller;

import com.gearshiftgaming.se_mod_manager.backend.data.ModlistRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.UserDataRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.ModlistService;
import com.gearshiftgaming.se_mod_manager.backend.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.backend.domain.UserDataService;
import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.ResultType;
import jakarta.xml.bind.JAXBException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class BackendFileController implements BackendController {

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

    public Result<Boolean> applyModlist(List<Mod> modList, String sandboxConfigPath) throws IOException {
        Result<String> modifiedSandboxConfigResult = sandboxService.injectModsIntoSandboxConfig(new File(sandboxConfigPath), modList);
        if (modifiedSandboxConfigResult.isSuccess()) {
            return sandboxService.saveSandboxConfig(sandboxConfigPath, modifiedSandboxConfigResult.getPayload());
        } else {
            Result<Boolean> failedModification = new Result<>();
            failedModification.addMessage(modifiedSandboxConfigResult.getMessages().getLast(), ResultType.FAILED);
            return failedModification;
        }
    }

    public Result<Boolean> saveUserData(UserConfiguration userConfiguration, File userConfigurationFile) {
        return userDataService.saveUserData(userConfiguration, userConfigurationFile);
    }
}
