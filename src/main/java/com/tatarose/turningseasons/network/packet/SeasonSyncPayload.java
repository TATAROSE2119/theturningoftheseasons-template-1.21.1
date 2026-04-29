package com.tatarose.turningseasons.network.packet;

import com.tatarose.turningseasons.TheTurningoftheSeasons;
import com.tatarose.turningseasons.common.season.SeasonState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端的季节状态同步包。
 *
 * <p>触发时机：玩家加入服务器、跨日推进、命令变更季节。</p>
 *
 * <p>这里把 {@link SeasonState} 直接作为唯一字段。如果未来要附带"剩余 ticks"
 * 等更细粒度的同步信息，可以扩展为多字段；网络协议向后兼容工作通过
 * {@link com.tatarose.turningseasons.network.ModNetwork} 的 versionedRegistrar
 * 协议号管理。</p>
 */
public record SeasonSyncPayload(SeasonState state) implements CustomPacketPayload {

    /** 频道 ID，必须全局唯一。 */
    public static final CustomPacketPayload.Type<SeasonSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(TheTurningoftheSeasons.MODID, "season_sync"));

    /** 通过 SeasonState 的 codec 派生：用 map 直接换包装类型。 */
    public static final StreamCodec<ByteBuf, SeasonSyncPayload> STREAM_CODEC =
            SeasonState.STREAM_CODEC.map(SeasonSyncPayload::new, SeasonSyncPayload::state);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
