package com.gearshiftgaming.se_mod_manager.frontend.models;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public enum ModImportType {
	STEAM_ID("Steam Workshop ID"),
	STEAM_COLLECTION("Steam Collection"),
	MOD_IO("Mod.io"),
	FILE("Modlist File");

	private final String name;

	ModImportType(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public static ModImportType fromString(String name) {
		for (ModImportType b : ModImportType.values()) {
			if (b.name.equalsIgnoreCase(name)) {
				return b;
			}
		}
		return null;
	}
}
