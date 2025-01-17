package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.SteamMod;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.view.ModlistManagerView;
import com.gearshiftgaming.se_mod_manager.frontend.view.helper.ModlistManagerHelper;
import com.gearshiftgaming.se_mod_manager.frontend.view.utility.Popup;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
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

    private final UiService UI_SERVICE;

    private final DataFormat SERIALIZED_MIME_TYPE;
    private final List<Mod> SELECTIONS;

    private TableRow<Mod> previousRow;

    private final ModlistManagerView MODLIST_MANAGER_VIEW;

    private final ModlistManagerHelper MODLIST_MANAGER_HELPER;

    private ScrollBar modTableVerticalScrollBar;

    private enum RowBorderType {
        TOP,
        BOTTOM
    }

    public ModTableRowFactory(UiService uiService, DataFormat serializedMimeType, List<Mod> selections, ModlistManagerView modlistManagerView, ModlistManagerHelper modlistManagerHelper) {
        this.UI_SERVICE = uiService;
        this.SERIALIZED_MIME_TYPE = serializedMimeType;
        this.SELECTIONS = selections;
        this.MODLIST_MANAGER_VIEW = modlistManagerView;
        this.MODLIST_MANAGER_HELPER = modlistManagerHelper;
    }

    @Override
    public ModTableRow call(@NotNull TableView<Mod> modTable) {
        modTableVerticalScrollBar = (ScrollBar) modTable.lookup(".scroll-bar:vertical");
        final ModTableRow row = new ModTableRow(UI_SERVICE);

        //Setup our context menu
        final ContextMenu TABLE_CONTEXT_MENU = new ContextMenu();

        final MenuItem WEB_BROWSE_MENU_ITEM = new MenuItem("Open selected mod pages");
        WEB_BROWSE_MENU_ITEM.setOnAction(actionEvent -> {
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

        final MenuItem ACTIVATE_MODS_MENU_ITEM = new MenuItem("Activate selected mods");
        ACTIVATE_MODS_MENU_ITEM.setOnAction(actionEvent -> {
            final List<Mod> selectedMods = new ArrayList<>(modTable.getSelectionModel().getSelectedItems());
            for (Mod m : selectedMods) {
				if(!m.isActive()) {
					m.setActive(true);
					UI_SERVICE.modifyActiveModCount(m);
				}
            }
            modTable.refresh();
            UI_SERVICE.saveUserData();
        });

        final MenuItem DEACTIVATE_MODS_MENU_ITEM = new MenuItem("Deactivate selected mods");
        DEACTIVATE_MODS_MENU_ITEM.setOnAction(actionEvent -> {
            final List<Mod> selectedMods = new ArrayList<>(modTable.getSelectionModel().getSelectedItems());
            for (Mod m : selectedMods) {
				if(m.isActive()) {
					m.setActive(false);
					UI_SERVICE.modifyActiveModCount(m);
				}
            }
            modTable.refresh();
            UI_SERVICE.saveUserData();
        });

        final MenuItem DELETE_MENU_ITEM = new MenuItem("Delete selected mods");
        DELETE_MENU_ITEM.disableProperty().bind(Bindings.isEmpty(modTable.getSelectionModel().getSelectedItems()));
        DELETE_MENU_ITEM.setOnAction(actionEvent -> {
            deleteMods(modTable);
        });

		final MenuItem SELECT_ALL_MENU_ITEM = new MenuItem("Select all");
		SELECT_ALL_MENU_ITEM.setOnAction(event -> {
			modTable.getSelectionModel().selectAll();
		});

        TABLE_CONTEXT_MENU.getItems().addAll(ACTIVATE_MODS_MENU_ITEM, DEACTIVATE_MODS_MENU_ITEM, WEB_BROWSE_MENU_ITEM, DELETE_MENU_ITEM, SELECT_ALL_MENU_ITEM);

        row.contextMenuProperty().bind(
                Bindings.when(Bindings.isNotNull(row.itemProperty()))
                        .then(TABLE_CONTEXT_MENU)
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
                    SELECTIONS.clear();

                    ObservableList<Mod> items = modTable.getSelectionModel().getSelectedItems();

                    SELECTIONS.addAll(items);

                    Dragboard dragboard = row.startDragAndDrop(TransferMode.MOVE);
                    dragboard.setDragView(row.snapshot(null, null));
                    ClipboardContent clipboardContent = new ClipboardContent();
                    clipboardContent.put(SERIALIZED_MIME_TYPE, index);
                    dragboard.setContent(clipboardContent);

                    dragEvent.consume();
                }
            }
        });

        row.setOnDragOver(dragEvent -> {
            Dragboard dragboard = dragEvent.getDragboard();
            if (dragboard.hasContent(SERIALIZED_MIME_TYPE)) {
                if (row.getIndex() != ((Integer) dragboard.getContent(SERIALIZED_MIME_TYPE))) {
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
                MODLIST_MANAGER_VIEW.setPreviousRow(previousRow);
            }

            if (dragEvent.getDragboard().hasContent(SERIALIZED_MIME_TYPE)) {
                if (!row.isEmpty()) {
                    addBorderToRow(RowBorderType.TOP, row);
                }
            }
            dragEvent.consume();
        });

        row.setOnDragExited(dragEvent -> {
            //If we are not the last item and the row isn't blank, set it to null. Else, set a bottom border.
            if (!row.isEmpty() && previousRow.getItem().equals(UI_SERVICE.getCurrentModList().getLast())) {
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

            if (dragboard.hasContent(SERIALIZED_MIME_TYPE)) {
                int dropIndex;
                Mod mod = null;

                if (row.isEmpty()) {
                    dropIndex = UI_SERVICE.getCurrentModList().size();
                } else {
                    dropIndex = row.getIndex();
                    mod = UI_SERVICE.getCurrentModList().get(dropIndex);
                }

                int delta = 0;
                if (mod != null) {
                    while (SELECTIONS.contains(mod)) {
                        delta = 1;
                        --dropIndex;
                        if (dropIndex < 0) {
                            mod = null;
                            dropIndex = 0;
                            break;
                        }
                        mod = UI_SERVICE.getCurrentModList().get(dropIndex);
                    }
                }

                for (Mod m : SELECTIONS) {
                    UI_SERVICE.getCurrentModList().remove(m);
                }

                if (mod != null) {
                    dropIndex = UI_SERVICE.getCurrentModList().indexOf(mod) + delta;
                } else if (dropIndex != 0) {
                    dropIndex = UI_SERVICE.getCurrentModList().size();
                }

                modTable.getSelectionModel().clearSelection();

                for (Mod m : SELECTIONS) {
                    UI_SERVICE.getCurrentModList().add(dropIndex, m);
                    modTable.getSelectionModel().select(dropIndex);
                    dropIndex++;
                }
                dragEvent.setDropCompleted(true);
                SELECTIONS.clear();

                MODLIST_MANAGER_HELPER.setCurrentModListLoadPriority(modTable, UI_SERVICE);

                //Redo our sort since our row order has changed
                modTable.sort();

				/*
					We shouldn't need this since currentModList which backs our table is an observable list backed by the currentModProfile.getModList,
					but for whatever reason the changes aren't propagating without this.
				 */
                //TODO: Look into why the changes don't propagate without setting it here. Indicative of a deeper issue or misunderstanding.
                UI_SERVICE.getCurrentModlistProfile().setModList(UI_SERVICE.getCurrentModList());
                UI_SERVICE.saveUserData();

                dragEvent.consume();
            }
        });

        row.setOnDragDone(dragEvent -> {
            if (MODLIST_MANAGER_VIEW.getScrollTimeline() != null) MODLIST_MANAGER_VIEW.getScrollTimeline().stop();

            // Remove any borders and perform clean-up actions here
            if (previousRow != null) previousRow.setBorder(null);
            dragEvent.consume();
        });

        //This is a dumb hack but I can't get the row's height any other way
        if (MODLIST_MANAGER_VIEW.getSingleTableRow() == null) MODLIST_MANAGER_VIEW.setSingleTableRow(row);

        return row;
    }

    private void addBorderToRow(RowBorderType rowBorderType, @NotNull ModTableRow row) {
        if (!row.isEmpty() || (row.getIndex() <= UI_SERVICE.getCurrentModList().size() && UI_SERVICE.getCurrentModList().get(row.getIndex() - 1) != null)) {
            Color indicatorColor = Color.web(MODLIST_MANAGER_HELPER.getSelectedCellBorderColor(UI_SERVICE));
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
		int choice = Popup.displayYesNoDialog("Are you sure you want to delete these mods?", MODLIST_MANAGER_VIEW.getSTAGE(), MessageType.WARN);
		if (choice == 1) {
			final List<Mod> selectedMods = new ArrayList<>(modTable.getSelectionModel().getSelectedItems());

			//TODO: Look into why if we use getCurrentModProfile it returns null. Indicative of a deeper issue or misunderstanding just like in the droprow.
			UI_SERVICE.getCurrentModList().removeAll(selectedMods);
			UI_SERVICE.getCurrentModlistProfile().setModList(UI_SERVICE.getCurrentModList());

			int previouslyActiveModCount = 0;

			for (Mod m : selectedMods) {
				if (m.isActive()) previouslyActiveModCount++;
			}

			//Update the priority of our columns
			UI_SERVICE.getCurrentModList().sort(Comparator.comparing(Mod::getLoadPriority));
			for (int i = 0; i < UI_SERVICE.getCurrentModList().size(); i++) {
				UI_SERVICE.getCurrentModList().get(i).setLoadPriority(i + 1);
			}

			if (!modTable.getSortOrder().isEmpty()) {
				TableColumn<Mod, ?> sortedColumn = modTable.getSortOrder().getFirst();
				TableColumn.SortType sortedColumnSortType = modTable.getSortOrder().getFirst().getSortType();
				sortedColumn.setSortType(null);
				modTable.refresh();
				sortedColumn.setSortType(sortedColumnSortType);
			}

			UI_SERVICE.modifyActiveModCount(-previouslyActiveModCount);
			UI_SERVICE.saveUserData();
		}
	}
}
