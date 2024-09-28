package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;

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

	private final CheckBox ACTIVATE_TOGGLE = new CheckBox();

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
			ACTIVATE_TOGGLE.setText(item.getFriendlyName());
			ACTIVATE_TOGGLE.setSelected(item.isActive());

			ACTIVATE_TOGGLE.setOnAction(actionEvent -> item.setActive(this.isSelected()));
			TableCell<Mod, Mod> tableCell = new TableCell<>();
			// TODO: We need to actually return a table cell containing our items. tableCell.itemProperty()
		}
	}
}
