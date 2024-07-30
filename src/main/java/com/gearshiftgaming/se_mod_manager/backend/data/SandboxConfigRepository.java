package com.gearshiftgaming.se_mod_manager.backend.data;

import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public interface SandboxConfigRepository {

    String getSandboxConfig(String sandboxConfigPath) throws IOException;

    void saveSandboxConfig(File sandboxFile, String modifiedSandboxConfig) throws IOException;
}
