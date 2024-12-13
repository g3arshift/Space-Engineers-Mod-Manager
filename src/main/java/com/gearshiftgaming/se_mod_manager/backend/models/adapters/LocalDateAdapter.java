package com.gearshiftgaming.se_mod_manager.backend.models.adapters;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class LocalDateAdapter extends XmlAdapter<String, LocalDate> {
    //Load
    //TODO: This isn't loading right. Needs totally reworked.
    @Override
    public LocalDate unmarshal(String s) throws Exception {

        //TODO: Totally fucked
        return s.length() == 10 ? LocalDate.parse(s, DateTimeFormatter.ofPattern("MMM d',' yyyy")) :
                LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy"));
//        return switch (s.length()) {
//            case 10://Mod.io day format
//                yield LocalDate.parse(s, DateTimeFormatter.ofPattern("MMM d',' yyyy"));
//            default: //Mod.io year format
//                yield LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy"));
//        };
    }

    //Save
    @Override
    public String marshal(LocalDate localDate) throws Exception {
        //TODO: We likely need the same switch ladder as above, but let's try this first.
        return localDate.toString();
    }
}
