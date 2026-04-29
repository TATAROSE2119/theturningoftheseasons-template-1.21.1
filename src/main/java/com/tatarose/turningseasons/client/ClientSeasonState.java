package com.tatarose.turningseasons.client;

import com.tatarose.turningseasons.common.season.SeasonState;
import org.jetbrains.annotations.Nullable;

/**
 * 客户端缓存的季节状态。
 *
 * <p>由 {@link com.tatarose.turningseasons.network.ModNetwork} 在收到服务端
 * SeasonSyncPayload 时更新。HUD、未来的视觉/音效模块都从这里读取。</p>
 *
 * <p>玩家未连接服务器（主菜单、世界未加载）时为 null；
 * 离开世界时调用 {@link #clear()} 清空，避免显示陈旧数据。</p>
 */
public final class ClientSeasonState {
    private ClientSeasonState() {}

    @Nullable
    private static volatile SeasonState current = null;

    public static void update(SeasonState state) {
        current = state;
    }

    public static void clear() {
        current = null;
    }

    @Nullable
    public static SeasonState get() {
        return current;
    }

    public static boolean isAvailable() {
        return current != null;
    }
}
