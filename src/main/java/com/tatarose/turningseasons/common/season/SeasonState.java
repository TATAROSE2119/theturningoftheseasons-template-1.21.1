package com.tatarose.turningseasons.common.season;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 季节状态快照（不可变）。
 *
 * <p>这是服务端 → 客户端同步、命令查询、HUD 显示等场景共用的数据结构。
 * 之所以做成 record 而不是字段挂在 SavedData 上直接读取，是为了：</p>
 * <ol>
 *     <li>客户端不持有 SavedData，只持有这个 record；</li>
 *     <li>序列化/反序列化只需一个 StreamCodec；</li>
 *     <li>future-proof：以后做温度、湿度、风等扩展时，只需扩展这个 record。</li>
 * </ol>
 *
 * @param season         当前季节
 * @param year           当前年份（从 1 开始）
 * @param dayOfSeason    当前季节内已过的天数（0 表示季节第 1 天的开始）
 * @param daysPerSeason  当前模式下一个季节总天数（用于计算进度条）
 */
public record SeasonState(Season season, int year, int dayOfSeason, int daysPerSeason) {

    /** 默认初始状态：第 1 年春季第 0 天，按 30 天一季。仅作占位，真正的初值由服务器配置决定。 */
    public static final SeasonState DEFAULT = new SeasonState(Season.SPRING, 1, 0, 30);

    /**
     * 网络序列化用的 StreamCodec。
     * 季节通过 ordinal 编码为 VAR_INT，节省字节，反序列化用 byOrdinal 防御异常。
     */
    public static final StreamCodec<ByteBuf, SeasonState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(Season::byOrdinal, Season::ordinal), SeasonState::season,
            ByteBufCodecs.VAR_INT, SeasonState::year,
            ByteBufCodecs.VAR_INT, SeasonState::dayOfSeason,
            ByteBufCodecs.VAR_INT, SeasonState::daysPerSeason,
            SeasonState::new
    );

    /** 当前季节进度 [0, 1]，用于进度条 / HUD。 */
    public float progress() {
        if (daysPerSeason <= 0) return 0f;
        return Math.min(1f, (float) dayOfSeason / (float) daysPerSeason);
    }
}
