package com.gearshiftgaming.se_mod_manager.frontend.view.helper;

import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.ModIoMod;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.SteamMod;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.Popup;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.List;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 * This provides common functions both the ModlistManager class and its factories both use for visual congruency.
 */
public class ModListManagerHelper {

    public void setCurrentModListLoadPriority(TableView<Mod> modTable, UiService uiService) {
        //If we are ascending or not sorted then set the load priority equal to the spot in the list, minus one.
        //If we are descending then set the load priority to its inverse position.
        if (modTable.getSortOrder().isEmpty() || modTable.getSortOrder().getFirst().getSortType().equals(TableColumn.SortType.ASCENDING)) {
            for (int i = 0; i < uiService.getCurrentModListProfile().getModList().size(); i++) {
                uiService.getCurrentModList().get(i).setLoadPriority(i + 1);
            }
        } else {
            for (int i = 0; i < uiService.getCurrentModListProfile().getModList().size(); i++) {
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

    /**
     * @return {@code true} if the provided ID matches an existing Mod.io Mod, returns that mod, otherwise null.
     */
    public static ModIoMod findDuplicateModIoMod(String modId, @NotNull List<Mod> modList) {
        for (Mod mod : modList) {
            if (mod instanceof ModIoMod modIoMod) {
                if (modIoMod.getId().equals(modId)) {
                    return modIoMod;
                }
            }
        }
        return null;
    }

    /**
     * @return {@code true} if the provided ID matches an existing Steam Mod, returns that mod, otherwise null.
     */
    public static SteamMod findDuplicateSteamMod(String modId, @NotNull List<Mod> modList) {
        for (Mod mod : modList) {
            if (mod instanceof SteamMod steamMod) {
                if (steamMod.getId().equals(modId)) {
                    return steamMod;
                }
            }
        }
        return null;
    }

    /**
     * @return {@code true} if the provided mod's friendly name is contained within another mods friendly name, returns a string with a message, otherwise returns an empty string.
     */
    public static String findDuplicateMod(Mod mod, @NotNull List<Mod> modList) {
        for (Mod m : modList) {
            //We only want to do this comparison when we are comparing different mod types, as we can otherwise assume the ID check has handled duplicates.
            if (!m.getClass().equals(mod.getClass())) {
                String shorterModName;
                String longerModName;
                if (m.getFriendlyName().length() < mod.getFriendlyName().length()) {
                    shorterModName = m.getFriendlyName();
                    longerModName = mod.getFriendlyName();
                } else {
                    shorterModName = mod.getFriendlyName();
                    longerModName = m.getFriendlyName();
                }

                if (longerModName.contains(shorterModName)) {
                    return String.format("Mod \"%s\" may be the same as \"%s\". Do you still want to add it?", mod.getFriendlyName(), m.getFriendlyName());
                }
            }
        }
        return "";
    }

	/**
	 *
	 * @param inputList The list of mods we want to remove the duplicates from.
	 * @param currentModList Our current active mod list.
	 */
	public static void removeDuplicateMods(List<Mod> inputList, List<Mod> currentModList) {
		HashMap<String, Mod> modHashMap = new HashMap<>();
		for(Mod mod : currentModList) {
			modHashMap.put(mod.getId(), mod);
		}
		inputList.removeIf(m -> modHashMap.containsKey(m.getId()));
	}

    public static void exportModlistFile(final Stage STAGE, final UiService UI_SERVICE) {
        FileChooser exportChooser = new FileChooser();
        exportChooser.setTitle("Export Modlist");
        exportChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        exportChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SEMM Modlists", "*.semm"));

        File savePath = exportChooser.showSaveDialog(STAGE);
        if (savePath != null) {
            Result<Void> exportModlistResult = UI_SERVICE.exportModlist(UI_SERVICE.getCurrentModListProfile(), savePath);
            if (!exportModlistResult.isSuccess()) UI_SERVICE.log(exportModlistResult);
            Popup.displaySimpleAlert(exportModlistResult, STAGE);
        }
    }
}
