package com.gearshiftgaming.se_mod_manager;

import com.gearshiftgaming.se_mod_manager.backend.models.utility.MessageType;
import com.gearshiftgaming.se_mod_manager.controller.ViewController;
import com.gearshiftgaming.se_mod_manager.frontend.view.Popup;
import jakarta.xml.bind.JAXBException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 * <p>
 *
 * @author Gear Shift
 */
public class SpaceEngineersModManager extends Application {

	private final static Logger logger = LogManager.getLogger(SpaceEngineersModManager.class);

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws IOException, XmlPullParserException, JAXBException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
		new ViewController(primaryStage, logger);
		Thread.setDefaultUncaughtExceptionHandler(SpaceEngineersModManager::logError);
		primaryStage.show();
	}

	//Log the error that caused our stacktrace to the log, and shutdown the application.
	private static void logError(Thread t, Throwable e){
		if (Platform.isFxApplicationThread()) {
			logger.error(e.getLocalizedMessage());
			logger.error(Arrays.toString(e.getStackTrace()));
			Popup.displaySimpleAlert("An unexpected error was encountered and the application will now exit. " +
					"Please submit a bug report along with your SEMM.log file located in the logs folder to", "https://spaceengineersmodmanager.com/bugreport", MessageType.ERROR);
			Platform.exit();
		}
	}
}