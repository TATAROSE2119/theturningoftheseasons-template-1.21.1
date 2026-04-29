package com.tatarose.turningseasons.client.visual;

import com.tatarose.turningseasons.client.ClientSeasonState;
import com.tatarose.turningseasons.common.season.Season;
import com.tatarose.turningseasons.common.season.SeasonState;

/**
 * 季节颜色 tint。
 *
 * <p>对原版 vanilla 群系算出的草色 / 叶色 / 水色应用一个 RGB 通道乘子，
 * 得到带季节风味的最终颜色。</p>
 *
 * <p>设计思路：</p>
 * <ul>
 *     <li><b>SPRING</b>：嫩绿偏黄，整体提亮 → 通道乘子大致 (1.05, 1.10, 0.85)。</li>
 *     <li><b>SUMMER</b>：保留 vanilla 表现（接近 1.0），略饱和。</li>
 *     <li><b>AUTUMN</b>：草色微黄，叶色强烈偏向橙红，水色略冷。</li>
 *     <li><b>WINTER</b>：去饱和（拉向灰），整体压暗，水色偏蓝绿。</li>
 * </ul>
 *
 * <p>v1 仅基于 {@link Season}；后续迭代会引入气候带（ClimateZone）调制，
 * 例如 TROPICAL 永远夏季表现，ARID 整体偏黄褐，ALPINE 冬季加重。</p>
 *
 * <p>性能：本方法每帧每染色像素都会被调用，绝不做对象分配 / 哈希查找；
 * 直接位运算 + 静态 final 数组。</p>
 */
public final class SeasonTint {
    private SeasonTint() {}

    // 通道乘子顺序与 Season.values() 一致（SPRING, SUMMER, AUTUMN, WINTER）。
    // 每行 = (r, g, b)，1.0f = 不变。

    private static final float[][] GRASS_MUL = {
            { 1.05f, 1.10f, 0.85f }, // SPRING
            { 1.00f, 1.00f, 1.00f }, // SUMMER
            { 1.10f, 0.90f, 0.55f }, // AUTUMN
            { 0.75f, 0.78f, 0.85f }, // WINTER
    };

    private static final float[][] FOLIAGE_MUL = {
            { 1.05f, 1.10f, 0.80f }, // SPRING
            { 0.95f, 1.05f, 0.95f }, // SUMMER（略深绿）
            { 1.30f, 0.65f, 0.30f }, // AUTUMN（强烈橙红）
            { 0.55f, 0.55f, 0.55f }, // WINTER（去饱和压暗）
    };

    private static final float[][] WATER_MUL = {
            { 1.00f, 1.05f, 1.05f }, // SPRING
            { 1.00f, 1.00f, 1.00f }, // SUMMER
            { 0.95f, 0.95f, 1.00f }, // AUTUMN
            { 0.80f, 0.85f, 1.05f }, // WINTER（更冷蓝）
    };

    /**
     * 当前季节，由 {@link com.tatarose.turningseasons.network.ModNetwork}
     * 通过 SeasonSyncPayload 在客户端缓存中更新。
     * 主菜单 / 未连接服务器时返回 null → 直接走 vanilla 颜色。
     */
    private static Season currentSeason() {
        SeasonState s = ClientSeasonState.get();
        return s == null ? null : s.season();
    }

    public static int applyGrass(int rgb) {
        Season s = currentSeason();
        return s == null ? rgb : multiply(rgb, GRASS_MUL[s.ordinal()]);
    }

    public static int applyFoliage(int rgb) {
        Season s = currentSeason();
        return s == null ? rgb : multiply(rgb, FOLIAGE_MUL[s.ordinal()]);
    }

    public static int applyWater(int rgb) {
        Season s = currentSeason();
        return s == null ? rgb : multiply(rgb, WATER_MUL[s.ordinal()]);
    }

    /**
     * 对 0xRRGGBB 的 24 位整数做通道乘法并 clamp 回 0-255。
     * 注意：vanilla BiomeColors 返回的就是 0xRRGGBB，无 alpha。
     */
    private static int multiply(int rgb, float[] mul) {
        int r = clamp((int) (((rgb >> 16) & 0xFF) * mul[0]));
        int g = clamp((int) (((rgb >> 8)  & 0xFF) * mul[1]));
        int b = clamp((int) (( rgb        & 0xFF) * mul[2]));
        return (r << 16) | (g << 8) | b;
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
