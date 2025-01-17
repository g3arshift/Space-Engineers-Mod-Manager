package com.gearshiftgaming.se_mod_manager.frontend.view.utility;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class WindowPositionUtility {

    /**
     * Centers a JavaFX stage on another stage and returns the listeners used to do so.
     * @param childStage The stage to be centered.
     * @param parentStage The stage to center the child stage on.
     */
    public static void centerStageOnStage(Stage childStage, Stage parentStage) {
        childStage.setX(parentStage.getX() + parentStage.getWidth() / 2 - childStage.getWidth() / 2);
        childStage.setY(parentStage.getY() + parentStage.getHeight() / 2 - childStage.getHeight() / 2);
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
