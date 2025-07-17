package com.gearshiftgaming.se_mod_manager.frontend.view.popup;

import com.gearshiftgaming.se_mod_manager.backend.models.shared.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.Result;
import com.gearshiftgaming.se_mod_manager.frontend.view.window.WindowDressingUtility;
import com.gearshiftgaming.se_mod_manager.frontend.view.window.WindowTitleBarColorUtility;
import javafx.application.Application;
import javafx.application.Platform;
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
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Displays a custom popup modal using icons from the Ikonli CarbonIcon icon pack.
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
//TODO: It might be a good idea to put the dialog boxes in a scroll pane with a fixed max size. That way if we have to log a massive message it doesn't get TOO big.
public class Popup {

    private static final int FONT_SIZE = 16;
    private static final int ICON_SIZE = 30;

    //Reusable style strings to reduce string allocation
    private static final String FONT_STYLE = "-fx-font-size: " + FONT_SIZE + ";";
    private static final String BUTTON_BAR_STYLE = "-fx-background-color: -color-neutral-subtle;";

    //Prevent actual new instances of the class from being made.
    private Popup() {
    }

    /**
     * Displays a Yes/No dialog centered on a specific stage
     *
     * @param message     The message to display
     * @param parentStage The stage this will be centered on
     * @param messageType The type of message this is
     * @return a TwoButtonChoice enum
     */
    public static TwoButtonChoice displayYesNoDialog(String message, Stage parentStage, MessageType messageType) {
        Stage stage = createStage();

        Label label = new Label(message);
        FontIcon messageIcon = new FontIcon();

        getIconByMessageType(messageType, messageIcon);

        return yesNoDialog(stage, parentStage, label, messageIcon);
    }

    /**
     * Displays a Yes/No dialog centered on the screen
     * @return a TwoButtonChoice enum
     */
    public static TwoButtonChoice displayYesNoDialog(String message, MessageType messageType) throws IOException {
        Stage stage = createStage();

        Label label = new Label(message);
        FontIcon messageIcon = new FontIcon();

        getIconByMessageType(messageType, messageIcon);

        return yesNoDialog(stage, label, messageIcon);
    }

    public static <T> TwoButtonChoice displayYesNoDialog(Result<T> result) throws IOException {
        Stage stage = createStage();

        Label label = new Label(result.getCurrentMessage());
        FontIcon messageIcon = new FontIcon();
        setResultWindowDressing(result, messageIcon);

        return yesNoDialog(stage, label, messageIcon);
    }

    /**
     * Displays a simple alert with only one option centered on a specific stage, with a result being the input
     *
     * @param parentStage The stage this popup will be centered on
     */
    public static <T> void displaySimpleAlert(Result<T> result, Stage parentStage) {
        Stage stage = createStage();

        Label label = new Label(result.getCurrentMessage());
        FontIcon messageIcon = new FontIcon();

        setResultWindowDressing(result, messageIcon);
        simpleAlert(stage, parentStage, label, messageIcon);
    }

    /**
     * Displays a simple alert with only one option centered on the screen, with a result being the input
     */
    public static <T> void displaySimpleAlert(Result<T> result) {
        Stage stage = createStage();

        Label label = new Label(result.getCurrentMessage());
        FontIcon messageIcon = new FontIcon();

        setResultWindowDressing(result, messageIcon);
        simpleAlert(stage, label, messageIcon);
    }

    /**
     * Displays a simple alert with only one option centered on a specific stage
     *
     * @param parentStage The stage this popup will be centered on
     */
    public static void displaySimpleAlert(String message, Stage parentStage, MessageType messageType) {
        Stage stage = createStage();

        Label label = new Label(message);
        FontIcon messageIcon = new FontIcon();

        getIconByMessageType(messageType, messageIcon);

        simpleAlert(stage, parentStage, label, messageIcon);
    }

    /**
     * Displays a simple alert with only one option centered on the screen
     */
    public static void displaySimpleAlert(String message, MessageType messageType) {
        Stage stage = createStage();

        Label label = new Label(message);
        FontIcon messageIcon = new FontIcon();

        getIconByMessageType(messageType, messageIcon);

        simpleAlert(stage, label, messageIcon);
    }

