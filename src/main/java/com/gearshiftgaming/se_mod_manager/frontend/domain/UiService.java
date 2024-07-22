package com.gearshiftgaming.se_mod_manager.frontend.domain;

import atlantafx.base.theme.PrimerLight;
import com.gearshiftgaming.se_mod_manager.backend.domain.ModlistService;
import com.gearshiftgaming.se_mod_manager.backend.domain.SandboxService;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.LogMessage;
import javafx.application.Application;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.Objects;

public class UiService {
    //TODO: Move the observable list and all the other controller junk here that isn't strictly DUMB UI related.
    @Getter
    private final ObservableList<LogMessage> applicationLog;

    @Getter
    private Logger logger;
    private SandboxService sandboxService;
    private ModlistService modlistService;

    private Scene scene;

    private final String DESKTOP_PATH = System.getProperty("user.home") + "/Desktop";
    private final String APP_DATA_PATH = System.getenv("APPDATA") + "/SpaceEngineers/Saves";

    public UiService(Logger logger, SandboxService sandboxService, ModlistService modlistService, Parent root, int minWidth, int minHeight) {
        //TODO: Let users choose the theme they wish from the AtlantaFX themes.
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        applicationLog = FXCollections.observableArrayList(logMessage ->
                new Observable[]{
                        logMessage.messageProperty(),
                        logMessage.messageTypeProperty()
                });

        this.logger = logger;
        this.sandboxService = sandboxService;
        this.modlistService = modlistService;
        this.scene = new Scene(root, minWidth, minHeight);
    }

    public void prepareStage(Stage stage) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader("pom.xml"));
        stage.setTitle("SEMM v" + model.getVersion());


        stage.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResourceAsStream("/icons/logo.png"))));

        stage.setScene(scene);
    }
}
