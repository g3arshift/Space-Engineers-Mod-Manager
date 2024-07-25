package com.gearshiftgaming.se_mod_manager.frontend.domain;

import atlantafx.base.theme.PrimerLight;
import com.gearshiftgaming.se_mod_manager.backend.domain.ModlistService;
import com.gearshiftgaming.se_mod_manager.backend.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.LogMessage;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.ResultType;
import com.gearshiftgaming.se_mod_manager.controller.fx.MainViewController;
import com.gearshiftgaming.se_mod_manager.frontend.models.MessageType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javafx.application.Application;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.Getter;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class UiService {
    //TODO: Move the observable list and all the other controller junk here that isn't strictly DUMB UI related.
    @Getter
    private final ObservableList<LogMessage> applicationLog;

    @Getter
    private final Logger logger;

    private final MainViewController mainViewController;

    private final Scene scene;
    private final int minWidth;
    private final int minHeight;

    private final String DESKTOP_PATH = System.getProperty("user.home") + "/Desktop";
    private final String APP_DATA_PATH = System.getenv("APPDATA") + "/SpaceEngineers/Saves";

    public UiService(Logger logger, int minWidth, int minHeight) throws IOException {
        //TODO: Let users choose the theme they wish from the AtlantaFX themes.
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main-view.fxml"));
        Parent root = loader.load();

        mainViewController = loader.getController();

        applicationLog = FXCollections.observableArrayList(logMessage ->
                new Observable[]{
                        logMessage.viewableLogMessageProperty(),
                        logMessage.messageTypeProperty()
                });

        this.logger = logger;
        this.scene = new Scene(root, 1100, 600);
        this.minWidth = minWidth;
        this.minHeight = minHeight;

        if (!Files.isDirectory(Path.of(APP_DATA_PATH))) {
            applicationLog.add(new LogMessage("Space Engineers save location not found.", MessageType.WARN, logger));
        }

        mainViewController.initController(applicationLog, logger);
    }

    public void prepareStage(Stage stage) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader("pom.xml"));
        stage.setTitle("SEMM v" + model.getVersion());

        stage.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResourceAsStream("/icons/logo.png"))));

        stage.setScene(scene);
        stage.setMinWidth(minWidth);
        stage.setMinHeight(minHeight);

        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (!mainViewController.isMainViewSplitDividerVisible()) {
                mainViewController.getMainViewSplit().setDividerPosition(0, 1);
            }
        });
    }

    public void log(String message, MessageType messageType) {
        applicationLog.add(new LogMessage(message, messageType, logger));
    }
}
