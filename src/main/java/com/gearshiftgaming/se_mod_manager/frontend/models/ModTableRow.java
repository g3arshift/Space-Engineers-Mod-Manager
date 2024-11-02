package com.gearshiftgaming.se_mod_manager.frontend.models;

import atlantafx.base.theme.PrimerLight;
import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import javafx.scene.Node;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

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

	public ModTableRow() {
		super();
	}

	@Override
	protected void updateItem(Mod item, boolean empty) {
		super.updateItem(item, empty);
		if (empty || item == null) {
			setGraphic(null);
			setStyle(null);
		} else {
			//TODO: We need to test setting style without totally wiping it. Lookup current name, do lookup(".table-view") or maybe table-cell, needs investigation.
			// Fundamentally we need to get the existing style and additively alter it.
		}
	}
}
