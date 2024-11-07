package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import javafx.scene.control.TableRow;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.

 */
public class ModTableRow extends TableRow<Mod> {

	final private UiService UI_SERVICE;

	public ModTableRow(UiService uiService) {
		super();
		this.UI_SERVICE = uiService;
	}

	@Override
	protected void updateItem(Mod item, boolean empty) {
		super.updateItem(item, empty);
		if (empty || item == null) {
			setGraphic(null);
			setStyle(null);
		} else {
			if(this.isSelected()) {
				setStyle("-color-cell-fg-selected: -color-fg-default;" +
						"-color-cell-fg-selected-focused: -color-fg-default;" +
						getSelectedCellColor(UI_SERVICE.getUSER_CONFIGURATION().getUserTheme()));
			} else {
				setStyle("-fx-background-color: -color-cell-border, -color-cell-bg;" +
						"-fx-background-insets: 0, 0 0 1 0;" +
						"-fx-padding: 0;" +
						"-fx-cell-size: 2.8em;");
			}
		}
	}

	//This is an extremely clunky way of doing this, and it's pretty dependent on the atlantaFX implementation, but I'm an idiot and can't figure out another way to actually get the damn current CSS style from my stylesheet.
	//TODO: In the future, look at replacing all this with getStylesheets.add("some_style.css);. But this works for now.
	private String getSelectedCellColor(String themeName) {
		return switch (themeName) {
			case "PrimerLight", "NordLight", "CupertinoLight": yield "-color-cell-bg-selected: -color-base-1;" +
					"-color-cell-bg-selected-focused: -color-base-1;";
			case "PrimerDark", "CupertinoDark": yield "-color-cell-bg-selected: -color-base-6;" +
					"-color-cell-bg-selected-focused: -color-base-6;";
			case "NordDark": yield "-color-cell-bg-selected: -color-base-7;" +
					"-color-cell-bg-selected-focused: -color-base-7;";
			default: yield "-color-cell-bg-selected: -color-accent-subtle;" +
					"-color-cell-bg-selected-focused: -color-accent-subtle;";
		};
	}
}
