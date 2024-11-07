package com.gearshiftgaming.se_mod_manager.frontend.models;

import atlantafx.base.theme.PrimerLight;
import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 *
 * @author Gear Shift
 */
public class ModTableRow extends TableRow<Mod> {

	private final String themeName;

	public ModTableRow(String themeName) {
		super();
		this.themeName = themeName;
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
						getSelectedCellColor());
			} else {
				setStyle("-fx-background-color: -color-cell-border, -color-cell-bg;" +
						"-fx-background-insets: 0, 0 0 1 0;" +
						"-fx-padding: 0;" +
						"-fx-cell-size: 2.8em;");
			}
		}
	}

	//This is an extremely clunky way of doing this, and it's pretty dependent on the atlantaFX implementation, but I'm an idiot and can't figure out another way to actually get the damn current CSS style from my stylesheet.
	private String getSelectedCellColor() {
		return switch (themeName) {
			case "Primer Light", "Nord Light", "Cupertino Light": yield "-color-cell-bg-selected: -color-base-1;" +
					"-color-cell-bg-selected-focused: -color-base-1;";
			case "Primer Dark", "Cupertino Dark": yield "-color-cell-bg-selected: -color-base-6;" +
					"-color-cell-bg-selected-focused: -color-base-6;";
			case "Nord Dark": yield "-color-cell-bg-selected: -color-base-7;" +
					"-color-cell-bg-selected-focused: -color-base-7;";
			default: yield "-color-cell-bg-selected: -color-accent-subtle;" +
					"-color-cell-bg-selected-focused: -color-accent-subtle;";
		};
	}
}
