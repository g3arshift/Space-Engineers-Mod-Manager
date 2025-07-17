package com.gearshiftgaming.se_mod_manager;

import com.gearshiftgaming.se_mod_manager.backend.models.shared.MessageType;
import com.gearshiftgaming.se_mod_manager.controller.ViewController;
import com.gearshiftgaming.se_mod_manager.frontend.view.popup.Popup;
import com.gearshiftgaming.se_mod_manager.frontend.view.window.WindowTitleBarColorUtility;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class SpaceEngineersModManager extends Application {

	private static final Logger LOGGER = LogManager.getLogger(SpaceEngineersModManager.class);

    public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, InterruptedException {
		new ViewController(primaryStage, LOGGER);
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			try {
				logError(t, e);
			} catch (Throwable ex) {
				Platform.exit();
			}
		});

		primaryStage.show();

		WindowTitleBarColorUtility.setWindowsTitleBar(primaryStage);
	}

	//Log the error that caused our stacktrace to the log, and shutdown the application.
	private static void logError(Thread t, Throwable e) throws Throwable {
		//Second condition is a dirty hack to prevent it from double displaying errors when we close the platform
		if (Platform.isFxApplicationThread()) {
			LOGGER.error("Uncaught exception in thread: {}", t.getName(), e);
			Popup.displaySimpleAlert("An unexpected error was encountered and the application will now exit. " +
					"Please submit a bug report along with your SEMM.log file located in the logs folder to the below link.", "https://bugreport.spaceengineersmodmanager.com", MessageType.ERROR);
			throw e;
		}
	}
}