package com.gearshiftgaming.se_mod_manager.data;

import com.gearshiftgaming.se_mod_manager.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.models.utility.ResultType;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class SandboxConfigFileRepository implements SandboxConfigRepository{
    @Override
    public Result<File> getSandboxConfig(File sandboxConfigFile) {
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

    @Override
    public Result<Boolean> saveSandboxConfig(Path savePath, String modifiedSandboxConfig) throws IOException {
        Result<Boolean> result = new Result<>();

        if(!FilenameUtils.getExtension(savePath.getFileName().toString()).equals("sbc")) {
            savePath = Path.of(savePath + ".sbc");
        }

        if(!savePath.toFile().exists()) {
            boolean fileCreationResult = (savePath.toFile().createNewFile());
            if(!fileCreationResult){
                result.addMessage("Failed to create modified Sandbox_config.sbc file.", ResultType.FAILED);
                return result;
            }
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(savePath.toFile()))) {
            bw.write(modifiedSandboxConfig);
        }
        result.addMessage("Successfully injected mod list into save", ResultType.SUCCESS);
        return result;
    }
}
