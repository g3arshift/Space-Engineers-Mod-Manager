package com.gearshiftgaming.se_mod_manager.backend.models.mod;

import com.gearshiftgaming.se_mod_manager.backend.models.adapters.LocalTimeAdapter;
import com.gearshiftgaming.se_mod_manager.backend.models.adapters.MonthDayAdapter;
import com.gearshiftgaming.se_mod_manager.backend.models.adapters.YearAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.time.MonthDay;
import java.time.Year;
import java.util.List;
import java.util.Objects;

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
        setPublishedServiceName("mod.io");
    }

    public ModIoMod(ModIoMod mod) {
        super(mod);
        setPublishedServiceName("mod.io");
        this.lastUpdatedYear = mod.getLastUpdatedYear();
        this.lastUpdatedMonthDay = mod.getLastUpdatedMonthDay();
        this.lastUpdatedHour = mod.getLastUpdatedHour();
    }
    
    public ModIoMod(String id,
                    String friendlyName,
                    String publishedServiceName,
                    int loadPriority,
                    List<String> categories,
                    boolean active,
                    String description,
                    Year lastUpdatedYear,
                    MonthDay lastUpdatedMonthDay,
                    LocalTime lastUpdatedHour,
                    ModDownloadStatus downloadStatus) {
        super(id, friendlyName, publishedServiceName, loadPriority, categories, active, description, downloadStatus);
        this.lastUpdatedYear = lastUpdatedYear;
        this.lastUpdatedMonthDay = lastUpdatedMonthDay;
        this.lastUpdatedHour = lastUpdatedHour;
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ModIoMod modIoMod = (ModIoMod) o;
        return Objects.equals(lastUpdatedYear, modIoMod.lastUpdatedYear) && Objects.equals(lastUpdatedMonthDay, modIoMod.lastUpdatedMonthDay) && Objects.equals(lastUpdatedHour, modIoMod.lastUpdatedHour);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), lastUpdatedYear, lastUpdatedMonthDay, lastUpdatedHour);
    }
}
