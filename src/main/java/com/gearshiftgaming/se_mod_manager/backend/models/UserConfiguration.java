package com.gearshiftgaming.se_mod_manager.backend.models;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */

@Setter
@Getter
@XmlRootElement(name = "userConfiguration")
public class UserConfiguration {

    private String userTheme;

    private UUID lastModifiedSaveProfileId;

    private UUID lastActiveModProfileId;

    private UUID lastActiveSaveProfileId;

    private List<SaveProfile> saveProfiles;

    private List<Triple<UUID, String, SpaceEngineersVersion>> modListProfilesBasicInfo;

    private boolean runFirstTimeSetup;

    /**
     * Creates an entirely new XML configuration file to store user information with.
     */
    public UserConfiguration() {
        saveProfiles = new ArrayList<>();
        modListProfilesBasicInfo = new ArrayList<>();
        userTheme = "PrimerLight";

        //The save profile is actually useless here because it has no save path.
        saveProfiles.add(new SaveProfile());
        //TODO: Need to move this to UI service startup.
        ModListProfile modListProfile = new ModListProfile("Default", SpaceEngineersVersion.SPACE_ENGINEERS_ONE);
        modListProfilesBasicInfo.add(modListProfile);
        modListProfilesBasicInfo.add(Triple.of(modListProfile.getID(), modListProfile.getProfileName(), modListProfile.getSPACE_ENGINEERS_VERSION()));
        lastActiveModProfileId = modListProfile.getID();
        runFirstTimeSetup = true;
    }

    public UserConfiguration(UserConfiguration userConfiguration) {
        this.userTheme = userConfiguration.getUserTheme();
        this.lastModifiedSaveProfileId = userConfiguration.getLastModifiedSaveProfileId();
        this.saveProfiles = userConfiguration.getSaveProfiles();
        this.modListProfilesBasicInfo = userConfiguration.getModListProfilesBasicInfo();
        this.lastActiveModProfileId = userConfiguration.getLastActiveModProfileId();
        this.lastActiveSaveProfileId = userConfiguration.getLastActiveSaveProfileId();
        this.runFirstTimeSetup = userConfiguration.isRunFirstTimeSetup();
    }

    public UserConfiguration(String userTheme, UUID lastModifiedSaveProfileId, UUID lastActiveModProfileId, UUID lastActiveSaveProfileId, boolean runFirstTimeSetup) {
        this.userTheme = userTheme;
        this.lastModifiedSaveProfileId = lastModifiedSaveProfileId;
        this.lastActiveModProfileId = lastActiveModProfileId;
        this.lastActiveSaveProfileId = lastActiveSaveProfileId;
        this.saveProfiles = new ArrayList<>();
        this.modListProfilesBasicInfo = new ArrayList<>();
        this.runFirstTimeSetup = runFirstTimeSetup;
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

    @XmlElementWrapper(name = "modlistProfiles")
    @XmlElement(name = "modlistProfile")
    public void setModListProfilesBasicInfo(List<Triple<UUID, String, SpaceEngineersVersion>> modListProfilesBasicInfo) {
        this.modListProfilesBasicInfo = modListProfilesBasicInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserConfiguration that)) return false;
		return Objects.equals(userTheme, that.userTheme) && Objects.equals(lastModifiedSaveProfileId, that.lastModifiedSaveProfileId) && Objects.equals(saveProfiles, that.saveProfiles) && Objects.equals(modListProfilesBasicInfo, that.modListProfilesBasicInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userTheme, lastModifiedSaveProfileId, saveProfiles, modListProfilesBasicInfo);
    }
}
