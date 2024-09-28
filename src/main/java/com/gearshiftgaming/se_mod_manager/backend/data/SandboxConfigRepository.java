package com.gearshiftgaming.se_mod_manager.backend.data;

import java.io.File;
import java.io.IOException;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 * @author Gear Shift
 */
public interface SandboxConfigRepository {

    String getSandboxInfo(File sandboxConfig) throws IOException;

    //TODO: Shouldn't be a file.
    void saveSandboxInfo(File sandboxFile, String modifiedSandboxConfig) throws IOException;
}
