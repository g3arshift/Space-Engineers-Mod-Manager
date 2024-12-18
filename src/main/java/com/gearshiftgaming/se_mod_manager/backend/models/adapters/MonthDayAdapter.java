package com.gearshiftgaming.se_mod_manager.backend.models.adapters;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.time.MonthDay;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class MonthDayAdapter extends XmlAdapter<String, MonthDay> {
    @Override
    public MonthDay unmarshal(String s) throws Exception {
        return MonthDay.parse(s);
    }

    @Override
    public String marshal(MonthDay monthDay) throws Exception {
        return monthDay.toString();
    }
}
