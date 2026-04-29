package com.tatarose.turningseasons.common.season;

/**
 * 季节日历模式 —— 决定一个季节持续多少 MC 天。
 *
 * <p>对应开发方案 §三.3 的四种模式：</p>
 * <ul>
 *     <li>{@link #STARDEW}    —— 30 天一季，对应"星露谷物语"风格，节奏快。</li>
 *     <li>{@link #REALISTIC}  —— 90 天一季，模拟现实四季。</li>
 *     <li>{@link #REAL_DATE}  —— 同步真实世界日期（阶段 0 暂不实现，预留枚举）。</li>
 *     <li>{@link #CUSTOM}     —— 由 ServerConfig 中的 customDaysPerSeason 决定。</li>
 * </ul>
 *
 * <p>{@link #defaultDaysPerSeason} 仅用于直接从枚举派生天数的模式。
 * REAL_DATE / CUSTOM 模式的实际天数由其它逻辑给出，因此返回 0。</p>
 */
public enum SeasonCalendarMode {
    STARDEW(30),
    REALISTIC(90),
    REAL_DATE(0),
    CUSTOM(0);

    private final int defaultDaysPerSeason;

    SeasonCalendarMode(int defaultDaysPerSeason) {
        this.defaultDaysPerSeason = defaultDaysPerSeason;
    }

    public int getDefaultDaysPerSeason() {
        return defaultDaysPerSeason;
    }
}
