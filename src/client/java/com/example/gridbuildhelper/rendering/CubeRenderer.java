package com.example.gridbuildhelper.rendering;

import com.example.gridbuildhelper.config.ConfigManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.Set;

public class CubeRenderer {

    public static Set<BlockPos> allGeneratedCubePositions = new HashSet<>();
    private static BlockPos originPos = null; // The fixed world origin of the entire grid
    private static boolean isEnabled = false;

    // Current config parameters (cached for performance)
    private static int currentStep = 0;
    private static int currentRadius = 0;
    private static int currentOffsetX = 0;
    private static int currentOffsetY = 0;
    private static int currentOffsetZ = 0;

    // For optimizing cube generation updates
    private static BlockPos lastCheckedPlayerBlockPos = null;
    private static int lastCheckedRadius = -1;
    private static int lastCheckedStep = -1;
    private static int lastCheckedOffsetX = Integer.MIN_VALUE;
    private static int lastCheckedOffsetY = Integer.MIN_VALUE;
    private static int lastCheckedOffsetZ = Integer.MIN_VALUE;


    public static void setEnabled(boolean enabled, BlockPos playerPosAtActivation) {
        isEnabled = enabled;
        if (enabled) {
            currentOffsetX = ConfigManager.selectedProfileInternal.startX;
            currentOffsetY = ConfigManager.selectedProfileInternal.startY;
            currentOffsetZ = ConfigManager.selectedProfileInternal.startZ;
            currentStep = ConfigManager.selectedProfileInternal.step;
            currentRadius = ConfigManager.selectedProfileInternal.radius;

            // Determine if the fixed origin or grid parameters have changed, requiring a reset
            boolean configChanged = (
                    currentOffsetX != lastCheckedOffsetX ||
                            currentOffsetY != lastCheckedOffsetY ||
                            currentOffsetZ != lastCheckedOffsetZ ||
                            currentStep != lastCheckedStep ||
                            currentRadius != lastCheckedRadius
            );

            if (originPos == null || configChanged) {
                // If config changed, clear all cubes and set new origin
                originPos = playerPosAtActivation.add(currentOffsetX, currentOffsetY, currentOffsetZ);
                allGeneratedCubePositions.clear();
                System.out.println("[CubeRenderer] Grid origin set to: " + originPos.toShortString() + " (Config Changed: " + configChanged + ")");

                // Update lastChecked parameters here to reflect the new state for setEnabled
                lastCheckedOffsetX = currentOffsetX;
                lastCheckedOffsetY = currentOffsetY;
                lastCheckedOffsetZ = currentOffsetZ;
                lastCheckedStep = currentStep;
                lastCheckedRadius = currentRadius;
            }

            // Reset lastCheckedPlayerBlockPos to force initial cube generation when mod is enabled
            lastCheckedPlayerBlockPos = null;

        } else {
            allGeneratedCubePositions.clear();
            originPos = null;
            // Reset all cached parameters and last checked positions for next activation
            currentStep = 0;
            currentRadius = 0;
            currentOffsetX = 0;
            currentOffsetY = 0;
            currentOffsetZ = 0;
            lastCheckedPlayerBlockPos = null;
            lastCheckedRadius = -1;
            lastCheckedStep = -1;
            lastCheckedOffsetX = Integer.MIN_VALUE;
            lastCheckedOffsetY = Integer.MIN_VALUE;
            lastCheckedOffsetZ = Integer.MIN_VALUE;
            System.out.println("[CubeRenderer] Grid disabled. All cubes cleared.");
        }
    }

    public static boolean isEnabled() {
        return isEnabled;
    }

