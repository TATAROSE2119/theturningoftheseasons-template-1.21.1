package com.tatarose.turningseasons.server.crop;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

/**
 * 温室检测器 —— 判断某个作物位置是否满足温室条件，
 * 从而绕过季节种植限制。
 *
 * <p>三个检测项（OR 关系，任一满足即视为温室）：</p>
 * <ol>
 *     <li><b>玻璃天花板</b> — 向上扫描 16 格，全为非遮挡方块</li>
 *     <li><b>人工光源</b>   — 方块光（火把等）≥ 9</li>
 *     <li><b>地下</b>       — Y &lt; 48 且无天空直接视野</li>
 * </ol>
 */
public final class GreenhouseDetector {
    private GreenhouseDetector() {}

    /** 向上扫描的最大距离。 */
    private static final int CEILING_SCAN_HEIGHT = 16;

    /** 人工光源最低亮度阈值。 */
    private static final int ARTIFICIAL_LIGHT_THRESHOLD = 9;

    /** 地下判定 Y 阈值。 */
    private static final int UNDERGROUND_Y_LEVEL = 48;

    /**
     * @return true 表示当前位置满足任一温室条件
     */
    public static boolean isGreenhouse(Level level, BlockPos pos) {
        return hasGlassCeiling(level, pos)
                || hasArtificialLight(level, pos)
                || isUnderground(level, pos);
    }

    /**
     * 检测1：向上逐格扫描。
     * 如果整条竖直线上所有方块都不遮挡（!canOcclude()），
     * 说明头顶是透明/无方块 → 视为玻璃天花板。
     */
    private static boolean hasGlassCeiling(Level level, BlockPos pos) {
        for (int dy = 1; dy <= CEILING_SCAN_HEIGHT; dy++) {
            BlockPos above = pos.above(dy);
            if (!level.isInWorldBounds(above)) {
                break;
            }
            if (level.getBlockState(above).canOcclude()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检测2：方块光（BLOCK 通道）≥ 阈值。
     * 只统计火把、荧石、灯笼等人工光源，不包括天空光。
     */
    private static boolean hasArtificialLight(Level level, BlockPos pos) {
        return level.getBrightness(LightLayer.BLOCK, pos) >= ARTIFICIAL_LIGHT_THRESHOLD;
    }

    /**
     * 检测3：Y 低于阈值且无法看到天空。
     * 洞穴农场 / 地下基地自动免受季节限制。
     */
    private static boolean isUnderground(Level level, BlockPos pos) {
        return pos.getY() < UNDERGROUND_Y_LEVEL && !level.canSeeSky(pos);
    }
}
