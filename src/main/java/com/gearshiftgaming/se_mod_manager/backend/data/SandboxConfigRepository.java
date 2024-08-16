package com.gearshiftgaming.se_mod_manager.backend.data;

import java.io.File;
import java.io.IOException;

public interface SandboxConfigRepository {

    String getSandboxInfo(File sandboxConfig) throws IOException;

    void saveSandboxInfo(File sandboxFile, String modifiedSandboxConfig) throws IOException;
}
