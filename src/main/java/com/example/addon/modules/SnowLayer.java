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
    private final SettingGroup sgActivity = settings.createGroup("Snow Activity");
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
        .description("Blocks around the detected surface to check for snow.")
        .defaultValue(4)
        .range(1, 8)
        .sliderRange(1, 8)
        .build()
    );

    private final Setting<Integer> scanDelay = sgGeneral.add(new IntSetting.Builder()
        .name("scan-delay")
        .description("Ticks between scans.")
        .defaultValue(10)
        .range(1, 40)
        .sliderRange(1, 40)
        .build()
    );

    private final Setting<Boolean> activity = sgActivity.add(new BoolSetting.Builder()
        .name("activity")
        .description("Mark areas where snow was observed increasing in layers.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> activityRadius = sgActivity.add(new IntSetting.Builder()
        .name("activity-radius")
        .description("Radius around detected snow activity.")
        .defaultValue(10)
        .range(1, 20)
        .sliderRange(1, 20)
        .build()
    );

    private final Setting<Integer> activityMemory = sgActivity.add(new IntSetting.Builder()
        .name("activity-memory")
        .description("How long activity markers remain visible, in seconds.")
        .defaultValue(60)
        .range(5, 600)
        .sliderRange(5, 300)
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

    private final Setting<SettingColor> activityColor = sgRender.add(new ColorSetting.Builder()
        .name("activity-color")
        .description("Color used to mark detected snow activity.")
        .defaultValue(new SettingColor(0, 255, 80, 60, false))
        .build()
    );

    private final Setting<Boolean> outline = sgRender.add(new BoolSetting.Builder()
        .name("outline")
        .description("Render an outline around matching snow.")
        .defaultValue(true)
        .build()
    );

    private final Map<BlockPos, Integer> foundSnow = new HashMap<>();
    private final Map<BlockPos, Integer> previousLayers = new HashMap<>();
    private final Map<BlockPos, Long> activityMarkers = new HashMap<>();

    private int tickCounter = 0;

    private int lastCenterX = Integer.MIN_VALUE;
    private int lastCenterZ = Integer.MIN_VALUE;

    public SnowLayer() {
        super(
            AddonTemplate.CATEGORY,
            "snow-layer",
            "Finds snow layers and detects observed snow accumulation activity."
        );
    }

    @Override
    public void onActivate() {
        foundSnow.clear();
        previousLayers.clear();
        activityMarkers.clear();

        tickCounter = scanDelay.get();

        lastCenterX = Integer.MIN_VALUE;
        lastCenterZ = Integer.MIN_VALUE;
    }

    @Override
    public void onDeactivate() {
        foundSnow.clear();
        previousLayers.clear();
        activityMarkers.clear();
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

        long now = System.currentTimeMillis();
        long memory = activityMemory.get() * 1000L;

        activityMarkers.entrySet().removeIf(
            entry -> now - entry.getValue() > memory
        );

        for (Map.Entry<BlockPos, Integer> entry : foundSnow.entrySet()) {
            Block
