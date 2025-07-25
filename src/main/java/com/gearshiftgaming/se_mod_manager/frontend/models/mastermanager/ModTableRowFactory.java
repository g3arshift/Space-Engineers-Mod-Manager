package com.gearshiftgaming.se_mod_manager.frontend.models.mastermanager;

import com.gearshiftgaming.se_mod_manager.backend.models.shared.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.SteamMod;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.view.MasterManager;
import com.gearshiftgaming.se_mod_manager.frontend.view.helper.ModListManagerHelper;
import com.gearshiftgaming.se_mod_manager.frontend.view.popup.Popup;
import com.gearshiftgaming.se_mod_manager.frontend.view.popup.TwoButtonChoice;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.MenuItem;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */

//TODO: Make the rows have a specific color with possible pattern like hatching or diagonal lines based on status or conflicts and stuff
public class ModTableRowFactory implements Callback<TableView<Mod>, TableRow<Mod>> {

    private final UiService uiService;

    private final DataFormat serializedMimeType;
    private final List<Mod> selections;

    private TableRow<Mod> previousRow;

    private final MasterManager modlistManagerView;

    private final ModListManagerHelper modlistManagerHelper;

    
    private enum RowBorderType {
        TOP,
        BOTTOM
    }

    public ModTableRowFactory(UiService uiService, DataFormat serializedMimeType, List<Mod> selections, MasterManager masterManager, ModListManagerHelper modlistManagerHelper) {
        this.uiService = uiService;
        this.serializedMimeType = serializedMimeType;
        this.selections = selections;
        this.modlistManagerView = masterManager;
        this.modlistManagerHelper = modlistManagerHelper;
    }

