package com.tatarose.turningseasons.client;

import com.tatarose.turningseasons.TheTurningoftheSeasons;
import com.tatarose.turningseasons.client.hud.SeasonHudOverlay;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 客户端 mod 入口（仅在 PHYSICAL CLIENT 加载）。
 *
 * <p>职责：</p>
 * <ol>
 *     <li>注册 mod config 屏幕入口（NeoForge 自动生成）；</li>
 *     <li>注册 HUD 渲染层；</li>
 *     <li>玩家断开连接时清空客户端缓存。</li>
 * </ol>
 */
@Mod(value = TheTurningoftheSeasons.MODID, dist = Dist.CLIENT)
public class TheTurningoftheSeasonsClient {

    public TheTurningoftheSeasonsClient(ModContainer container) {
        // 注册 mod 选项菜单里的"配置"按钮，自动根据 ModConfigSpec 生成 GUI
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    /**
     * Mod 总线上的客户端事件 —— 注册 HUD 层。
     *
     * <p>NeoForge 21.1 后期废弃了 {@code bus} 参数，框架会根据事件类型自动路由：
     * {@link RegisterGuiLayersEvent} 是 mod 总线事件，无需手动指定。</p>
     */
    @EventBusSubscriber(modid = TheTurningoftheSeasons.MODID, value = Dist.CLIENT)
    public static final class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
            // 注册到所有 vanilla 层之上，避免被 hotbar / 经验条等覆盖
            event.registerAboveAll(
                    ResourceLocation.fromNamespaceAndPath(TheTurningoftheSeasons.MODID, "season_hud"),
                    SeasonHudOverlay.INSTANCE
            );
        }
    }

    /**
     * 游戏总线上的客户端事件 —— 处理玩家断线/登入时的客户端缓存。
     */
    @EventBusSubscriber(modid = TheTurningoftheSeasons.MODID, value = Dist.CLIENT)
    public static final class GameBusEvents {

        @SubscribeEvent
        public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
            // 离开服务器或单人世界时清空缓存，避免下次加载新世界时短暂显示旧状态
            ClientSeasonState.clear();
        }

        @SubscribeEvent
        public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
            // 进入新世界先清一次，等待服务端 PlayerLoggedInEvent 推送同步包
            ClientSeasonState.clear();
        }
    }
}
