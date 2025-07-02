package com.gearshiftgaming.se_mod_manager.backend.models;

import java.util.UUID;

//TODO: There are a LOT of places we can use this instead of a normal ModProfile. Find and replace them before release to prevent unwanted mutation.
/**
 * Interface for save profiles to allow for what is functionally read only version of SaveProfiles where they are needed.
 * <p>
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public interface SaveProfileInfo {
    UUID getId();

    String getProfileName();

    String getSaveName();

    String getSavePath();

    UUID getLastUsedModListProfileId();

    SaveStatus getLastSaveStatus();

    String getLastSaved();

    boolean isSaveExists();

    SpaceEngineersVersion getSpaceEngineersVersion();

    SaveType getSaveType();
}
