package com.gearshiftgaming.se_mod_manager.data;

import com.gearshiftgaming.se_mod_manager.models.utility.Result;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public interface SandboxConfigRepository {

    Result<File> getSandboxConfig(String sandboxConfigPath);

    Result<Boolean> saveSandboxConfig(String savePath, String modifiedSandboxConfig) throws IOException;
}
