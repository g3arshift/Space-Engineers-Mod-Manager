package com.gearshiftgaming.se_mod_manager.backend.models;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

import java.util.*;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.

 */
@Getter
@XmlRootElement(name = "modlistProfile")
public class ModListProfile {

    @XmlElement
    private final UUID ID;

    private String profileName;

    private List<Mod> modList = new ArrayList<>();

    @XmlElement
    private final SpaceEngineersVersion SPACE_ENGINEERS_VERSION;

    private  HashMap<String, List<Mod>> conflictTable = new HashMap<>();

    public ModListProfile() {
        ID = UUID.randomUUID();
        profileName = "New Mod Profile";
        SPACE_ENGINEERS_VERSION = SpaceEngineersVersion.SPACE_ENGINEERS_ONE;
    }

    public ModListProfile(SpaceEngineersVersion spaceEngineersVersion) {
        ID = UUID.randomUUID();
        profileName = "New Mod Profile";
        this.SPACE_ENGINEERS_VERSION = spaceEngineersVersion;
    }

    public ModListProfile(String profileName, SpaceEngineersVersion spaceEngineersVersion) {
        this.SPACE_ENGINEERS_VERSION = spaceEngineersVersion;
        ID = UUID.randomUUID();
        this.profileName = profileName;
    }

    public ModListProfile(ModListProfile modListProfile) {
        this.ID = UUID.randomUUID();
        this.profileName = modListProfile.getProfileName();
        if(modListProfile.getModList() != null){
            for(Mod m : modListProfile.getModList()) {
                if(m instanceof SteamMod) {
                    this.modList.add(new SteamMod((SteamMod) m));
                } else {
                    this.modList.add(new ModIoMod((ModIoMod) m));
                }
            }
        }
        SPACE_ENGINEERS_VERSION = modListProfile.getSPACE_ENGINEERS_VERSION();
        conflictTable = modListProfile.getConflictTable();
    }

    @XmlAttribute
    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

   @XmlElementWrapper(name = "mods")
   @XmlElement(name = "mod")
    public void setModList(List<Mod> modList) {
        this.modList = modList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModListProfile that)) return false;
		return Objects.equals(ID, that.ID) && Objects.equals(profileName, that.profileName) && Objects.equals(modList, that.modList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ID, profileName, modList);
    }
}
