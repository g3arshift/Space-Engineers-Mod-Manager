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
public class SaveProfile {

    private final UUID id;

    @Setter
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

    public SaveProfile(){
        id = UUID.randomUUID();
        this.saveName = "None";
        this.lastModifiedBy = ModlistChangeSourceType.NOT_MODIFIED;
        this.lastSaveStatus = SaveStatus.NONE;
    }

    public SaveProfile(String savePath) {
        id = UUID.randomUUID();
        this.lastModifiedBy = ModlistChangeSourceType.NOT_MODIFIED;
        this.lastSaveStatus = SaveStatus.NONE;
        this.savePath = savePath;
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
