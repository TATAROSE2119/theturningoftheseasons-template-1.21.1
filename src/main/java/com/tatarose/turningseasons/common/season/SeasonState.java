package com.tatarose.turningseasons.common.season;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 季节状态快照（不可变）。
 *
 * <p>这是服务端 → 客户端同步、命令查询、HUD 显示等场景共用的数据结构。</p>
 *
 * @param season             当前季节
 * @param year               当前年份（从 1 开始）
 * @param dayOfSeason        当前季节内已过的天数（0 表示季节第 1 天的开始）
 * @param daysPerSeason      当前模式下一个季节总天数（用于计算进度条）
 * @param growthMultiplier   全局作物生长倍率（1.0 = 原版速度）
 */
public record SeasonState(Season season, int year, int dayOfSeason, int daysPerSeason,
        double growthMultiplier) {

    /** 默认初始状态。仅作占位，真正的初值由服务器配置决定。 */
    public static final SeasonState DEFAULT = new SeasonState(Season.SPRING, 1, 0, 30, 1.0);

    /** 网络序列化用的 StreamCodec。 */
    public static final StreamCodec<ByteBuf, SeasonState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(Season::byOrdinal, Season::ordinal), SeasonState::season,
            ByteBufCodecs.VAR_INT, SeasonState::year,
            ByteBufCodecs.VAR_INT, SeasonState::dayOfSeason,
            ByteBufCodecs.VAR_INT, SeasonState::daysPerSeason,
            ByteBufCodecs.DOUBLE, SeasonState::growthMultiplier,
            SeasonState::new
    );

    /** 当前季节进度 [0, 1]，用于进度条 / HUD。 */
    public float progress() {
        if (daysPerSeason <= 0) return 0f;
        return Math.min(1f, (float) dayOfSeason / (float) daysPerSeason);
    }
}
