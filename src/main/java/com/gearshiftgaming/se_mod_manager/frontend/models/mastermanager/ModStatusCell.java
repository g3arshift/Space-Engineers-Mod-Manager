package com.gearshiftgaming.se_mod_manager.frontend.models.mastermanager;

import com.gearshiftgaming.se_mod_manager.backend.models.mod.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.Result;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ModStatusCell extends TableCell<Mod, Object> {

    private final HBox layout;

    private ProgressBar progressBar;

    public ModStatusCell() {
        super();
        layout = new HBox();
    }

    @Override
    protected void updateItem(Object item, boolean empty) {
        super.updateItem(item, empty);
    }
}
