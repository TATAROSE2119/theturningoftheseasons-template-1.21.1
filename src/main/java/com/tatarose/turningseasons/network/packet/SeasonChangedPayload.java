package com.tatarose.turningseasons.network.packet;

import com.tatarose.turningseasons.TheTurningoftheSeasons;
import com.tatarose.turningseasons.common.season.Season;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 季节"切换"通知包（S → C）。
 *
 * <p>与 {@link SeasonSyncPayload} 的区别：</p>
 * <ul>
 *     <li>{@link SeasonSyncPayload} —— 状态全量同步，玩家登录、跨日推进、命令变更后都会发，
 *         客户端用它更新缓存与 HUD。</li>
 *     <li>{@code SeasonChangedPayload} —— 只在"季节真的发生切换"时发一次，
 *         客户端用它弹标题 + 播放音效。永远跟随一个 SeasonSyncPayload 一起送出。</li>
 * </ul>
 *
 * <p>之所以拆成两个包：</p>
 * <ol>
 *     <li>同步是高频事件（每跨日都发一次状态），不应每次都触发标题动画；</li>
 *     <li>未来可能在客户端单独缓存"上一季节"，便于做过渡动画，单独的事件包语义更清晰。</li>
 * </ol>
 */
public record SeasonChangedPayload(Season previousSeason, Season newSeason, int year) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SeasonChangedPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(TheTurningoftheSeasons.MODID, "season_changed"));

    public static final StreamCodec<ByteBuf, SeasonChangedPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(Season::byOrdinal, Season::ordinal), SeasonChangedPayload::previousSeason,
            ByteBufCodecs.VAR_INT.map(Season::byOrdinal, Season::ordinal), SeasonChangedPayload::newSeason,
            ByteBufCodecs.VAR_INT, SeasonChangedPayload::year,
            SeasonChangedPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
