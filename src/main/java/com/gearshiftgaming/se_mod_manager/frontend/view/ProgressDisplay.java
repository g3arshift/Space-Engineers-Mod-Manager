package com.gearshiftgaming.se_mod_manager.frontend.view;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * This class represents all the UI logic surrounding the display elements shown when display a progress panel.
 * <p>
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ProgressDisplay extends StackPane {

    @FXML
    private ProgressBar progressBar;

    @FXML
    private ProgressIndicator progressWheel;

    @FXML
    //This variable holds the value of the message that can be optionally displayed above the progress bar.
    private Label progressTitle;

    @FXML
    private Label progressMessage;

    public ProgressDisplay() {
        final FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/progress-display.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new FXMLLoadException("Failed to load ProgressDisplay FXML", e);
        }
    }

    public void bindProgressAndUpdateValues(ReadOnlyStringProperty updateMessage, ReadOnlyDoubleProperty updateProgress) {
        progressMessage.textProperty().bind(updateMessage);
        progressBar.progressProperty().bind(updateProgress);
    }

    public void unbindProgressAndUpdateValues() {
        progressMessage.textProperty().unbind();
        progressBar.progressProperty().unbind();
    }

    public void setToolNameText(String newToolName) {
        progressTitle.setText(newToolName);
    }

    public void setAllDownloadsCompleteState() {
        progressWheel.setVisible(false);
    }

    public void setDefaultState() {
        setToolNameText("Title Name");
        progressWheel.setVisible(true);
        this.setVisible(false);
        setProgressTitleVisible(false);
        unbindProgressAndUpdateValues();
        this.setOpacity(1d);
    }

    public void setProgressTitleVisible(boolean visible) {
        progressTitle.setVisible(visible);
    }

    public void setProgressTitleName(String title) {
        progressTitle.setText(title);
    }

    public void setProgressWheelVisible(boolean visible) {
        progressWheel.setVisible(visible);
    }
}
