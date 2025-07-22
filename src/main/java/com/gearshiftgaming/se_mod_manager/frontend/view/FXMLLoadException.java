package com.gearshiftgaming.se_mod_manager.frontend.view;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class FXMLLoadException extends RuntimeException {
    public FXMLLoadException(String message) {
        super(message);
    }

    public FXMLLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
