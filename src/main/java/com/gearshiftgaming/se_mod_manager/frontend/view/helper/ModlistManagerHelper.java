package com.gearshiftgaming.se_mod_manager.frontend.view.helper;

import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 * This provides common functions both the ModlistManager class and its factories both use for visual congruency.
 */
public class ModlistManagerHelper {

	public void setCurrentModListLoadPriority(TableView<Mod> modTable, UiService uiService){
		//If we are ascending or not sorted then set the load priority equal to the spot in the list, minus one.
		//If we are descending then set the load priority to its inverse position.
		if (modTable.getSortOrder().isEmpty() || modTable.getSortOrder().getFirst().getSortType().equals(TableColumn.SortType.ASCENDING)) {
			for (int i = 0; i < uiService.getCurrentModlistProfile().getModList().size(); i++) {
				uiService.getCurrentModList().get(i).setLoadPriority(i + 1);
			}
		} else {
			for (int i = 0; i < uiService.getCurrentModlistProfile().getModList().size(); i++) {
				uiService.getCurrentModList().get(i).setLoadPriority(getIntendedLoadPriority(modTable, i, uiService));
			}
		}
	}

	private int getIntendedLoadPriority(TableView<Mod> modTable, int index, UiService uiService) {
		//Check if we are in ascending/default order, else we're in descending order
		if (modTable.getSortOrder().isEmpty() || modTable.getSortOrder().getFirst().getSortType().equals(TableColumn.SortType.ASCENDING)) {
			return index;
		} else {
			return uiService.getCurrentModList().size() - index;
		}
	}

	public String getSelectedCellBorderColor(UiService uiService) {
		return switch (uiService.getUSER_CONFIGURATION().getUserTheme()) {
			case "PrimerLight", "NordLight", "CupertinoLight":
				yield "#24292f";
			case "PrimerDark", "CupertinoDark":
				yield "#f0f6fc";
			case "NordDark":
				yield "#ECEFF4";
			default:
				yield "#f8f8f2";
		};
	}
}
