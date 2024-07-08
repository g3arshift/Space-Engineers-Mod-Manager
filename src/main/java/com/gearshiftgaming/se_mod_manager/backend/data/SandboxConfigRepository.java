package com.gearshiftgaming.se_mod_manager.backend.data;

import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;

import java.io.File;
import java.io.IOException;

public interface SandboxConfigRepository {

    Result<File> getSandboxConfig(String sandboxConfigPath);

    Result<Boolean> saveSandboxConfig(String savePath, String modifiedSandboxConfig) throws IOException;
}
