package com.gearshiftgaming.se_mod_manager.frontend.view;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Objects;

/**
 * Displays a custom alert modal using icons from the Ikonli Carbon Icon icon pack.
 */
public class Alert {
    public static <T> void display(Result<T> result, Stage parentStage) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        Label label = new Label(result.getMessages().getLast());
        FontIcon messageIcon = new FontIcon();

        switch (result.getType()) {

            case SUCCESS -> {
                messageIcon.setStyle("-fx-icon-color: -color-accent-emphasis;");
                messageIcon.setIconLiteral("ci-information-square");
                stage.setTitle("Success");
            }
            case INVALID -> {
                messageIcon.setStyle("-fx-icon-color: -color-warning-emphasis;");
                messageIcon.setIconLiteral("ci-warning-alt");
                stage.setTitle("Invalid");
            }
            case CANCELLED, FAILED -> {
                messageIcon.setStyle("-fx-icon-color: -color-danger-emphasis;");
                messageIcon.setIconLiteral("ci-warning-square");
                stage.setTitle("Failed");
            }
            default -> {
                messageIcon.setStyle("-fx-icon-color: -color-neutral-emphasis;");
                messageIcon.setIconLiteral("ci-unknown");
                stage.setTitle("Unknown");
            }
        }
        finishDisplay(stage, parentStage, label, messageIcon);
    }

    public static void display(String message, Stage parentStage, MessageType messageType) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        Label label = new Label(message);
        FontIcon messageIcon = new FontIcon();

        switch (messageType) {
            case INFO -> {
                messageIcon.setStyle("-fx-icon-color: -color-accent-emphasis;");
                messageIcon.setIconLiteral("ci-information-square");
                stage.setTitle("Info");
            }
            case WARN -> {
                messageIcon.setStyle("-fx-icon-color: -color-warning-emphasis;");
                messageIcon.setIconLiteral("ci-warning-alt");
                stage.setTitle("Warn");
            }
            case ERROR -> {
                messageIcon.setStyle("-fx-icon-color: -color-danger-emphasis;");
                messageIcon.setIconLiteral("ci-warning-square");
                stage.setTitle("Error");
            }
            default -> {
                messageIcon.setStyle("-fx-icon-color: -color-neutral-emphasis;");
                messageIcon.setIconLiteral("ci-unknown");
                stage.setTitle("Unknown");
            }
        }
        finishDisplay(stage, parentStage, label, messageIcon);
    }

    private static void finishDisplay(Stage childStage, Stage parentStage, Label label, FontIcon messageIcon) {
        label.setStyle("-fx-font-size: 14;");
        messageIcon.setScaleX(1.2);
        messageIcon.setScaleY(1.2);

        HBox layout = new HBox(messageIcon, label);
        layout.setAlignment(Pos.CENTER);
        layout.setSpacing(5d);
        layout.setPadding(new Insets(5, 5, 5, 5));
        layout.setStyle("-fx-background-color: -color-bg-subtle;");

        Scene scene = new Scene(layout);
        childStage.setTitle("Error");
        childStage.getIcons().add(new Image(Objects.requireNonNull(Alert.class.getResourceAsStream("/icons/logo.png"))));
        childStage.setResizable(false);

        childStage.setScene(scene);

        //Center the alert in the middle of the application window by using listeners that will fire off when the window is created but not yet visible.
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

        childStage.show();
    }
}