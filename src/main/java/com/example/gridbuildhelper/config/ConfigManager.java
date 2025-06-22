package com.example.gridbuildhelper.config;

import com.example.gridbuildhelper.GridBuildHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;

public class ConfigManager {
    private static final String CONFIG_FILE = "blockoutlines.json";
    private static final File CONFIG_PATH = new File(FabricLoader.getInstance().getConfigDir().toFile(), CONFIG_FILE);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static ModConfig config;

    public static ModConfig.Profile selectedProfileInternal = null;

    public static ModConfig.Profile getSelectedProfile() {
        return selectedProfileInternal;
    }

    public static void setSelectedProfileByName(String name) {
        ModConfig.Profile profile = getProfileByName(name);
        if (profile != null) {
            setSelectedProfile(profile);
        }
    }

    public static void setSelectedProfile(ModConfig.Profile profile) {
        if (profile != null) {
            selectedProfileInternal = profile;
            config.lastUsedProfile = profile.name;
        }
    }

    public static void loadConfig() {
        if (CONFIG_PATH.exists()) {
            try (FileReader reader = new FileReader(CONFIG_PATH)) {
                config = GSON.fromJson(reader, ModConfig.class);
            } catch (IOException e) {
                GridBuildHelper.LOGGER.error("Failed to load config", e);
            }
        }

        if (config == null) {
            config = new ModConfig();
            ModConfig.Profile defaultProfile = new ModConfig.Profile();
            config.profiles.add(defaultProfile);
            config.lastUsedProfile = defaultProfile.name;
            saveProfile();
        }

        ModConfig.Profile found = getProfileByName(config.lastUsedProfile);
        if (found != null) {
            setSelectedProfile(found);
        } else if (!config.profiles.isEmpty()) {
            setSelectedProfile(config.profiles.getFirst());
        }
    }

    public static void saveProfile() {
        if (selectedProfileInternal != null) {
            config.lastUsedProfile = selectedProfileInternal.name;
        }

        try (FileWriter writer = new FileWriter(CONFIG_PATH)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            GridBuildHelper.LOGGER.error("Failed to save config", e);
        }
    }

    public static ModConfig.Profile getProfileByName(String name) {
        return config.profiles.stream()
                .filter(p -> p.name.equals(name))
                .findFirst()
                .orElse(null);
    }

    public static void renameProfile(String oldName, String newName) {
        Optional<ModConfig.Profile> match = config.profiles.stream()
                .filter(p -> p.name.equals(oldName))
                .findFirst();

        match.ifPresent(p -> {
            p.name = newName;

            if (config.lastUsedProfile.equals(oldName)) {
                config.lastUsedProfile = newName;
            }

            if (selectedProfileInternal != null && selectedProfileInternal.name.equals(oldName)) {
                selectedProfileInternal.name = newName;
            }

            saveProfile();
        });
    }

    public static void removeProfile(String profileName) {
        config.profiles.removeIf(p -> p.name.equals(profileName));

        if (selectedProfileInternal != null && selectedProfileInternal.name.equals(profileName)) {
            selectedProfileInternal = null;
        }

        if (profileName.equals(config.lastUsedProfile)) {
            if (!config.profiles.isEmpty()) {
                setSelectedProfile(config.profiles.getFirst());
            } else {
                config.lastUsedProfile = null;
            }
        }

        saveProfile();
    }
}