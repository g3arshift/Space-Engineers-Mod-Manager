package com.gearshiftgaming.se_mod_manager.backend.domain.tool;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ToolDownloadFailedException extends RuntimeException {
    public ToolDownloadFailedException(String message) {
        super(message);
    }
}
