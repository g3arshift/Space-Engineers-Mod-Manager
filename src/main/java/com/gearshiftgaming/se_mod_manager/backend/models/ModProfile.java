package com.gearshiftgaming.se_mod_manager.backend.models;

import jakarta.xml.bind.annotation.*;
import lombok.Getter;

import java.util.*;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.

 */
@Getter
public class ModProfile {

    private final UUID ID;

    private String profileName;

    private List<Mod> modList;

    public ModProfile() {
        ID = UUID.randomUUID();
        modList = new ArrayList<>();
        //TODO: Add increment if duplicate.
        profileName = "New Mod Profile";
    }

    public ModProfile(String profileName) {
        ID = UUID.randomUUID();
        modList = new ArrayList<>();
        this.profileName = profileName;
    }

    public ModProfile(ModProfile modProfile) {
        ID = UUID.randomUUID();
        profileName = modProfile.getProfileName();
        modList = new ArrayList<>();
        if(modProfile.getModList() != null){
            modList.addAll(modProfile.getModList());
        }
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
        if (!(o instanceof ModProfile that)) return false;
		return Objects.equals(ID, that.ID) && Objects.equals(profileName, that.profileName) && Objects.equals(modList, that.modList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ID, profileName, modList);
    }
}
