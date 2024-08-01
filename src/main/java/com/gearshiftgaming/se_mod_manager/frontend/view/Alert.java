package com.gearshiftgaming.se_mod_manager.frontend.view;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Objects;

/**
 * Displays a custom alert modal using icons from the Ikonli Carbon Icon icon pack.
 */
public class Alert {
    public static <T> void display(Result<T> result) {
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
        setupDisplay(stage, label, messageIcon);
        stage.show();
    }

    public static void display(String message, MessageType messageType) {
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
        setupDisplay(stage, label, messageIcon);
        stage.show();
    }

    private static void setupDisplay(Stage stage, Label label, FontIcon messageIcon) {
        label.setStyle("-fx-font-size: 14;");
        messageIcon.setScaleX(1.2);
        messageIcon.setScaleY(1.2);

        HBox layout = new HBox(messageIcon, label);
        layout.setAlignment(Pos.CENTER);
        layout.setSpacing(5d);

        int windowWidth = 275;
        int windowHeight = 100;

        Scene scene = new Scene(layout, windowWidth, windowHeight);
        stage.setTitle("Error");
        stage.getIcons().add(new Image(Objects.requireNonNull(Alert.class.getResourceAsStream("/icons/logo.png"))));
        stage.setMinWidth(windowWidth);
        stage.setMinHeight(windowHeight);
        stage.setMaxWidth(windowWidth);
        stage.setMaxHeight(windowHeight);

        stage.setScene(scene);
    }
}