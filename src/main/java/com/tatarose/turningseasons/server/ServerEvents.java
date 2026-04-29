package com.tatarose.turningseasons.server;

import com.tatarose.turningseasons.TheTurningoftheSeasons;
import com.tatarose.turningseasons.common.command.SeasonCommand;
import com.tatarose.turningseasons.server.season.SeasonManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * 服务端事件订阅入口。
 *
 * <p>使用 {@link EventBusSubscriber} 自动注册到 NeoForge 的"游戏事件总线"
 * （即 {@code NeoForge.EVENT_BUS}），区别于 Mod 总线。</p>
 *
 * <p>关注的事件：</p>
 * <ul>
 *     <li>{@link LevelTickEvent.Post}      —— 每 tick 推进季节、检测跨日；</li>
 *     <li>{@link PlayerEvent.PlayerLoggedInEvent} —— 玩家加入时同步初始季节；</li>
 *     <li>{@link PlayerEvent.PlayerChangedDimensionEvent} —— 玩家切维度时重新同步；</li>
 *     <li>{@link RegisterCommandsEvent}    —— 注册 /season 命令。</li>
 * </ul>
 */
@EventBusSubscriber(modid = TheTurningoftheSeasons.MODID)
public final class ServerEvents {
    private ServerEvents() {}

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        // LevelTickEvent 在客户端世界也会触发，需要过滤掉
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            SeasonManager.tick(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SeasonManager.syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SeasonManager.syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SeasonCommand.register(event.getDispatcher());
    }
}
