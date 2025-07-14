package com.gearshiftgaming.se_mod_manager.backend.models.modlist;

import com.gearshiftgaming.se_mod_manager.backend.models.shared.SpaceEngineersVersion;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.ModIoMod;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.SteamMod;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.*;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
@XmlRootElement(name = "modlistProfile")
public class ModListProfile implements ModProfileInfo {

    @XmlElement
    private final UUID id;

    private String profileName;

    private List<Mod> modList = new ArrayList<>();

    //TODO: Replace with a stable value in J25.
    @XmlElement
    private final SpaceEngineersVersion spaceEngineersVersion;

    private HashMap<String, List<Mod>> conflictTable = new HashMap<>();

    public ModListProfile() {
        id = UUID.randomUUID();
        profileName = "New Mod Profile";
        spaceEngineersVersion = SpaceEngineersVersion.SPACE_ENGINEERS_ONE;
    }

    public ModListProfile(SpaceEngineersVersion spaceEngineersVersion) {
        id = UUID.randomUUID();
        profileName = "New Mod Profile";
        this.spaceEngineersVersion = spaceEngineersVersion;
    }

    public ModListProfile(String profileName, SpaceEngineersVersion spaceEngineersVersion) {
        this.spaceEngineersVersion = spaceEngineersVersion;
        id = UUID.randomUUID();
        this.profileName = profileName;
    }

    public ModListProfile(ModListProfile modListProfile) {
        this.id = UUID.randomUUID();
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
        spaceEngineersVersion = modListProfile.getSpaceEngineersVersion();
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

    public ModListProfile(UUID id, String profileName, SpaceEngineersVersion spaceEngineersVersion) {
        this.id = id;
        this.profileName = profileName;
        this.spaceEngineersVersion = spaceEngineersVersion;
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
		return Objects.equals(id, that.id) && Objects.equals(profileName, that.profileName) && Objects.equals(modList, that.modList);
    }

    public void generateConflictTable() {
        for(Mod m : modList) {
            for(String modifiedPath : m.getModifiedPaths()) {
                conflictTable.computeIfAbsent(modifiedPath, k -> new ArrayList<>()).add(m);
            }
        }
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getProfileName() {
        return profileName;
    }

    @Override
    public List<Mod> getModList() {
        return modList;
    }

    @Override
    public SpaceEngineersVersion getSpaceEngineersVersion() {
        return spaceEngineersVersion;
    }

    @Override
    public HashMap<String, List<Mod>> getConflictTable() {
        return conflictTable;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, profileName, modList);
    }
}
