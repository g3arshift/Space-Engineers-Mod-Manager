package com.gearshiftgaming.se_mod_manager.frontend.view.utility;

import com.gearshiftgaming.se_mod_manager.backend.models.MessageType;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Displays a custom popup modal using icons from the Ikonli CarbonIcon icon pack.
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class Popup {

	private static final int FONT_SIZE = 16;
	private static final int ICON_SIZE = 30;

	/**
	 * Displays a Yes/No dialog centered on a specific stage
	 *
	 * @param message     The message to display
	 * @param parentStage The stage this will be centered on
	 * @param messageType The type of message this is
	 */

	public static int displayYesNoDialog(String message, Stage parentStage, MessageType messageType) {
		Stage stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.initStyle(StageStyle.UNDECORATED);

		Label label = new Label(message);
		FontIcon messageIcon = new FontIcon();

		getIconByMessageType(messageType, messageIcon);

		return yesNoDialog(stage, parentStage, label, messageIcon);
	}

	/**
	 * Displays a Yes/No dialog centered on the screen
	 */
	public static int displayYesNoDialog(String message, MessageType messageType) throws IOException {
		Stage stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.initStyle(StageStyle.UNDECORATED);

		Label label = new Label(message);
		FontIcon messageIcon = new FontIcon();

		getIconByMessageType(messageType, messageIcon);

		return yesNoDialog(stage, label, messageIcon);
	}

	public static <T> int displayYesNoDialog(Result<T> result) throws IOException {
		Stage stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.initStyle(StageStyle.UNDECORATED);

		Label label = new Label(result.getCurrentMessage());
		FontIcon messageIcon = new FontIcon();
		setResultWindowDressing(result, stage, messageIcon);

		return yesNoDialog(stage, label, messageIcon);
	}

	/**
	 * Displays a simple alert with only one option centered on a specific stage, with a result being the input
	 *
	 * @param parentStage The stage this popup will be centered on
	 */
	public static <T> void displaySimpleAlert(Result<T> result, Stage parentStage) {
		Stage stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.initStyle(StageStyle.UNDECORATED);

		Label label = new Label(result.getCurrentMessage());
		FontIcon messageIcon = new FontIcon();

		setResultWindowDressing(result, stage, messageIcon);
		simpleAlert(stage, parentStage, label, messageIcon);
	}

	/**
	 * Displays a simple alert with only one option centered on the screen, with a result being the input
	 */
	public static <T> void displaySimpleAlert(Result<T> result) {
		Stage stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.initStyle(StageStyle.UNDECORATED);

		Label label = new Label(result.getCurrentMessage());
		FontIcon messageIcon = new FontIcon();

		setResultWindowDressing(result, stage, messageIcon);
		simpleAlert(stage, label, messageIcon);
	}

	/**
	 * Displays a simple alert with only one option centered on a specific stage
	 *
	 * @param parentStage The stage this popup will be centered on
	 */
	public static void displaySimpleAlert(String message, Stage parentStage, MessageType messageType) {
		Stage stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.initStyle(StageStyle.UNDECORATED);

		Label label = new Label(message);
		FontIcon messageIcon = new FontIcon();

		getIconByMessageType(messageType, messageIcon);

		simpleAlert(stage, parentStage, label, messageIcon);
	}

	/**
	 * Displays a simple alert with only one option centered on the screen
	 */
	public static void displaySimpleAlert(String message, MessageType messageType) {
		Stage stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.initStyle(StageStyle.UNDECORATED);

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
		Stage stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.initStyle(StageStyle.UNDECORATED);

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
		Stage stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.initStyle(StageStyle.UNDECORATED);

		FontIcon messageIcon = new FontIcon();
		messageIcon.setStyle("-fx-icon-color: -color-accent-emphasis;");
		messageIcon.setIconLiteral("ci-information-square");

		simpleAlert(stage, message, link, titleMessage, messageIcon);
	}

	/**
	 * Displays a dialog centered on a specific stage that has three choices the user can make.
	 */
	public static int displayThreeChoiceDialog(String message, Stage parentStage, MessageType messageType, String leftButtonMessage, String centerButtonMessage, String rightButtonMessage) {
		Stage stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.initStyle(StageStyle.UNDECORATED);

		Label label = new Label(message);
		FontIcon messageIcon = new FontIcon();

		getIconByMessageType(messageType, messageIcon);
		return threeChoice(stage, parentStage, label, messageIcon, leftButtonMessage, centerButtonMessage, rightButtonMessage);
	}


	public static <T> void setResultWindowDressing(Result<T> result, Stage stage, FontIcon messageIcon) {
		switch (result.getType()) {
			case SUCCESS -> {
				messageIcon.setStyle("-fx-icon-color: -color-accent-emphasis;");
				messageIcon.setIconLiteral("ci-information-square");
			}
			case INVALID -> {
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
	 * Creates a yes/no dialog centered on a specific stage
	 * @param childStage The stage the popup will use for its own display
	 * @param parentStage The stage the popup will be centered on
	 * @return The button selected. 1 for yes, 0 for no.
	 */
	private static int yesNoDialog(Stage childStage, Stage parentStage, Label label, FontIcon messageIcon) {
		AtomicInteger choice = new AtomicInteger(-1);

		HBox dialogBox = makeDialog(label, messageIcon);

		HBox buttonBar = makeYesNoBar(choice, childStage);

		createPopup(childStage, parentStage, dialogBox, buttonBar);

		return choice.intValue();
	}

	/**
	 *
	 * Creates a yes/no dialog centered on the screen
	 * @param childStage The stage the popup will use for its own display
	 * @return The button selected. 1 for yes, 0 for no.
	 */
	private static int yesNoDialog(Stage childStage, Label label, FontIcon messageIcon) throws IOException {
		AtomicInteger choice = new AtomicInteger(-1);

		HBox dialogBox = makeDialog(label, messageIcon);

		HBox buttonBar = makeYesNoBar(choice, childStage);

		createPopup(childStage, dialogBox, buttonBar);

		return choice.intValue();
	}

	/**
	 *
	 * Creates a yes/no dialog centered on a specific stage.
	 * @param childStage The stage the popup will use for its own display
	 * @param parentStage The stage the popup will be centered on
	 * @return The button selected. 2 for left button, 1 for center, 0 for right. Right/0 is assumed to be a cancel option.
	 */
	private static int threeChoice(Stage childStage, Stage parentStage, Label label, FontIcon messageIcon, String leftButtonMessage, String centerButtonMessage, String rightButtonMessage) {
		AtomicInteger choice = new AtomicInteger(-1);

		HBox dialogBox = makeDialog(label, messageIcon);

		HBox buttonBar = makeThreeChoiceBar(choice, childStage, leftButtonMessage, centerButtonMessage, rightButtonMessage);

		createPopup(childStage, parentStage, dialogBox, buttonBar);

		return choice.intValue();
	}

	private static HBox makeThreeChoiceBar(AtomicInteger choice, Stage childStage, String leftButtonMessage, String centerButtonMessage, String rightButtonMessage) {
		Button leftButton = new Button();
		Button centerButton = new Button();
		Button rightButton = new Button();

		leftButton.setText(leftButtonMessage);
		centerButton.setText(centerButtonMessage);
		rightButton.setText(rightButtonMessage);

		leftButton.setOnAction((ActionEvent event) -> {
			choice.set(2);
			childStage.close();
			childStage.setHeight(childStage.getHeight() - 1);
			Platform.exitNestedEventLoop(childStage, null);
		});

		centerButton.setOnAction((ActionEvent event) -> {
			choice.set(1);
			childStage.close();
			childStage.setHeight(childStage.getHeight() - 1);
			Platform.exitNestedEventLoop(childStage, null);
		});

		rightButton.setOnAction((ActionEvent event) -> {
			choice.set(0);
			childStage.close();
			childStage.setHeight(childStage.getHeight() - 1);
			Platform.exitNestedEventLoop(childStage, null);
		});

		leftButton.setMinWidth(80d);
		leftButton.setMinHeight(36d);
		leftButton.setMaxHeight(36d);

		centerButton.setMinWidth(80d);
		centerButton.setMinHeight(36d);
		centerButton.setMaxHeight(36d);

		rightButton.setMinWidth(80d);
		rightButton.setMinHeight(36d);
		rightButton.setMaxHeight(36d);

		rightButton.setCancelButton(true);

		HBox buttonBar = new HBox(leftButton, centerButton, rightButton);

		buttonBar.setPadding(new Insets(5, 5, 5, 5));
		buttonBar.setStyle("-fx-background-color: -color-neutral-subtle;");
		buttonBar.setAlignment(Pos.CENTER_RIGHT);
		buttonBar.setSpacing(10);

		return buttonBar;
	}


	private static HBox makeYesNoBar(AtomicInteger choice, Stage childStage) {
		Button noButton = new Button();
		Button yesButton = new Button();

		noButton.setText("No");
		yesButton.setText("Yes");

		noButton.setOnAction((ActionEvent event) -> {
			choice.set(0);
			childStage.close();
			childStage.setHeight(childStage.getHeight() - 1);
			Platform.exitNestedEventLoop(childStage, null);
		});

		yesButton.setOnAction((ActionEvent event) -> {
			choice.set(1);
			childStage.close();
			childStage.setHeight(childStage.getHeight() - 1);
			Platform.exitNestedEventLoop(childStage, null);
		});

		noButton.setMinWidth(80d);
		noButton.setMinHeight(36d);
		noButton.setMaxHeight(36d);

		yesButton.setMinWidth(80d);
		yesButton.setMinHeight(36d);
		yesButton.setMaxHeight(36d);

		noButton.setCancelButton(true);

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
		quitButton.setOnAction((ActionEvent event) -> {
			childStage.close();
			childStage.setHeight(childStage.getHeight() - 1);
			Platform.exitNestedEventLoop(childStage, null);
		});

		quitButton.setMinWidth(80d);
		quitButton.setMinHeight(36d);
		quitButton.setMaxHeight(36d);

		quitButton.setCancelButton(true);
		quitButton.setDefaultButton(true);

		HBox buttonBar = new HBox(quitButton);
		buttonBar.setPadding(new Insets(5, 5, 5, 5));
		buttonBar.setStyle("-fx-background-color: -color-neutral-subtle;");
		buttonBar.setAlignment(Pos.CENTER);
		return buttonBar;
	}

	//Creates a dialog box message
	private static HBox makeDialog(Label label, FontIcon messageIcon) {
		VBox contentBox = new VBox(makeTitleBar(messageIcon), getDialogBox(label, messageIcon));

		return new HBox(contentBox);
	}

	@NotNull
	private static HBox getDialogBox(Label label, FontIcon messageIcon) {
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
	private static HBox makeErrorDialogWithLink(String message, String link, FontIcon messageIcon) {
		messageIcon.getStyleClass().clear();
		messageIcon.setIconSize(ICON_SIZE);

		Label label = new Label(message);
		label.setStyle("-fx-font-size: " + FONT_SIZE + ";");
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
		label.setStyle("-fx-font-size: " + FONT_SIZE + ";");
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
		hyperlink.setStyle("-fx-font-size: " + FONT_SIZE + ";");

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
		Image logo = new Image(Objects.requireNonNull(WindowDressingUtility.class.getResourceAsStream("/icons/logo_16.png")));

		Label title = new Label(switch(messageIcon.getIconLiteral()) {
			case "ci-information-square" -> "Success";
			case "ci-warning-alt" -> "Warning";
			case "ci-warning-square" -> "Error";
			default -> "Unknown";
		});

		return makeTitleBarContent(logo, title);
	}

	@NotNull
	private static HBox makeTitleBarContent(Image logo, Label title) {
		HBox titleBox = new HBox(new ImageView(logo), title);
		titleBox.setAlignment(Pos.CENTER_LEFT);
		titleBox.setPadding(new Insets(5, 0, 5, 5));
		titleBox.setSpacing(5d);
		Background background;
		if(StringUtils.substringAfter(Application.getUserAgentStylesheet(), "theme/").contains("light")) {
			background = new Background(new BackgroundFill(Color.WHITE, null, null));
		} else {
			background = new Background(new BackgroundFill(Color.BLACK, null, null));
		}

		titleBox.setBackground(background);

		return titleBox;
	}

	private static HBox makeTitleBar(String titleMessage) {
		Image logo = new Image(Objects.requireNonNull(WindowDressingUtility.class.getResourceAsStream("/icons/logo_16.png")));

		Label title = new Label(titleMessage);

		return makeTitleBarContent(logo, title);
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

	private static void prepareStage(Stage childStage, HBox dialogBox, HBox buttonBar) {
		VBox contents = new VBox(dialogBox, buttonBar);
		Color borderColor;

		if(Application.getUserAgentStylesheet().contains("light")) {
			borderColor = Color.BLACK;
		} else {
			borderColor = Color.web("39393a");
		}

		contents.setBorder(new Border(new BorderStroke(borderColor, borderColor, borderColor, borderColor,
				BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
				CornerRadii.EMPTY, new BorderWidths(0.5), Insets.EMPTY)));
		contents.setSpacing(10);

		Scene scene = new Scene(contents);
		WindowDressingUtility.appendStageIcon(childStage);
		childStage.setResizable(false);

		childStage.setScene(scene);
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
		buttonBar.getChildren().getLast().requestFocus();
		WindowTitleBarColorUtility.SetWindowsTitleBar(childStage);
		Platform.enterNestedEventLoop(childStage);
	}

	private static void createPopup(Stage childStage, HBox dialogBox, HBox buttonBar) {
		prepareStage(childStage, dialogBox, buttonBar);

		WindowPositionUtility.centerStageOnScreen(childStage);

		childStage.show();
		buttonBar.getChildren().getLast().requestFocus();
		WindowTitleBarColorUtility.SetWindowsTitleBar(childStage);
		Platform.enterNestedEventLoop(childStage);
	}
}