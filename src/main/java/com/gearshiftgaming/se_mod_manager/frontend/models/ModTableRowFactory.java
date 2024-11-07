package com.gearshiftgaming.se_mod_manager.frontend.models;

import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.ModType;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.util.Callback;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


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

//TODO: Make the rows have a specific color with possible pattern like hatching or diagonal lines based on status or conflicts and stuff
//TODO: Make rows reorderable
//TODO: Save on row deletion and reorder
//TODO: Only update priority when drag-dropping
public class ModTableRowFactory implements Callback<TableView<Mod>, TableRow<Mod>> {

	private final UiService UI_SERVICE;

	private final DataFormat SERIALIZED_MIME_TYPE;

	final ArrayList<Mod> SELECTIONS;

	public ModTableRowFactory(UiService uiService, DataFormat serializedMimeType) {
		this.UI_SERVICE = uiService;
		this.SERIALIZED_MIME_TYPE = serializedMimeType;
		SELECTIONS = new ArrayList<>();
	}


	//TODO: Add listener for delete key to delete mods
	//TODO: Add popup for "are you sure you want to delete this/these mods?" Use Popup class.

	@Override
	public ModTableRow call(TableView<Mod> modTableView) {
		final ModTableRow row = new ModTableRow(UI_SERVICE.getUSER_CONFIGURATION().getUserTheme());

		//Setup our context menu
		final MenuItem WEB_BROWSE_MENU_ITEM = new MenuItem("Open mod page");
		final ContextMenu TABLE_CONTEXT_MENU = new ContextMenu();

		WEB_BROWSE_MENU_ITEM.setOnAction(actionEvent -> {
			final List<Mod> selectedMods = new ArrayList<>(modTableView.getSelectionModel().getSelectedItems());
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
		DELETE_MENU_ITEM.disableProperty().bind(Bindings.isEmpty(modTableView.getSelectionModel().getSelectedItems()));
		DELETE_MENU_ITEM.setOnAction(actionEvent -> {
			final List<Mod> selectedMods = new ArrayList<>(modTableView.getSelectionModel().getSelectedItems());

			//TODO: Switch to removeall
			for (Mod m : selectedMods) {
				modTableView.getItems().remove(m);
				UI_SERVICE.getCurrentModProfile().getModList().remove(m);
			}
			UI_SERVICE.saveUserData();
		});

		TABLE_CONTEXT_MENU.getItems().addAll(WEB_BROWSE_MENU_ITEM, DELETE_MENU_ITEM);

		row.contextMenuProperty().bind(
				Bindings.when(Bindings.isNotNull(row.itemProperty()))
						.then(TABLE_CONTEXT_MENU)
						.otherwise((ContextMenu) null));

		//TODO: https://stackoverflow.com/questions/36087741/javafx-getting-cell-value-on-hover
		// Make it so that when hovering over a cell and drag dropping, it creates at the bottom of it an arrow or something to indicate where the cells will be dropped when dropped.
		// Do this maybe by checking if our dragboard for the event has anything?
		// https://docs.oracle.com/javafx/2/drag_drop/jfxpub-drag_drop.htm This may be a better solution, as it uses DRAG_OVER event handler.
		//Setup drag and drop reordering for the table
		row.setOnDragDetected(dragEvent -> {
			if (!row.isEmpty()) {
				Integer index = row.getIndex();
				SELECTIONS.clear();

				ObservableList<Mod> items = modTableView.getSelectionModel().getSelectedItems();

				SELECTIONS.addAll(items);

				Dragboard dragboard = row.startDragAndDrop(TransferMode.MOVE);
				dragboard.setDragView(row.snapshot(null, null));
				ClipboardContent clipboardContent = new ClipboardContent();
				clipboardContent.put(SERIALIZED_MIME_TYPE, index);
				dragboard.setContent(clipboardContent);
				dragEvent.consume();
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

		//TODO: Maybe for the arrow thing to show where you're dragging we should use onDragEntered and onDragExited?
		//TODO: Do it by adding a stackpane to the cell, and stacking an SVG arrow at the bottom of the image when we enter a cell
		//TODO: Break the arrow up into a line and the arrow so we can programmatically have the line scale. Use JAVAFX line for the line, but SVG for the arrow

		row.setOnDragDropped(dragEvent -> {
			Dragboard dragboard = dragEvent.getDragboard();

			if (dragboard.hasContent(SERIALIZED_MIME_TYPE)) {
				int dropIndex;
				Mod mod = null;

				if (row.isEmpty()) {
					dropIndex = modTableView.getItems().size();
				} else {
					dropIndex = row.getIndex();
					mod = modTableView.getItems().get(dropIndex);
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
						mod = modTableView.getItems().get(dropIndex);
					}
				}

				for (Mod m : SELECTIONS) {
					modTableView.getItems().remove(m);
				}

				if (mod != null) {
					dropIndex = modTableView.getItems().indexOf(mod) + delta;
				} else if (dropIndex != 0) {
					dropIndex = modTableView.getItems().size();
				}

				modTableView.getSelectionModel().clearSelection();

				for (Mod m : SELECTIONS) {
					modTableView.getItems().add(dropIndex, m);
					modTableView.getSelectionModel().select(dropIndex);
					dropIndex++;
				}
				dragEvent.setDropCompleted(true);

				SELECTIONS.clear();

				/*
					We shouldn't need this since currentModList which backs our table is an observable list backed by the currentModProfile.getModList,
					but for whatever reason the changes aren't propagating without this.
				 */
				//TODO: Look into why the changes don't propagate without setting it here
				UI_SERVICE.getCurrentModProfile().setModList(UI_SERVICE.getCurrentModList());
				UI_SERVICE.saveUserData();

				dragEvent.consume();
			}
		});

		return row;
	}
}
