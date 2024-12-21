package com.gearshiftgaming.se_mod_manager.backend.models;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import lombok.Getter;

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
@Getter
public class ModlistProfile {

    private final UUID ID;

    private String profileName;

    private List<Mod> modList;

    public ModlistProfile() {
        ID = UUID.randomUUID();
        modList = new ArrayList<>();
        //TODO: Add increment if duplicate.
        profileName = "New Mod Profile";
    }

    public ModlistProfile(String profileName) {
        ID = UUID.randomUUID();
        modList = new ArrayList<>();
        this.profileName = profileName;
    }

    public ModlistProfile(ModlistProfile modlistProfile) {
        ID = UUID.randomUUID();
        profileName = modlistProfile.getProfileName();
        modList = new ArrayList<>();
        if(modlistProfile.getModList() != null){
            modList.addAll(modlistProfile.getModList());
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
        if (!(o instanceof ModlistProfile that)) return false;
		return Objects.equals(ID, that.ID) && Objects.equals(profileName, that.profileName) && Objects.equals(modList, that.modList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ID, profileName, modList);
    }
}
