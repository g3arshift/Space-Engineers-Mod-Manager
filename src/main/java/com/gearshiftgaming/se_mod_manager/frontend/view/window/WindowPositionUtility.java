package com.gearshiftgaming.se_mod_manager.frontend.view.window;

import javafx.stage.Stage;

public class WindowPositionUtility {

    private WindowPositionUtility(){}

    /**
     * Centers a JavaFX stage on another stage and returns the listeners used to do so.
     * @param childStage The stage to be centered.
     * @param parentStage The stage to center the child stage on.
     */
    public static void centerStageOnStage(Stage childStage, Stage parentStage) {
        childStage.setX(parentStage.getX() + parentStage.getWidth() / 2 - childStage.getWidth() / 2);
        childStage.setY(parentStage.getY() + parentStage.getHeight() / 2 - childStage.getHeight() / 2);
    }
}
