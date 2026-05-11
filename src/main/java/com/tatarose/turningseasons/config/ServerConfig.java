package com.tatarose.turningseasons.config;

import com.tatarose.turningseasons.common.season.Season;
import com.tatarose.turningseasons.common.season.SeasonCalendarMode;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 服务端权威配置 —— 季节系统的核心规则，单机时由集成服务器加载。
 *
 * <p>对应开发方案 §五.2 Server Config，包含：</p>
 * <ul>
 *     <li>季节日历模式</li>
 *     <li>世界初始季节</li>
 *     <li>各维度是否启用季节</li>
 *     <li>自定义模式下的每季天数</li>
 * </ul>
 *
 * <p>这些值不应在客户端读取，客户端只通过 {@link com.tatarose.turningseasons.common.season.SeasonState}
 * 接收同步结果。</p>
 */
public final class ServerConfig {
    private ServerConfig() {}

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<SeasonCalendarMode> CALENDAR_MODE;
    public static final ModConfigSpec.IntValue CUSTOM_DAYS_PER_SEASON;
    public static final ModConfigSpec.EnumValue<Season> INITIAL_SEASON;

    public static final ModConfigSpec.BooleanValue ENABLE_OVERWORLD;
    public static final ModConfigSpec.BooleanValue ENABLE_NETHER;
    public static final ModConfigSpec.BooleanValue ENABLE_END;

    public static final ModConfigSpec.BooleanValue ENABLE_CROP_RESTRICTIONS;
    public static final ModConfigSpec.DoubleValue GLOBAL_GROWTH_MULTIPLIER;

    static {
        // === 季节核心规则 ===
        BUILDER.comment("Core season rules. Server-authoritative; clients only receive sync results.")
                .push("season");

        CALENDAR_MODE = BUILDER
                .comment(
                        "Season calendar mode.",
                        "STARDEW    = 30 MC days per season",
                        "REALISTIC  = 90 MC days per season",
                        "REAL_DATE  = sync to real-world date (NOT IMPLEMENTED in phase 0)",
                        "CUSTOM     = use customDaysPerSeason below"
                )
                .defineEnum("calendarMode", SeasonCalendarMode.STARDEW);

        CUSTOM_DAYS_PER_SEASON = BUILDER
                .comment("Days per season when calendarMode = CUSTOM. Range: [1, 36500].")
                .defineInRange("customDaysPerSeason", 30, 1, 36500);

        INITIAL_SEASON = BUILDER
                .comment("Initial season for newly created worlds.")
                .defineEnum("initialSeason", Season.SPRING);

        BUILDER.pop();

        // === 维度开关 ===
        BUILDER.comment("Per-dimension toggles. Disabled dimensions will not advance season state.")
                .push("dimensions");

        ENABLE_OVERWORLD = BUILDER.define("enableOverworld", true);
        ENABLE_NETHER = BUILDER.define("enableNether", false);
        ENABLE_END = BUILDER.define("enableEnd", false);

        BUILDER.pop();

        // === 作物生长限制 ===
        BUILDER.comment(
                "Seasonal crop restrictions.",
                "Controls which crops can grow in which seasons based on climate zone tags.",
                "Greenhouse detection (glass ceiling / artificial light / underground) bypasses all restrictions."
        )
                .push("crops");

        ENABLE_CROP_RESTRICTIONS = BUILDER
                .comment(
                        "Master toggle for seasonal crop restrictions.",
                        "When false, all crops grow normally regardless of season (vanilla behavior).",
                        "When true, crops only grow when their block is tagged as in-season for the current climate zone."
                )
                .define("enableCropRestrictions", true);

        GLOBAL_GROWTH_MULTIPLIER = BUILDER
                .comment(
                        "Global growth speed multiplier for in-season crops.",
                        "1.0 = vanilla speed. 2.0 = 2x faster (forces growth each random tick).",
                        "0.5 = half speed. 0.0 = stop growing entirely (even for in-season tags).",
                        "Range: [0.0, 10.0]."
                )
                .defineInRange("globalGrowthMultiplier", 1.0, 0.0, 10.0);

        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    /**
     * 取当前模式实际生效的"每季天数"。
     * STARDEW / REALISTIC 直接返回枚举默认值；CUSTOM 走配置值；
     * REAL_DATE 在阶段 0 尚未实现，临时退化为 STARDEW 的 30 天，
     * 后续阶段会替换成"真实日期 → 季节进度"映射。
     */
    public static int resolveDaysPerSeason() {
        SeasonCalendarMode mode = CALENDAR_MODE.get();
        return switch (mode) {
            case STARDEW, REALISTIC -> mode.getDefaultDaysPerSeason();
            case CUSTOM -> CUSTOM_DAYS_PER_SEASON.get();
            case REAL_DATE -> SeasonCalendarMode.STARDEW.getDefaultDaysPerSeason(); // TODO 阶段 6+ 实现
        };
    }
}