    //TODO: We need a "Update mods" button.
    //TODO: Split this up into smaller functions, this is fucking massive.
    @Override
    public ModTableRow call(@NotNull TableView<Mod> modTable) {
        ScrollBar modTableVerticalScrollBar;
        modTableVerticalScrollBar = (ScrollBar) modTable.lookup(".scroll-bar:vertical");
        final ModTableRow row = new ModTableRow(uiService);

        //Setup our context menu
        final ContextMenu tableContextMenu = new ContextMenu();

        final MenuItem openSelectedModPages = new MenuItem("Open selected mod pages");
        openSelectedModPages.setOnAction(actionEvent -> {
            final List<Mod> selectedMods = new ArrayList<>(modTable.getSelectionModel().getSelectedItems());
            for (Mod m : selectedMods) {
                try {
                    if (m instanceof SteamMod) {
                        Desktop.getDesktop().browse(new URI("https://steamcommunity.com/workshop/filedetails/?id=" + m.getId()));
                    } else {
                        Desktop.getDesktop().browse(new URI("https://mod.io/search/mods/" + m.getId()));
                    }
                } catch (URISyntaxException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        final MenuItem activateSelectedMods = new MenuItem("Activate selected mods");
        activateSelectedMods.setOnAction(actionEvent -> {
            final List<Mod> selectedMods = new ArrayList<>(modTable.getSelectionModel().getSelectedItems());
            for (Mod m : selectedMods) {
				if(!m.isActive()) {
					m.setActive(true);
					uiService.modifyActiveModCount(m);
				}
            }
            modTable.refresh();
            Result<Void> updateModListActiveStateResult = uiService.updateModListActiveMods();
            checkResult(updateModListActiveStateResult, "Failed to update active mods. See the log for more information.");
        });

        final MenuItem deactivateSelectedMods = new MenuItem("Deactivate selected mods");
        deactivateSelectedMods.setOnAction(actionEvent -> {
            final List<Mod> selectedMods = new ArrayList<>(modTable.getSelectionModel().getSelectedItems());
            for (Mod m : selectedMods) {
				if(m.isActive()) {
					m.setActive(false);
					uiService.modifyActiveModCount(m);
				}
            }
            modTable.refresh();
            Result<Void> updateModListActiveStateResult = uiService.updateModListActiveMods();
            checkResult(updateModListActiveStateResult, "Failed to update active mods. See the log for more information.");
        });

        final MenuItem deleteSelectedMods = new MenuItem("Delete selected mods");
        deleteSelectedMods.disableProperty().bind(Bindings.isEmpty(modTable.getSelectionModel().getSelectedItems()));
        deleteSelectedMods.setOnAction(actionEvent -> deleteMods(modTable));

		final MenuItem selectAll = new MenuItem("Select all");
		selectAll.setOnAction(event -> modTable.getSelectionModel().selectAll());

        tableContextMenu.getItems().addAll(activateSelectedMods, deactivateSelectedMods, openSelectedModPages, deleteSelectedMods, selectAll);

        row.contextMenuProperty().bind(
                Bindings.when(Bindings.isNotNull(row.itemProperty()))
                        .then(tableContextMenu)
                        .otherwise((ContextMenu) null));

        //Lets the user press the delete key to delete selected mods instead of having to right click every time.
        modTable.setOnKeyPressed(event -> {
            List<Mod> selectedItems = modTable.getSelectionModel().getSelectedItems();
            if(event.getCode().equals(KeyCode.DELETE)) {
                if(!selectedItems.isEmpty()) {
                    deleteMods(modTable);
                }
            }
        });

        //Setup drag and drop reordering for the table
        row.setOnDragDetected(dragEvent -> {
            //Don't allow dragging if a sortOrder is applied, or if the sort order that's applied isn't an ascending sort on loadPriority
            if (modTable.getSortOrder().isEmpty() || modTable.getSortOrder().getFirst().getId().equals("loadPriority")) {
                if (!row.isEmpty()) {
                    Integer index = row.getIndex();
                    selections.clear();

                    ObservableList<Mod> items = modTable.getSelectionModel().getSelectedItems();

                    selections.addAll(items);

                    Dragboard dragboard = row.startDragAndDrop(TransferMode.MOVE);
                    dragboard.setDragView(row.snapshot(null, null));
                    ClipboardContent clipboardContent = new ClipboardContent();
                    clipboardContent.put(serializedMimeType, index);
                    dragboard.setContent(clipboardContent);

                    dragEvent.consume();
                }
            }
        });

        row.setOnDragOver(dragEvent -> {
            Dragboard dragboard = dragEvent.getDragboard();
            if (dragboard.hasContent(serializedMimeType)) {
                if (row.getIndex() != ((Integer) dragboard.getContent(serializedMimeType))) {
                    dragEvent.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                    dragEvent.consume();
                }
            }
        });

        row.setOnDragEntered(dragEvent -> {
            if (previousRow != null && !row.isEmpty()) {
                previousRow.setBorder(null);
            }

            if (!row.isEmpty()) {
                previousRow = row;
                modlistManagerView.setPreviousRow(previousRow);
            }

            if (dragEvent.getDragboard().hasContent(serializedMimeType)) {
                if (!row.isEmpty()) {
                    addBorderToRow(RowBorderType.TOP, row);
                }
            }
            dragEvent.consume();
        });

        row.setOnDragExited(dragEvent -> {
            //If we are not the last item and the row isn't blank, set it to null. Else, set a bottom border.
            if (!row.isEmpty() && previousRow.getItem().equals(uiService.getCurrentModList().getLast())) {
                //We don't want to add a border if the table isn't big enough to display all mods at once since we'll end up with a double border
                if (!modTableVerticalScrollBar.isVisible()) {
                    addBorderToRow(RowBorderType.BOTTOM, row);
                } else {
                    row.setBorder(null);
                }
            } else {
                row.setBorder(null);
            }
            dragEvent.consume();
        });

        row.setOnDragDropped(dragEvent -> {
            row.setBorder(null);
            Dragboard dragboard = dragEvent.getDragboard();

            if (dragboard.hasContent(serializedMimeType)) {
                int dropIndex;
                Mod mod = null;

                if (row.isEmpty()) {
                    dropIndex = uiService.getCurrentModList().size();
                } else {
                    dropIndex = row.getIndex();
                    mod = uiService.getCurrentModList().get(dropIndex);
                }

                int delta = 0;
                if (mod != null) {
                    while (selections.contains(mod)) {
                        delta = 1;
                        --dropIndex;
                        if (dropIndex < 0) {
                            mod = null;
                            dropIndex = 0;
                            break;
                        }
                        mod = uiService.getCurrentModList().get(dropIndex);
                    }
                }

                for (Mod m : selections) {
                    uiService.getCurrentModList().remove(m);
                }

                if (mod != null) {
                    dropIndex = uiService.getCurrentModList().indexOf(mod) + delta;
                } else if (dropIndex != 0) {
                    dropIndex = uiService.getCurrentModList().size();
                }

                modTable.getSelectionModel().clearSelection();

                for (Mod m : selections) {
                    uiService.getCurrentModList().add(dropIndex, m);
                    modTable.getSelectionModel().select(dropIndex);
                    dropIndex++;
                }
                dragEvent.setDropCompleted(true);
                selections.clear();

                modlistManagerHelper.setCurrentModListLoadPriority(modTable, uiService);

                //Redo our sort since our row order has changed
                modTable.sort();

				/*
					We shouldn't need this since currentModList which backs our table is an observable list backed by the currentModProfile.getModList,
					but for whatever reason the changes aren't propagating without this.
				 */
                //TODO: Look into why the changes don't propagate without setting it here. Indicative of a deeper issue or misunderstanding.
                //TODO: We might be able to fix this with the new memory model. Investigate.
                uiService.getCurrentModListProfile().setModList(uiService.getCurrentModList());
                Result<Void> updateModListLoadPriorityResult = uiService.updateModListLoadPriority();
                checkResult(updateModListLoadPriorityResult, "Failed to update mod list load priority. See the log for more information.");

                dragEvent.consume();
            }
        });

        row.setOnDragDone(dragEvent -> {
            if (modlistManagerView.getScrollTimeline() != null) modlistManagerView.getScrollTimeline().stop();

            // Remove any borders and perform clean-up actions here
            if (previousRow != null) previousRow.setBorder(null);
            dragEvent.consume();
        });

        //This is a dumb hack but I can't get the row's height any other way
        if (modlistManagerView.getSingleTableRow() == null) modlistManagerView.setSingleTableRow(row);

        return row;
    }

    private void addBorderToRow(RowBorderType rowBorderType, @NotNull ModTableRow row) {
        if (!row.isEmpty() || (row.getIndex() <= uiService.getCurrentModList().size() && uiService.getCurrentModList().get(row.getIndex() - 1) != null)) {
            Color indicatorColor = Color.web(modlistManagerHelper.getSelectedCellBorderColor(uiService));
            Border dropIndicator;
            if (rowBorderType.equals(RowBorderType.TOP)) {
                dropIndicator = new Border(new BorderStroke(indicatorColor, indicatorColor, indicatorColor, indicatorColor,
                        BorderStrokeStyle.SOLID, BorderStrokeStyle.NONE, BorderStrokeStyle.NONE, BorderStrokeStyle.NONE,
                        CornerRadii.EMPTY, new BorderWidths(2), Insets.EMPTY));
            } else {
                dropIndicator = new Border(new BorderStroke(indicatorColor, indicatorColor, indicatorColor, indicatorColor,
                        BorderStrokeStyle.NONE, BorderStrokeStyle.NONE, BorderStrokeStyle.SOLID, BorderStrokeStyle.NONE,
                        CornerRadii.EMPTY, new BorderWidths(2), new Insets(0, 0, 2, 0)));
            }
            row.setBorder(dropIndicator);
        }
    }

	private void deleteMods(TableView<Mod> modTable) {
		TwoButtonChoice choice = Popup.displayYesNoDialog("Are you sure you want to delete these mods?", modlistManagerView.getStage(), MessageType.WARN);
		if (choice == TwoButtonChoice.YES) {
			final List<Mod> selectedMods = new ArrayList<>(modTable.getSelectionModel().getSelectedItems());

			//TODO: Look into why if we use getCurrentModProfile it returns null. Indicative of a deeper issue or misunderstanding just like in the droprow.
			uiService.getCurrentModList().removeAll(selectedMods);
			uiService.getCurrentModListProfile().setModList(uiService.getCurrentModList());

			int previouslyActiveModCount = 0;

			for (Mod m : selectedMods) {
				if (m.isActive()) previouslyActiveModCount++;
			}

			//Update the priority of our columns
			uiService.getCurrentModList().sort(Comparator.comparing(Mod::getLoadPriority));
			for (int i = 0; i < uiService.getCurrentModList().size(); i++) {
				uiService.getCurrentModList().get(i).setLoadPriority(i + 1);
			}

			if (!modTable.getSortOrder().isEmpty()) {
				TableColumn<Mod, ?> sortedColumn = modTable.getSortOrder().getFirst();
				TableColumn.SortType sortedColumnSortType = modTable.getSortOrder().getFirst().getSortType();
				sortedColumn.setSortType(null);
				modTable.refresh();
				sortedColumn.setSortType(sortedColumnSortType);
			}

			uiService.modifyActiveModCount(-previouslyActiveModCount);
			Result<Void> updateResult = uiService.updateModListProfileModList();
            if(updateResult.isFailure()) {
                uiService.log(updateResult);
                Popup.displaySimpleAlert("Failed to delete mod from modlist. See log for more information.", MessageType.ERROR);
            }
		}
	}

    private void checkResult(Result<?> result, String failMessage) {
        if(result.isFailure()) {
            uiService.log(result);
            Popup.displaySimpleAlert(failMessage, MessageType.ERROR);
            throw new RuntimeException(result.getCurrentMessage());
        } else {
            uiService.logPrivate(result);
        }
    }
}
