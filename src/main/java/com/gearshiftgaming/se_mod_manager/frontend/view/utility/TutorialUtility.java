package com.gearshiftgaming.se_mod_manager.frontend.view.utility;

import javafx.beans.NamedArg;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class TutorialUtility {

    public static void tutorialElementHighlight(@NamedArg("panes") Pane[] panes, @NamedArg("stageWidth") double stageWidth, @NamedArg("stageHeight") double stageHeight, @NamedArg("node") Node node) {
        final double MARGIN = 5.0;

        Bounds buttonBounds = node.localToScene(node.getBoundsInLocal());
        double buttonX = buttonBounds.getMinX();
        double buttonY = buttonBounds.getMinY();
        double buttonWidth = buttonBounds.getWidth();
        double buttonHeight = buttonBounds.getHeight();

        //Top Pane
        panes[0].setLayoutX(0);
        panes[0].setLayoutY(0);
        panes[0].setPrefWidth(stageWidth);
        panes[0].setPrefHeight(buttonY - MARGIN);

        //Right Pane
        panes[1].setLayoutX(buttonX + buttonWidth + MARGIN);
        panes[1].setLayoutY(buttonY - MARGIN);
        panes[1].setPrefWidth(stageWidth - (buttonX + buttonWidth + MARGIN));
        panes[1].setPrefHeight(buttonHeight + 2 * MARGIN);

        //Bottom Pane
        panes[2].setLayoutX(0);
        panes[2].setLayoutY(buttonY + buttonHeight + MARGIN);
        panes[2].setPrefWidth(stageWidth);
        panes[2].setPrefHeight(stageHeight - (buttonY + buttonHeight + MARGIN));

        //Left Pane
        panes[3].setLayoutX(0);
        panes[3].setLayoutY(buttonY - MARGIN);
        panes[3].setPrefWidth(buttonX - MARGIN);
        panes[3].setPrefHeight(buttonHeight + 2 * MARGIN);
    }

    public static void tutorialCoverStage(@NamedArg("panes") Pane[] panes, @NamedArg("stage") Stage stage) {
        panes[0].setLayoutX(0);
        panes[0].setLayoutY(0);
        panes[0].setPrefWidth(stage.getWidth());
        panes[0].setPrefHeight(stage.getHeight());

        panes[1].setLayoutX(0);
        panes[1].setLayoutY(0);
        panes[1].setPrefWidth(0);
        panes[1].setPrefHeight(0);

        panes[2].setLayoutX(0);
        panes[2].setLayoutY(0);
        panes[2].setPrefWidth(0);
        panes[2].setPrefHeight(0);

        panes[3].setLayoutX(0);
        panes[3].setLayoutY(0);
        panes[3].setPrefWidth(0);
        panes[3].setPrefHeight(0);
    }
}
