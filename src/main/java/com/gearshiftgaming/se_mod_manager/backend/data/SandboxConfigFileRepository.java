package com.gearshiftgaming.se_mod_manager.backend.data;

import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.ResultType;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SandboxConfigFileRepository implements SandboxConfigRepository {
    @Override
    public String getSandboxConfig(File sandboxFile) throws IOException {
        return Files.readString(sandboxFile.toPath());
    }

    @Override
    public void saveSandboxConfig(File sandboxFile, String modifiedSandboxConfig) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(sandboxFile))) {
            bw.write(modifiedSandboxConfig);
        }
    }
}
