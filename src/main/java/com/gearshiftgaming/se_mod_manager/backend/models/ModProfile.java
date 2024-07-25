package com.gearshiftgaming.se_mod_manager.backend.models;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ModProfile {

    private UUID id;

    private String profileName;

    private List<Mod> modList;

    public ModProfile() {
        id = UUID.randomUUID();
        modList = new ArrayList<>();
        //TODO: Add incremment.
        profileName = "New Mod Profile";
    }

    public ModProfile(String profileName) {
        modList = new ArrayList<>();
        this.profileName = profileName;
    }

    public ModProfile(List<Mod> modList) {
        this.modList = modList;
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
}
