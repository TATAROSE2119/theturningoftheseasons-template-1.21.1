package com.tatarose.turningseasons.common.season;

import com.tatarose.turningseasons.common.tags.ModBiomeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * 把 {@link Holder Biome 实例}解析成 {@link ClimateZone}。
 *
 * <p>查询遵循固定优先级（最特殊 → 最一般），保证当一个 biome 同时出现在
 * 多个气候 tag 中时，结果是确定的：</p>
 *
 * <ol>
 *     <li>{@link ClimateZone#ALPINE}    —— 山地、雪山，最特殊</li>
 *     <li>{@link ClimateZone#COLD}      —— 寒带，对季节表现影响最强</li>
 *     <li>{@link ClimateZone#TROPICAL}  —— 热带</li>
 *     <li>{@link ClimateZone#ARID}      —— 干旱</li>
 *     <li>{@link ClimateZone#OCEANIC}   —— 海洋性，常和其它带重叠（如 cold_ocean），优先级低</li>
 *     <li>{@link ClimateZone#TEMPERATE} —— 温带，作 fallback</li>
 * </ol>
 *
 * <p>这个优先级可以理解为"哪个气候特征最能定义玩家在该位置的季节体验"。
 * 例如 {@code snowy_beach} 同时是 cold 和 oceanic，最终归类为 cold，
 * 因为下雪的视觉/玩法影响远大于"靠海"。</p>
 *
 * <p>未在任何 climate tag 中的 biome 会落到 {@link ClimateZone#TEMPERATE}。
 * 这是为了让未配置的模组群系也能至少有"温带"表现，而不是直接没有季节。</p>
 *
 * <p>本类是纯函数（无状态），可以在任何线程调用；不持有 cache，
 * 因为 tag 查询本身是 O(1) hash 查表，性能足够。</p>
 */
public final class ClimateZoneResolver {
    private ClimateZoneResolver() {}

    /** 默认气候带。未匹配任何 tag 时使用。 */
    public static final ClimateZone DEFAULT = ClimateZone.TEMPERATE;

    /** 根据 biome holder 解析气候带。 */
    public static ClimateZone resolve(Holder<Biome> biome) {
        if (biome.is(ModBiomeTags.CLIMATE_ALPINE))    return ClimateZone.ALPINE;
        if (biome.is(ModBiomeTags.CLIMATE_COLD))      return ClimateZone.COLD;
        if (biome.is(ModBiomeTags.CLIMATE_TROPICAL))  return ClimateZone.TROPICAL;
        if (biome.is(ModBiomeTags.CLIMATE_ARID))      return ClimateZone.ARID;
        if (biome.is(ModBiomeTags.CLIMATE_OCEANIC))   return ClimateZone.OCEANIC;
        if (biome.is(ModBiomeTags.CLIMATE_TEMPERATE)) return ClimateZone.TEMPERATE;
        return DEFAULT;
    }

    /** 便捷方法：根据世界 + 坐标解析。 */
    public static ClimateZone resolve(Level level, BlockPos pos) {
        return resolve(level.getBiome(pos));
    }
}
