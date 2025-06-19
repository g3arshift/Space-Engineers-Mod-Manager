package com.gearshiftgaming.se_mod_manager.frontend.view;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import lombok.Setter;

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

    @Setter
    private DoubleProperty downloadPercentage;

    private ProgressBar downloadProgressBar;

    private ProgressIndicator downloadProgressWheel;

    private Label toolNamePrefix;

    private Label toolName;

    private Pane toolBlankingPane;

    private StackPane toolDownloadWindow;

    public ToolManager() {
        downloadPercentage = new SimpleDoubleProperty(0d);
        toolNamePrefix = new Label();
        toolName = new Label();
        toolBlankingPane = new Pane();
        downloadProgressBar = new ProgressBar();
        downloadProgressWheel = new ProgressIndicator();

        toolDownloadWindow = new StackPane();

        toolBlankingPane.setOpacity(0.4);
        //TODO: We need to setup our values and settings, esp for stuff like the pane.
    }

    private DoubleProperty getDownloadPercentageProperty() {
        if(downloadPercentage == null)
            downloadPercentage = new SimpleDoubleProperty(0d);
        return downloadPercentage;
    }

    private Double getDownloadPercentage() {
        return downloadPercentage.get();
    }
}
