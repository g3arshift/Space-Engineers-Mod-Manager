package com.gearshiftgaming.se_mod_manager.backend.models;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import lombok.Getter;
import lombok.Setter;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
public class ModProfile {

    private final UUID id;

    private String profileName;

    private List<Mod> modList;

    @XmlAttribute
    private String lastSaved;

    public ModProfile() {
        id = UUID.randomUUID();
        modList = new ArrayList<>();
        //TODO: Add increment if duplicate.
        profileName = "New Mod Profile";
        lastSaved = getCurrentTime();
    }

    public ModProfile(String profileName) {
        id = UUID.randomUUID();
        modList = new ArrayList<>();
        this.profileName = profileName;
        lastSaved = getCurrentTime();
    }

    @XmlAttribute
    public void setProfileName(String profileName) {
        this.profileName = profileName;
        lastSaved = getCurrentTime();
    }

    @XmlElementWrapper(name = "mods")
    @XmlElement(name = "mod")
    public void setModList(List<Mod> modList) {
        this.modList = modList;
        lastSaved = getCurrentTime();
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm a"));
    }
}
