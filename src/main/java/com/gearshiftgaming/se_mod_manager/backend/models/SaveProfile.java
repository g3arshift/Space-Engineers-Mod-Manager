package com.gearshiftgaming.se_mod_manager.backend.models;

import jakarta.xml.bind.annotation.XmlAttribute;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;


/**
 * This represents a single Space Engineers save within SEMM.
 * @author Gear Shift
 * @version 1.0.1
 */
@Getter
@Setter
public class SaveProfile {

    private final UUID id;

    private String profileName;

    private String saveName;

    private String savePath;

    private UUID lastAppliedModProfileId;

    private ModlistChangeSourceType lastModifiedBy;

    private SaveStatus lastSaveStatus;

    @XmlAttribute
    private String lastSaved;

    public SaveProfile(){
        id = UUID.randomUUID();
        this.profileName = "New Save Profile";
        this.lastModifiedBy = ModlistChangeSourceType.NOT_MODIFIED;
        this.lastSaveStatus = SaveStatus.NONE;
    }

    public SaveProfile(String profileName) {
        id = UUID.randomUUID();
        this.profileName = profileName;
        this.lastModifiedBy = ModlistChangeSourceType.NOT_MODIFIED;
        this.lastSaveStatus = SaveStatus.NONE;
    }

    @XmlAttribute
    public void setProfileName(String profileName) {
        this.profileName = profileName;
        lastSaved = getCurrentTime();
    }

    public void setLastAppliedModProfileId(UUID lastAppliedModProfileId) {
        this.lastAppliedModProfileId = lastAppliedModProfileId;
        lastSaved = getCurrentTime();
    }

    public void setSaveName(String saveName) {
        this.saveName = saveName;
        lastSaved = getCurrentTime();
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm a"));
    }
}
