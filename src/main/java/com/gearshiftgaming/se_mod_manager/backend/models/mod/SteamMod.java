package com.gearshiftgaming.se_mod_manager.backend.models.mod;

import com.gearshiftgaming.se_mod_manager.backend.models.adapters.LocalDateTimeAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
@Getter
@NoArgsConstructor
public class SteamMod extends Mod {

    //TODO: We need to store this as UTC in the DB.
    private LocalDateTime lastUpdated;

    public SteamMod(String id) {
        super(id);
        setPublishedServiceName("Steam");
    }

    public SteamMod(SteamMod mod) {
        super(mod);
        setPublishedServiceName("Steam");
        this.lastUpdated = mod.getLastUpdated();
    }

    public SteamMod(String id, String friendlyName, String publishedServiceName, int loadPriority, List<String> categories, boolean active, String description, LocalDateTime lastUpdated) {
        super(id, friendlyName, publishedServiceName, loadPriority, categories, active, description);
        this.lastUpdated = lastUpdated;
    }

    @XmlJavaTypeAdapter(value = LocalDateTimeAdapter.class)
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

}
