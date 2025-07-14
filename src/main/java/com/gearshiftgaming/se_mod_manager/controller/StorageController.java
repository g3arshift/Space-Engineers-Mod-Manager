package com.gearshiftgaming.se_mod_manager.controller;

import com.gearshiftgaming.se_mod_manager.backend.data.sandbox.SandboxConfigRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.save.SaveRepository;
import com.gearshiftgaming.se_mod_manager.backend.data.user.UserDataRepository;
import com.gearshiftgaming.se_mod_manager.backend.domain.sandbox.SandboxService;
import com.gearshiftgaming.se_mod_manager.backend.domain.save.SaveService;
import com.gearshiftgaming.se_mod_manager.backend.domain.user.UserDataService;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.modlist.ModListProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.save.SaveProfile;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.ResultType;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.SpaceEngineersVersion;
import com.gearshiftgaming.se_mod_manager.backend.models.user.UserConfiguration;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class StorageController {

    private static final Logger log = LogManager.getLogger(StorageController.class);
    private final SandboxService SANDBOX_SERVICE;

    private final UserDataService USER_DATA_SERVICE;

    private final SaveService SAVE_SERVICE;

    public StorageController(SandboxConfigRepository sandboxConfigRepository, UserDataRepository userDataRepository, SaveRepository saveRepository) {
        this.SANDBOX_SERVICE = new SandboxService(sandboxConfigRepository);
        this.USER_DATA_SERVICE = new UserDataService(userDataRepository);
        this.SAVE_SERVICE = new SaveService(saveRepository, SANDBOX_SERVICE);
    }
    
    public Result<Void> initializeData() {
        return USER_DATA_SERVICE.initializeData();
    }

    public Result<UserConfiguration> loadStartupData() {
        return USER_DATA_SERVICE.loadStartupData();
    }

	public Result<Void> saveUserConfiguration(UserConfiguration userConfiguration) {
		return USER_DATA_SERVICE.saveUserConfiguration(userConfiguration);
	}

    public Result<ModListProfile> loadFirstModListProfile() {
        return USER_DATA_SERVICE.loadFirstModListProfile();
    }

	public Result<ModListProfile> loadModListProfileById(UUID modListProfileId) {
		return USER_DATA_SERVICE.loadModListProfileById(modListProfileId);
	}

    public Result<Void> saveModListProfileDetails(Triple<UUID, String, SpaceEngineersVersion> modListProfileDetails) {
        return USER_DATA_SERVICE.saveModListProfileDetails(modListProfileDetails.getLeft(), modListProfileDetails.getMiddle(), modListProfileDetails.getRight());
    }

    public Result<Void> saveModListProfile(ModListProfile modListProfile) {
        return USER_DATA_SERVICE.saveModListProfile(modListProfile);
    }

    public Result<Void> deleteModListProfile(UUID modListProfileId) {
		return USER_DATA_SERVICE.deleteModListProfile(modListProfileId);
	}

	public Result<Void> updateModInformation(List<Mod> modList) {
		return USER_DATA_SERVICE.updateModInformation(modList);
	}

	public Result<Void> deleteSaveProfile(SaveProfile saveProfile) {
		return USER_DATA_SERVICE.deleteSaveProfile(saveProfile);
	}

	public Result<Void> updateModListProfileModList(UUID modListProfileId, List<Mod> modList) {
		return USER_DATA_SERVICE.updateModListProfileModList(modListProfileId, modList);
	}

	public Result<Void> updateModListActiveMods(UUID modListProfileId, List<Mod> modList) {
		return USER_DATA_SERVICE.updateModListActiveMods(modListProfileId, modList);
	}

	public Result<Void> updateModListLoadPriority(UUID modListProfileId, List<Mod> modList) {
		return USER_DATA_SERVICE.updateModListLoadPriority(modListProfileId, modList);
	}

	public Result<Void> saveSaveProfile(SaveProfile saveProfile) {
		return USER_DATA_SERVICE.saveSaveProfile(saveProfile);
	}

	public Result<Void> applyModlist(List<Mod> modList, SaveProfile saveProfile) throws IOException {
        File sandboxConfigFile = new File(saveProfile.getSavePath());
        Result<String> modifiedSandboxConfigResult = SANDBOX_SERVICE.injectModsIntoSandboxConfig(sandboxConfigFile, modList);
        if (modifiedSandboxConfigResult.isFailure()) {
            Result<Void> failedModification = new Result<>();
            failedModification.addMessage(modifiedSandboxConfigResult.getCurrentMessage(), ResultType.FAILED);
            return failedModification;
        }

        return SANDBOX_SERVICE.saveSandboxConfigToFile(saveProfile.getSavePath(), modifiedSandboxConfigResult.getPayload());
    }

    //TODO: Space Engineers version checking. Right now "getSessionName" is setup for only SE1.
    public Result<SaveProfile> getSpaceEngineersOneSaveProfile(File sandboxConfigFile) throws IOException {
        Result<SaveProfile> saveProfileResult = new Result<>();
        Result<String> sandboxFileResult = SANDBOX_SERVICE.getSandboxFromFile(sandboxConfigFile);
        if (sandboxFileResult.isFailure()) {
            saveProfileResult.addMessage(sandboxFileResult.getCurrentMessage(), sandboxFileResult.getType());
            return saveProfileResult;
        }

        String sandboxConfig = sandboxFileResult.getPayload();

        //Technically, the name of the save the game reads is in Sandbox.sbc, not Sandbox_config.sbc.
        // But when you rename a save in the game it changes both files, so this is probably fine since you can't rename it any other way than through manual modification.
        SaveProfile saveProfile = new SaveProfile(sandboxConfigFile, SpaceEngineersVersion.SPACE_ENGINEERS_ONE);
        saveProfile.setSaveName(SAVE_SERVICE.getSessionName(sandboxConfig, sandboxConfigFile.getPath()));

        saveProfileResult.setPayload(saveProfile);
        saveProfileResult.addMessage("Successfully loaded save: " + saveProfile.getSaveName(), ResultType.SUCCESS);
        return saveProfileResult;
    }

    public Result<String> getSaveName(File sandboxConfigFile) throws IOException {
        Result<String> sandboxfileResult = SANDBOX_SERVICE.getSandboxFromFile(sandboxConfigFile);
        if (sandboxfileResult.isSuccess()) {
            sandboxfileResult.setPayload(SAVE_SERVICE.getSessionName(sandboxfileResult.getPayload(), sandboxConfigFile.getPath()));
        }
        return sandboxfileResult;
    }

    public Result<SaveProfile> copySaveProfile(SaveProfile sourceSaveProfile, List<SaveProfile> saveProfileList) throws IOException {
        Result<SaveProfile> copyResult = SAVE_SERVICE.copySaveFiles(sourceSaveProfile, saveProfileList);
        if (copyResult.isSuccess()) {
            copyResult.addMessage("Successfully copied save \"" + sourceSaveProfile.getProfileName() + "\".", ResultType.SUCCESS);
        }
        return copyResult;
    }

    public Result<List<Mod>> getModlistFromSave(File sandboxConfigFile) {
        return SANDBOX_SERVICE.getModListFromSandboxConfig(sandboxConfigFile);
    }


    public Result<Void> exportModListProfile(ModListProfile modListProfile, File saveLocation) {
        return USER_DATA_SERVICE.exportModlistProfile(modListProfile, saveLocation);
    }


    public Result<ModListProfile> importModListProfile(File saveLocation) {
        return USER_DATA_SERVICE.importModlistProfile(saveLocation);
    }

	public Result<Void> resetData() {
		Result<Void> userConfigResetResult = USER_DATA_SERVICE.resetData();
		if (userConfigResetResult.isSuccess()) {
            UserConfiguration userConfiguration = new UserConfiguration();
            ModListProfile modListProfile = new ModListProfile();
            userConfiguration.setLastActiveModProfileId(modListProfile.getId());
            Result<Void> saveResult = USER_DATA_SERVICE.saveCurrentData(userConfiguration, modListProfile, userConfiguration.getSaveProfiles().getFirst());
            if(saveResult.isFailure()) {
                log.error(saveResult.getCurrentMessage());
                throw new RuntimeException(saveResult.getCurrentMessage());
            }
            userConfigResetResult.addMessage("Successfully deleted existing user configuration and saved new one.", ResultType.SUCCESS);
        }

		return userConfigResetResult;
	}

    //Only here for development purposes
//    public Result<Void> createTestUserData(Theme theme) {
//
//        SaveProfile testSaveProfile = new SaveProfile("Test Profile", "./Storage/fake.sbc", SpaceEngineersVersion.SPACE_ENGINEERS_ONE);
//        testSaveProfile.setSaveName("Test save");
//        ModListProfile testModListProfile = new ModListProfile("Test Profile", SpaceEngineersVersion.SPACE_ENGINEERS_ONE);
//
//        SteamMod testMod = new SteamMod("123456789");
//        List<String> testCategories = new ArrayList<>();
//        testCategories.add("Test Category");
//        testCategories.add("Three Category test");
//        testMod.setFriendlyName("Test Mod");
//        testMod.setCategories(testCategories);
//        testModListProfile.getModList().add(testMod);
//
//        SteamMod secondTestMod = new SteamMod("0987654321");
//        secondTestMod.setFriendlyName("Second test mod");
//        secondTestMod.setCategories(testCategories);
//        testModListProfile.getModList().add(secondTestMod);
//
//        ModIoMod thirdTestMod = new ModIoMod("122122");
//        thirdTestMod.setFriendlyName("Third test mod");
//        thirdTestMod.setCategories(testCategories);
//        testModListProfile.getModList().add(thirdTestMod);
//
//        testSaveProfile.setLastUsedModProfileId(testModListProfile.getID());
//
//        UserConfiguration userConfiguration = new UserConfiguration();
//        userConfiguration.getSaveProfiles().removeFirst();
//        userConfiguration.getSaveProfiles().add(testSaveProfile);
//        userConfiguration.getModListProfilesBasicInfo().add(testModListProfile);
//        userConfiguration.setUserTheme(theme.getName());
//
//        System.out.println("Created test user data.");
//        try {
//            return USER_DATA_SERVICE.saveUserData(userConfiguration);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

//    private UserConfiguration sortUserConfigurationModLists(UserConfiguration userConfiguration) {
//        UserConfiguration sortedUserConfiguration = new UserConfiguration(userConfiguration);
//        for (ModListProfile m : sortedUserConfiguration.getModListProfilesBasicInfo()) {
//            List<Mod> sortedModList = m.getModList().stream()
//                    .sorted(Comparator.comparing(Mod::getLoadPriority))
//                    .toList();
//            m.setModList(sortedModList);
//        }
//        return sortedUserConfiguration;
//    }
}
