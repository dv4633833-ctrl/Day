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

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

public class SuspiciousGrow extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("How far to scan for cactus and sweet berry bushes.")
        .defaultValue(32)
        .range(4, 128)
        .sliderRange(4, 64)
        .build()
    );

    private final Setting<Integer> memoryTime = sgGeneral.add(new IntSetting.Builder()
        .name("memory-time")
        .description("How long a detected growth remains marked, in seconds.")
        .defaultValue(10)
        .range(1, 60)
        .sliderRange(1, 30)
        .build()
    );

    private final Setting<Boolean> cactus = sgGeneral.add(new BoolSetting.Builder()
        .name("cactus")
        .description("Detect cactus growth.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> berries = sgGeneral.add(new BoolSetting.Builder()
        .name("sweet-berries")
        .description("Detect sweet berry bush growth.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
        .name("color")
        .description("Color of detected growth.")
        .defaultValue(Color.RED)
        .build()
    );

    private final Map<BlockPos, Integer> lastAge = new HashMap<>();
    private final Map<BlockPos, Long> detected = new HashMap<>();

    private int tickCounter;

    public SuspiciousGrow() {
        super(
            AddonTemplate.CATEGORY,
            "suspicious-grow",
            "Detects cactus and sweet berry bushes when their growth stage increases."
        );
    }

    @Override
    public void onActivate() {
        lastAge.clear();
        detected.clear();
        tickCounter = 0;
    }

    @Override
    public void onDeactivate() {
        lastAge.clear();
        detected.clear();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) return;

        /*
         * Scanning every render frame would be unnecessarily expensive.
         * Scan approximately every 5 ticks instead.
         */
        tickCounter++;

        if (tickCounter >= 5) {
            tickCounter = 0;
            scan();
        }

        long now = System.currentTimeMillis();
        long lifetime = memoryTime.get() * 1000L;

        detected.entrySet().removeIf(entry -> now - entry.getValue() > lifetime);

        for (BlockPos pos : detected.keySet()) {
            event.renderer.box(
                pos,
                color.get(),
                color.get(),
                ShapeMode.Both,
                0
            );
        }
    }

    private void scan() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) return;

        BlockPos center = mc.player.blockPosition();
        int r = range.get();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {

                    if (x * x + y * y + z * z > r * r) continue;

                    BlockPos pos = center.offset(x, y, z);

                    if (!mc.level.hasChunkAt(pos)) continue;

                    BlockState state = mc.level.getBlockState(pos);

                    int age = getAge(state);

                    if (age < 0) continue;

                    BlockPos key = pos.immutable();

                    Integer oldAge = lastAge.get(key);

                    if (oldAge != null && age > oldAge) {
                        detected.put(key, System.currentTimeMillis());
                    }

                    lastAge.put(key, age);
                }
            }
        }

        /*
         * Remove positions that are no longer relevant from memory.
         * This prevents the map from growing forever while travelling.
         */
        if (lastAge.size() > 100000) {
            lastAge.clear();
        }
    }

    private int getAge(BlockState state) {
        if (cactus.get() && state.is(Blocks.CACTUS)) {
            return state.getValue(CactusBlock.AGE);
        }

        if (berries.get() && state.is(Blocks.SWEET_BERRY_BUSH)) {
            return state.getValue(SweetBerryBushBlock.AGE);
        }

        return -1;
    }
}



