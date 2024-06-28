package com.gearshiftgaming.se_mod_manager.domain;

import com.gearshiftgaming.se_mod_manager.data.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.data.SandboxConfigRepository;
import com.gearshiftgaming.se_mod_manager.models.Mod;
import com.gearshiftgaming.se_mod_manager.models.utility.Result;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class SandboxService {
    private final SandboxConfigRepository sandboxConfigFileRepository;

    public SandboxService(SandboxConfigFileRepository sandboxConfigRepository) {
        this.sandboxConfigFileRepository = sandboxConfigRepository;
    }

    public Result<File> getSandboxConfigFromFile(JFileChooser fc) {
        File sandboxConfig = fc.getSelectedFile();
        return sandboxConfigFileRepository.getAll(sandboxConfig);
    }

    public Result<Boolean> addModsToSandboxConfig(File sandboxConfig, List<Mod> modList) throws IOException {
        String modifiedSandboxConfig = injectModsIntoSandboxConfig(sandboxConfig, modList);
        Result<Boolean> result = sandboxConfigFileRepository.saveSandboxConfig(sandboxConfig, modifiedSandboxConfig);

        if()
        //TODO: Add a check for if the path the user wants to save the file to already has a file, and ask them if they want to overwrite
        //TODO: Inject the mods
        //TODO: Save the mods, and add the result here to the log, else log a success
        //TODO: Save the mods, and add the result here to the log, else log a success
    }

    private String injectModsIntoSandboxConfig(File sandboxConfig, List<Mod> modList) {

    }
}
