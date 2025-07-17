package com.gearshiftgaming.se_mod_manager.backend.domain.mod;

import com.gearshiftgaming.se_mod_manager.backend.models.shared.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.save.SaveProfileInfo;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public interface ModDownloadService {

    public Result<Void> downloadMod(String modId, SaveProfileInfo saveProfile) throws IOException, InterruptedException;

    public boolean isModDownloaded(String modId, SaveProfileInfo saveProfileInfo) throws IOException;

    public Result<Path> getModPath(String modId, SaveProfileInfo saveProfileInfo) throws IOException;

    public boolean shouldUpdateMod(String modId, int remoteFileSize, SaveProfileInfo saveProfileInfo);
}
