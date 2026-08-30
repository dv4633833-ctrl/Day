package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;

public class SnowFinder extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // Range
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("How far to search for snow.")
        .defaultValue(16)
        .min(1)
        .max(64)
        .sliderRange(1, 32)
        .build()
    );

    // Layer selection
    private final Setting<Boolean> layer1 = sgGeneral.add(new BoolSetting.Builder()
        .name("layer-1")
        .description("Detect 1-layer snow.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> layer2 = sgGeneral.add(new BoolSetting.Builder()
        .name("layer-2")
        .description("Detect 2-layer snow.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> layer3 = sgGeneral.add(new BoolSetting.Builder()
        .name("layer-3")
        .description("Detect 3-layer snow.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> layer4 = sgGeneral.add(new BoolSetting.Builder()
        .name("layer-4")
        .description("Detect 4-layer snow.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> layer5 = sgGeneral.add(new BoolSetting.Builder()
        .name("layer-5")
        .description("Detect 5-layer snow.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> layer6 = sgGeneral.add(new BoolSetting.Builder()
        .name("layer-6")
        .description("Detect 6-layer snow.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> layer7 = sgGeneral.add(new BoolSetting.Builder()
        .name("layer-7")
        .description("Detect 7-layer snow.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> layer8 = sgGeneral.add(new BoolSetting.Builder()
        .name("layer-8")
        .description("Detect 8-layer snow.")
        .defaultValue(false)
        .build()
    );

    // Render
    private final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
        .name("color")
        .description("ESP color.")
        .defaultValue(new SettingColor(25, 255, 25, 120))
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the snow ESP is rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<Boolean> showLayer = sgRender.add(new BoolSetting.Builder()
        .name("show-layer")
        .description("Show the selected snow layer in chat when found.")
        .defaultValue(false)
        .build()
    );

    private final Set<BlockPos> foundSnow = new HashSet<>();

    public SnowFinder() {
        super(
            AddonTemplate.CATEGORY,
            "snow-finder",
            "Detects selected snow layer heights around you."
        );
    }

    @Override
    public void onActivate() {
        foundSnow.clear();
    }

    @Override
    public void onDeactivate() {
        foundSnow.clear();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.world == null || mc.player == null) return;

        foundSnow.clear();

        BlockPos center = mc.player.getBlockPos();
        int r = range.get();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {

                    if (x * x + y * y + z * z > r * r) continue;

                    BlockPos pos = center.add(x, y, z);

                    if (!mc.world.isChunkLoaded(
                        pos.getX() >> 4,
                        pos.getZ() >> 4
                    )) continue;

                    BlockState state = mc.world.getBlockState(pos);

                    if (!state.isOf(Blocks.SNOW)) continue;

                    int layers = state.get(SnowBlock.LAYERS);

                    if (!isSelected(layers)) continue;

                    foundSnow.add(pos.toImmutable());
                }
            }
        }

        Color espColor = new Color(color.get());

        for (BlockPos pos : foundSnow) {
            event.renderer.box(
                pos,
                espColor,
                espColor,
                shapeMode.get(),
                0
            );
        }
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


