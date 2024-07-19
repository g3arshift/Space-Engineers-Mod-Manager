package com.gearshiftgaming.se_mod_manager;

import com.gearshiftgaming.se_mod_manager.controller.ModManagerController;
import com.gearshiftgaming.se_mod_manager.backend.data.ModFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.SandboxConfigFileRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.ModService;
import com.gearshiftgaming.se_mod_manager.backend.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.frontend.ui.ModManagerView;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class SpaceEngineersModManager extends Application {

    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {

        final Logger logger = LogManager.getLogger(SpaceEngineersModManager.class);
        logger.info("Started application...");
        launch();
/*
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        List<Mod> modList = new ArrayList<>();
        ModManagerView modManagerView = new ModManagerView();
        SandboxService sandboxService = new SandboxService(new SandboxConfigFileRepository(), logger);

        final String DESKTOP_PATH = System.getProperty("user.home") + "/Desktop";
        final String APP_DATA_PATH = System.getenv("APPDATA") + "/SpaceEngineers/Saves";

        if (!Files.isDirectory(Path.of(APP_DATA_PATH))) {
            logger.warn("Space Engineers save location not found.");
        }

        ModService modService = new ModService(new ModFileRepository(), logger);


        ModManagerController modManagerController = new ModManagerController(logger, modList, modManagerView, modService, sandboxService, DESKTOP_PATH, APP_DATA_PATH);

        //Get the party started
        modManagerController.injectModList();
        logger.info("Application finished. Closing.");
*/
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hello World!");
        Button btn = new Button();
        btn.setText("Say 'Hello World'");
        btn.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                System.out.println("Hello World!");
            }
        });

        StackPane root = new StackPane();
        root.getChildren().add(btn);
        primaryStage.setScene(new Scene(root, 300, 250));
        primaryStage.show();

        //TODO: Set a context menu on the menu header ONLY per this https://stackoverflow.com/questions/47786125/how-to-add-a-context-menu-to-an-empty-tableviews-header-row. This may however, not be a good idea. Check the notes for the todo. Fourth item, sub-item of item 3. Also use this to inject checkboxes!
    }
}