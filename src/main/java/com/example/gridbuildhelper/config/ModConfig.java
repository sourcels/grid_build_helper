package com.example.gridbuildhelper.config;

import com.example.gridbuildhelper.GridBuildHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    public int toggleConfigMenu = GLFW.GLFW_KEY_O;
    public int toggleVisibility = GLFW.GLFW_KEY_K;
    public String lastUsedProfile = "profile";

    public List<Profile> profiles = new ArrayList<>();

    public static class Profile {
        public String name = "profile";
        public int startX = 0;
        public int startY = 0;
        public int startZ = 0;
        public int step = 5;
        public int radius = 32;
        public String outlineColor = "FF0000";

        public Profile() {}

        public Profile(String name) {
            this.name = name;
        }

        public int getOutlineColorAsInt() {
            try {
                return (int) Long.parseLong("FF" + outlineColor, 16);
            } catch (NumberFormatException e) {
                GridBuildHelper.LOGGER.error("Invalid outline color: {}", outlineColor, e);
                return 0xFFFF0000;
            }
        }
    }
}
