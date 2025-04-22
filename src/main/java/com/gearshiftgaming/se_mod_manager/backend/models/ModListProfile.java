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

    private HashMap<String, List<Mod>> conflictTable = new HashMap<>();

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
            for(int i = 0; i < modListProfile.getModList().size(); i++) {
                Mod m = modListProfile.getModList().get(i);
                if(m instanceof SteamMod) {
                    SteamMod steamMod = new SteamMod((SteamMod) m);
                    steamMod.setLoadPriority(i + 1);
                    this.modList.add(steamMod);
                } else {
                    ModIoMod modIoMod = new ModIoMod((ModIoMod) m);
                    modIoMod.setLoadPriority(i + 1);
                    this.modList.add(modIoMod);
                }
            }
        }
        SPACE_ENGINEERS_VERSION = modListProfile.getSPACE_ENGINEERS_VERSION();
        conflictTable = new HashMap<>();
        for(Map.Entry<String, List<Mod>> entry : modListProfile.getConflictTable().entrySet()) {
            List<Mod> copiedModConflictList = new ArrayList<>();
            for(Mod mod : entry.getValue()) {
                if(mod instanceof SteamMod) {
                    copiedModConflictList.add(new SteamMod((SteamMod) mod));
                } else {
                    copiedModConflictList.add(new ModIoMod((ModIoMod) mod));
                }
            }
            conflictTable.put(entry.getKey(), copiedModConflictList);
        }
    }

    public ModListProfile(UUID ID, String profileName, SpaceEngineersVersion SPACE_ENGINEERS_VERSION) {
        this.ID = ID;
        this.profileName = profileName;
        this.SPACE_ENGINEERS_VERSION = SPACE_ENGINEERS_VERSION;
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

    public void generateConflictTable() {
        for(Mod m : modList) {
            for(String modifiedPath : m.getModifiedPaths()) {
                conflictTable.computeIfAbsent(modifiedPath, k -> new ArrayList<>()).add(m);
            }
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(ID, profileName, modList);
    }
}
