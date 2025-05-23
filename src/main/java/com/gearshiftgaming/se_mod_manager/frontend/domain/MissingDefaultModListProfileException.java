package com.gearshiftgaming.se_mod_manager.frontend.domain;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class MissingDefaultModListProfileException extends RuntimeException {
    public MissingDefaultModListProfileException() {
        super("Could not load the default mod list profile.");
    }
}
