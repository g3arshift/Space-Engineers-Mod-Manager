package com.gearshiftgaming.se_mod_manager.frontend.models;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ModProfileManagerCell extends ModProfileCell{
	public ModProfileManagerCell(String themeName) {
		super("-fx-border-color: transparent transparent -color-border-muted transparent; -fx-border-width: 1px; -fx-border-insets: 0 5 0 5;", themeName);
	}
}
