package com.gearshiftgaming.se_mod_manager;

import com.gearshiftgaming.se_mod_manager.controller.ViewController;
import jakarta.xml.bind.JAXBException;
import javafx.application.Application;
import javafx.stage.Stage;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

public class SpaceEngineersModManager extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException, XmlPullParserException, JAXBException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        new ViewController(primaryStage);
        primaryStage.show();
    }
}