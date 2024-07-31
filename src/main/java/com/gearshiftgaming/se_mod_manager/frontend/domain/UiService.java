package com.gearshiftgaming.se_mod_manager.frontend.domain;

import atlantafx.base.theme.*;
import com.gearshiftgaming.se_mod_manager.backend.models.ModProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.backend.models.utility.LogMessage;
import com.gearshiftgaming.se_mod_manager.frontend.view.MainWindowView;
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
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import javax.swing.text.html.Option;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class UiService {
    //TODO: Move the observable list and all the other controller junk here that isn't strictly DUMB UI related.
    @Getter
    private final ObservableList<LogMessage> userLog;

    private final MainWindowView mainWindowView;

    private final Scene scene;
    private final int minWidth;
    private final int minHeight;

    private final UserConfiguration userConfiguration;

    //TODO: Pass the objects to the UI instead and parse to observable lists
    //private final List<ModProfile> modProfiles;
    //private final List<SaveProfile> saveProfiles;


    public UiService(UserConfiguration userConfiguration, int minWidth, int minHeight) throws IOException {

        //Set the theme for our application.
        //This makes it clunky to add any new themes in the future, but for the moment it's a straightforwards approach to it.
        switch (userConfiguration.getUserTheme()) {
            case "Primer Dark" -> Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
            case "Nord Light" -> Application.setUserAgentStylesheet(new NordLight().getUserAgentStylesheet());
            case "Nord Dark" -> Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());
            case "Cupertino Light" -> Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
            case "Cupertino Dark" -> Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
            case "Dracula" -> Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());
            default -> Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        }

        this.userConfiguration = userConfiguration;

        //Load the FXML for our main window
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main-window.fxml"));
        Parent root = loader.load();
        mainWindowView = loader.getController();

        //Initialize the list we use to store log messages shown to the user
        userLog = FXCollections.observableArrayList(logMessage ->
                new Observable[]{
                        logMessage.viewableLogMessageProperty(),
                        logMessage.messageTypeProperty()
                });

        this.scene = new Scene(root, minWidth, minHeight);
        this.minWidth = minWidth;
        this.minHeight = minHeight;

        //Get the last used save profile, and if it still exists, make that our current profile in the UI.
        Optional<SaveProfile> lastUsedSaveProfile = findLastUsedSaveProfile();
        if(lastUsedSaveProfile.isPresent()) {
            mainWindowView.initView(userLog, lastUsedSaveProfile.get());
        } else mainWindowView.initView(userLog);
    }

    /**
     * Sets the name of the application, as well as the icon for it, and adds a listener for the stage to ensure the tab pane behaves correctly.
     * Application version information is retrieved from pom.xml
     *
     * @param stage The stage for the primary window that is the core of the application
     */
    public void prepareMainStage(Stage stage) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader("pom.xml"));
        stage.setTitle("SEMM v" + model.getVersion());

        stage.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResourceAsStream("/icons/logo.png"))));

        stage.setScene(scene);
        stage.setMinWidth(minWidth);
        stage.setMinHeight(minHeight);

        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (!mainWindowView.isMainViewSplitDividerVisible()) {
                mainWindowView.getMainViewSplit().setDividerPosition(0, 1);
            }
        });
    }

    public void addMessageToLog(LogMessage logMessage) {
        userLog.add(logMessage);
    }

    private Optional<SaveProfile> findLastUsedSaveProfile() {
        Optional<SaveProfile> lastUsedSaveProfile = Optional.empty();
        if (userConfiguration.getLastUsedSaveProfileId() != null) {
            UUID lastUsedSaveProfileId = userConfiguration.getLastUsedSaveProfileId();

            lastUsedSaveProfile = userConfiguration.getSaveProfiles().stream()
                    .filter(saveProfile -> saveProfile.getId().equals(lastUsedSaveProfileId))
                    .findFirst();
        }
        return lastUsedSaveProfile;
    }
}
