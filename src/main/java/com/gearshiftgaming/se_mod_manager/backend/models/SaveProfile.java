package com.gearshiftgaming.se_mod_manager.backend.models;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;


/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
@Getter
@AllArgsConstructor
public class SaveProfile {

    @XmlElement
    private final UUID ID;

    private String profileName;

    @Setter
    private String saveName;

    @Setter
    private String savePath;

    private UUID lastUsedModListProfileId;

    @Setter
    private SaveStatus lastSaveStatus;

    @XmlAttribute
    private String lastSaved;

    private boolean saveExists;

    @XmlElement
    private final SpaceEngineersVersion SPACE_ENGINEERS_VERSION;

    //This represents our base save profile that only exists when the application is launched for the first time.
    public SaveProfile(){
        ID = UUID.randomUUID();
        this.profileName = "None";
        this.saveName = "None";
        this.lastSaveStatus = SaveStatus.NONE;
        saveExists = false;
        SPACE_ENGINEERS_VERSION = SpaceEngineersVersion.SPACE_ENGINEERS_ONE;
    }

    public SaveProfile(String profileName, String savePath, String saveName, SpaceEngineersVersion spaceEngineersVersion) {
        ID = UUID.randomUUID();
        this.profileName = profileName;
        this.lastSaveStatus = SaveStatus.NONE;
        this.savePath = savePath;
        this.saveName = saveName;
        saveExists = true;
        this.SPACE_ENGINEERS_VERSION = spaceEngineersVersion;
    }

    public SaveProfile(File saveFile, SpaceEngineersVersion spaceEngineersVersion) {
        ID = UUID.randomUUID();
        this.profileName = "Default";
        this.lastSaveStatus = SaveStatus.NONE;
        this.savePath = saveFile.getPath();
        saveExists = true;
        this.SPACE_ENGINEERS_VERSION = spaceEngineersVersion;
    }

    public SaveProfile(SaveProfile saveProfile) {
        ID = UUID.randomUUID();
        this.profileName = saveProfile.getProfileName();
        this.saveName = saveProfile.getSaveName();
        this.savePath = saveProfile.getSavePath();
        this.lastUsedModListProfileId = saveProfile.getLastUsedModListProfileId();
        this.lastSaveStatus = saveProfile.getLastSaveStatus();
        this.lastSaved = saveProfile.getLastSaved();
        this.saveExists = saveProfile.isSaveExists();
        this.SPACE_ENGINEERS_VERSION = saveProfile.getSPACE_ENGINEERS_VERSION();
    }

    public void setLastUsedModListProfileId(UUID lastUsedModListProfileId) {
        this.lastUsedModListProfileId = lastUsedModListProfileId;
        lastSaved = getCurrentTime();
    }

    @XmlAttribute
    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d',' yyyy '@' h:mma"));
    }

    @XmlAttribute
    public void setSaveExists(boolean saveExists) {
        this.saveExists = saveExists;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SaveProfile that)) return false;
		return saveExists == that.saveExists && Objects.equals(ID, that.ID) && Objects.equals(profileName, that.profileName) && Objects.equals(saveName, that.saveName) && Objects.equals(savePath, that.savePath) && Objects.equals(lastUsedModListProfileId, that.lastUsedModListProfileId) && lastSaveStatus == that.lastSaveStatus && Objects.equals(lastSaved, that.lastSaved);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ID, profileName, saveName, savePath, lastUsedModListProfileId, lastSaveStatus, lastSaved, saveExists);
    }
}
