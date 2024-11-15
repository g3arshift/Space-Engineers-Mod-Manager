package com.gearshiftgaming.se_mod_manager.backend.data;

import com.gearshiftgaming.se_mod_manager.backend.models.Mod;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.

 */
public interface ModlistRepository {

    //TODO: Shouldn't be a file.
    List<Mod> getSteamModList(File modListFile) throws IOException;
}
