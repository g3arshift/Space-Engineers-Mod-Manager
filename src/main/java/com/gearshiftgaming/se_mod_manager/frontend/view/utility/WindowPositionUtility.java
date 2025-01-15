package com.gearshiftgaming.se_mod_manager.frontend.view.utility;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class WindowPositionUtility {

    public static void centerStageOnStage(Stage childStage, Stage parentStage) {
        //Center the alert in the middle of the provided stage by using listeners that will fire off when the window is created.
        ChangeListener<Number> widthListener = (observable, oldValue, newValue) -> {
            double stageWidth = newValue.doubleValue();
            childStage.setX(parentStage.getX() + parentStage.getWidth() / 2 - stageWidth / 2);
        };

        ChangeListener<Number> heightListener = (observable, oldValue, newValue) -> {
            double stageHeight = newValue.doubleValue();
            childStage.setY(parentStage.getY() + parentStage.getHeight() / 2 - stageHeight / 2);
        };

        childStage.widthProperty().addListener(widthListener);
        childStage.heightProperty().addListener(heightListener);

        //Once the window is visible, remove the listeners.
        childStage.setOnShown(e -> {
            childStage.widthProperty().removeListener(widthListener);
            childStage.heightProperty().removeListener(heightListener);
        });
    }

    public static void centerStageOnScreen(Stage stage) {
        //Center the alert in the middle of the computer screen by using listeners that will fire off when the window is created.
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        ChangeListener<Number> widthListener = (observable, oldValue, newValue) -> {
            stage.setX((screenBounds.getWidth() - stage.getWidth()) / 2);
        };
        ChangeListener<Number> heightListener = (observable, oldValue, newValue) -> {
            stage.setY((screenBounds.getHeight() - stage.getHeight()) / 2);
        };

        stage.widthProperty().addListener(widthListener);
        stage.heightProperty().addListener(heightListener);

        //Once the window is visible, remove the listeners.
        stage.setOnShown(e -> {
            stage.widthProperty().removeListener(widthListener);
            stage.heightProperty().removeListener(heightListener);
        });
    }
}
