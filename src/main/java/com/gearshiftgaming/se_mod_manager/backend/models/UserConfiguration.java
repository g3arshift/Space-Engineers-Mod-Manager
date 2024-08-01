package com.gearshiftgaming.se_mod_manager.backend.models;

import atlantafx.base.theme.PrimerLight;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Setter;

/**
 * Stores a users information, including their preferred theme, their mod profiles, and their save profiles.
 * @author Gear Shift
 * @version 1.0
 */

@Getter
@Setter
@XmlRootElement(name = "userConfiguration")
@XmlType(propOrder = {"userTheme", "lastUsedSaveProfileId", "saveProfiles", "modProfiles"})
public class UserConfiguration {

    private String userTheme;

    private UUID lastUsedSaveProfileId;

    private List<SaveProfile> saveProfiles;

    private List<ModProfile> modProfiles;

    /**
     * Creates an entirely new XML configuration file to store user information with.
     */
    public UserConfiguration() {
        this.saveProfiles = new ArrayList<>();
        this.modProfiles = new ArrayList<>();
        this.userTheme = new PrimerLight().getName();
    }

    public UserConfiguration(List<SaveProfile> saveProfiles, List<ModProfile> modProfiles, String userTheme) {
        this.saveProfiles = saveProfiles;
        this.modProfiles = modProfiles;
        this.userTheme = userTheme;
    }

    @XmlElement(name = "userTheme")
    public void setUserTheme(String userTheme) {
        this.userTheme = userTheme;
    }

    @XmlElementWrapper(name = "saveProfiles")
    @XmlElement(name = "saveProfile")
    public void setSaveProfiles(List<SaveProfile> saveProfiles) {
        this.saveProfiles = saveProfiles;
    }

    @XmlElementWrapper(name = "modProfiles")
    @XmlElement(name = "modProfile")
    public void setModProfiles(List<ModProfile> modProfiles) {
        this.modProfiles = modProfiles;
    }
}
