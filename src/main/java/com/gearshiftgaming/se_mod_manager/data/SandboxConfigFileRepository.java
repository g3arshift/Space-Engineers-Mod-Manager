package com.gearshiftgaming.se_mod_manager.data;

import com.gearshiftgaming.se_mod_manager.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.models.utility.ResultType;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SandboxConfigFileRepository implements SandboxConfigRepository{
    @Override
    public Result<File> getAll(File sandboxConfigFile) {
        Result<File> result = new Result<>();
        if (!sandboxConfigFile.exists()) {
            result.addMessage("File does not exist.", ResultType.INVALID);
        } else if (FilenameUtils.getExtension(sandboxConfigFile.getName()).equals("sbc")) {
            result.addMessage(sandboxConfigFile.getName() + " selected.", ResultType.SUCCESS);
            result.setPayload(sandboxConfigFile);
        } else {
            result.addMessage("Incorrect file type selected. Please select a .txt or .doc file.", ResultType.INVALID);
        }
        return result;
    }

    //TODO: Implement
    @Override
    public Result getSandboxConfig(File sandBoxConfigFile) {
        return null;
    }

    @Override
    public Result<Boolean> saveSandboxConfig(File sandboxConfig, String modifiedSandboxConfig) throws IOException {
        Result<Boolean> result = new Result<>();
        String savePath;

        if(!FilenameUtils.getExtension(sandboxConfig.getName()).equals("sbc")) {
            savePath = sandboxConfig.getPath() + ".sbc";
        } else savePath = sandboxConfig.getPath();

        File file = new File(savePath);

        if(!sandboxConfig.exists()) {
            boolean fileCreationResult = (sandboxConfig.createNewFile());
            if(!fileCreationResult){
                result.addMessage("Failed to create modified Sandbox_config.sbc file.", ResultType.FAILED);
                return result;
            }
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(sandboxConfig))) {
            bw.write(modifiedSandboxConfig);
        }
        return result;
    }
}
