package com.gearshiftgaming.se_mod_manager.frontend.view;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Objects;

/**
 * Displays a custom alert modal using icons from the Ikonli CarbonIcon icon pack.
 */
public class Alert {
    public static <T> void display(Result<T> result, Stage parentStage) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        Label label = new Label(result.getMessages().getLast());
        FontIcon messageIcon = new FontIcon();

        label.setStyle("-fx-font-size: 16;");
        messageIcon.getStyleClass().clear();
        messageIcon.setIconSize(30);

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
        //TODO: Should instead have different functions called in the switch cases depending on what case it hits.
        // Use our current one as an "OK only" error display, but set others to have different options. Useful for when we save!
        errorDisplay(stage, parentStage, label, messageIcon);
    }

    public static void display(String message, Stage parentStage, MessageType messageType) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        Label label = new Label(message);
        FontIcon messageIcon = new FontIcon();
        label.setStyle("-fx-font-size: 16;");
        messageIcon.getStyleClass().clear();
        messageIcon.setIconSize(30);

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
        //TODO: Should instead have different functions called in the switch cases depending on what case it hits.
        errorDisplay(stage, parentStage, label, messageIcon);
    }

    private static void errorDisplay(Stage childStage, Stage parentStage, Label label, FontIcon messageIcon) {
        HBox errorInformation = new HBox(messageIcon, label);
        label.setWrapText(true);
        errorInformation.setAlignment(Pos.TOP_LEFT);
        errorInformation.setPadding(new Insets(0, 5, 0 ,5));
        errorInformation.setSpacing(5d);
        errorInformation.setMaxWidth(600);

        //Setup our button
        HBox buttonBar = getButtonBar(childStage);

        VBox contents = new VBox(errorInformation, buttonBar);
        contents.setSpacing(10);

        Scene scene = new Scene(contents);
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

    private static HBox getButtonBar(Stage childStage) {
        Button quitButton = new Button();
        quitButton.setText("OK");
        quitButton.setOnAction((ActionEvent event) -> {
            childStage.close();
        });

        quitButton.setMinWidth(80d);
        quitButton.setMinHeight(36d);
        quitButton.setMaxHeight(36d);

        HBox buttonBar = new HBox(quitButton);
        buttonBar.setPadding(new Insets(5, 5, 5, 5));
        buttonBar.setStyle("-fx-background-color: -color-neutral-subtle;");
        buttonBar.setAlignment(Pos.CENTER);
        return buttonBar;
    }
}