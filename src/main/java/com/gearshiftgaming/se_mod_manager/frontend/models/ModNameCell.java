package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;

import java.util.Optional;

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

		if (empty || item == null) {
			setGraphic(null);
			setStyle(null);
		} else {
			ACTIVATE_TOGGLE.setText(item.getFriendlyName());
			ACTIVATE_TOGGLE.setSelected(item.isActive());

			ACTIVATE_TOGGLE.setOnAction(actionEvent -> item.setActive(ACTIVATE_TOGGLE.isSelected()));
			setGraphic(ACTIVATE_TOGGLE);
		}
	}
}