    public static void ensureCubesAroundPlayer(BlockPos currentPlayerBlockPos) {
        if (!isEnabled || originPos == null || currentPlayerBlockPos == null || ConfigManager.selectedProfileInternal == null) {
            return;
        }

        // Cache current config values to avoid repeated lookups
        int newOffsetX = ConfigManager.selectedProfileInternal.startX;
        int newOffsetY = ConfigManager.selectedProfileInternal.startY;
        int newOffsetZ = ConfigManager.selectedProfileInternal.startZ;
        int newStep = ConfigManager.selectedProfileInternal.step;
        int newRadius = ConfigManager.selectedProfileInternal.radius;

        if (newRadius <= 0 || newStep <= 0) {
            // System.out.println("[CubeRenderer] Invalid radius or step. Not generating cubes.");
            return;
        }

        // Check if player moved significantly OR if config changed
        boolean playerMovedSignificantly = (
                lastCheckedPlayerBlockPos == null ||
                        Math.abs(currentPlayerBlockPos.getX() - lastCheckedPlayerBlockPos.getX()) >= newStep / 2 ||
                        Math.abs(currentPlayerBlockPos.getY() - lastCheckedPlayerBlockPos.getY()) >= newStep / 2 ||
                        Math.abs(currentPlayerBlockPos.getZ() - lastCheckedPlayerBlockPos.getZ()) >= newStep / 2
        );

        boolean configChanged = (
                newOffsetX != lastCheckedOffsetX ||
                        newOffsetY != lastCheckedOffsetY ||
                        newOffsetZ != lastCheckedOffsetZ ||
                        newStep != lastCheckedStep ||
                        newRadius != lastCheckedRadius
        );

        if (!playerMovedSignificantly && !configChanged) {
            return; // No need to generate new cubes
        }

        // Update cached config values and last checked position
        currentOffsetX = newOffsetX;
        currentOffsetY = newOffsetY;
        currentOffsetZ = newOffsetZ;
        currentStep = newStep;
        currentRadius = newRadius;

        lastCheckedPlayerBlockPos = currentPlayerBlockPos;
        lastCheckedRadius = newRadius;
        lastCheckedStep = newStep;
        lastCheckedOffsetX = newOffsetX;
        lastCheckedOffsetY = newOffsetY;
        lastCheckedOffsetZ = newOffsetZ;

        // Determine the range to generate cubes around the player's current position.
        // This range should be sufficient to cover the renderRadius plus a buffer,
        // using the largest of the step or radius for calculation to ensure coverage.
        // Using Math.max to account for large steps.
        int effectiveGenerationRange = Math.max(newRadius, newStep) + newStep * 3; // Added more buffer

        // Calculate the world coordinates of the bounding box for generation,
        // snapping them to the grid defined by originPos and currentStep.
        int minX = (int) Math.floor((double)(currentPlayerBlockPos.getX() - effectiveGenerationRange - originPos.getX()) / newStep) * newStep + originPos.getX();
        int maxX = (int) Math.ceil((double)(currentPlayerBlockPos.getX() + effectiveGenerationRange - originPos.getX()) / newStep) * newStep + originPos.getX();
        int minY = (int) Math.floor((double)(currentPlayerBlockPos.getY() - effectiveGenerationRange - originPos.getY()) / newStep) * newStep + originPos.getY();
        int maxY = (int) Math.ceil((double)(currentPlayerBlockPos.getY() + effectiveGenerationRange - originPos.getY()) / newStep) * newStep + originPos.getY();
        int minZ = (int) Math.floor((double)(currentPlayerBlockPos.getZ() - effectiveGenerationRange - originPos.getZ()) / newStep) * newStep + originPos.getZ();
        int maxZ = (int) Math.ceil((double)(currentPlayerBlockPos.getZ() + effectiveGenerationRange - originPos.getZ()) / newStep) * newStep + originPos.getZ();

        // System.out.println("[CubeRenderer] Generating cubes around player " + currentPlayerBlockPos.toShortString() +
        //                    " with step " + newStep + ", radius " + newRadius +
        //                    ". Range: [" + minX + "," + maxX + "]x[" + minY + "," + maxY + "]x[" + minZ + "," + maxZ + "]");

        int cubesAddedThisTick = 0;

        // Iterate through the calculated range, generating potential cube positions
        for (int x = minX; x <= maxX; x += newStep) {
            for (int y = minY; y <= maxY; y += newStep) {
                for (int z = minZ; z <= maxZ; z += newStep) {
                    BlockPos potentialCubePos = new BlockPos(x, y, z);

                    // The alignment check is crucial: ensure this potential cube actually lies on OUR grid.
                    // This is handled by how min/max X/Y/Z are calculated and the iteration step.
                    // So, these checks should ideally always be true if the math is correct.
                    // boolean alignsWithGridX = (potentialCubePos.getX() - originPos.getX()) % newStep == 0;
                    // boolean alignsWithGridY = (potentialCubePos.getY() - originPos.getY()) % newStep == 0;
                    // boolean alignsWithGridZ = (potentialCubePos.getZ() - originPos.getZ()) % newStep == 0;

                    // It's still good practice to explicitly check, or trust the loop's bounds.
                    // For robustness, let's keep the explicit modulo check, though it might be redundant with floor/ceil + step logic.
                    if ((potentialCubePos.getX() - originPos.getX()) % newStep == 0 &&
                            (potentialCubePos.getY() - originPos.getY()) % newStep == 0 &&
                            (potentialCubePos.getZ() - originPos.getZ()) % newStep == 0) {

                        // Check if this potential cube is within the actual spherical radius
                        // from the player's *dynamic* origin for generation purposes.
                        // This prevents generating cubes too far from the player's current view.
                        Vec3d dynamicCheckOrigin = currentPlayerBlockPos.add(currentOffsetX, currentOffsetY, currentOffsetZ).toCenterPos();
                        if (potentialCubePos.toCenterPos().distanceTo(dynamicCheckOrigin) <= newRadius + newStep) { // Add some buffer
                            if (allGeneratedCubePositions.add(potentialCubePos)) {
                                cubesAddedThisTick++;
                            }
                        }
                    }
                }
            }
        }
        // if (cubesAddedThisTick > 0) {
        //     System.out.println("[CubeRenderer] Added " + cubesAddedThisTick + " new cubes. Total: " + allGeneratedCubePositions.size());
        // }
    }


