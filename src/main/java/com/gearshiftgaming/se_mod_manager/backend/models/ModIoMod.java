package com.gearshiftgaming.se_mod_manager.backend.models;

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
@Getter
@NoArgsConstructor
public class ModIoMod extends Mod {

    private LocalDate lastUpdated;

    public ModIoMod(String id) {
        super(id);

        setPublishedServiceName("Mod.io");
    }

    @XmlJavaTypeAdapter(value = LocalDateAdapter.class)
    public void setLastUpdated(LocalDate lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

}
