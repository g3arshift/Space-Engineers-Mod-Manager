package com.gearshiftgaming.se_mod_manager.backend.models;

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
@Getter
public class SteamMod extends Mod{

    private LocalDateTime lastUpdated;

    public SteamMod(String id) {
        super(id);

       setPublishedServiceName("Steam");
    }

    @XmlJavaTypeAdapter(value = LocalDateTimeAdapter.class)
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

}
