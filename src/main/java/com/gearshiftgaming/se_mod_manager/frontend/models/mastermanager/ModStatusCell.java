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
public class ModStatusCell extends TableCell<Mod, Object> {

    private final HBox layout;

    private final StackPane progressLayout;

    private final ProgressBar progressBar;

    private final Label progressMessage;

    private Task<Result<Void>> modDownloadTask;

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
    protected void updateItem(Object item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            setStyle(null);
            return;
        }
        ModDownloadStatus modDownloadStatus = ((Mod) item).getModDownloadStatus();
        if (modDownloadStatus != null)
            progressMessage.setText(modDownloadStatus.getDisplayName());
        else
            progressMessage.setText("Unknown");

        setGraphic(layout);
    }

    //TODO: Move this to the row factory. Also need to modify the factory and the tableview...
    public void setModDownloadTask(Task<Result<Void>> task) {
        this.modDownloadTask = task;

        if (task != null) {
            progressMessage.textProperty().bind(task.messageProperty());
            progressBar.progressProperty().bind(task.progressProperty());

            task.setOnSucceeded(event -> cleanup());
            task.setOnFailed(event -> cleanup());
            task.setOnCancelled(event -> cleanup());
        }
    }

    private void cleanup() {
        Platform.runLater(() -> {
            unbindTask();
            progressMessage.setText(getTableRow().getItem().getModDownloadStatus().getDisplayName());
            progressLayout.getChildren().remove(progressBar);
        });
    }

    private void unbindTask() {
        if (modDownloadTask != null) {
            progressBar.progressProperty().unbind();
            progressMessage.textProperty().unbind();
            modDownloadTask = null;
        }
    }
}
