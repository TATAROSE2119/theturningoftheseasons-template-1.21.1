package com.tatarose.turningseasons.server.crop;

import com.tatarose.turningseasons.TheTurningoftheSeasons;
import com.tatarose.turningseasons.common.season.ClimateZone;
import com.tatarose.turningseasons.common.season.ClimateZoneResolver;
import com.tatarose.turningseasons.common.season.Season;
import com.tatarose.turningseasons.config.CommonConfig;
import com.tatarose.turningseasons.config.ServerConfig;
import com.tatarose.turningseasons.server.season.SeasonManager;
import com.tatarose.turningseasons.server.season.SeasonSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

/**
 * 服务端作物生长事件处理器。
 *
 * <p>拦截两个事件：</p>
 * <ul>
 *     <li>{@link CropGrowEvent.Pre} — 随机刻生长（CropBlock / StemBlock.randomTick）</li>
 *     <li>{@link BonemealEvent}   — 骨粉催熟</li>
 * </ul>
 *
 * <p>处理流程：</p>
 * <ol>
 *     <li>如果维度未启用季节 → 放行</li>
 *     <li>如果配置关闭了作物限制 → 放行</li>
 *     <li>如果当前位置是温室 → 放行</li>
 *     <li>查询作物-季节标签 → 当季则按倍率处理，非当季则阻止</li>
 * </ol>
 */
@EventBusSubscriber(modid = TheTurningoftheSeasons.MODID)
public final class CropEventHandler {
    private CropEventHandler() {}

    /** 生长结果三分：放行 / 强制生长 / 阻止。 */
    private enum GrowthResult {
        ALLOW,
        FORCE_GROW,
        BLOCK
    }

    // ===== 随机生长 =====

    @SubscribeEvent
    public static void onCropGrowPre(CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        GrowthResult result = evaluate(serverLevel, event.getPos(), event.getState());
        switch (result) {
            case ALLOW -> { /* DEFAULT — 原版概率判定 */ }
            case FORCE_GROW -> event.setResult(CropGrowEvent.Pre.Result.GROW);
            case BLOCK -> event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
        }
    }

    // ===== 骨粉催熟 =====

    @SubscribeEvent
    public static void onBonemeal(BonemealEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        GrowthResult result = evaluate(serverLevel, event.getPos(), event.getState());
        if (result == GrowthResult.BLOCK) {
            event.setSuccessful(false);
        }
    }

    // ===== 核心判定 =====

    /**
     * 统一判定逻辑：根据配置、温室状态、季节标签和倍率，
     * 返回 ALLOW / FORCE_GROW / BLOCK。
     */
    private static GrowthResult evaluate(ServerLevel level, BlockPos pos, BlockState state) {
        // 1. 维度未启用 → 放行
        if (!SeasonManager.isDimensionEnabled(level)) {
            return GrowthResult.ALLOW;
        }

        // 2. 配置关闭作物限制 → 放行
        if (!ServerConfig.ENABLE_CROP_RESTRICTIONS.get()) {
            return GrowthResult.ALLOW;
        }

        // 3. 温室 → 放行
        if (GreenhouseDetector.isGreenhouse(level, pos)) {
            if (CommonConfig.ENABLE_DEBUG_LOGGING.get()) {
                TheTurningoftheSeasons.LOGGER.debug(
                        "[CropRestriction] Greenhouse bypass at {}", pos);
            }
            return GrowthResult.ALLOW;
        }

        // 4. 获取当前季节和气候带
        Season season = SeasonSavedData.get(level).getSeason();
        ClimateZone zone = ClimateZoneResolver.resolve(level, pos);

        // 5. 查询是否当季
        if (CropSeasonResolver.isInSeason(state.getBlock(), season, zone)) {
            return applyGrowthMultiplier(level, ServerConfig.GLOBAL_GROWTH_MULTIPLIER.get());
        }

        // 6. 非当季 → 阻止生长
        if (CommonConfig.ENABLE_DEBUG_LOGGING.get()) {
            TheTurningoftheSeasons.LOGGER.debug(
                    "[CropRestriction] Blocked {} at {} (season={}, zone={})",
                    state.getBlock().builtInRegistryHolder().key().location(),
                    pos, season, zone);
        }
        return GrowthResult.BLOCK;
    }

    /**
     * 根据全局倍率决定生长结果。
     *
     * <ul>
     *   <li>mult &ge; 2.0 → 强制生长（每次随机刻都 +1 阶段）</li>
     *   <li>mult &le; 0.0 → 完全阻止</li>
     *   <li>1.0 &lt; mult &lt; 2.0 → 有概率强制生长</li>
     *   <li>0.0 &lt; mult &lt; 1.0 → 有概率阻止生长</li>
     *   <li>mult = 1.0 → 原版行为</li>
     * </ul>
     */
    private static GrowthResult applyGrowthMultiplier(ServerLevel level, double mult) {
        if (mult >= 2.0) {
            return GrowthResult.FORCE_GROW;
        }
        if (mult <= 0.0) {
            return GrowthResult.BLOCK;
        }
        if (mult > 1.0) {
            // e.g. mult=1.5 → 50% 概率强制生长, 50% 概率原版
            return level.random.nextDouble() < (mult - 1.0)
                    ? GrowthResult.FORCE_GROW
                    : GrowthResult.ALLOW;
        }
        // 0 < mult < 1
        // e.g. mult=0.5 → 50% 概率原版, 50% 概率阻止
        return level.random.nextDouble() < mult
                ? GrowthResult.ALLOW
                : GrowthResult.BLOCK;
    }
}
