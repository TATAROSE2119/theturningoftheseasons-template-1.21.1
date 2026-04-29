package com.tatarose.turningseasons.server.season;

import com.tatarose.turningseasons.TheTurningoftheSeasons;
import com.tatarose.turningseasons.common.event.SeasonChangedEvent;
import com.tatarose.turningseasons.common.season.Season;
import com.tatarose.turningseasons.common.season.SeasonState;
import com.tatarose.turningseasons.config.CommonConfig;
import com.tatarose.turningseasons.config.ServerConfig;
import com.tatarose.turningseasons.network.packet.SeasonChangedPayload;
import com.tatarose.turningseasons.network.packet.SeasonSyncPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 服务端季节推进与同步逻辑。
 *
 * <p>本类是季节状态变更的"唯一入口"——命令、自动推进都必须经由这里，
 * 以保证：</p>
 * <ol>
 *     <li>状态修改 + 持久化（通过 SeasonSavedData）</li>
 *     <li>状态全量同步给客户端（SeasonSyncPayload）</li>
 *     <li>季节真正切换时触发 {@link SeasonChangedEvent} + 客户端通知（SeasonChangedPayload）</li>
 * </ol>
 *
 * <p>事件订阅在 {@link com.tatarose.turningseasons.server.ServerEvents}。</p>
 */
public final class SeasonManager {
    private SeasonManager() {}

    /** 一个 MC 日 = 24000 ticks。 */
    private static final long TICKS_PER_DAY = 24000L;

    /** 判断指定维度是否在配置中启用季节。 */
    public static boolean isDimensionEnabled(ServerLevel level) {
        var dim = level.dimension();
        if (dim == Level.OVERWORLD) return ServerConfig.ENABLE_OVERWORLD.get();
        if (dim == Level.NETHER) return ServerConfig.ENABLE_NETHER.get();
        if (dim == Level.END) return ServerConfig.ENABLE_END.get();
        // 其它模组维度暂时不启用，后续可扩展为白名单 / 数据驱动 tag
        return false;
    }

    /**
     * 每 tick 调用一次。检测跨日并推进季节。
     */
    public static void tick(ServerLevel level) {
        if (!isDimensionEnabled(level)) return;

        SeasonSavedData data = SeasonSavedData.get(level);
        long currentDay = level.getDayTime() / TICKS_PER_DAY;

        if (data.getLastTickedDay() < 0) {
            // 第一次启动：直接对齐到当前日，不补推进
            data.setLastTickedDay(currentDay);
            syncStateToLevel(level, data);
            return;
        }

        if (currentDay > data.getLastTickedDay()) {
            int daysPerSeason = ServerConfig.resolveDaysPerSeason();
            long delta = currentDay - data.getLastTickedDay();
            // 一次性推进多个日（玩家用 /time add 跳过多日时也能正确处理）
            for (long i = 0; i < delta; i++) {
                stepOneDayAndMaybeFire(level, data, daysPerSeason);
            }
            data.setLastTickedDay(currentDay);

            if (CommonConfig.ENABLE_DEBUG_LOGGING.get()) {
                TheTurningoftheSeasons.LOGGER.debug(
                        "[SeasonManager] {} advanced {} day(s) -> {} year={} day={}/{}",
                        level.dimension().location(), delta,
                        data.getSeason(), data.getYear(),
                        data.getDayOfSeason(), daysPerSeason);
            }

            syncStateToLevel(level, data);
        }
    }

    /**
     * 命令入口：把当前维度推进若干"季节天"，不修改世界 dayTime。
     * 跨过季节边界时会触发 SeasonChangedEvent + 客户端通知。
     *
     * @return 实际推进的天数（始终等于参数 days）
     */
    public static int advanceDays(ServerLevel level, int days) {
        SeasonSavedData data = SeasonSavedData.get(level);
        int daysPerSeason = ServerConfig.resolveDaysPerSeason();
        for (int i = 0; i < days; i++) {
            stepOneDayAndMaybeFire(level, data, daysPerSeason);
        }
        syncStateToLevel(level, data);
        return days;
    }

    /**
     * 命令入口：直接强制设置季节并把进度归零。
     * 仅当 newSeason ≠ 当前季节时才触发切换事件 / 客户端通知。
     */
    public static void forceSetSeason(ServerLevel level, Season newSeason) {
        SeasonSavedData data = SeasonSavedData.get(level);
        Season previous = data.getSeason();
        data.setSeason(newSeason);
        data.setDayOfSeason(0);
        if (previous != newSeason) {
            fireSeasonChanged(level, previous, newSeason, data.getYear());
        }
        syncStateToLevel(level, data);
    }

    /** 给单个玩家发送当前所在维度的季节状态（玩家登录或切换维度时调用）。 */
    public static void syncToPlayer(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!isDimensionEnabled(level)) {
            // 维度未启用，不发送：客户端缓存保持 null，HUD 不显示
            return;
        }
        SeasonSavedData data = SeasonSavedData.get(level);
        SeasonState state = data.toState(ServerConfig.resolveDaysPerSeason());
        PacketDistributor.sendToPlayer(player, new SeasonSyncPayload(state));
    }

    /** 把当前数据全量同步给该维度的所有在线玩家。 */
    public static void syncStateToLevel(ServerLevel level, SeasonSavedData data) {
        SeasonState state = data.toState(ServerConfig.resolveDaysPerSeason());
        var payload = new SeasonSyncPayload(state);
        for (ServerPlayer player : level.players()) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    // -----------------------------------------------------------------------
    // 内部辅助
    // -----------------------------------------------------------------------

    /**
     * 推进一天，并在跨季时触发事件 / 通知。
     * 注：这里不调用 syncStateToLevel，状态同步由调用方在循环结束后统一发一次，避免风暴。
     */
    private static void stepOneDayAndMaybeFire(ServerLevel level, SeasonSavedData data, int daysPerSeason) {
        Season before = data.getSeason();
        data.advanceOneDay(daysPerSeason);
        Season after = data.getSeason();
        if (before != after) {
            fireSeasonChanged(level, before, after, data.getYear());
        }
    }

    /**
     * 同时做两件事：</br>
     *   1. 在 NeoForge.EVENT_BUS 上发布 SeasonChangedEvent，给同进程的其它模块；</br>
     *   2. 把 SeasonChangedPayload 发给该维度所有玩家，触发客户端标题/音效。
     */
    private static void fireSeasonChanged(ServerLevel level, Season previous, Season next, int year) {
        NeoForge.EVENT_BUS.post(new SeasonChangedEvent(level, previous, next, year));

        var payload = new SeasonChangedPayload(previous, next, year);
        for (ServerPlayer player : level.players()) {
            PacketDistributor.sendToPlayer(player, payload);
        }

        if (CommonConfig.ENABLE_DEBUG_LOGGING.get()) {
            TheTurningoftheSeasons.LOGGER.debug(
                    "[SeasonManager] season changed in {}: {} -> {} (year {})",
                    level.dimension().location(), previous, next, year);
        }
    }
}
