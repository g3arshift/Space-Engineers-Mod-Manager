package com.gearshiftgaming.se_mod_manager.frontend.view;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import lombok.Getter;

/**
 * This class contains all the UI logic surrounding the display elements shown when downloading a tool during initial setup.
 * <p>
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ToolManager {

    @FXML
    private ProgressBar toolDownloadProgressBar;

    @FXML
    private ProgressIndicator toolDownloadProgressWheel;

    @FXML
    private Label toolName;

    @FXML
    private Label toolDownloadMessage;

    @FXML
    @Getter
    private StackPane toolDownloadProgressPanel;

    public void bindProgressAndUpdateValues(ReadOnlyStringProperty updateMessage, ReadOnlyDoubleProperty updateProgress) {
        toolDownloadMessage.textProperty().bind(updateMessage);
        toolDownloadProgressBar.progressProperty().bind(updateProgress);
    }

    public void setToolNameText(String newToolName) {
        toolName.setText(newToolName);
    }

    public void setAllDownloadsCompleteState() {
        toolDownloadProgressWheel.setVisible(false);
    }

    public void setDefaultState() {
        setToolNameText("Tool Name");
        toolDownloadProgressWheel.setVisible(true);
    }
}
