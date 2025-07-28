package com.gearshiftgaming.se_mod_manager.backend.models.mod;

import lombok.Getter;

/**
 * This class represents the current state of a mod download operation. It models a simple state machine with three stages:
 * <ul>
 *     <li>{@link #UNSTARTED} — the download has not begun.</li>
 *     <li>{@link #DOWNLOADING} — the download is currently ongoing.</li>
 *     <li>{@link #DOWNLOADED} — the download has completed.</li>
 * </ul>
 * <p>
 * The {@code ModDownloadStatus} enum also provides a human-readable {@link #getDisplayName() display name}
 * suitable for user interfaces.
 * <p>
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * ModDownloadStatus status = ModDownloadStatus.UNSTARTED;
 * System.out.println(status.getDisplayName()); // "Not started"
 * status = status.nextState();
 * }</pre>
 * <p>
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
@Getter
public enum ModDownloadStatus {
    UNSTARTED("Not Downloaded") {
        @Override
        public ModDownloadStatus nextState() {
            return DOWNLOADING;
        }
    },
    DOWNLOADING("Downloading") {
        @Override
        public ModDownloadStatus nextState() {
            return DOWNLOADED;
        }
    },
    DOWNLOADED("Downloaded") {
        @Override
        public ModDownloadStatus nextState() {
            return this;
        }
    };

    private final String displayName;

    ModDownloadStatus(String displayName) {
        this.displayName = displayName;
    }

    public static ModDownloadStatus fromString(String name) {
        for(ModDownloadStatus b: ModDownloadStatus.values())
            if(b.displayName.equalsIgnoreCase(name))
                return b;
        return null;
    }

    public abstract ModDownloadStatus nextState();
}
