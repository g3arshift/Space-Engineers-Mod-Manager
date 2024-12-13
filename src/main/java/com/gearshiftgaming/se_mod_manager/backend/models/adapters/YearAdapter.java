package com.gearshiftgaming.se_mod_manager.backend.models.adapters;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.time.Year;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */

//TODO: Implement
public class YearAdapter extends XmlAdapter<String, Year> {
    @Override
    public Year unmarshal(String s) throws Exception {
        return Year.parse(s);
    }

    @Override
    public String marshal(Year year) throws Exception {
        return year.toString();
    }
}
