package com.gearshiftgaming.se_mod_manager.backend.models;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.

 */
public enum SaveStatus {
    SAVED("Saved"),
    UNSAVED("Unsaved"),
    FAILED("Failed"),
    NONE("None");

    private final String name;

    SaveStatus(String name) {this.name = name;}

    public static SaveStatus fromString(String name) {
        for(SaveStatus b : SaveStatus.values()) {
            if(b.name.equalsIgnoreCase(name)) {
                return b;
            }
        }
        return null;
    }
}
