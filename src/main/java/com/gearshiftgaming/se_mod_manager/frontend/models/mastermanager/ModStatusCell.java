package com.gearshiftgaming.se_mod_manager.frontend.models.mastermanager;

import com.gearshiftgaming.se_mod_manager.backend.models.mod.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.ModDownloadStatus;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.Result;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.layout.*;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ModStatusCell extends TableCell<Mod, Mod> {

    private final HBox layout;

    private final StackPane progressLayout;

    private final ProgressBar progressBar;

    private final Label progressMessage;

    private Task<Result<Void>> boundTask = null;

    private static final String UNKNOWN_STATUS_MESSAGE = "Unknown";

    public ModStatusCell() {
        super();
        progressBar = new ProgressBar();
        progressBar.setStyle("-fx-border-color: -color-border-default;-fx-border-width: 1px; -fx-border-radius: 4;");
        progressMessage = new Label();
        progressLayout = new StackPane(progressBar, progressMessage);
        progressLayout.setAlignment(Pos.CENTER);

        layout = new HBox(5d, progressLayout);
        layout.setAlignment(Pos.CENTER_LEFT);
    }


    //TODO: Set some display options based on the current mod status.
    @Override
    protected void updateItem(Mod item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            cleanupBindings();
            setGraphic(null);
            setStyle(null);
            return;
        }

        ModDownloadStatus modDownloadStatus = item.getModDownloadStatus();
        ModTableRow row = (ModTableRow) getTableRow();
        Task<Result<Void>> task = row.getTask();
        if (task != null && !task.isDone()) {
            if (task != boundTask) { //Only change it when the task is actually different, cause it'll screw things up otherwise
                cleanupBindings();
                boundTask = task;
                if(!progressBar.isVisible())
                    progressBar.setVisible(true);

                progressBar.progressProperty().bind(task.progressProperty());
                progressMessage.textProperty().bind(task.messageProperty());

                task.setOnSucceeded(e -> Platform.runLater(() -> {
                    if (boundTask == task) {
                        cleanupBindings();
                        progressMessage.setText(modDownloadStatus != null ? modDownloadStatus.getDisplayName() : UNKNOWN_STATUS_MESSAGE);
                        if(progressBar.isVisible())
                            progressBar.setVisible(false);
                    }
                }));

                task.setOnFailed(e -> Platform.runLater(() -> {
                    if (boundTask == task) {
                        cleanupBindings();
                        progressMessage.setText(modDownloadStatus != null ? modDownloadStatus.getDisplayName() : UNKNOWN_STATUS_MESSAGE);
                        if(progressBar.isVisible())
                            progressBar.setVisible(false);
                    }
                }));
            }
        } else {
            progressMessage.setText(modDownloadStatus != null ? modDownloadStatus.getDisplayName() : UNKNOWN_STATUS_MESSAGE);
            if(progressBar.isVisible())
                progressBar.setVisible(false);
        }


        setGraphic(layout);
    }

    private void cleanupBindings() {
        progressBar.progressProperty().unbind();
        progressMessage.textProperty().unbind();
        boundTask = null;
    }
}
