package com.tatarose.turningseasons.network;

import com.tatarose.turningseasons.TheTurningoftheSeasons;
import com.tatarose.turningseasons.client.ClientSeasonState;
import com.tatarose.turningseasons.client.SeasonChangeNotifier;
import com.tatarose.turningseasons.network.packet.SeasonChangedPayload;
import com.tatarose.turningseasons.network.packet.SeasonSyncPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 网络包注册中心。
 *
 * <p>使用 NeoForge 1.21.1 的新版 PayloadRegistrar 体系：</p>
 * <ul>
 *     <li>在 {@link RegisterPayloadHandlersEvent} 中拿到 PayloadRegistrar；</li>
 *     <li>用 {@code playToClient} 注册 S → C 包；</li>
 *     <li>handler 接收 (payload, IPayloadContext)，IPayloadContext 已自动调度到主线程。</li>
 * </ul>
 *
 * <p>{@code versioned("1")} 的 "1" 是协议版本号；将来字段不兼容变更时升一档，
 * 服务器和客户端版本不同会拒绝连接，避免奇怪的运行期错误。</p>
 *
 * <p>注：NeoForge 21.1 后期把 {@code bus} 参数标记为 {@code @Deprecated(forRemoval=true)}，
 * 框架会根据事件类型（这里是 {@link RegisterPayloadHandlersEvent}，属于 mod 总线事件）
 * 自动路由，因此不再显式指定 bus。</p>
 */
@EventBusSubscriber(modid = TheTurningoftheSeasons.MODID)
public final class ModNetwork {
    private ModNetwork() {}

    /** 协议版本号；任何 packet 字段不兼容修改都应升级此值。 */
    public static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        // S -> C：服务器同步季节状态到客户端（高频，每跨日 + 玩家登录都会发）
        registrar.playToClient(
                SeasonSyncPayload.TYPE,
                SeasonSyncPayload.STREAM_CODEC,
                (payload, ctx) -> {
                    // ctx 已经把回调调度到了客户端主线程，这里直接更新缓存
                    if (ctx.flow().isClientbound()) {
                        ClientSeasonState.update(payload.state());
                    }
                }
        );

        // S -> C：季节切换的"事件"通知，用于触发标题动画 + 音效（低频，仅切季时发一次）
        registrar.playToClient(
                SeasonChangedPayload.TYPE,
                SeasonChangedPayload.STREAM_CODEC,
                (payload, ctx) -> {
                    if (ctx.flow().isClientbound()) {
                        SeasonChangeNotifier.show(
                                payload.previousSeason(),
                                payload.newSeason(),
                                payload.year());
                    }
                }
        );
    }
}
