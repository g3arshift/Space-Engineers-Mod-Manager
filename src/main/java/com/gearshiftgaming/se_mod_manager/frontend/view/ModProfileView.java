package com.gearshiftgaming.se_mod_manager.frontend.view;

import com.gearshiftgaming.se_mod_manager.backend.models.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.ModProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

public class ModProfileView {

    @FXML
    private ListView<ModProfile> profileList;

    @FXML
    private Button createNewProfile;

    @FXML
    private Button copyProfile;

    @FXML
    private Button removeProfile;

    @FXML
    private Button renameProfile;

    @FXML
    private Button selectProfile;

    @FXML
    private Button closeProfileWindow;

    @Getter
    private Stage stage;

    private Scene scene;

    private ObservableList<ModProfile> modProfiles;

    private UiService uiService;

    @Setter
    private UserConfiguration userConfiguration;

    private ModProfileCreateView modProfileCreateView;

    public void initController(ObservableList<ModProfile> modProfiles, Parent root, UiService uiService, ModProfileCreateView modProfileCreateView, Properties properties) throws IOException, XmlPullParserException {
        this.modProfiles = modProfiles;
        this.scene = new Scene(root);
        this.uiService = uiService;
        this.modProfileCreateView = modProfileCreateView;
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        stage.setTitle("Mod Profile Manager");
        stage.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResourceAsStream("/icons/logo.png"))));

        stage.setMinWidth(Double.parseDouble(properties.getProperty("semm.profileView.resolution.minWidth")));
        stage.setMinHeight(Double.parseDouble(properties.getProperty("semm.profileView.resolution.minHeight")));

        profileList.setItems(modProfiles);
        profileList.setCellFactory(param -> new ModProfileCell());

        stage.setScene(scene);
    }

    @FXML
    private void createNewProfile() {
        modProfileCreateView.getStage().showAndWait();
        ModProfile newModProfile = new ModProfile(modProfileCreateView.getProfileCreateInput().getText());
        //TODO: Add duplicate checking
        modProfiles.add(newModProfile);
    }

    @FXML
    private void copyProfile() {
        //TODO: Implement
    }

    @FXML
    private void removeProfile() {
        //TODO: Prevent users from removing active profile
        //TODO: Implement
    }

    @FXML
    private void renameProfile() {
        //TODO: Implement
    }

    @FXML
    private void selectProfile() {
        userConfiguration.setLastUsedSaveProfileId(profileList.getSelectionModel().getSelectedItem().getId());
    }

    @FXML
    private void closeProfileWindow() {
        stage.close();
    }

}
