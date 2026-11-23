package dev.fieldframe.arcana.client;

import com.mojang.logging.LogUtils;
import dev.fieldframe.arcana.ARegistry;
import dev.fieldframe.arcana.FieldframeArcana;
import dev.fieldframe.arcana.item.ArcanaComponents;
import dev.fieldframe.arcana.item.Charge;
import dev.fieldframe.arcana.item.GalvanicBowItem;
import dev.fieldframe.arcana.item.GalvanicJetpackItem;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/**
 * Development-only automated client check, inert unless the JVM runs with -Dfieldframe_arcana.autotest (only the Gradle
 * dev run passes it with -Pautotest, see build.gradle and tools/run_client_test.sh). In a fresh superflat survival world
 * "fatest" it holds the Galvanic Sword off and on, draws and fires the charged Galvanic Bow at two husks, switches it to quick fire and fires once, then wears the
 * Galvanic Jetpack and holds jump, then hovers. It takes screenshots of each, logs "FATEST" lines (husk health, height
 * and fall speed) and stops the client.
 */
@EventBusSubscriber(modid = FieldframeArcana.MODID, value = Dist.CLIENT)
public final class FAClientAutotest {
    private static final String MODE = System.getProperty("fieldframe_arcana.autotest");
    private static final Logger LOG = LogUtils.getLogger();
    private static final String WORLD = "fatest";
    private static final long TIMEOUT_NANOS = 4L * 60L * 1_000_000_000L;

    private static boolean titleSeen;
    private static boolean creating;
    private static long titleAt;
    private static int idleTicks;
    private static int worldTicks;
    private static boolean finished;
    private static double liftFrom;

    private FAClientAutotest() {
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        if (MODE == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || finished) {
            return;
        }
        if (titleSeen && System.nanoTime() - titleAt > TIMEOUT_NANOS) {
            LOG.info("FATEST timeout");
            finish(mc);
            return;
        }
        try {
            step(mc);
        } catch (Throwable t) {
            LOG.error("FATEST step failed", t);
        }
    }

