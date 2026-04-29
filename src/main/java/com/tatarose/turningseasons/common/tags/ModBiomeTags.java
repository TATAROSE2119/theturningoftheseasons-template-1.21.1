package com.tatarose.turningseasons.common.tags;

import com.tatarose.turningseasons.TheTurningoftheSeasons;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * 本 mod 注册的所有 biome tag。
 *
 * <p>对应 6 个气候带 —— 每个气候带一个 tag。</p>
 *
 * <p>tag 文件路径约定：</p>
 * <pre>
 * data/theturningoftheseasons/tags/worldgen/biome/climate/&lt;name&gt;.json
 * </pre>
 *
 * <p>这里的 TagKey 只是"声明"，真正的成员由 JSON 文件定义。
 * 整合包作者可以建同名 tag 覆盖或追加（vanilla 数据包合并机制）。</p>
 *
 * <p>注：tag 路径里的 {@code climate/} 子目录只是命名空间下的文件夹，
 * Minecraft 不会因此把它当成"嵌套 tag"。</p>
 */
public final class ModBiomeTags {
    private ModBiomeTags() {}

    public static final TagKey<Biome> CLIMATE_TEMPERATE = create("climate/temperate");
    public static final TagKey<Biome> CLIMATE_COLD      = create("climate/cold");
    public static final TagKey<Biome> CLIMATE_ARID      = create("climate/arid");
    public static final TagKey<Biome> CLIMATE_TROPICAL  = create("climate/tropical");
    public static final TagKey<Biome> CLIMATE_ALPINE    = create("climate/alpine");
    public static final TagKey<Biome> CLIMATE_OCEANIC   = create("climate/oceanic");

    private static TagKey<Biome> create(String path) {
        return TagKey.create(
                Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath(TheTurningoftheSeasons.MODID, path)
        );
    }
}
