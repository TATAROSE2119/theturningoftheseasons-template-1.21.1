package com.tatarose.turningseasons.client.visual;

import com.tatarose.turningseasons.TheTurningoftheSeasons;
import com.tatarose.turningseasons.client.ClientSeasonState;
import com.tatarose.turningseasons.common.season.ClimateZone;
import com.tatarose.turningseasons.common.season.ClimateZoneResolver;
import com.tatarose.turningseasons.common.season.Season;
import com.tatarose.turningseasons.common.season.SeasonState;
import com.tatarose.turningseasons.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 季节环境粒子。
 *
 * <p>每个客户端 tick 在玩家附近随机刷一些与当前季节匹配的粒子。
 * 使用两层过滤：</p>
 * <ol>
 *     <li><b>气候带过滤</b>：根据玩家所在 biome 的 ClimateZone 决定该季节是否生成粒子。
 *         例如 TROPICAL 永远没有秋叶 / 冬雪。</li>
 *     <li><b>邻近方块过滤</b>：粒子必须刷在合适的方块上方 / 下方。
 *         秋叶只在树冠下，春花瓣只在花丛 / 树叶 / 树苗附近，雪花要露天。</li>
 * </ol>
 *
 * <p>粒子类型当前全部使用 vanilla（CHERRY_LEAVES / FALLING_DUST / SNOWFLAKE），
 * 自定义贴图后续迭代再考虑。</p>
 *
 * <p>性能：每 tick 仅做 1 次气候带解析 + N 次方块查询（N = 粒子数）。</p>
 */
@EventBusSubscriber(modid = TheTurningoftheSeasons.MODID, value = Dist.CLIENT)
public final class SeasonParticleSpawner {
    private SeasonParticleSpawner() {}

    /** 玩家周围的水平半径，单位方块。 */
    private static final int RADIUS_HORIZ = 16;

    /** 春季花瓣的"源头"垂直偏移：脚下到头顶 6 格。 */
    private static final int SPRING_DY_MIN = -2;
    private static final int SPRING_DY_MAX = 6;

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        if (!ClientConfig.SHOW_SEASON_PARTICLES.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) return;

        SeasonState state = ClientSeasonState.get();
        if (state == null) return;
        Season season = state.season();
        if (season == Season.SUMMER) return;

        // 1) 气候带过滤（玩家所在 biome 解析 1 次/tick）
        ClimateZone zone = ClimateZoneResolver.resolve(level, player.blockPosition());
        if (!isAllowed(season, zone)) return;

