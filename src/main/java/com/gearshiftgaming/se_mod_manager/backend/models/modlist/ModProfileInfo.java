package com.gearshiftgaming.se_mod_manager.backend.models.modlist;

import com.gearshiftgaming.se_mod_manager.backend.models.shared.SpaceEngineersVersion;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

//TODO: There are a LOT of places we can use this instead of a normal ModProfile. Find and replace them before release to prevent unwanted mutation.
/**
 * Interface for save profiles to allow for what is functionally read only version of ModProfiles where they are needed.
 * <p>
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public interface ModProfileInfo {
    UUID getId();

    String getProfileName();

    List<Mod> getModList();

    SpaceEngineersVersion getSpaceEngineersVersion();

    HashMap<String, List<Mod>> getConflictTable();
}
