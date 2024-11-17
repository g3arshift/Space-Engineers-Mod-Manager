package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.ModType;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import com.gearshiftgaming.se_mod_manager.frontend.view.ModlistManagerView;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.MenuItem;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
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

	private enum RowBorderType {
		TOP,
		BOTTOM
	}


	public ModTableRowFactory(UiService uiService, DataFormat serializedMimeType, List<Mod> selections, ModlistManagerView modlistManagerView) {
		this.UI_SERVICE = uiService;
		this.SERIALIZED_MIME_TYPE = serializedMimeType;
		this.SELECTIONS = selections;
		this.MODLIST_MANAGER_VIEW = modlistManagerView;
	}


	//TODO: Add listener for delete key to delete mods
	//TODO: Add popup for "are you sure you want to delete this/these mods?" Use Popup class.

	@Override
	public ModTableRow call(TableView<Mod> modTable) {
		final ModTableRow row = new ModTableRow(UI_SERVICE);

		//Setup our context menu
		final MenuItem WEB_BROWSE_MENU_ITEM = new MenuItem("Open mod page");
		final ContextMenu TABLE_CONTEXT_MENU = new ContextMenu();

		WEB_BROWSE_MENU_ITEM.setOnAction(actionEvent -> {
			final List<Mod> selectedMods = new ArrayList<>(modTable.getSelectionModel().getSelectedItems());
			for (Mod m : selectedMods) {
				try {
					if (m.getModType() == ModType.STEAM) {
						Desktop.getDesktop().browse(new URI("https://steamcommunity.com/workshop/filedetails/?id=" + m.getId()));
					} else {
						Desktop.getDesktop().browse(new URI("https://mod.io/search/mods/" + m.getId()));
					}
				} catch (URISyntaxException | IOException e) {
					throw new RuntimeException(e);
				}
			}
		});

		final MenuItem DELETE_MENU_ITEM = new MenuItem("Delete selected mod");
		DELETE_MENU_ITEM.disableProperty().bind(Bindings.isEmpty(modTable.getSelectionModel().getSelectedItems()));
		DELETE_MENU_ITEM.setOnAction(actionEvent -> {
			final List<Mod> selectedMods = new ArrayList<>(modTable.getSelectionModel().getSelectedItems());

			//TODO: Look into why if we use getCurrentModProfile it returns null. Indicative of a deeper issue or misunderstanding just like in the droprow.
			modTable.getItems().removeAll(selectedMods);
			UI_SERVICE.getCurrentModList().removeAll(selectedMods);
			UI_SERVICE.getCurrentModProfile().setModList(UI_SERVICE.getCurrentModList());

			//Update the priority of our columns

			modTable.getItems().sort(Comparator.comparing(Mod::getLoadPriority));
			for (int i = 0; i < modTable.getItems().size(); i++) {
				modTable.getItems().get(i).setLoadPriority(i + 1);
			}

			if (!modTable.getSortOrder().isEmpty()) {
				TableColumn<Mod, ?> sortedColumn = modTable.getSortOrder().getFirst();
				TableColumn.SortType sortedColumnSortType = modTable.getSortOrder().getFirst().getSortType();
				sortedColumn.setSortType(null);
				modTable.refresh();
				sortedColumn.setSortType(sortedColumnSortType);
			}
			UI_SERVICE.saveUserData();
		});


		TABLE_CONTEXT_MENU.getItems().addAll(WEB_BROWSE_MENU_ITEM, DELETE_MENU_ITEM);

		row.contextMenuProperty().bind(
				Bindings.when(Bindings.isNotNull(row.itemProperty()))
						.then(TABLE_CONTEXT_MENU)
						.otherwise((ContextMenu) null));

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
					addBorderToRow(RowBorderType.TOP, modTable, row);
				}
			}
			dragEvent.consume();
		});

		row.setOnDragExited(dragEvent -> {
			//If we are not the last item and the row isn't blank, set it to null. Else, set a bottom border.
			if (!row.isEmpty() && previousRow.getItem().equals(modTable.getItems().getLast())) {
				//Our if conditions are organized this way because the .lookup function is not wholly inexpensive and it's getting called often.
				ScrollBar verticalScrollBar = (ScrollBar) modTable.lookup(".scroll-bar:vertical");
				//We don't want to add a border if the table isn't big enough to display all mods at once since we'll end up with a double border
				if (!verticalScrollBar.isVisible()) {
					addBorderToRow(RowBorderType.BOTTOM, modTable, row);
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
					dropIndex = modTable.getItems().size();
				} else {
					dropIndex = row.getIndex();
					mod = modTable.getItems().get(dropIndex);
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
						mod = modTable.getItems().get(dropIndex);
					}
				}

				for (Mod m : SELECTIONS) {
					modTable.getItems().remove(m);
				}

				if (mod != null) {
					dropIndex = modTable.getItems().indexOf(mod) + delta;
				} else if (dropIndex != 0) {
					dropIndex = modTable.getItems().size();
				}

				modTable.getSelectionModel().clearSelection();

				for (Mod m : SELECTIONS) {
					modTable.getItems().add(dropIndex, m);
					modTable.getSelectionModel().select(dropIndex);
					dropIndex++;
				}
				dragEvent.setDropCompleted(true);
				SELECTIONS.clear();

				//TODO: Move to a helper class
				//If we are ascending or not sorted then set the load priority equal to the spot in the list, minus one.
				//If we are descending then set the load priority to its inverse position.
				if (modTable.getSortOrder().isEmpty() || modTable.getSortOrder().getFirst().getSortType().equals(TableColumn.SortType.ASCENDING)) {
					for (int i = 0; i < UI_SERVICE.getCurrentModProfile().getModList().size(); i++) {
						UI_SERVICE.getCurrentModList().get(i).setLoadPriority(i + 1);
					}
				} else {
					for (int i = 0; i < UI_SERVICE.getCurrentModProfile().getModList().size(); i++) {
						UI_SERVICE.getCurrentModList().get(i).setLoadPriority(getIntendedLoadPriority(modTable, i));
					}
				}

				//Redo our sort since our row order has changed
				modTable.sort();

				/*
					We shouldn't need this since currentModList which backs our table is an observable list backed by the currentModProfile.getModList,
					but for whatever reason the changes aren't propagating without this.
				 */

				//TODO: Look into why the changes don't propagate without setting it here. Indicative of a deeper issue or misunderstanding.
				UI_SERVICE.getCurrentModProfile().setModList(UI_SERVICE.getCurrentModList());
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

	private void addBorderToRow(RowBorderType rowBorderType, TableView<Mod> modTable, ModTableRow row) {
		if (!row.isEmpty() || (row.getIndex() <= modTable.getItems().size() && modTable.getItems().get(row.getIndex() - 1) != null)) {
			Color indicatorColor = Color.web(getSelectedCellBorderColor());
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

	/*
		Gets the actual position for our load priority. If we are in descending order, we actually need to set load priority to the exact opposite number in the list from where it was dropped.
		So if we are descending and drop the item at the very top of the list, we actually want to make its load priority the last, while keeping its actual position in the list where we dropped it.
	 */
	private int getIntendedLoadPriority(TableView<Mod> modTable, int index) {
		//Check if we are in ascending/default order, else we're in descending order
		if (modTable.getSortOrder().isEmpty() || modTable.getSortOrder().getFirst().getSortType().equals(TableColumn.SortType.ASCENDING)) {
			return index;
		} else {
			return UI_SERVICE.getCurrentModList().size() - index;
		}
	}

	private String getSelectedCellBorderColor() {
		return switch (UI_SERVICE.getUSER_CONFIGURATION().getUserTheme()) {
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
