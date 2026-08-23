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
import net.minecraft.block.CactusBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class SuspiciousGrow extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("Scan range.")
        .defaultValue(32)
        .range(4, 128)
        .sliderRange(4, 64)
        .build()
    );

    private final Setting<Integer> memoryTime = sgGeneral.add(new IntSetting.Builder()
        .name("memory-time")
        .description("How long detected growth remains visible.")
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
            "Detects cactus and sweet berry bush growth."
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
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.world == null || mc.player == null) return;

        tickCounter++;

        if (tickCounter >= 5) {
            tickCounter = 0;
            scan(mc);
        }

        long now = System.currentTimeMillis();
        long lifetime = memoryTime.get() * 1000L;

        detected.entrySet().removeIf(
            entry -> now - entry.getValue() > lifetime
        );

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

    private void scan(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return;

        BlockPos center = mc.player.getBlockPos();
        int r = range.get();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {

                    if (x * x + y * y + z * z > r * r) continue;

                    BlockPos pos = center.add(x, y, z);

                    if (!mc.world.isChunkLoaded(pos)) continue;

                    BlockState state = mc.world.getBlockState(pos);
                    int age = getAge(state);

                    if (age < 0) continue;

                    BlockPos key = pos.toImmutable();
                    Integer oldAge = lastAge.get(key);

                    if (oldAge != null && age > oldAge) {
                        detected.put(key, System.currentTimeMillis());
                    }

                    lastAge.put(key, age);
                }
            }
        }

        if (lastAge.size() > 100000) {
            lastAge.clear();
        }
    }

    private int getAge(BlockState state) {
        if (cactus.get() && state.isOf(Blocks.CACTUS)) {
            return state.get(CactusBlock.AGE);
        }

        if (berries.get() && state.isOf(Blocks.SWEET_BERRY_BUSH)) {
            return state.get(SweetBerryBushBlock.AGE);
        }

        return -1;
    }
}



        
    
                