    /**
     * Displays a simple alert with only one option centered on the screen, with a clickable link for the end of the error message.
     *
     * @param message The message itself
     * @param link    The link that will be displayed and clickable in the message
     */
    public static void displaySimpleAlert(String message, String link, MessageType messageType) {
        Stage stage = createStage();

        FontIcon messageIcon = new FontIcon();

        getIconByMessageType(messageType, messageIcon);

        simpleAlert(stage, message, link, messageIcon);
    }

    /**
     * Displays a simple alert with only one option centered on the screen, with a clickable link for the end of the error message and a custom title message.
     *
     * @param message The message itself
     * @param link    The link that will be displayed and clickable in the message
     */
    public static void displayInfoMessageWithLink(String message, String link, String titleMessage, MessageType messageType) {
        Stage stage = createStage();

        FontIcon messageIcon = new FontIcon();
        getIconByMessageType(messageType, messageIcon);

        simpleAlert(stage, message, link, titleMessage, messageIcon);
    }

    /**
     * Displays a simple alert with only one option centered on the stage, with a clickable link for the end of the error message and a custom title message.
     *
     * @param message The message itself
     * @param link    The link that will be displayed and clickable in the message
     */
    public static void displayInfoMessageWithLink(String message, String link, String titleMessage, Stage parentStage, MessageType messageType) {
        Stage stage = createStage();

        FontIcon messageIcon = new FontIcon();
        getIconByMessageType(messageType, messageIcon);

        simpleAlert(stage, parentStage, message, link, titleMessage, messageIcon);
    }

    /**
     * Displays a dialog centered on a specific stage that has three choices the user can make.
     * <p>
     * @return The button selected
     */
    public static ThreeButtonChoice displayThreeChoiceDialog(String message, Stage parentStage, MessageType messageType, String leftButtonMessage, String centerButtonMessage, String cancelButtonMessage) {
        Stage stage = createStage();

        Label label = new Label(message);
        FontIcon messageIcon = new FontIcon();

        getIconByMessageType(messageType, messageIcon);
        return threeChoice(stage, parentStage, label, messageIcon, leftButtonMessage, centerButtonMessage, cancelButtonMessage);
    }

    public static ThreeButtonChoice displayThreeChoiceDialog(String message, MessageType messageType, String leftButtonMessage, String centerButtonMessage, String cancelButtonMessage) {
        Stage stage = createStage();

        Label label = new Label(message);
        FontIcon messageIcon = new FontIcon();
        getIconByMessageType(messageType, messageIcon);
        return threeChoice(stage, label, messageIcon, leftButtonMessage, centerButtonMessage, cancelButtonMessage);
    }

    public static void displayNavigationDialog(List<String> messages, Stage parentStage, MessageType messageType, String title) {
        Stage stage = createStage();

        AtomicInteger currentStep = new AtomicInteger(0);
        Label label = new Label(messages.get(currentStep.get()));
        FontIcon messageIcon = new FontIcon();

        getIconByMessageType(messageType, messageIcon);

        makeNavigationDialog(stage, parentStage, messages, currentStep, label, messageIcon, title);
    }

