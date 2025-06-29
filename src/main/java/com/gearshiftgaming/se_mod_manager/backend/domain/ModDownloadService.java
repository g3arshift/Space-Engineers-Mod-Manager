package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveType;

import java.util.List;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public interface ModDownloadService {

    public Result<String> downloadMod(String modId, SaveType saveType);

    public List<Result<String>> downloadModList(List<String> modIds);

    public boolean isModDownloaded(String modId);

    public String getModPath(String modId);
}
