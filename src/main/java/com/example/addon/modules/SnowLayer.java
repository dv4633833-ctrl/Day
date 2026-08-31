package com.example.addon.modules;

import com.example.addon.AddonTemplate;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.Map;

public class SnowLayer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgLayers = settings.createGroup("Snow Layers");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("Horizontal scan range.")
        .defaultValue(128)
        .range(64, 128)
        .sliderRange(64, 128)
        .build());

    private final Setting<Integer> surfaceScan = sgGeneral.add(new IntSetting.Builder()
        .name("surface-scan")
        .description("Extra blocks around the surface.")
        .defaultValue(8)
        .range(1, 32)
        .sliderRange(1, 32)
        .build());

    private final Setting<Integer> scanDelay = sgGeneral.add(new IntSetting.Builder()
        .name("scan-delay")
        .description("Ticks between scans.")
        .defaultValue(10)
        .range(1, 40)
        .sliderRange(1, 40)
        .build());

    private final Setting<Boolean> layer1 = sgLayers.add(new BoolSetting.Builder()
        .name("layer-1")
        .description("Mark 1-layer snow.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> layer2 = sgLayers.add(new BoolSetting.Builder()
        .name("layer-2")
        .description("Mark 2-layer snow.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> layer3 = sgLayers.add(new BoolSetting.Builder()
        .name("layer-3")
        .description("Mark 3-layer snow.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> layer4 = sgLayers.add(new BoolSetting.Builder()
        .name("layer-4")
        .description("Mark 4-layer snow.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> layer5 = sgLayers.add(new BoolSetting.Builder()
        .name("layer-5")
        .description("Mark 5-layer snow.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> layer6 = sgLayers.add(new BoolSetting.Builder()
        .name("layer-6")
        .description("Mark 6-layer snow.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> layer7 = sgLayers.add(new BoolSetting.Builder()
        .name("layer-7")
        .description("Mark 7-layer snow.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> layer8 = sgLayers.add(new BoolSetting.Builder()
        .name("layer-8")
        .description("Mark 8-layer snow.")
        .defaultValue(false)
        .build());

    private final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
        .name("color")
        .description("Snow marker color.")
        .defaultValue(new SettingColor(0, 150, 255, 60, false))
        .build());

    private final Setting<Boolean> outline = sgRender.add(new BoolSetting.Builder()
        .name("outline")
        .description("Render outlines.")
        .defaultValue(true)
        .build());

    private final Map<BlockPos, Integer> foundSnow = new HashMap<>();

    private int tickCounter = 0;
    private int lastCenterX = Integer.MIN_VALUE;
    private int lastCenterZ = Integer.MIN_VALUE;

    public SnowLayer() {
        super(
            AddonTemplate.CATEGORY,
            "snow-layer",
            "Finds selected snow layers around loaded chunks."
        );
    }

    @Override
    public void onActivate() {
        foundSnow.clear();
        tickCounter = scanDelay.get();
        lastCenterX = Integer.MIN_VALUE;
        lastCenterZ = Integer.MIN_VALUE;
    }

    @Override
    public void onDeactivate() {
        foundSnow.clear();
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (mc.world == null || mc.player == null) {
            return;
        }

        tickCounter++;

        int playerX = mc.player.getBlockX();
        int playerZ = mc.player.getBlockZ();

        boolean movedEnough =
            Math.abs(playerX - lastCenterX) >= 8 ||
            Math.abs(playerZ - lastCenterZ) >= 8;

        if (tickCounter >= scanDelay.get() || movedEnough) {
            scanSnow(playerX, playerZ);

            lastCenterX = playerX;
            lastCenterZ = playerZ;
            tickCounter = 0;
        }

        for (Map.Entry<BlockPos, Integer> entry : foundSnow.entrySet()) {
            BlockPos pos = entry.getKey();
            int layers = entry.getValue();

            double height = layers / 8.0;

            event.renderer.box(
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                pos.getX() + 1.0,
                pos.getY() + height,
                pos.getZ() + 1.0,
                color.get(),
                color.get(),
                outline.get() ? ShapeMode.Both : ShapeMode.Sides,
                0
            );
        }
    }

    private void scanSnow(int centerX, int centerZ) {
        if (mc.world == null || mc.player == null) {
            return;
        }

        int r = range.get();
        int scan = surfaceScan.get();

        Map<BlockPos, Integer> newResults = new HashMap<>();

        int minChunkX = (centerX - r) >> 4;
        int maxChunkX = (centerX + r) >> 4;
        int minChunkZ = (centerZ - r) >> 4;
        int maxChunkZ = (centerZ + r) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {

                int startX = chunkX << 4;
                int startZ = chunkZ << 4;

                int chunkCenterX = startX + 8;
                int chunkCenterZ = startZ + 8;

                int dx = chunkCenterX - centerX;
                int dz = chunkCenterZ - centerZ;

                int maxDistance = r + 16;

                if (dx * dx + dz * dz > maxDistance * maxDistance) {
                    continue;
                }

                BlockPos chunkCheckPos = new BlockPos(
                    chunkCenterX,
                    mc.player.getBlockY(),
                    chunkCenterZ
                );

                if (!mc.world.isChunkLoaded(chunkCheckPos)) {
                    continue;
                }

                for (int localX = 0; localX < 16; localX++) {
                    int worldX = startX + localX;

                    int xDistance = worldX - centerX;

                    if (Math.abs(xDistance) > r) {
                        continue;
                    }

                    for (int localZ = 0; localZ < 16; localZ++) {
                        int worldZ = startZ + localZ;

                        int zDistance = worldZ - centerZ;

                        if (Math.abs(zDistance) > r) {
                            continue;
                        }

                        if (xDistance * xDistance + zDistance * zDistance > r * r) {
                            continue;
                        }

                        int surfaceY = mc.world.getTopY(
                            Heightmap.Type.WORLD_SURFACE,
                            worldX,
                            worldZ
                        );

                        for (int offset = -scan; offset <= scan; offset++) {
                            int y = surfaceY + offset;

                            BlockPos pos = new BlockPos(
                                worldX,
                                y,
                                worldZ
                            );

                            BlockState state = mc.world.getBlockState(pos);

                            if (!state.isOf(Blocks.SNOW)) {
                                continue;
                            }

                            int layers = state.get(SnowBlock.LAYERS);

                            if (!isSelected(layers)) {
                                continue;
                            }

                            newResults.put(pos.toImmutable(), layers);
                        }
                    }
                }
            }
        }

        foundSnow.clear();
        foundSnow.putAll(newResults);
    }

    private boolean isSelected(int layers) {
        return switch (layers) {
            case 1 -> layer1.get();
            case 2 -> layer2.get();
            case 3 -> layer3.get();
            case 4 -> layer4.get();
            case 5 -> layer5.get();
            case 6 -> layer6.get();
            case 7 -> layer7.get();
            case 8 -> layer8.get();
            default -> false;
        };
    }
                }
