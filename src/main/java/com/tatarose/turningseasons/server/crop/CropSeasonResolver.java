package com.tatarose.turningseasons.server.crop;

import com.tatarose.turningseasons.common.season.ClimateZone;
import com.tatarose.turningseasons.common.season.Season;
import com.tatarose.turningseasons.common.tags.ModCropTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * 无状态工具：查询作物方块是否"当季"。
 *
 * <p>通过 {@link ModCropTags#ALL_TAGS} 2D 表，
 * 用 [zone.ordinal()][season.ordinal()] 直接索引目标标签，
 * 然后调用 {@code block.builtInRegistryHolder().is(tag)} 查询。</p>
 *
 * <p>完全模仿 {@code ClimateZoneResolver} 的无状态纯函数模式。</p>
 */
public final class CropSeasonResolver {
    private CropSeasonResolver() {}

    /**
     * 判断指定作物在给定的 (季节, 气候带) 下是否当季。
     *
     * @param block  作物方块
     * @param season 当前季节
     * @param zone   当前位置的气候带
     * @return true 表示该作物在当前的 season + zone 下可以生长
     */
    public static boolean isInSeason(Block block, Season season, ClimateZone zone) {
        TagKey<Block> tag = ModCropTags.ALL_TAGS[zone.ordinal()][season.ordinal()];
        return block.builtInRegistryHolder().is(tag);
    }
}
