package com.gearshiftgaming.se_mod_manager.backend.data;

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

    List<String> getSteamModList(File modListFile) throws IOException;

    List<String> getModIoModUrls(File modListFile) throws IOException;
}
