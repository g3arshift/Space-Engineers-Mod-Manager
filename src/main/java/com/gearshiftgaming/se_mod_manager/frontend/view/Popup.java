package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/** Displays a custom popup modal using icons from the Ikonli CarbonIcon icon pack.
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.

 */
public class Popup {

    private static final int FONT_SIZE = 16;
    private static final int ICON_SIZE = 30;

    //Displays a Yes/No dialog centered on a specific stage
    public static int displayYesNoDialog(String message, Stage parentStage, MessageType messageType) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        Label label = new Label(message);
        FontIcon messageIcon = new FontIcon();

        getIconByMessageType(messageType, messageIcon, stage);

        return yesNoDialog(stage, parentStage, label, messageIcon);
    }

    //Displays a Yes/No dialog centered on the screen
    public static int displayYesNoDialog(String message, MessageType messageType) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        Label label = new Label(message);
        FontIcon messageIcon = new FontIcon();

        getIconByMessageType(messageType, messageIcon, stage);

        return yesNoDialog(stage, label, messageIcon);
    }

    //Displays a simple alert with only one option centered on a specific stage, with a result being the input
    public static <T> void displaySimpleAlert(Result<T> result, Stage parentStage) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        Label label = new Label(result.getCurrentMessage());
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
        simpleAlert(stage, parentStage, label, messageIcon);
    }

    //Displays a simple alert with only one option centered on a specific stage
    public static void displaySimpleAlert(String message, Stage parentStage, MessageType messageType) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        Label label = new Label(message);
        FontIcon messageIcon = new FontIcon();

        getIconByMessageType(messageType, messageIcon, stage);

        simpleAlert(stage, parentStage, label, messageIcon);
    }

    //Displays a simple alert with only one option centered on the screen
    public static void displaySimpleAlert(String message, MessageType messageType) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        Label label = new Label(message);
        FontIcon messageIcon = new FontIcon();

        getIconByMessageType(messageType, messageIcon, stage);

        simpleAlert(stage, label, messageIcon);
    }

    //Displays a simple alert with only one option centered on the screen, with a clickable link for the end of the error message.
    public static void displaySimpleAlert(String message, String link, MessageType messageType) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        FontIcon messageIcon = new FontIcon();

        getIconByMessageType(messageType, messageIcon, stage);

        simpleAlert(stage, message, link, messageIcon);
    }

    //Creates a simple alert centered on a specific stage
    private static void simpleAlert(Stage childStage, Stage parentStage, Label label, FontIcon messageIcon) {
        HBox dialogBox = makeDialogBoxWithLink(label, messageIcon);

        //Setup our button
        HBox buttonBar = makeOkBar(childStage);

        createPopup(childStage, parentStage, dialogBox, buttonBar);
    }

    //Creates a simple alert centered on the screen
    private static void simpleAlert(Stage childStage, Label label, FontIcon messageIcon) {
        HBox dialogBox = makeDialogBoxWithLink(label, messageIcon);

        //Setup our button
        HBox buttonBar = makeOkBar(childStage);

        createPopup(childStage, dialogBox, buttonBar);
    }

    //Creates a simple alert centered on the screen, with a clickable link
    private static void simpleAlert(Stage childStage, String message, String link, FontIcon messageIcon) {
        HBox dialogBox = makeDialogBoxWithLink(message, link, messageIcon);

        //Setup our button
        HBox buttonBar = makeOkBar(childStage);

        createPopup(childStage, dialogBox, buttonBar);
    }

    //Creates a yes/no dialog centered on a specific stage
    private static int yesNoDialog(Stage childStage, Stage parentStage, Label label, FontIcon messageIcon) {
        AtomicInteger choice = new AtomicInteger(-1);

        HBox dialogBox = makeDialogBoxWithLink(label, messageIcon);

        HBox buttonBar = makeYesNoBar(choice, childStage);

        createPopup(childStage, parentStage, dialogBox, buttonBar);

        return choice.intValue();
    }

    //Creates a yes/no dialog centered the screen
    private static int yesNoDialog(Stage childStage, Label label, FontIcon messageIcon) {
        AtomicInteger choice = new AtomicInteger(-1);

        HBox dialogBox = makeDialogBoxWithLink(label, messageIcon);

        HBox buttonBar = makeYesNoBar(choice, childStage);

        createPopup(childStage, dialogBox, buttonBar);

        return choice.intValue();
    }

    private static HBox makeYesNoBar(AtomicInteger choice, Stage stage) {
        Button noButton = new Button();
        Button yesButton = new Button();

        noButton.setText("No");
        yesButton.setText("Yes");

        noButton.setOnAction((ActionEvent event) -> {
            choice.set(0);
            stage.close();
        });

        yesButton.setOnAction((ActionEvent event) -> {
            choice.set(1);
            stage.close();
        });

        noButton.setMinWidth(80d);
        noButton.setMinHeight(36d);
        noButton.setMaxHeight(36d);

        yesButton.setMinWidth(80d);
        yesButton.setMinHeight(36d);
        yesButton.setMaxHeight(36d);

        HBox buttonBar = new HBox(yesButton, noButton);

        buttonBar.setPadding(new Insets(5, 5, 5, 5));
        buttonBar.setStyle("-fx-background-color: -color-neutral-subtle;");
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setSpacing(10);

        return buttonBar;
    }

    private static HBox makeOkBar(Stage childStage) {
        Button quitButton = new Button();
        quitButton.setText("OK");
        quitButton.setOnAction((ActionEvent event) -> childStage.close());

        quitButton.setMinWidth(80d);
        quitButton.setMinHeight(36d);
        quitButton.setMaxHeight(36d);

        HBox buttonBar = new HBox(quitButton);
        buttonBar.setPadding(new Insets(5, 5, 5, 5));
        buttonBar.setStyle("-fx-background-color: -color-neutral-subtle;");
        buttonBar.setAlignment(Pos.CENTER);
        return buttonBar;
    }

    private static void centerStage(Stage childStage, Stage parentStage) {
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

    private static void centerStage(Stage stage) {
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

    //Creates a dialog box message
    private static HBox makeDialogBoxWithLink(Label label, FontIcon messageIcon) {
        label.setStyle("-fx-font-size: " + FONT_SIZE + ";");
        messageIcon.getStyleClass().clear();
        messageIcon.setIconSize(ICON_SIZE);
        label.setWrapText(true);

        HBox dialogBox = new HBox(messageIcon, label);
        dialogBox.setAlignment(Pos.TOP_LEFT);
        dialogBox.setPadding(new Insets(0, 5, 0, 5));
        dialogBox.setSpacing(5d);
        dialogBox.setMaxWidth(600);

        return dialogBox;
    }

    //Creates a dialog box message, with a hyperlink
    private static HBox makeDialogBoxWithLink(String message, String link, FontIcon messageIcon) {
        messageIcon.getStyleClass().clear();
        messageIcon.setIconSize(ICON_SIZE);

        Label label = new Label(message);
        label.setStyle("-fx-font-size: " + FONT_SIZE + ";");
        label.setWrapText(true);

        HBox dialogBox = createLinkBox(link, messageIcon, label);
        dialogBox.setAlignment(Pos.TOP_LEFT);
        dialogBox.setPadding(new Insets(0, 5, 0, 5));
        dialogBox.setSpacing(5d);
        dialogBox.setMaxWidth(600);

        return dialogBox;
    }

    private static HBox createLinkBox(String link, FontIcon messageIcon, Label label) {
        Hyperlink hyperlink = new Hyperlink("https://spaceengineersmodmanager.com/bugreport");
        hyperlink.setStyle("-fx-font-size: " + FONT_SIZE + ";");

        hyperlink.setOnAction(actionEvent -> {
            try {
                Desktop.getDesktop().browse(new URI(link));
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });

        VBox textLayout = new VBox(label, hyperlink);
        textLayout.setAlignment(Pos.CENTER);

		return new HBox(messageIcon, textLayout);
    }

    private static void getIconByMessageType(MessageType messageType, FontIcon messageIcon, Stage stage) {
        switch (messageType) {
            case INFO -> {
                messageIcon.setStyle("-fx-icon-color: -color-accent-emphasis;");
                messageIcon.setIconLiteral("ci-information-square");
                stage.setTitle("Info");
            }
            case WARN -> {
                messageIcon.setStyle("-fx-icon-color: -color-warning-emphasis;");
                messageIcon.setIconLiteral("ci-warning-alt");
                stage.setTitle("Warning");
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
    }

    private static void createPopup(Stage childStage, Stage parentStage, HBox dialogBox, HBox buttonBar) {
        prepareStage(childStage, dialogBox, buttonBar);

        centerStage(childStage, parentStage);

        childStage.showAndWait();
    }

    private static void createPopup(Stage childStage, HBox dialogBox, HBox buttonBar) {
        prepareStage(childStage, dialogBox, buttonBar);

        centerStage(childStage);

        childStage.showAndWait();
    }

    private static void prepareStage(Stage childStage, HBox dialogBox, HBox buttonBar) {
        VBox contents = new VBox(dialogBox, buttonBar);
        contents.setSpacing(10);

        Scene scene = new Scene(contents);
        childStage.getIcons().add(new Image(Objects.requireNonNull(Popup.class.getResourceAsStream("/icons/logo.png"))));
        childStage.setResizable(false);

        childStage.setScene(scene);
    }
}