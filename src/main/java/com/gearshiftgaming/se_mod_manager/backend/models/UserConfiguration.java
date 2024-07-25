package com.gearshiftgaming.se_mod_manager.backend.models;

import atlantafx.base.theme.PrimerLight;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.*;
import jakarta.xml.bind.annotation.XmlType;
import javafx.application.Application;
import lombok.Getter;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import atlantafx.base.theme.Theme;
import lombok.Setter;

/**
 * Stores a users information, including their preferred theme, their mod profiles, and their save profiles.
 *
 * @author Gear Shift
 * @version 1.0
 */

@Getter
@Setter
@XmlRootElement(name = "userConfiguration")
@XmlType(propOrder = {"userTheme", "saveProfiles", "modProfiles"})
public class UserConfiguration {

    private String userTheme;

    private List<SaveProfile> saveProfiles;

    private List<ModProfile> modProfiles;

    /**
     * Creates an entirely new XML configuration file to store user information with.
     */
    public UserConfiguration() {
        this.saveProfiles = new ArrayList<>();
        this.modProfiles = new ArrayList<>();
        this.userTheme = new PrimerLight().getName();
    }

    /**
     * Creates a new object from an existing file
     *
     * @param userConfigurationFile
     */
    public UserConfiguration(File userConfigurationFile) {
        //TODO: Unmarshal the XML File
    }

    //TODO: Remove
    public void saveXMLTest() throws JAXBException {
        ModProfile testModProfile = new ModProfile();
        Mod testMod = new Mod("123456789");
        List<String> testCategories = new ArrayList<>();
        testCategories.add("Test Category");
        testCategories.add("Three Cateory test");
        testMod.setCategories(testCategories);
        testModProfile.getModList().add(testMod);
        testModProfile.getModList().add(testMod);

        SaveProfile testSaveProfile = new SaveProfile();
        testSaveProfile.setSaveName("Test Save");
        testSaveProfile.setSavePath("Fake/Path");
        testSaveProfile.setLastAppliedModProfile(testModProfile.getId());
        testSaveProfile.setLastModifiedBy(ModProfileModificationSourceType.SEMM);
        saveProfiles.add(testSaveProfile);

        modProfiles.add(testModProfile);
        modProfiles.add(testModProfile);
        try {
            JAXBContext context = JAXBContext.newInstance(UserConfiguration.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter sw = new StringWriter();
            marshaller.marshal(this, sw);
            System.out.println(sw);
        } catch (JAXBException ex) {
            throw ex;
        }
    }

    @XmlElement(name = "userTheme")
    public void setUserTheme(String userTheme) {
        this.userTheme = userTheme;
    }

    @XmlElementWrapper(name = "saveProfiles")
    @XmlElement(name = "saveProfile")
    public void setSaveProfiles(List<SaveProfile> saveProfiles) {
        this.saveProfiles = saveProfiles;
    }

    @XmlElementWrapper(name = "modProfiles")
    @XmlElement(name = "modProfile")
    public void setModProfiles(List<ModProfile> modProfiles) {
        this.modProfiles = modProfiles;
    }
}
