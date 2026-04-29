package com.tatarose.turningseasons.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 公共配置 —— 客户端和服务端各自加载一份，不会被自动同步。
 *
 * <p>用于一些"双端各自需要、但又不属于业务规则"的开关，
 * 例如调试日志开关。真正影响游戏规则的项请放到 {@link ServerConfig}。</p>
 */
public final class CommonConfig {
    private CommonConfig() {}

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_DEBUG_LOGGING;

    static {
        BUILDER.comment("Common (per-side) settings. Not synced between client and server.")
                .push("debug");

        ENABLE_DEBUG_LOGGING = BUILDER
                .comment("Print verbose season-system logs (e.g. day advance, sync packets).")
                .define("enableDebugLogging", false);

        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
