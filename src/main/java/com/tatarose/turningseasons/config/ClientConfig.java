package com.tatarose.turningseasons.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 客户端配置 —— 仅影响本地表现，不影响游戏规则。
 *
 * <p>对应开发方案 §五.3，阶段 0 只覆盖 HUD 显示相关。
 * 后续阶段再追加：树叶颜色强度、雾效强度、落叶粒子密度、BGM 开关、
 * shader 兼容修正等。</p>
 */
public final class ClientConfig {
    private ClientConfig() {}

    /** HUD 锚点位置。 */
    public enum HudAnchor {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ===== HUD =====
    public static final ModConfigSpec.BooleanValue SHOW_DEBUG_HUD;
    public static final ModConfigSpec.EnumValue<HudAnchor> HUD_ANCHOR;
    public static final ModConfigSpec.IntValue HUD_OFFSET_X;
    public static final ModConfigSpec.IntValue HUD_OFFSET_Y;

    // ===== 季节切换通知 =====
    public static final ModConfigSpec.BooleanValue SHOW_SEASON_TITLE;
    public static final ModConfigSpec.BooleanValue PLAY_SEASON_SOUND;

    // ===== 季节粒子 =====
    public static final ModConfigSpec.BooleanValue SHOW_SEASON_PARTICLES;
    public static final ModConfigSpec.IntValue PARTICLE_DENSITY;

    // ===== 季节雾 =====
    public static final ModConfigSpec.BooleanValue SHOW_SEASON_FOG;

    // ===== 作物指示器 =====
    public static final ModConfigSpec.BooleanValue SHOW_CROP_INDICATOR;

    static {
        BUILDER.comment("Client-only visual settings. Do not affect game rules.")
                .push("hud");

        SHOW_DEBUG_HUD = BUILDER
                .comment("Show the season debug HUD (current season, year, day, progress).")
                .define("showDebugHud", true);

        HUD_ANCHOR = BUILDER
                .comment("HUD anchor corner.")
                .defineEnum("hudAnchor", HudAnchor.TOP_RIGHT);

        HUD_OFFSET_X = BUILDER
                .comment("Horizontal offset (pixels) from anchor edge.")
                .defineInRange("hudOffsetX", 4, -2000, 2000);

        HUD_OFFSET_Y = BUILDER
                .comment("Vertical offset (pixels) from anchor edge.")
                .defineInRange("hudOffsetY", 4, -2000, 2000);

        BUILDER.pop();

        // 季节切换时的通知效果
        BUILDER.comment("Notifications shown when the season changes.")
                .push("notifications");

        SHOW_SEASON_TITLE = BUILDER
                .comment("Show a centered Title/Subtitle when the season changes.")
                .define("showSeasonTitle", true);

        PLAY_SEASON_SOUND = BUILDER
                .comment("Play a brief sound effect when the season changes.")
                .define("playSeasonSound", true);

        BUILDER.pop();

        // 季节环境粒子（落叶、雪花、花瓣等）
        BUILDER.comment("Ambient particle effects spawned around the player based on the current season.")
                .push("particles");

        SHOW_SEASON_PARTICLES = BUILDER
                .comment("Spawn ambient particles (autumn leaves, winter snow, spring petals) around the player.")
                .define("showSeasonParticles", true);

        PARTICLE_DENSITY = BUILDER
                .comment("Particle spawn rate (1 = sparse, 5 = lush). Higher values cost more CPU on the client.")
                .defineInRange("particleDensity", 3, 1, 5);

        BUILDER.pop();

        // 季节雾（仅秋晨 / 冬季 / 海洋带触发）
        BUILDER.comment("Season-driven fog overlay. Triggers only on autumn mornings, winter, and oceanic biomes.")
                .push("fog");

        SHOW_SEASON_FOG = BUILDER
                .comment("Tint and tighten the vanilla fog based on the current season and climate zone.")
                .define("showSeasonFog", true);

        BUILDER.pop();

        // 作物指示器 —— 准星对准作物时在上方显示生长信息
        BUILDER.comment("Crop growth indicator shown above crop blocks when looked at.")
                .push("cropIndicator");

        SHOW_CROP_INDICATOR = BUILDER
                .comment("Show crop growth info (progress bar, season, climate zone) when looking at a crop.")
                .define("showCropIndicator", true);

        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
