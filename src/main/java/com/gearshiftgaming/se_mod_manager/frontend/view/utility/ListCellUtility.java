package com.gearshiftgaming.se_mod_manager.frontend.view.utility;

/**
 * A simple utility to provide a common way to get the color of selected ListCells across many different list types.
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ListCellUtility {

	//This is an extremely clunky way of doing this, and it's pretty dependent on the atlantaFX implementation, but I'm an idiot and can't figure out another way to actually get the damn current CSS style from my stylesheet, then add onto it.
	public static String getSelectedCellColor(String themeName) {
		return switch (themeName) {
			case "PrimerLight", "NordLight", "CupertinoLight":
				yield "-color-cell-bg-selected: -color-base-2;" +
						"-color-cell-bg-selected-focused: -color-accent-2;";
			case "PrimerDark", "CupertinoDark":
				yield "-color-cell-bg-selected: -color-base-5;" +
						"-color-cell-bg-selected-focused: -color-accent-5;";
			case "NordDark":
				yield "-color-cell-bg-selected: -color-base-6;" +
						"-color-cell-bg-selected-focused: -color-accent-6;";
			default:
				yield "-color-cell-bg-selected: -color-accent-subtle;" +
						"-color-cell-bg-selected-focused: -color-accent-subtle;";
		};
	}
}
