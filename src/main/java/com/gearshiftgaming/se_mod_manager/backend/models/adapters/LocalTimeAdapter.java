package com.gearshiftgaming.se_mod_manager.backend.models.adapters;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.time.LocalTime;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class LocalTimeAdapter extends XmlAdapter<String, LocalTime> {


    @Override
    public LocalTime unmarshal(String s) throws Exception {
        return null;
    }

    @Override
    public String marshal(LocalTime localTime) throws Exception {
        return "";
    }
}
