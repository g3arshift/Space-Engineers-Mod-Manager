package com.gearshiftgaming.se_mod_manager.frontend.view;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;

/**
 * A UI control that displays progress of a given task to the user, using both a linear {@link  javafx.scene.control.ProgressBar}
 * and a circular {@link javafx.scene.control.ProgressIndicator}.
 * <p>
 * This control is designed to be used in contexts where background tasks report progress updates and optional messages.
 * It supports dynamic binding to task properties and provides utility methods for updating its display state.
 * <p>
 * The control fades out using a {@link javafx.animation.FadeTransition} when closed.
 * A custom action can optionally be run after the close transition completes.
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
        this.setVisible(false);
    }

    public void setDefaultState() {
        setToolNameText("Title Name");
        progressWheel.setVisible(true);
        this.setVisible(false);
        setProgressTitleVisible(false);
        unbindProgressAndUpdateValues();
        this.setOpacity(1d);
        progressBar.setProgress(0);
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

    public void setAllOperationsCompleteState() {
        progressWheel.setVisible(false);
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

    public void show() {
        unbindProgressAndUpdateValues();
        this.setVisible(true);
    }

    public void hide() {
        unbindProgressAndUpdateValues();
        this.setVisible(false);
    }

    public void showWithMessageAndProgressBinding(ReadOnlyStringProperty messageProperty, ReadOnlyDoubleProperty progressProperty) {
        unbindProgressAndUpdateValues();
        this.bindProgressAndUpdateValues(messageProperty, progressProperty);
        this.setVisible(true);
    }

    public void showWithMessageBinding(ReadOnlyStringProperty updateMessage) {
        unbindProgressAndUpdateValues();
        progressMessage.textProperty().bind(updateMessage);
        progressBar.setProgress(-1);
        this.setVisible(true);
    }

    public void close() {
        this.setProgressTitleVisible(false);

        Platform.runLater(() -> {
            FadeTransition fadeTransition = new FadeTransition(Duration.millis(1100), this);
            fadeTransition.setFromValue(1d);
            fadeTransition.setToValue(0d);

            fadeTransition.setOnFinished(actionEvent -> {
                this.setDefaultState();
            });
        });
    }

    public void closeWithCustomPostProcessing(Runnable runnable) {
        this.setProgressTitleVisible(false);

        Platform.runLater(() -> {
            FadeTransition fadeTransition = new FadeTransition(Duration.millis(1100), this);
            fadeTransition.setFromValue(1d);
            fadeTransition.setToValue(0d);

            fadeTransition.setOnFinished(actionEvent -> {
                this.setDefaultState();
                if (runnable != null)
                    runnable.run();
            });
        });
    }
}
