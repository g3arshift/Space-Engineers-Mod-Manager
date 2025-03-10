package com.gearshiftgaming.se_mod_manager.backend.data;

import com.gearshiftgaming.se_mod_manager.backend.models.ModListProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.ResultType;
import com.gearshiftgaming.se_mod_manager.backend.models.UserConfiguration;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import lombok.Getter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
@Getter
public class UserDataFileRepository implements UserDataRepository {
    private final File USER_CONFIGURATION_FILE;

    public UserDataFileRepository(File userConfigurationFile) {
        USER_CONFIGURATION_FILE = userConfigurationFile;
    }

    @Override
    public Result<UserConfiguration> loadUserData() {
        Result<UserConfiguration> userConfigurationResult = new Result<>();
        if (!USER_CONFIGURATION_FILE.exists()) {
            userConfigurationResult.addMessage("User data was not found. Defaulting to new user configuration.", ResultType.FAILED);
            userConfigurationResult.setPayload(new UserConfiguration());
        } else {
            try {
                JAXBContext context = JAXBContext.newInstance(UserConfiguration.class);
                Unmarshaller unmarshaller = context.createUnmarshaller();
                UserConfiguration userConfiguration = (UserConfiguration) unmarshaller.unmarshal(USER_CONFIGURATION_FILE);
                userConfigurationResult.addMessage("Successfully loaded user data.", ResultType.SUCCESS);
                userConfigurationResult.setPayload(userConfiguration);
            } catch (JAXBException f) {
                userConfigurationResult.addMessage("Failed to load user configuration. Error Details: " + f, ResultType.FAILED);
            }
        }
        return userConfigurationResult;
    }

    @Override
    public Result<Void> saveUserData(UserConfiguration userConfiguration) {
        Result<Void> saveResult = new Result<>();
        try {
            if (!USER_CONFIGURATION_FILE.exists()) {
                if (!USER_CONFIGURATION_FILE.getParentFile().exists()) {
                    Files.createDirectory(Path.of(USER_CONFIGURATION_FILE.getParent()));
                }
                Files.createFile(USER_CONFIGURATION_FILE.toPath());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USER_CONFIGURATION_FILE))) {
            JAXBContext context = JAXBContext.newInstance(UserConfiguration.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter sw = new StringWriter();

            marshaller.marshal(userConfiguration, sw);
            bw.write(sw.toString());
            saveResult.addMessage("Successfully saved user configuration.", ResultType.SUCCESS);
        } catch (JAXBException | IOException e) {
            saveResult.addMessage(e.toString(), ResultType.FAILED);
            saveResult.addMessage("Failed to save user data.", ResultType.FAILED);
        }
        return saveResult;
    }

    @Override
    public Result<Void> exportModlist(ModListProfile modListProfile, File modlistLocation) {
        ModListProfile copiedProfile = new ModListProfile(modListProfile);

        Result<Void> result = new Result<>();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(modlistLocation))) {
            JAXBContext context = JAXBContext.newInstance(ModListProfile.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter sw = new StringWriter();

            marshaller.marshal(copiedProfile, sw);
            bw.write(sw.toString());
            result.addMessage("Successfully exported modlist.", ResultType.SUCCESS);
        } catch (JAXBException | IOException e) {
            result.addMessage(e.toString(), ResultType.FAILED);
        }
        return result;
    }

    @Override
    public Result<ModListProfile> importModlist(File modlistLocation) {
        Result<ModListProfile> modlistProfileResult = new Result<>();
        try {
            JAXBContext context = JAXBContext.newInstance(ModListProfile.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            ModListProfile modListProfile = (ModListProfile) unmarshaller.unmarshal(modlistLocation);
            modlistProfileResult.addMessage("Successfully loaded mod profile.", ResultType.SUCCESS);
            modlistProfileResult.setPayload(modListProfile);
        } catch (JAXBException e) {
            modlistProfileResult.addMessage("Failed to load mod profile. Error Details: " + e, ResultType.FAILED);
        }
        return modlistProfileResult;
    }

    @Override
    public Result<Void> resetUserConfiguration() {
        try {
            Files.delete(USER_CONFIGURATION_FILE.toPath());
        } catch (IOException e) {
            Result<Void> failedResult = new Result<>();
            failedResult.addMessage(e.toString(), ResultType.FAILED);
            return failedResult;
        }

        Result<Void> successfulResult = new Result<>();
        successfulResult.addMessage("Successfully deleted user configuration file.", ResultType.SUCCESS);

        return successfulResult;
    }
}
