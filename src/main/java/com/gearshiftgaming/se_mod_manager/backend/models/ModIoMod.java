package com.gearshiftgaming.se_mod_manager.backend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.adapters.LocalTimeAdapter;
import com.gearshiftgaming.se_mod_manager.backend.models.adapters.MonthDayAdapter;
import com.gearshiftgaming.se_mod_manager.backend.models.adapters.YearAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.time.MonthDay;
import java.time.Year;

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

    private Year lastUpdatedYear;

    private MonthDay lastUpdatedMonthDay;

    private LocalTime lastUpdatedHour;

    public ModIoMod(String id) {
        super(id);

        setPublishedServiceName("Mod.io");
    }

    @XmlJavaTypeAdapter(value = YearAdapter.class)
    public void setLastUpdatedYear(Year lastUpdatedYear) {
        this.lastUpdatedYear = lastUpdatedYear;
    }

    @XmlJavaTypeAdapter(value = MonthDayAdapter.class)
    public void setLastUpdatedMonthDay(MonthDay lastUpdatedMonthDay) {
        this.lastUpdatedMonthDay = lastUpdatedMonthDay;
    }

    @XmlJavaTypeAdapter(value = LocalTimeAdapter.class)
    public void setLastUpdatedHour(LocalTime lastUpdatedHour) {
        this.lastUpdatedHour = lastUpdatedHour;
    }
}