    /**
     * Creates a stage with common settings to reduce code duplication
     */
    private static Stage createStage() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        return stage;
    }

    private static void makeNavigationDialog(Stage childStage, Stage parentStage, List<String> messages, AtomicInteger currentStep, Label label, FontIcon messageIcon, String title) {
        HBox dialogBox = makeDialog(label, messageIcon, title);

        HBox buttonBar = makeNavigationBar(childStage, messages, currentStep, label);

        createPopup(childStage, parentStage, dialogBox, buttonBar);
    }

    private static HBox makeNavigationBar(Stage childStage, List<String> messages, AtomicInteger currentStep, Label label) {
        Button backButton = createButton("Back", 80d, 36d);
        Button nextButton = createButton("Next", 80d, 36d);
        backButton.setDisable(true);

        backButton.setOnAction((ActionEvent event) -> {
            if (currentStep.get() - 1 <= 0) {
                backButton.setDisable(true);
            }

            if (currentStep.get() > 0) {
                currentStep.getAndDecrement();
                label.setText(messages.get(currentStep.get()));
                childStage.sizeToScene();
            }
        });

        nextButton.setOnAction((ActionEvent event) -> {
            if (currentStep.get() < messages.size() - 1) {
                backButton.setDisable(false);
                currentStep.getAndIncrement();
                label.setText(messages.get(currentStep.get()));
                childStage.sizeToScene();
            } else {
                cleanupAndClose(childStage);
            }
        });

        return createButtonBar(Pos.CENTER, 10, backButton, nextButton);
    }

    private static <T> void setResultWindowDressing(Result<T> result, FontIcon messageIcon) {
        switch (result.getType()) {
            case SUCCESS -> {
                messageIcon.setStyle("-fx-icon-color: -color-accent-emphasis;");
                messageIcon.setIconLiteral("ci-information-square");
            }
            case INVALID, WARN -> {
                messageIcon.setStyle("-fx-icon-color: -color-warning-emphasis;");
                messageIcon.setIconLiteral("ci-warning-alt");
            }
            case CANCELLED, FAILED -> {
                messageIcon.setStyle("-fx-icon-color: -color-danger-emphasis;");
                messageIcon.setIconLiteral("ci-warning-square");
            }
            default -> {
                messageIcon.setStyle("-fx-icon-color: -color-neutral-emphasis;");
                messageIcon.setIconLiteral("ci-unknown");
            }
        }
    }

    /**
     * Creates a simple alert centered on a specific stage
     *
     * @param childStage  The stage popup will use for its own display
     * @param parentStage The stage we will center the popup on
     */
    private static void simpleAlert(Stage childStage, Stage parentStage, Label label, FontIcon messageIcon) {
        HBox dialogBox = makeDialog(label, messageIcon);

        //Setup our button
        HBox buttonBar = makeOkBar(childStage);

        createPopup(childStage, parentStage, dialogBox, buttonBar);
    }

    /**
     * Creates a simple alert centered on the screen
     *
     * @param childStage The stage popup will use for its own display
     */
    private static void simpleAlert(Stage childStage, Label label, FontIcon messageIcon) {
        HBox dialogBox = makeDialog(label, messageIcon);

        //Setup our button
        HBox buttonBar = makeOkBar(childStage);

        createPopup(childStage, dialogBox, buttonBar);
    }

    /**
     * Creates a simple alert centered on the screen, with a clickable link
     *
     * @param childStage The stage popup will use for its own display
     * @param link       The link that will be displayed and clickable in the message
     */
    private static void simpleAlert(Stage childStage, String message, String link, FontIcon messageIcon) {
        HBox dialogBox = makeErrorDialogWithLink(message, link, messageIcon);

        //Setup our button
        HBox buttonBar = makeOkBar(childStage);

        createPopup(childStage, dialogBox, buttonBar);
    }

    /**
     * Creates a simple alert centered on the screen, with a clickable link and a customized title message.
     *
     * @param childStage The stage popup will use for its own display
     * @param link       The link that will be displayed and clickable in the message
     */
    private static void simpleAlert(Stage childStage, String message, String link, String titleMessage, FontIcon messageIcon) {
        HBox dialogBox = makeErrorDialogWithLink(message, link, titleMessage, messageIcon);

        //Setup our button
        HBox buttonBar = makeOkBar(childStage);

        createPopup(childStage, dialogBox, buttonBar);
    }

    /**
     * Creates a simple alert centered on the stage, with a clickable link and a customized title message.
     *
     * @param childStage The stage popup will use for its own display
     * @param link       The link that will be displayed and clickable in the message
     */
    private static void simpleAlert(Stage childStage, Stage parentStage, String message, String link, String titleMessage, FontIcon messageIcon) {
        HBox dialogBox = makeErrorDialogWithLink(message, link, titleMessage, messageIcon);

        //Setup our button
        HBox buttonBar = makeOkBar(childStage);

        createPopup(childStage, parentStage, dialogBox, buttonBar);
    }

    /**
     * Creates a yes/no dialog centered on a specific stage
     *
     * @param childStage  The stage the popup will use for its own display
     * @param parentStage The stage the popup will be centered on
     * @return The button selected. 1 for yes, 0 for no.
     */
    private static TwoButtonChoice yesNoDialog(Stage childStage, Stage parentStage, Label label, FontIcon messageIcon) {
        AtomicInteger choice = new AtomicInteger(-1);

        HBox dialogBox = makeDialog(label, messageIcon);

        HBox buttonBar = makeYesNoBar(choice, childStage);

        createPopup(childStage, parentStage, dialogBox, buttonBar);

        if(choice.intValue() == 1)
            return TwoButtonChoice.YES;
        else
            return TwoButtonChoice.NO;
    }

    /**
     * Creates a yes/no dialog centered on the screen
     *
     * @param childStage The stage the popup will use for its own display
     * @return The button selected. 1 for yes, 0 for no.
     */
    private static TwoButtonChoice yesNoDialog(Stage childStage, Label label, FontIcon messageIcon) throws IOException {
        AtomicInteger choice = new AtomicInteger(-1);

        HBox dialogBox = makeDialog(label, messageIcon);

        HBox buttonBar = makeYesNoBar(choice, childStage);

        createPopup(childStage, dialogBox, buttonBar);

        if(choice.intValue() == 1)
            return TwoButtonChoice.YES;
        else
            return TwoButtonChoice.NO;
    }

    /**
     * Creates a yes/no dialog centered on a specific stage.
     *
     * @param childStage  The stage the popup will use for its own display
     * @param parentStage The stage the popup will be centered on
     * @return The button choice selected.
     */
    private static ThreeButtonChoice threeChoice(Stage childStage, Stage parentStage, Label label, FontIcon messageIcon, String leftButtonMessage, String centerButtonMessage, String cancelButtonMessage) {
        AtomicInteger choice = new AtomicInteger(-1);

        HBox dialogBox = makeDialog(label, messageIcon);

        HBox buttonBar = makeThreeChoiceBar(choice, childStage, leftButtonMessage, centerButtonMessage, cancelButtonMessage);

        createPopup(childStage, parentStage, dialogBox, buttonBar);

        if (choice.intValue() == 2)
            return ThreeButtonChoice.LEFT;
        else if (choice.intValue() == 1)
            return ThreeButtonChoice.MIDDLE;
        else
            return ThreeButtonChoice.CANCEL;
    }

    /**
     * Creates a yes/no dialog centered on the screen.
     *
     * @param childStage  The stage the popup will use for its own display.
     * @return The button choice selected.
     */
    private static ThreeButtonChoice threeChoice(Stage childStage, Label label, FontIcon messageIcon, String leftButtonMessage, String centerButtonMessage, String cancelButtonMessage) {
        AtomicInteger choice = new AtomicInteger(-1);

        HBox dialogBox = makeDialog(label, messageIcon);

        HBox buttonBar = makeThreeChoiceBar(choice, childStage, leftButtonMessage, centerButtonMessage, cancelButtonMessage);

        createPopup(childStage, dialogBox, buttonBar);

        if (choice.intValue() == 2)
            return ThreeButtonChoice.LEFT;
        else if (choice.intValue() == 1)
            return ThreeButtonChoice.MIDDLE;
        else
            return ThreeButtonChoice.CANCEL;
    }

    private static HBox makeThreeChoiceBar(AtomicInteger choice, Stage childStage, String leftButtonMessage, String centerButtonMessage, String cancelButtonMessage) {
        Button leftButton = createButton(leftButtonMessage, 80d, 36d);
        Button centerButton = createButton(centerButtonMessage, 80d, 36d);
        Button rightButton = createButton(cancelButtonMessage, 80d, 36d);

        leftButton.setOnAction((ActionEvent event) -> {
            choice.set(2);
            childStage.close();
            Platform.exitNestedEventLoop(childStage, null);
        });

        centerButton.setOnAction((ActionEvent event) -> {
            choice.set(1);
            childStage.close();
            Platform.exitNestedEventLoop(childStage, null);
        });

        rightButton.setOnAction((ActionEvent event) -> {
            choice.set(0);
            childStage.close();
            Platform.exitNestedEventLoop(childStage, null);
        });

        rightButton.setCancelButton(true);

        return createButtonBar(Pos.CENTER_RIGHT, 10, leftButton, centerButton, rightButton);
    }


    private static HBox makeYesNoBar(AtomicInteger choice, Stage childStage) {
        Button noButton = createButton("No", 80d, 36d);
        Button yesButton = createButton("Yes", 80d, 36d);

        noButton.setOnAction((ActionEvent event) -> {
            choice.set(0);
            cleanupAndClose(childStage);
        });

        yesButton.setOnAction((ActionEvent event) -> {
            choice.set(1);
            cleanupAndClose(childStage);
        });

        noButton.setCancelButton(true);

        return createButtonBar(Pos.CENTER_RIGHT, 10, yesButton, noButton);
    }

    private static HBox makeOkBar(Stage childStage) {
        Button quitButton = createButton("OK", 80d, 36d);
        quitButton.setOnAction((ActionEvent event) -> cleanupAndClose(childStage));
        quitButton.setCancelButton(true);
        quitButton.setDefaultButton(true);

        return createButtonBar(Pos.CENTER, 0, quitButton);
    }

    private static Button createButton(String text, double minWidth, double minHeight) {
        Button button = new Button(text);
        button.setMinWidth(minWidth);
        button.setMinHeight(minHeight);
        button.setMaxHeight(minHeight);
        return button;
    }

    private static HBox createButtonBar(Pos alignment, double spacing, Button... buttons) {
        HBox buttonBar = new HBox(buttons);
        buttonBar.setPadding(new Insets(5, 5, 5, 5));
        buttonBar.setStyle(BUTTON_BAR_STYLE);
        buttonBar.setAlignment(alignment);
        buttonBar.setSpacing(spacing);
        return buttonBar;
    }

    /**
     * Centralized cleanup and close method to ensure proper resource cleanup
     */
    private static void cleanupAndClose(Stage stage) {
        stage.close();
        Platform.exitNestedEventLoop(stage, null);
    }

    //Creates a dialog box message
    private static HBox makeDialog(Label label, FontIcon messageIcon) {
        VBox contentBox = new VBox(makeTitleBar(messageIcon), getDialogBox(label, messageIcon));

        return new HBox(contentBox);
    }

    //Creates a dialog box message with a custom title
    private static HBox makeDialog(Label label, FontIcon messageIcon, String title) {
        VBox contentBox = new VBox(makeTitleBar(title), getDialogBox(label, messageIcon));

        return new HBox(contentBox);
    }

    @NotNull
    private static HBox getDialogBox(Label label, FontIcon messageIcon) {
        label.setStyle(FONT_STYLE);
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
    private static HBox makeErrorDialogWithLink(String message, String link, FontIcon messageIcon) {
        messageIcon.getStyleClass().clear();
        messageIcon.setIconSize(ICON_SIZE);

        Label label = new Label(message);
        label.setStyle(FONT_STYLE);
        label.setWrapText(true);

        HBox dialogBox = createErrorLinkBox(link, messageIcon, label);
        dialogBox.setAlignment(Pos.TOP_LEFT);
        dialogBox.setPadding(new Insets(0, 5, 0, 5));
        dialogBox.setSpacing(5d);
        dialogBox.setMaxWidth(600);

        return dialogBox;
    }

    //Creates a dialog box message, with a hyperlink and a customized title message
    private static HBox makeErrorDialogWithLink(String message, String link, String titleMessage, FontIcon messageIcon) {
        messageIcon.getStyleClass().clear();
        messageIcon.setIconSize(ICON_SIZE);

        Label label = new Label(message);
        label.setStyle(FONT_STYLE);
        label.setWrapText(true);

        HBox dialogBox = createErrorLinkBox(link, messageIcon, titleMessage, label);
        dialogBox.setAlignment(Pos.TOP_LEFT);
        dialogBox.setPadding(new Insets(0, 5, 0, 5));
        dialogBox.setSpacing(5d);
        dialogBox.setMaxWidth(600);

        return dialogBox;
    }

    private static HBox createErrorLinkBox(String link, FontIcon messageIcon, Label label) {
        return createErrorLinkBoxContent(link, messageIcon, label, makeTitleBar(messageIcon));
    }

    @NotNull
    private static HBox createErrorLinkBoxContent(String link, FontIcon messageIcon, Label label, HBox hBox) {
        Hyperlink hyperlink = new Hyperlink("bugreport.spaceengineersmodmanager.com");
        hyperlink.setStyle(FONT_STYLE);

        hyperlink.setOnAction(actionEvent -> {
            try {
                Desktop.getDesktop().browse(new URI(link));
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });

        HBox messageBox = new HBox(messageIcon, label);
        messageBox.setSpacing(5d);
        VBox textLayout = new VBox(hBox, messageBox, hyperlink);
        textLayout.setAlignment(Pos.CENTER);

        return new HBox(textLayout);
    }

    private static HBox createErrorLinkBox(String link, FontIcon messageIcon, String titleMessage, Label label) {
        return createErrorLinkBoxContent(link, messageIcon, label, makeTitleBar(titleMessage));
    }

    private static HBox makeTitleBar(FontIcon messageIcon) {
        Label title = new Label(switch (messageIcon.getIconLiteral()) {
            case "ci-information-square" -> "Info";
            case "ci-warning-alt" -> "Warning";
            case "ci-warning-square" -> "Error";
            default -> "Unknown";
        });
        return makeTitleBarContent(WindowDressingUtility.getICONS().getLast(), title);
    }

    private static HBox makeTitleBar(String titleMessage) {
        return makeTitleBarContent(WindowDressingUtility.getICONS().getLast(), new Label(titleMessage));
    }

    @NotNull
    private static HBox makeTitleBarContent(Image logo, Label title) {
        HBox titleBox = new HBox(new ImageView(logo), title);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setPadding(new Insets(5, 0, 5, 5));
        titleBox.setSpacing(5d);
        Background background;
        if (StringUtils.substringAfter(Application.getUserAgentStylesheet(), "theme/").contains("light")) {
            background = new Background(new BackgroundFill(Color.WHITE, null, null));
        } else {
            background = new Background(new BackgroundFill(Color.BLACK, null, null));
        }

        titleBox.setBackground(background);

        return titleBox;
    }

    private static void getIconByMessageType(MessageType messageType, FontIcon messageIcon) {
        switch (messageType) {
            case INFO -> {
                messageIcon.setStyle("-fx-icon-color: -color-accent-emphasis;");
                messageIcon.setIconLiteral("ci-information-square");
            }
            case WARN -> {
                messageIcon.setStyle("-fx-icon-color: -color-warning-emphasis;");
                messageIcon.setIconLiteral("ci-warning-alt");
            }
            case ERROR -> {
                messageIcon.setStyle("-fx-icon-color: -color-danger-emphasis;");
                messageIcon.setIconLiteral("ci-warning-square");
            }
            default -> {
                messageIcon.setStyle("-fx-icon-color: -color-neutral-emphasis;");
                messageIcon.setIconLiteral("ci-unknown");
            }
        }
    }

    private static void createPopup(Stage childStage, Stage parentStage, HBox dialogBox, HBox buttonBar) {
        prepareStage(childStage, dialogBox, buttonBar);

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

        //Prevents a null exception if we somehow have an empty button bar
        if (!buttonBar.getChildren().isEmpty()) {
            buttonBar.getChildren().getLast().requestFocus();
        }
        WindowTitleBarColorUtility.setWindowsTitleBar(childStage);
        Platform.enterNestedEventLoop(childStage);
    }

    private static void createPopup(Stage childStage, HBox dialogBox, HBox buttonBar) {
        prepareStage(childStage, dialogBox, buttonBar);

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        //Center the alert in the middle of the computer screen by using listeners that will fire off when the window is created.
        ChangeListener<Number> widthListener = (observable, oldValue, newValue) -> childStage.setX((screenBounds.getWidth() - childStage.getWidth()) / 2);
        ChangeListener<Number> heightListener = (observable, oldValue, newValue) -> childStage.setY((screenBounds.getHeight() - childStage.getHeight()) / 2);

        childStage.widthProperty().addListener(widthListener);
        childStage.heightProperty().addListener(heightListener);

        //Once the window is visible, remove the listeners.
        childStage.setOnShown(e -> {
            childStage.widthProperty().removeListener(widthListener);
            childStage.heightProperty().removeListener(heightListener);
        });

        childStage.show();

        //Prevents a null exception if we somehow have an empty button bar
        if (!buttonBar.getChildren().isEmpty()) {
            buttonBar.getChildren().getLast().requestFocus();
        }
        WindowTitleBarColorUtility.setWindowsTitleBar(childStage);
        Platform.enterNestedEventLoop(childStage);
    }

    private static void prepareStage(Stage childStage, HBox dialogBox, HBox buttonBar) {
        VBox contents = new VBox(dialogBox, buttonBar);

        Color borderColor = Application.getUserAgentStylesheet().contains("light")
                ? Color.BLACK
                : Color.web("39393a");


        contents.setBorder(new Border(new BorderStroke(borderColor, borderColor, borderColor, borderColor,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, new BorderWidths(0.5), Insets.EMPTY)));
        contents.setSpacing(10);

        Scene scene = new Scene(contents);
        WindowDressingUtility.appendStageIcon(childStage);
        childStage.setResizable(false);

        childStage.setScene(scene);
    }
}