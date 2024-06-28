package com.gearshiftgaming.se_mod_manager.data;

import com.gearshiftgaming.se_mod_manager.models.utility.Result;

import java.io.File;
import java.io.IOException;

public interface SandboxConfigRepository {

    Result<File> getAll(File sandboxConfigFile);

    Result<File> getSandboxConfig(File sandBoxConfigFile);


    Result<Boolean> saveSandboxConfig(File sandboxConfig, String modifiedSandboxConfig) throws IOException;
}
