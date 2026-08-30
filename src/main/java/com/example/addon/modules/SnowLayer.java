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
        .description("Horizontal distance to scan for snow.")
        .defaultValue(128)
        .range(64, 128)
        .sliderRange(64, 128)
        .build()
    );

    private final Setting<Integer> surfaceScan = sgGeneral.add(new IntSetting.Builder()
        .name("surface-scan")
        .description("Blocks above and below the detected surface to check.")
        .defaultValue(4)
        .range(1, 8)
        .sliderRange(1, 8)
        .build()
    );

    private final Setting<Integer> scanDelay = sgGeneral.add(new IntSetting.Builder()
        .name("scan-delay")
        .description("Ticks between scans. Lower is faster but uses more CPU.")
        .defaultValue(10)
        .range(1, 40)
        .sliderRange(1, 40)
        .build()
    );

    private final Setting<Boolean> layer1 = sgLayers.add(new BoolSetting.Builder()
        .name("layer-1")
        .description("Mark snow with 1 layer.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> layer2 = sgLayers.add(new BoolSetting.Builder()
        .name("layer-2")
        .description("Mark snow with 2 layers.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> layer3 = sgLayers.add(new BoolSetting.Builder()
        .name("layer-3")
        .description("Mark snow with 3 layers.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> layer4 = sgLayers.add(new BoolSetting.Builder()
        .name("layer-4")
        .description("Mark snow with 4 layers.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> layer5 = sgLayers.add(new BoolSetting.Builder()
        .name("layer-5")
        .description("Mark snow with 5 layers.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> layer6 = sgLayers.add(new BoolSetting.Builder()
        .name("layer-6")
        .description("Mark snow with 6 layers.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> layer7 = sgLayers.add(new BoolSetting.Builder()
        .name("layer-7")
        .description("Mark snow with 7 layers.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> layer8 = sgLayers.add(new BoolSetting.Builder()
        .name("layer-8")
        .description("Mark snow with 8 layers.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
        .name("color")
        .description("Color used to mark matching snow.")
        .defaultValue(new SettingColor(0, 150, 255, 60, false))
        .build()
    );

    private final Setting<Boolean> outline = sgRender.add(new BoolSetting.Builder()
        .name("outline")
        .description("Render an outline around matching snow.")
        .defaultValue(true)
        .build()
    );

    private final Map<BlockPos, Integer> foundSnow = new HashMap<>();

    private int tickCounter = 0;

    private int lastCenterX = Integer.MIN_VALUE;
    private int lastCenterZ = Integer.MIN_VALUE;

    public SnowLayer() {
        super(
            AddonTemplate.CATEGORY,
            "snow-layer",
            "Finds snow layers around loaded chunks, including when flying high above them."
        );
    }

    @Override
    public void onActivate() {
        foundSnow.clear();
        tickCounter = scanDelay.get();
    }

    @Override
    public void onDeactivate() {
        foundSnow.clear();
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;

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
        if (mc.world == null) return;

        int r = range.get();
        int scan = surfaceScan.get();

        Map<BlockPos, Integer> newResults = new HashMap<>();

        int minChunkX = (centerX - r) >> 4;
        int maxChunkX = (centerX + r) >> 4;

        int minChunkZ = (centerZ - r) >> 4;
        int maxChunkZ = (centerZ + r) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {

                int chunkCenterX = (chunkX << 4) + 8;
                int chunkCenterZ = (chunkZ << 4) + 8;

                int dx = chunkCenterX - centerX;
                int dz = chunkCenterZ - centerZ;

                int maxDistance = r + 16;

                if (dx * dx + dz * dz > maxDistance * maxDistance) {
                    continue;
                }

                BlockPos chunkCheckPos =
                    new BlockPos(chunkCenterX, mc.player.getBlockY(), chunkCenterZ);

                if (!mc.world.isChunkLoaded(chunkCheckPos)) {
                    continue;
                }

                int startX = chunkX << 4;
                int startZ = chunkZ << 4;

                for (int localX = 0; localX < 16; localX++) {
                    int worldX = startX + localX;

                    int xDistance = worldX - centerX;

                    if (Math.abs(xDistance) > r) continue;

                    for (int localZ = 0; localZ < 16; localZ++) {
                        int worldZ = startZ + localZ;

                        int zDistance = worldZ - centerZ;

                        if (Math.abs(zDistance) > r) continue;

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

                            BlockPos pos = new BlockPos(worldX, y, worldZ);

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
