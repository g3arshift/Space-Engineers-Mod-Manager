package com.gearshiftgaming.se_mod_manager.backend.data;

import com.gearshiftgaming.se_mod_manager.backend.models.*;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/** Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.

 */
public class UserDataFileRepository implements UserDataRepository {
    public Result<UserConfiguration> loadUserData(File userConfigurationFile) {
        Result<UserConfiguration> userConfigurationResult = new Result<>();
        try {
            JAXBContext context = JAXBContext.newInstance(UserConfiguration.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            UserConfiguration userConfiguration = (UserConfiguration) unmarshaller.unmarshal(userConfigurationFile);
            userConfigurationResult.addMessage("Successfully loaded user data.", ResultType.SUCCESS);
            userConfigurationResult.setPayload(userConfiguration);
        } catch (JAXBException f) {
            userConfigurationResult.addMessage("Failed to load user configuration. Error Details: " + f, ResultType.FAILED);
        }
        return userConfigurationResult;
    }

    public boolean saveUserData(UserConfiguration userConfiguration, File userConfigurationFile) throws IOException {
        if(!userConfigurationFile.exists()) {
            if(!userConfigurationFile.getParentFile().exists()) {
                Files.createDirectory(Path.of(userConfigurationFile.getParent()));
            }
            Files.createFile(userConfigurationFile.toPath());
        }

        try(BufferedWriter bw = new BufferedWriter(new FileWriter(userConfigurationFile))) {
            JAXBContext context = JAXBContext.newInstance(UserConfiguration.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter sw = new StringWriter();

            marshaller.marshal(userConfiguration, sw);
            bw.write(sw.toString());
            return true;
        } catch (JAXBException | IOException e) {
            return false;
        }
    }

    @Override
    public Result<Void> exportModlist(ModlistProfile modlistProfile, File modlistLocation) {
        ModlistProfile copiedProfile = new ModlistProfile(modlistProfile);

        Result<Void> result = new Result<>();

        try(BufferedWriter bw = new BufferedWriter(new FileWriter(modlistLocation))) {
            JAXBContext context = JAXBContext.newInstance(ModlistProfile.class);
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
    public Result<ModlistProfile> importModlist(File modlistLocation) {
        Result<ModlistProfile> modlistProfileResult = new Result<>();
        try {
            JAXBContext context = JAXBContext.newInstance(ModlistProfile.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            ModlistProfile modlistProfile = (ModlistProfile) unmarshaller.unmarshal(modlistLocation);
            modlistProfileResult.addMessage("Successfully loaded mod profile.", ResultType.SUCCESS);
            modlistProfileResult.setPayload(modlistProfile);
        } catch (JAXBException e) {
            modlistProfileResult.addMessage("Failed to load mod profile. Error Details: " + e, ResultType.FAILED);
		}
		return modlistProfileResult;
    }

    @Override
    public Result<Void> resetUserConfiguration(File userConfigurationLocation) {
        try {
            Files.delete(userConfigurationLocation.toPath());
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
