package com.example.gridbuildhelper;

import com.example.gridbuildhelper.config.ConfigManager;
import com.example.gridbuildhelper.rendering.CubeRenderer;
import com.example.gridbuildhelper.ui.ModConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class GridBuildHelperClient implements ClientModInitializer {
    private static KeyBinding toggleRenderKey;
    private static KeyBinding openGuiKey;

    public static boolean isRenderingEnabled = false;

    @Override
    public void onInitializeClient() {
        ConfigManager.loadConfig();

        toggleRenderKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.gridbuildhelper.toggle_rendering",
                InputUtil.Type.KEYSYM,
                ConfigManager.config.toggleVisibility,
                "category.gridbuildhelper"
        ));

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.gridbuildhelper.open_gui",
                InputUtil.Type.KEYSYM,
                ConfigManager.config.toggleConfigMenu,
                "category.gridbuildhelper"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleRenderKey.wasPressed()) {
                isRenderingEnabled = !isRenderingEnabled;
                if (isRenderingEnabled) {
                    if (client.player != null) {
                        // При включении, передаем текущую позицию игрока,
                        // чтобы CubeRenderer зафиксировал originPos.
                        CubeRenderer.setEnabled(true, client.player.getBlockPos());
                        client.player.sendMessage(Text.translatable("gridbuildhelper.grid_enabled"), false);
                    } else {
                        isRenderingEnabled = false;
                        CubeRenderer.setEnabled(false, null);
                    }
                } else {
                    CubeRenderer.setEnabled(false, null);
                    if (client.player != null) {
                        client.player.sendMessage(Text.translatable("gridbuildhelper.grid_disabled"), false);
                    }
                }
            }

            if (openGuiKey.wasPressed()) {
                client.setScreen(new ModConfigScreen(client.currentScreen));
            }

            if (isRenderingEnabled && client.player != null && ConfigManager.selectedProfileInternal != null) {
                CubeRenderer.ensureCubesAroundPlayer(client.player.getBlockPos());
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (isRenderingEnabled && MinecraftClient.getInstance().player != null && ConfigManager.selectedProfileInternal != null) {
                CubeRenderer.renderCubes(context.matrixStack(), context.camera());
            }
        });
    }
}