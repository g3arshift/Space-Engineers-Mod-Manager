package com.gearshiftgaming.se_mod_manager.data;

import com.gearshiftgaming.se_mod_manager.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.models.utility.ResultType;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class SandboxConfigFileRepository implements SandboxConfigRepository {
    @Override
    public Result<File> getSandboxConfig(String sandboxConfigFilePath) {
        File sandboxConfigFile = new File(sandboxConfigFilePath);
        Result<File> result = new Result<>();
        if (!sandboxConfigFile.exists()) {
            result.addMessage("File does not exist.", ResultType.INVALID);
        } else if (FilenameUtils.getExtension(sandboxConfigFile.getName()).equals("sbc")) {
            result.addMessage(sandboxConfigFile.getName() + " selected.", ResultType.SUCCESS);
            result.setPayload(sandboxConfigFile);
        } else {
            result.addMessage("Incorrect file type selected. Please select a .sbc file.", ResultType.INVALID);
        }
        return result;
    }

    @Override
    public Result<Boolean> saveSandboxConfig(String savePath, String modifiedSandboxConfig) {
        Result<Boolean> result = new Result<>();

        if (!FilenameUtils.getExtension(savePath).equals("sbc")) {
            savePath = FilenameUtils.removeExtension(savePath);
            savePath = savePath + ".sbc";
        }

        try {
            File sandboxFile = new File(savePath);
            sandboxFile.getCanonicalPath();
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(sandboxFile))) {
                bw.write(modifiedSandboxConfig);
                result.addMessage("Successfully injected mod list into save", ResultType.SUCCESS);
            }
        } catch (IOException e) {
            result.addMessage("File path or name contains invalid characters.", ResultType.FAILED);
        }
        return result;
    }
}