        // 2) 按密度档位刷
        int count = particlesPerTick(ClientConfig.PARTICLE_DENSITY.get());
        for (int i = 0; i < count; i++) {
            switch (season) {
                case SPRING -> spawnSpring(level, player);
                case AUTUMN -> spawnAutumn(level, player);
                case WINTER -> spawnWinter(level, player);
                case SUMMER -> {} // 已在外面 return
            }
        }
    }

    /**
     * 气候带 × 季节允许矩阵：
     * <ul>
     *     <li>SPRING：仅 TEMPERATE / OCEANIC 看得到飘花瓣（沙漠 / 寒带 / 高山没花）</li>
     *     <li>AUTUMN：除 TROPICAL / ARID 外都有落叶（热带常绿、沙漠没叶）</li>
     *     <li>WINTER：仅 COLD / ALPINE / OCEANIC 飘雪</li>
     * </ul>
     */
    private static boolean isAllowed(Season season, ClimateZone zone) {
        return switch (season) {
            case SPRING -> zone == ClimateZone.TEMPERATE || zone == ClimateZone.OCEANIC;
            case AUTUMN -> zone != ClimateZone.TROPICAL && zone != ClimateZone.ARID;
            case WINTER -> zone == ClimateZone.COLD
                        || zone == ClimateZone.ALPINE
                        || zone == ClimateZone.OCEANIC;
            case SUMMER -> false;
        };
    }

    /** 把 1-5 的密度档位映射到每 tick 粒子数。 */
    private static int particlesPerTick(int density) {
        return switch (density) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            case 4 -> 5;
            default -> 8;
        };
    }

    /**
     * 春季：在 SMALL_FLOWERS / TALL_FLOWERS / LEAVES / SAPLINGS 附近刷花瓣。
     *
     * <p>策略：随机取一个候选格子，看 block 是不是花/叶/苗，
     * 不是就放弃这次粒子（不强行 retry，避免坏 chunk 导致死循环）。</p>
     */
    private static void spawnSpring(ClientLevel level, LocalPlayer player) {
        BlockPos pos = randomNearby(level, player, SPRING_DY_MIN, SPRING_DY_MAX);
        BlockState state = level.getBlockState(pos);
        if (!isSpringSource(state)) return;

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        level.addParticle(ParticleTypes.CHERRY_LEAVES,
                pos.getX() + rng.nextDouble(),
                pos.getY() + 0.6 + rng.nextDouble() * 1.4,
                pos.getZ() + rng.nextDouble(),
                0.0, 0.0, 0.0);
    }

    private static boolean isSpringSource(BlockState state) {
        return state.is(BlockTags.SMALL_FLOWERS)
            || state.is(BlockTags.TALL_FLOWERS)
            || state.is(BlockTags.LEAVES)
            || state.is(BlockTags.SAPLINGS);
    }

    /**
     * 秋季：仅在树冠（LEAVES）正下方刷"落叶屑"。
     *
     * <p>用 FALLING_DUST 配 leaves 方块状态，颜色取自叶子贴图，
     * 还会被 BiomeColors mixin 染上秋季 tint。</p>
     */
    private static void spawnAutumn(ClientLevel level, LocalPlayer player) {
        BlockPos top = randomTop(level, player, Heightmap.Types.MOTION_BLOCKING);
        BlockPos canopyPos = top.below();
        BlockState canopy = level.getBlockState(canopyPos);
        if (!canopy.is(BlockTags.LEAVES)) return;

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        level.addParticle(
                new BlockParticleOption(ParticleTypes.FALLING_DUST, canopy),
                canopyPos.getX() + rng.nextDouble(),
                canopyPos.getY() - 0.05,
                canopyPos.getZ() + rng.nextDouble(),
                0.0, 0.0, 0.0);
    }

    /**
     * 冬季：雪花从天空向下飘。
     *
     * <p>只在玩家头顶有天空时刷（避免在洞穴里下雪）。
     * 用 heightmap 顶部高度 + 玩家高度比较：玩家在顶部之下才算"在户外"。</p>
     */
    private static void spawnWinter(ClientLevel level, LocalPlayer player) {
        BlockPos playerPos = player.blockPosition();
        int playerTop = level.getHeight(Heightmap.Types.MOTION_BLOCKING, playerPos.getX(), playerPos.getZ());
        // 玩家头顶有方块（在洞穴 / 室内）→ 不刷
        if (playerPos.getY() + 2 < playerTop) return;

        BlockPos top = randomTop(level, player, Heightmap.Types.MOTION_BLOCKING);
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        level.addParticle(ParticleTypes.SNOWFLAKE,
                top.getX() + rng.nextDouble(),
                top.getY() + 2.0 + rng.nextDouble() * 6.0,
                top.getZ() + rng.nextDouble(),
                0.0, -0.05, 0.0);
    }

    /** 玩家周围随机 X/Z 列的 heightmap 顶部。 */
    private static BlockPos randomTop(ClientLevel level, LocalPlayer player, Heightmap.Types type) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int dx = rng.nextInt(-RADIUS_HORIZ, RADIUS_HORIZ + 1);
        int dz = rng.nextInt(-RADIUS_HORIZ, RADIUS_HORIZ + 1);
        int x = player.blockPosition().getX() + dx;
        int z = player.blockPosition().getZ() + dz;
        int y = level.getHeight(type, x, z);
        return new BlockPos(x, y, z);
    }

    /** 玩家周围随机 X/Y/Z 偏移。 */
    private static BlockPos randomNearby(ClientLevel level, LocalPlayer player, int dyMin, int dyMax) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int dx = rng.nextInt(-RADIUS_HORIZ, RADIUS_HORIZ + 1);
        int dy = rng.nextInt(dyMin, dyMax + 1);
        int dz = rng.nextInt(-RADIUS_HORIZ, RADIUS_HORIZ + 1);
        return player.blockPosition().offset(dx, dy, dz);
    }
}
