package com.tatarose.turningseasons.client.visual;

import com.tatarose.turningseasons.client.ClientSeasonState;
import com.tatarose.turningseasons.common.season.Season;
import com.tatarose.turningseasons.common.season.SeasonState;

/**
 * 季节颜色 tint。
 *
 * <p>对原版 vanilla 群系算出的草色 / 叶色 / 水色应用季节性后处理，
 * 得到带季节风味的最终颜色。</p>
 *
 * <p>变换由两步组成：</p>
 * <ol>
 *     <li><b>去饱和（desaturation）</b>：把 RGB 朝感知亮度（luma）拉近一定比例。
 *         单纯的逐通道乘法在数学上无法把"绿色"变成"灰色"——必须先把饱和度抽掉，
 *         否则不论怎么缩放，主导通道仍然会保留原色调。</li>
 *     <li><b>通道乘法</b>：在去饱和后的颜色上再做 R/G/B 各自乘子，
 *         用来加暖（autumn）、加冷（winter water）、提亮（spring）等。</li>
 * </ol>
 *
 * <p>设计思路：</p>
 * <ul>
 *     <li><b>SPRING</b>：嫩绿偏黄，整体提亮，几乎不去饱和。</li>
 *     <li><b>SUMMER</b>：保留 vanilla 表现（identity）。</li>
 *     <li><b>AUTUMN</b>：草色微黄，叶色强烈偏向橙红，水色略冷；轻度去饱和。</li>
 *     <li><b>WINTER</b>：重度去饱和（拉向灰），整体压暗，水色偏冷蓝。</li>
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

    // 每行参数顺序：{ desaturation, mulR, mulG, mulB }
    // desaturation：0 = 不动，1 = 完全灰度。
    // 数组顺序与 Season.values() 一致：SPRING, SUMMER, AUTUMN, WINTER。

    private static final float[][] GRASS = {
            { 0.05f, 1.05f, 1.10f, 0.85f }, // SPRING
            { 0.00f, 1.00f, 1.00f, 1.00f }, // SUMMER
            { 0.10f, 1.10f, 0.90f, 0.55f }, // AUTUMN
            { 0.70f, 0.85f, 0.85f, 0.95f }, // WINTER：重度去饱和 + 略冷
    };

    private static final float[][] FOLIAGE = {
            { 0.05f, 1.05f, 1.10f, 0.80f }, // SPRING
            { 0.00f, 0.95f, 1.05f, 0.95f }, // SUMMER（略深绿）
            { 0.05f, 1.30f, 0.65f, 0.30f }, // AUTUMN（强烈橙红）
            { 0.85f, 0.55f, 0.55f, 0.60f }, // WINTER：极强去饱和（雪压枝头观感）
    };

    private static final float[][] WATER = {
            { 0.00f, 1.00f, 1.05f, 1.05f }, // SPRING
            { 0.00f, 1.00f, 1.00f, 1.00f }, // SUMMER
            { 0.05f, 0.95f, 0.95f, 1.00f }, // AUTUMN
            { 0.30f, 0.75f, 0.85f, 1.10f }, // WINTER（去饱和 + 冷蓝）
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
        return s == null ? rgb : transform(rgb, GRASS[s.ordinal()]);
    }

    public static int applyFoliage(int rgb) {
        Season s = currentSeason();
        return s == null ? rgb : transform(rgb, FOLIAGE[s.ordinal()]);
    }

    public static int applyWater(int rgb) {
        Season s = currentSeason();
        return s == null ? rgb : transform(rgb, WATER[s.ordinal()]);
    }

    /**
     * 对 0xRRGGBB 做 (去饱和 → 通道乘 → clamp)。
     * 注意：vanilla BiomeColors 返回的就是 0xRRGGBB，无 alpha。
     */
    private static int transform(int rgb, float[] params) {
        float r = (rgb >> 16) & 0xFF;
        float g = (rgb >> 8)  & 0xFF;
        float b = (rgb)       & 0xFF;

        float desat = params[0];
        if (desat > 0f) {
            // 感知亮度（Rec. 601 luma）：人眼对绿色最敏感，所以系数最大。
            float luma = 0.299f * r + 0.587f * g + 0.114f * b;
            r += (luma - r) * desat;
            g += (luma - g) * desat;
            b += (luma - b) * desat;
        }

        int newR = clamp((int) (r * params[1]));
        int newG = clamp((int) (g * params[2]));
        int newB = clamp((int) (b * params[3]));
        return (newR << 16) | (newG << 8) | newB;
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
