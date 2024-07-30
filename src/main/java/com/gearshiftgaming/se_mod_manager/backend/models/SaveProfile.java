package com.gearshiftgaming.se_mod_manager.backend.models;

import jakarta.xml.bind.annotation.XmlAttribute;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


/**
 * This represents a single Space Engineers save within SEMM.
 * @author Gear Shift
 * @version 1.0
 */
@Getter
@Setter
public class SaveProfile {

    private String profileName;

    private String saveName;

    private String savePath;

    private UUID lastAppliedModProfile;

    private ModlistChangeSourceType lastModifiedBy;

    public SaveProfile(){
        this.profileName = "New Save Profile";
    }

    public SaveProfile(String profileName) {
        this.profileName = profileName;
    }

    @XmlAttribute
    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }
}
