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
        .description("Ticks between scans. Lower is faster but uses more CPU.")
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
        .sliderRange(1, 20
