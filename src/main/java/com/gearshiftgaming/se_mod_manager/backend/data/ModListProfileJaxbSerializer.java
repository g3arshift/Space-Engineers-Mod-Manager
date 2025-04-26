package com.gearshiftgaming.se_mod_manager.backend.data;

import com.gearshiftgaming.se_mod_manager.backend.models.ModListProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.ResultType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import java.io.*;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public abstract class ModListProfileJaxbSerializer {


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
            result.addMessage(getStackTrace(e), ResultType.FAILED);
        }
        return result;
    }
}
