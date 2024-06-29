package com.gearshiftgaming.se_mod_manager.data;

import com.gearshiftgaming.se_mod_manager.models.utility.Result;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public interface SandboxConfigRepository {

    Result<File> getSandboxConfig(File sandboxConfigFile);

    Result<Boolean> saveSandboxConfig(Path savePath, String modifiedSandboxConfig) throws IOException;
}
