package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;

import java.util.Optional;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.

 */
//TODO: Save on mod activation status toggle
public class ModNameCell extends TableCell<Mod, Mod> {
	private final UiService UI_SERVICE;

	private final CheckBox ACTIVATE_TOGGLE = new CheckBox();
	private final Label MOD_NAME = new Label();

	private final HBox LAYOUT;

	public ModNameCell(UiService uiService) {
		super();
		this.UI_SERVICE = uiService;
		 LAYOUT = new HBox(ACTIVATE_TOGGLE, MOD_NAME);
		LAYOUT.setAlignment(Pos.CENTER_LEFT);
	}

	@Override
	protected void updateItem(Mod item, boolean empty) {
		super.updateItem(item, empty);
		if (empty || item == null) {
			setGraphic(null);
			setStyle(null);
		} else {
			ACTIVATE_TOGGLE.setSelected(item.isActive());
			MOD_NAME.setText(item.getFriendlyName());

			ACTIVATE_TOGGLE.setOnAction(actionEvent -> {
				item.setActive(ACTIVATE_TOGGLE.isSelected());
				UI_SERVICE.saveUserData();
			});
			setGraphic(LAYOUT);
		}
	}
}
