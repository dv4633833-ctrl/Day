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
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.util.math.BlockPos;

public class SnowLayer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgLayers = settings.createGroup("Snow Layers");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("Horizontal distance to scan for snow.")
        .defaultValue(16)
        .range(4, 32)
        .sliderRange(4, 20)
        .build()
    );

    private final Setting<Integer> verticalRange = sgGeneral.add(new IntSetting.Builder()
        .name("vertical-range")
        .description("Vertical distance to scan.")
        .defaultValue(8)
        .range(1, 32)
        .sliderRange(1, 16)
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

    public SnowLayer() {
        super(
            AddonTemplate.CATEGORY,
            "snow-layer",
            "Finds snow layers with selected thickness."
        );
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;

        BlockPos center = mc.player.getBlockPos();

        int r = range.get();
        int vr = verticalRange.get();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {

                if (x * x + z * z > r * r) continue;

                for (int y = -vr; y <= vr; y++) {
                    BlockPos pos = center.add(x, y, z);

                    if (!mc.world.isChunkLoaded(pos)) continue;

                    BlockState state = mc.world.getBlockState(pos);

                    if (!state.isOf(Blocks.SNOW)) continue;

                    int layers = state.get(SnowBlock.LAYERS);

                    if (!isSelected(layers)) continue;

                    double height = layers / 8.0;

                    double minX = pos.getX();
                    double minY = pos.getY();
                    double minZ = pos.getZ();

                    double maxX = minX + 1.0;
                    double maxY = minY + height;
                    double maxZ = minZ + 1.0;

                    ShapeMode shapeMode = outline.get()
                        ? ShapeMode.Both
                        : ShapeMode.Sides;

                    event.renderer.box(
                        minX,
                        minY,
                        minZ,
                        maxX,
                        maxY,
                        maxZ,
                        color.get(),
                        color.get(),
                        shapeMode,
                        0
                    );
                }
            }
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
