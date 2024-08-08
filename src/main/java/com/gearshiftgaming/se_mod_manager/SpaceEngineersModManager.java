package com.gearshiftgaming.se_mod_manager;

import com.gearshiftgaming.se_mod_manager.controller.ViewController;
import jakarta.xml.bind.JAXBException;
import javafx.application.Application;
import javafx.stage.Stage;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class SpaceEngineersModManager extends Application {

    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException, XmlPullParserException, JAXBException {

        ViewController viewController = new ViewController(primaryStage);
        primaryStage.show();
    }
}