package com.gearshiftgaming.se_mod_manager.backend.models;

import jakarta.xml.bind.annotation.*;
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
@XmlRootElement(name = "modlistProfile")
public class ModList {

    @XmlElement
    private final UUID ID;

    private String profileName;

    private List<Mod> modList;

    public ModList() {
        ID = UUID.randomUUID();
        modList = new ArrayList<>();
        profileName = "New Mod Profile";
    }

    public ModList(String profileName) {
        ID = UUID.randomUUID();
        modList = new ArrayList<>();
        this.profileName = profileName;
    }

    public ModList(ModList modList) {
        this.ID = UUID.randomUUID();
        this.profileName = modList.getProfileName();
        this.modList = new ArrayList<>();
        if(modList.getModList() != null){
            for(Mod m : modList.getModList()) {
                if(m instanceof SteamMod) {
                    this.modList.add(new SteamMod((SteamMod) m));
                } else {
                    this.modList.add(new ModIoMod((ModIoMod) m));
                }
            }
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
        if (!(o instanceof ModList that)) return false;
		return Objects.equals(ID, that.ID) && Objects.equals(profileName, that.profileName) && Objects.equals(modList, that.modList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ID, profileName, modList);
    }
}
