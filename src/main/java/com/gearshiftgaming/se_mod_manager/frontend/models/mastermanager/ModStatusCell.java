package com.gearshiftgaming.se_mod_manager.frontend.models.mastermanager;

import com.gearshiftgaming.se_mod_manager.backend.models.mod.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.ModDownloadStatus;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.Result;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
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

    private final ProgressBar progressBar;

    private final Label progressMessage;

    private Task<Result<Void>> boundTask = null;

    private ModTableRow boundRow = null;

    private static final String UNKNOWN_STATUS_MESSAGE = "Unknown";

    private final ChangeListener<Task<Result<Void>>> taskChangeListener = (obs, oldTask, newTask) -> Platform.runLater(() -> bindToTask(newTask));

    public ModStatusCell() {
        super();
        progressBar = new ProgressBar();
        progressBar.setStyle("-fx-border-color: -color-border-default;-fx-border-width: 1px; -fx-border-radius: 4;");
        progressMessage = new Label();
        StackPane progressLayout = new StackPane(progressBar, progressMessage);
        progressLayout.setAlignment(Pos.CENTER);

        layout = new HBox(5d, progressLayout);
        layout.setAlignment(Pos.CENTER_LEFT);

        tableRowProperty().addListener((obs, oldRow, newRow) -> {
            if(boundRow != null)
                boundRow.getModDownloadTaskProperty().removeListener(taskChangeListener);

            boundRow = (ModTableRow) newRow;
            ((ModTableRow) newRow).getModDownloadTaskProperty().addListener(taskChangeListener);
        });
    }

    @Override
    protected void updateItem(Mod item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            unbindFromTask();
            setGraphic(null);
            setStyle(null);
            return;
        }

        if(boundTask == null || boundTask.isDone()) {
            unbindFromTask();
            progressMessage.setText(item.getModDownloadStatus() != null ? item.getModDownloadStatus().getDisplayName() : UNKNOWN_STATUS_MESSAGE);
            progressBar.setVisible(false);
        }

        setGraphic(layout);
    }

    private void bindToTask(Task<Result<Void>> task) {
        if (task == boundTask) return; // no change

        unbindFromTask();
        boundTask = task;

        ModDownloadStatus modDownloadStatus = getItem().getModDownloadStatus();

        if (task == null) {
            progressBar.setVisible(false);
            progressMessage.setText("");
            setGraphic(null);
            return;
        }

        progressBar.setVisible(true);
        progressBar.progressProperty().bind(task.progressProperty());
        progressMessage.textProperty().bind(task.messageProperty());
        setGraphic(layout);

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (boundTask == task) {
                unbindFromTask();
                progressMessage.setText(modDownloadStatus != null ? modDownloadStatus.getDisplayName() : UNKNOWN_STATUS_MESSAGE);
                progressBar.setVisible(false);
            }
        }));

        task.setOnCancelled(e -> Platform.runLater(() -> {
            if (boundTask == task) {
                unbindFromTask();
                progressMessage.setText(modDownloadStatus != null ? modDownloadStatus.getDisplayName() : UNKNOWN_STATUS_MESSAGE);
                progressBar.setVisible(false);
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            if (boundTask == task) {
                unbindFromTask();
                progressMessage.setText(modDownloadStatus != null ? modDownloadStatus.getDisplayName() : UNKNOWN_STATUS_MESSAGE);
                progressBar.setVisible(false);
            }
        }));
    }

    private void unbindFromTask() {
        if (boundTask != null) {
            cleanupBindings();
        }
    }

    private void cleanupBindings() {
        progressBar.progressProperty().unbind();
        progressMessage.textProperty().unbind();
        boundTask = null;
    }
}
