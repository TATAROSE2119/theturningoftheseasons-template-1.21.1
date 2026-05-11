package com.tatarose.turningseasons.client;

import com.tatarose.turningseasons.TheTurningoftheSeasons;
import com.tatarose.turningseasons.client.hud.SeasonHudOverlay;
import com.tatarose.turningseasons.client.indicator.CropIndicatorOverlay;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 客户端 mod 入口（仅在 PHYSICAL CLIENT 加载）。
 *
 * <p>职责：</p>
 * <ol>
 *     <li>注册 mod config 屏幕入口（NeoForge 自动生成）；</li>
 *     <li>注册 HUD 渲染层（季节 HUD + 作物指示器）；</li>
 *     <li>注册快捷键；</li>
 *     <li>玩家断开连接时清空客户端缓存。</li>
 * </ol>
 */
@Mod(value = TheTurningoftheSeasons.MODID, dist = Dist.CLIENT)
public class TheTurningoftheSeasonsClient {

    public TheTurningoftheSeasonsClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    /**
     * Mod 总线上的客户端事件 —— 注册 HUD 层和快捷键。
     */
    @EventBusSubscriber(modid = TheTurningoftheSeasons.MODID, value = Dist.CLIENT)
    public static final class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
            event.registerAboveAll(
                    ResourceLocation.fromNamespaceAndPath(TheTurningoftheSeasons.MODID, "season_hud"),
                    SeasonHudOverlay.INSTANCE
            );
        }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(CropIndicatorOverlay.TOGGLE_KEY);
        }
    }

    /**
     * 游戏总线上的客户端事件 —— 处理断线/登入和快捷键轮询。
     */
    @EventBusSubscriber(modid = TheTurningoftheSeasons.MODID, value = Dist.CLIENT)
    public static final class GameBusEvents {

        @SubscribeEvent
        public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientSeasonState.clear();
        }

        @SubscribeEvent
        public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
            ClientSeasonState.clear();
        }
    }
}
