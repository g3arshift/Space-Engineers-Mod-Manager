package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;

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
public class ModNameCell extends TableCell<Mod, Mod> {

	private final CheckBox activateToggle = new CheckBox();

	public ModNameCell() {
		super();
	}

	@Override
	protected void updateItem(Mod item, boolean empty) {
		super.updateItem(item, empty);

		if(empty || item == null) {
			setGraphic(null);
			setStyle(null);
		} else {
			activateToggle.setText(item.getFriendlyName());
			activateToggle.setSelected(item.isActive());

			activateToggle.setOnAction(actionEvent -> item.setActive(this.isSelected()));
			setGraphic(activateToggle);
		}
	}
}