    private static void step(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            if (!creating && mc.screen != null && !(mc.screen instanceof TitleScreen) && mc.getOverlay() == null && ++idleTicks > 200) {
                LOG.info("FATEST dismissing {}", mc.screen.getClass().getName());
                mc.setScreen(new TitleScreen());
                idleTicks = 0;
            }
            if (!creating && mc.screen instanceof TitleScreen title && mc.getOverlay() == null) {
                creating = true;
                titleSeen = true;
                titleAt = System.nanoTime();
                LOG.info("FATEST title");
                mc.options.pauseOnLostFocus = false;
                mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
                mc.options.framerateLimit().set(30);
                mc.options.renderDistance().set(4);
                mc.options.simulationDistance().set(4);
                deleteOldWorld(mc);
                LevelSettings settings = new LevelSettings(WORLD, GameType.SURVIVAL, false, net.minecraft.world.Difficulty.NORMAL, true, new net.minecraft.world.level.GameRules(), WorldDataConfiguration.DEFAULT);
                LOG.info("FATEST creating world {}", WORLD);
                mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(20261105L, false, false), ra -> ra.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), title);
            }
            return;
        }
        if (mc.screen instanceof PauseScreen) {
            mc.setScreen(null);
        }
        worldTicks++;
        switch (worldTicks) {
            case 1 -> LOG.info("FATEST joined");
            case 60 -> setUp(mc);
            case 100 -> shot(mc, "sword_off");
            case 105 -> onServer(mc, p -> {
                p.getMainHandItem().set(ArcanaComponents.ACTIVE.get(), Unit.INSTANCE);
                LOG.info("FATEST sword switched on");
            });
            case 125 -> shot(mc, "sword_on");
            case 128 -> shot(mc, "sword_on_frame2");
            case 135 -> {
                mc.player.getInventory().selected = 1;
                LOG.info("FATEST husks before {}", "(logged at release)");
            }
            case 140 -> {
                mc.options.keyUse.setDown(true);
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                LOG.info("FATEST bow using {}", mc.player.isUsingItem());
            }
            case 146 -> shot(mc, "bow_draw");
            case 152 -> {
                logHusks(mc, "before");
                mc.options.keyUse.setDown(false);
                LOG.info("FATEST bow released");
            }
            case 190 -> logHusks(mc, "after");
            case 191 -> mc.options.keyShift.setDown(true);
            case 192 -> mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            case 193 -> {
                mc.options.keyShift.setDown(false);
                onServer(mc, p -> LOG.info("FATEST bow mode {} rf {}", GalvanicBowItem.mode(p.getMainHandItem()), Charge.stored(p.getMainHandItem())));
            }
            case 194 -> {
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                LOG.info("FATEST bow quick fire, using {}", mc.player.isUsingItem());
            }
            // read the server a few ticks later: the use packet is handled after a same-tick server task
            case 198 -> onServer(mc, p -> LOG.info("FATEST bow quick rf {} cooldown {}", Charge.stored(p.getMainHandItem()),
                    p.getCooldowns().isOnCooldown(p.getMainHandItem().getItem())));
            case 200 -> {
                mc.player.getInventory().selected = 2;
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
            case 210 -> shot(mc, "jetpack_back");
            case 215 -> {
                liftFrom = mc.player.getY();
                mc.options.keyJump.setDown(true);
                LOG.info("FATEST jump held at y {}", liftFrom);
            }
            case 235 -> shot(mc, "jetpack_lift");
            case 240 -> {
                LOG.info("FATEST lift y {} -> {} (vy {})", liftFrom, mc.player.getY(), mc.player.getDeltaMovement().y);
                mc.options.keyJump.setDown(false);
                onServer(mc, p -> {
                    p.getItemBySlot(EquipmentSlot.CHEST).set(ArcanaComponents.MODE.get(), GalvanicJetpackItem.HOVER);
                    LOG.info("FATEST jetpack hover mode");
                });
            }
            case 260 -> {
                LOG.info("FATEST hover y {} vy {}", mc.player.getY(), mc.player.getDeltaMovement().y);
                shot(mc, "jetpack_hover");
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            }
            case 275 -> {
                LOG.info("FATEST hover y {} vy {}", mc.player.getY(), mc.player.getDeltaMovement().y);
                shot(mc, "jetpack_front");
            }
            case 290 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                LOG.info("FATEST done");
                finish(mc);
            }
            default -> {
            }
        }
    }

    private static void deleteOldWorld(Minecraft mc) {
        LevelStorageSource source = mc.getLevelSource();
        if (!source.levelExists(WORLD)) {
            return;
        }
        try (LevelStorageSource.LevelStorageAccess access = source.createAccess(WORLD)) {
            access.deleteLevel();
            LOG.info("FATEST deleted old world {}", WORLD);
        } catch (Exception e) {
            LOG.warn("FATEST could not delete old world {}", WORLD, e);
        }
    }

    /**
     * Superflat: grass at y=-61, the player stands at y=-60 facing north. Two husks (they don't burn) stand still 10
     * blocks ahead, 2 apart, so a bolt hits one and arcs to the other. Hotbar: charged sword, charged bow, empty hand;
     * charged jetpack worn. Chat stays hidden.
     */
    private static void setUp(Minecraft mc) {
        mc.options.hideGui = false;
        mc.options.chatVisibility().set(ChatVisiblity.HIDDEN);
        run(mc, List.of(
            "time set noon", "weather clear", "gamerule advance_time false", "gamerule advance_weather false",
            "gamerule spawn_mobs false", "difficulty easy", "fill -10 -60 -14 10 -52 8 minecraft:air",
            "summon minecraft:husk 0.5 -60 -7.5 {NoAI:1b,PersistenceRequired:1b,Silent:1b}",
            "summon minecraft:husk 2.5 -60 -7.5 {NoAI:1b,PersistenceRequired:1b,Silent:1b}",
            "clear @a", "tp @a 0.5 -60 3.5 180 0"));
        onServer(mc, p -> {
            ItemStack sword = new ItemStack(ARegistry.GALVANIC_SWORD.get());
            ItemStack bow = new ItemStack(ARegistry.GALVANIC_BOW.get());
            ItemStack pack = new ItemStack(ARegistry.GALVANIC_JETPACK.get());
            Charge.fill(sword);
            Charge.fill(bow);
            Charge.fill(pack);
            p.getInventory().setItem(0, sword);
            p.getInventory().setItem(1, bow);
            p.setItemSlot(EquipmentSlot.CHEST, pack);
            LOG.info("FATEST gear given: sword {}, bow {}, jetpack {} RF", Charge.stored(sword), Charge.stored(bow), Charge.stored(pack));
        });
        mc.player.getInventory().selected = 0;
    }

    private static void logHusks(Minecraft mc, String when) {
        onServer(mc, p -> {
            List<LivingEntity> husks = p.level().getEntitiesOfClass(LivingEntity.class, new AABB(-5, -61, -12, 6, -55, -3), e -> !(e instanceof ServerPlayer));
            StringBuilder sb = new StringBuilder();
            for (LivingEntity h : husks) {
                sb.append(String.format("%.1f@x%.1f ", h.getHealth(), h.getX()));
            }
            LOG.info("FATEST husks {}: {}bow rf {}", when, sb, Charge.stored(p.getInventory().getItem(1)));
        });
    }

    private static void onServer(Minecraft mc, Consumer<ServerPlayer> action) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) {
            LOG.error("FATEST no integrated server");
            return;
        }
        server.execute(() -> {
            if (!server.getPlayerList().getPlayers().isEmpty()) {
                action.accept(server.getPlayerList().getPlayers().get(0));
            }
        });
    }

    private static void run(Minecraft mc, List<String> commands) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) {
            LOG.error("FATEST no integrated server");
            return;
        }
        server.execute(() -> {
            for (String c : commands) {
                LOG.info("FATEST /{}", c);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
            }
        });
    }

    private static void shot(Minecraft mc, String view) {
        String name = "fatest_" + view + "_" + (System.currentTimeMillis() / 1000) + ".png";
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> LOG.info("FATEST screenshot message: {}", msg.getString()));
        LOG.info("FATEST shot {}", name);
    }

    private static void finish(Minecraft mc) {
        finished = true;
        LOG.info("FATEST stopping client");
        mc.stop();
    }
}