    public static void renderCubes(MatrixStack matrices, Camera camera) {
        if (!isEnabled || allGeneratedCubePositions.isEmpty() || originPos == null) return;

        Vec3d cameraPos = camera.getPos();

        int color = ConfigManager.selectedProfileInternal.getOutlineColorAsInt();

        float a = ((color >> 24) & 0xFF) / 255.0F;
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();

        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer buffer = immediate.getBuffer(RenderLayer.getLines());

        int renderVisibilityRadius = ConfigManager.selectedProfileInternal.radius;

        for (BlockPos pos : allGeneratedCubePositions) {
            // Render the cube only if it's within the renderVisibilityRadius from the PLAYER'S CAMERA
            if (Vec3d.ofCenter(pos).distanceTo(cameraPos) <= renderVisibilityRadius) {
                Box box = new Box(pos).offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                drawBoxLines(matrices, buffer, box, r, g, b, a);
            }
        }

        immediate.draw();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void drawLine(VertexConsumer buffer, MatrixStack.Entry entry,
                                 double x1, double y1, double z1,
                                 double x2, double y2, double z2,
                                 float r, float g, float b, float a) {
        Matrix4f positionMatrix = entry.getPositionMatrix();

        buffer.vertex(positionMatrix, (float) x1, (float) y1, (float) z1)
                .color(r, g, b, a)
                .normal(entry, 0, 1, 0);
        buffer.vertex(positionMatrix, (float) x2, (float) y2, (float) z2)
                .color(r, g, b, a)
                .normal(entry, 0, 1, 0);
    }

    private static void drawBoxLines(MatrixStack matrices, VertexConsumer buffer, Box box, float r, float g, float b, float a) {
        MatrixStack.Entry entry = matrices.peek();

        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;

        drawLine(buffer, entry, x1, y1, z1, x2, y1, z1, r, g, b, a);
        drawLine(buffer, entry, x1, y1, z1, x1, y1, z2, r, g, b, a);
        drawLine(buffer, entry, x2, y1, z1, x2, y1, z2, r, g, b, a);
        drawLine(buffer, entry, x1, y1, z2, x2, y1, z2, r, g, b, a);

        drawLine(buffer, entry, x1, y2, z1, x2, y2, z1, r, g, b, a);
        drawLine(buffer, entry, x1, y2, z1, x1, y2, z2, r, g, b, a);
        drawLine(buffer, entry, x2, y2, z1, x2, y2, z2, r, g, b, a);
        drawLine(buffer, entry, x1, y2, z2, x2, y2, z2, r, g, b, a);

        drawLine(buffer, entry, x1, y1, z1, x1, y2, z1, r, g, b, a);
        drawLine(buffer, entry, x2, y1, z1, x2, y2, z1, r, g, b, a);
        drawLine(buffer, entry, x1, y1, z2, x1, y2, z2, r, g, b, a);
        drawLine(buffer, entry, x2, y1, z2, x2, y2, z2, r, g, b, a);
    }
}