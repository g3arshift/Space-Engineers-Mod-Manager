package com.gearshiftgaming.se_mod_manager.backend.models;

import jakarta.xml.bind.annotation.XmlAttribute;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;


/**
 * This represents a single Space Engineers save within SEMM.
 * @author Gear Shift
 * @version 1.0.1
 */
@Getter
public class SaveProfile {

    private final UUID id;

    private String profileName;

    private String saveName;

    @Setter
    private String savePath;

    @Setter
    private UUID lastAppliedModProfileId;

    @Setter
    private ModlistChangeSourceType lastModifiedBy;

    @Setter
    private SaveStatus lastSaveStatus;

    @XmlAttribute
    private String lastSaved;

    private boolean saveExists;

    //This represents our base save profile that only exists when the application is launched for the first time.
    public SaveProfile(){
        id = UUID.randomUUID();
        this.profileName = "None";
        this.saveName = "None";
        this.lastModifiedBy = ModlistChangeSourceType.NOT_MODIFIED;
        this.lastSaveStatus = SaveStatus.NONE;
        saveExists = false;
    }

    public SaveProfile(String profileName, String savePath) {
        id = UUID.randomUUID();
        this.profileName = profileName;
        this.lastModifiedBy = ModlistChangeSourceType.NOT_MODIFIED;
        this.lastSaveStatus = SaveStatus.NONE;
        this.savePath = savePath;
        saveExists = true;
    }

    public SaveProfile(File saveFile) {
        id = UUID.randomUUID();
        this.profileName = "Default";
        this.lastModifiedBy = ModlistChangeSourceType.NOT_MODIFIED;
        this.lastSaveStatus = SaveStatus.NONE;
        this.savePath = saveFile.getPath();
        saveExists = true;
    }

    public SaveProfile(SaveProfile saveProfile) {
        id = UUID.randomUUID();
        this.profileName = saveProfile.getProfileName();
        this.saveName = saveProfile.getSaveName();
        this.savePath = saveProfile.getSavePath();
        this.lastAppliedModProfileId = saveProfile.getLastAppliedModProfileId();
        this.lastModifiedBy = saveProfile.getLastModifiedBy();
        this.lastSaveStatus = saveProfile.getLastSaveStatus();
        this.lastSaved = saveProfile.getLastSaved();
        this.saveExists = saveProfile.isSaveExists();
    }

    public void setLastAppliedModProfileId(UUID lastAppliedModProfileId) {
        this.lastAppliedModProfileId = lastAppliedModProfileId;
        lastSaved = getCurrentTime();
    }

    @XmlAttribute
    public void setProfileName(String profileName) {
        this.profileName = profileName;
        lastSaved = getCurrentTime();
    }

    public void setSaveName(String saveName) {
        this.saveName = saveName;
        lastSaved = getCurrentTime();
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm a"));
    }

    @XmlAttribute
    public void setSaveExists(boolean saveExists) {
        this.saveExists = saveExists;
    }
}
