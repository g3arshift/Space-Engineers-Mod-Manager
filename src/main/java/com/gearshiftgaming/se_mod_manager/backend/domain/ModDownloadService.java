package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfileInfo;

import java.io.IOException;

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

    public Result<String> getModLocation(String modId, SaveProfileInfo saveProfileInfo);

    public boolean shouldUpdateMod(String modId, int remoteFileSize, SaveProfileInfo saveProfileInfo);
}
