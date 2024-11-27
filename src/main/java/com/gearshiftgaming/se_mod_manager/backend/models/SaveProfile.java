package com.gearshiftgaming.se_mod_manager.backend.models;

import jakarta.xml.bind.annotation.XmlAttribute;
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
public class SaveProfile {

    private final UUID ID;

    private String profileName;

    @Setter
    private String saveName;

    @Setter
    private String savePath;

    private UUID lastUsedModProfile;

    @Setter
    private ModlistChangeSourceType lastModifiedBy;

    @Setter
    private SaveStatus lastSaveStatus;

    @XmlAttribute
    private String lastSaved;

    private boolean saveExists;

    //This represents our base save profile that only exists when the application is launched for the first time.
    public SaveProfile(){
        ID = UUID.randomUUID();
        this.profileName = "None";
        this.saveName = "None";
        this.lastModifiedBy = ModlistChangeSourceType.NOT_MODIFIED;
        this.lastSaveStatus = SaveStatus.NONE;
        saveExists = false;
    }

    public SaveProfile(String profileName, String savePath) {
        ID = UUID.randomUUID();
        this.profileName = profileName;
        this.lastModifiedBy = ModlistChangeSourceType.NOT_MODIFIED;
        this.lastSaveStatus = SaveStatus.NONE;
        this.savePath = savePath;
        saveExists = true;
    }

    public SaveProfile(File saveFile) {
        ID = UUID.randomUUID();
        this.profileName = "Default";
        this.lastModifiedBy = ModlistChangeSourceType.NOT_MODIFIED;
        this.lastSaveStatus = SaveStatus.NONE;
        this.savePath = saveFile.getPath();
        saveExists = true;
    }

    public SaveProfile(SaveProfile saveProfile) {
        ID = UUID.randomUUID();
        this.profileName = saveProfile.getProfileName();
        this.saveName = saveProfile.getSaveName();
        this.savePath = saveProfile.getSavePath();
        this.lastUsedModProfile = saveProfile.getLastUsedModProfile();
        this.lastModifiedBy = saveProfile.getLastModifiedBy();
        this.lastSaveStatus = saveProfile.getLastSaveStatus();
        this.lastSaved = saveProfile.getLastSaved();
        this.saveExists = saveProfile.isSaveExists();
    }

    public void setLastUsedModProfile(UUID lastUsedModProfile) {
        this.lastUsedModProfile = lastUsedModProfile;
        lastSaved = getCurrentTime();
    }

    @XmlAttribute
    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm a"));
    }

    @XmlAttribute
    public void setSaveExists(boolean saveExists) {
        this.saveExists = saveExists;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SaveProfile that)) return false;
		return saveExists == that.saveExists && Objects.equals(ID, that.ID) && Objects.equals(profileName, that.profileName) && Objects.equals(saveName, that.saveName) && Objects.equals(savePath, that.savePath) && Objects.equals(lastUsedModProfile, that.lastUsedModProfile) && lastModifiedBy == that.lastModifiedBy && lastSaveStatus == that.lastSaveStatus && Objects.equals(lastSaved, that.lastSaved);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ID, profileName, saveName, savePath, lastUsedModProfile, lastModifiedBy, lastSaveStatus, lastSaved, saveExists);
    }
}
