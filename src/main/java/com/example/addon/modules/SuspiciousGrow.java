package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
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
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SuspiciousGrow extends Module {
    private final SettingGroup sgRender = settings.getDefaultGroup();
    private final SettingGroup sgRange = settings.createGroup("Range");
    private final SettingGroup sgThreading = settings.createGroup("Threading");

    // Render
    private final Setting<Boolean> notifications = sgRender.add(new BoolSetting.Builder()
        .name("notifications")
        .description("Show a notification when growth is detected.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> espColor = sgRender.add(new ColorSetting.Builder()
        .name("esp-color")
        .description("Color of the ESP boxes.")
        .defaultValue(new SettingColor(255, 25, 25, 120))
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("Rendering mode for ESP boxes.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<Boolean> showTracers = sgRender.add(new BoolSetting.Builder()
        .name("show-tracers")
        .description("Draw tracer lines to detected growth.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> tracerColor = sgRender.add(new ColorSetting.Builder()
        .name("tracer-color")
        .description("Color of tracer lines.")
        .defaultValue(new SettingColor(255, 25, 25, 200))
        .visible(showTracers::get)
        .build()
    );

    private final Setting<Boolean> chatFeedback = sgRender.add(new BoolSetting.Builder()
        .name("chat-feedback")
        .description("Announce growth detections in chat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cactus = sgRender.add(new BoolSetting.Builder()
        .name("cactus")
        .description("Detect cactus growth.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> sweetBerries = sgRender.add(new BoolSetting.Builder()
        .name("sweet-berries")
        .description("Detect sweet berry bush growth.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> memoryTime = sgRender.add(new IntSetting.Builder()
        .name("memory-time")
        .description("How long a detected growth stays highlighted.")
        .defaultValue(10)
        .min(1)
        .max(60)
        .sliderRange(1, 30)
        .build()
    );

    // Range
    private final Setting<Integer> range = sgRange.add(new IntSetting.Builder()
        .name("range")
        .description("Horizontal and vertical scan range around you.")
        .defaultValue(32)
        .min(4)
        .max(128)
        .sliderRange(4, 64)
        .build()
    );

    private final Setting<Integer> minY = sgRange.add(new IntSetting.Builder()
        .name("min-y")
        .description("Minimum Y level to scan.")
        .defaultValue(-64)
        .min(-64)
        .max(320)
        .sliderRange(-64, 128)
        .build()
    );

    private final Setting<Integer> maxY = sgRange.add(new IntSetting.Builder()
        .name("max-y")
        .description("Maximum Y level to scan.")
        .defaultValue(320)
        .min(-64)
        .max(320)
        .sliderRange(0, 320)
        .build()
    );

    // Threading
    private final Setting<Boolean> useThreading = sgThreading.add(new BoolSetting.Builder()
        .name("enable-threading")
        .description("Use a worker pool for chunk scanning.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> threadPoolSize = sgThreading.add(new IntSetting.Builder()
        .name("thread-pool-size")
        .description("Number of worker threads used for chunk scanning.")
        .defaultValue(2)
        .min(1)
        .max(8)
        .sliderRange(1, 8)
        .visible(useThreading::get)
        .build()
    );

    private final Setting<Boolean> limitChatSpam = sgThreading.add(new BoolSetting.Builder()
        .name("limit-chat-spam")
        .description("Limit repeated chat messages for the same position.")
        .defaultValue(true)
        .visible(chatFeedback::get)
        .build()
    );

    private final Set<BlockPos> detected = ConcurrentHashMap.newKeySet();
    private final Map<BlockPos, Long> detectedTime = new ConcurrentHashMap<>();
    private final Map<BlockPos, Integer> knownAge = new ConcurrentHashMap<>();
    private final Map<BlockPos, Long> lastMessage = new ConcurrentHashMap<>();

    private ExecutorService threadPool;

    public SuspiciousGrow() {
        super(
            AddonTemplate.CATEGORY,
            "suspicious-grow",
            "Detects cactus and sweet berry bush growth."
        );
    }

    @Override
    public void onActivate() {
        detected.clear();
        detectedTime.clear();
        knownAge.clear();
        lastMessage.clear();

        createThreadPool();

        // Scan chunks that are already loaded around the player.
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.world != null && mc.player != null) {
            scanLoadedArea();
        }
    }

    @Override
    public void onDeactivate() {
        detected.clear();
        detectedTime.clear();
        knownAge.clear();
        lastMessage.clear();

        shutdownThreadPool();
    }

    private void createThreadPool() {
        shutdownThreadPool();

        if (!useThreading.get()) return;

        threadPool = Executors.newFixedThreadPool(threadPoolSize.get());
    }

    private void shutdownThreadPool() {
        if (threadPool != null) {
            threadPool.shutdownNow();

            try {
                threadPool.awaitTermination(250, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            threadPool = null;
        }
    }

    @EventHandler
    private void onChunkLoad(ChunkDataEvent event) {
        if (!isActive()) return;

        WorldChunk chunk = event.chunk();

        if (useThreading.get() && threadPool != null) {
            threadPool.submit(() -> scanChunk(chunk));
        } else {
            scanChunk(chunk);
        }
    }

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        if (!isActive()) return;

        BlockPos pos = event.pos;

        if (!insideRange(pos)) return;
        if (pos.getY() < minY.get() || pos.getY() > maxY.get()) return;

        int oldAge = getAge(event.oldState);
        int newAge = getAge(event.newState);

        if (oldAge < 0 || newAge < 0) return;

        // This is the important part:
        // only report when the plant's growth stage actually increases.
        if (newAge > oldAge) {
            detectGrowth(pos.toImmutable(), oldAge, newAge);
        }

        knownAge.put(pos.toImmutable(), newAge);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null) return;

        long now = System.currentTimeMillis();
        long lifetime = memoryTime.get() * 1000L;

        detected.removeIf(pos -> {
            Long time = detectedTime.get(pos);

            if (time == null || now - time > lifetime) {
                detectedTime.remove(pos);
                return true;
            }

            return false;
        });

        Color sideColor = new Color(espColor.get());
        Color lineColor = new Color(espColor.get());
        Color tracer = new Color(tracerColor.get());

        Vec3d cameraPos = mc.player.getCameraPosVec(event.tickDelta);

        for (BlockPos pos : detected) {
            event.renderer.box(
                pos,
                sideColor,
                lineColor,
                shapeMode.get(),
                0
            );

            if (showTracers.get()) {
                Vec3d target = Vec3d.ofCenter(pos);

                event.renderer.line(
                    cameraPos.x,
                    cameraPos.y,
                    cameraPos.z,
                    target.x,
                    target.y,
                    target.z,
                    tracer
                );
            }
        }
    }

    private void scanLoadedArea() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.world == null || mc.player == null) return;

        BlockPos center = mc.player.getBlockPos();

        int r = range.get();
        int minChunkX = Math.floorDiv(center.getX() - r, 16);
        int maxChunkX = Math.floorDiv(center.getX() + r, 16);
        int minChunkZ = Math.floorDiv(center.getZ() - r, 16);
        int maxChunkZ = Math.floorDiv(center.getZ() + r, 16);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);

                if (!mc.world.isChunkLoaded(chunkPos.x, chunkPos.z)) continue;

                WorldChunk chunk = mc.world.getChunk(chunkX, chunkZ);

                if (useThreading.get() && threadPool != null) {
                    threadPool.submit(() -> scanChunk(chunk));
                } else {
                    scanChunk(chunk);
                }
            }
        }
    }

    private void scanChunk(WorldChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null) return;

        int playerChunkX = Math.floorDiv(mc.player.getBlockX(), 16);
        int playerChunkZ = Math.floorDiv(mc.player.getBlockZ(), 16);

        int chunkDistance = Math.max(
            Math.abs(chunkPos.x - playerChunkX),
            Math.abs(chunkPos.z - playerChunkZ)
        );

        if (chunkDistance * 16 > range.get() + 16) return;

        int bottom = Math.max(minY.get(), -64);
        int top = Math.min(maxY.get(), 320);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = bottom; y <= top; y++) {
                    BlockPos pos = new BlockPos(
                        chunkPos.getStartX() + x,
                        y,
                        chunkPos.getStartZ() + z
                    );

                    if (!insideRange(pos)) continue;

                    BlockState state = chunk.getBlockState(pos);

                    int age = getAge(state);

                    if (age < 0) continue;

                    BlockPos key = pos.toImmutable();

                    /*
                     * A chunk scan establishes the current state.
                     * It does NOT falsely report every already-grown plant.
                     *
                     * Future age increases are caught by BlockUpdateEvent.
                     */
                    knownAge.put(key, age);
                }
            }
        }
    }

    private void detectGrowth(BlockPos pos, int oldAge, int newAge) {
        detected.add(pos);
        detectedTime.put(pos, System.currentTimeMillis());

        if (!chatFeedback.get()) return;

        long now = System.currentTimeMillis();

        if (limitChatSpam.get()) {
            Long last = lastMessage.get(pos);

            if (last != null && now - last < 3000L) {
                return;
            }

            lastMessage.put(pos, now);
        }

        String type = getPlantName(pos);

        if (notifications.get()) {
            info(
                "SuspiciousGrow: %s grew at %s (%d -> %d)",
                type,
                pos.toShortString(),
                oldAge,
                newAge
            );
        }
    }

    private String getPlantName(BlockPos pos) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.world == null) return "Plant";

        BlockState state = mc.world.getBlockState(pos);

        if (state.isOf(Blocks.CACTUS)) {
            return "Cactus";
        }

        if (state.isOf(Blocks.SWEET_BERRY_BUSH)) {
            return "Sweet Berry Bush";
        }

        return "Plant";
    }

    private int getAge(BlockState state) {
        if (cactus.get() && state.isOf(Blocks.CACTUS)) {
            return state.get(CactusBlock.AGE);
        }

        if (sweetBerries.get() && state.isOf(Blocks.SWEET_BERRY_BUSH)) {
            return state.get(SweetBerryBushBlock.AGE);
        }

        return -1;
    }

    private boolean insideRange(BlockPos pos) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null) return false;

        BlockPos player = mc.player.getBlockPos();

        int dx = pos.getX() - player.getX();
        int dy = pos.getY() - player.getY();
        int dz = pos.getZ() - player.getZ();

        int r = range.get();

        return dx * dx + dy * dy + dz * dz <= r * r;
    }
}


