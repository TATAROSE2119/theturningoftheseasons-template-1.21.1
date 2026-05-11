package com.tatarose.turningseasons.common.tags;

import com.tatarose.turningseasons.TheTurningoftheSeasons;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * 作物-季节对应标签。
 *
 * <p>每个 (气候带, 季节) 组合对应一个 TagKey&lt;Block&gt;，
 * 列出"当季"的作物方块。完全模仿 {@link ModBiomeTags} 的模式。</p>
 *
 * <p>tag 文件路径约定：</p>
 * <pre>
 * data/theturningoftheseasons/tags/block/crop/&lt;zone&gt;/&lt;season&gt;.json
 * </pre>
 *
 * <p>这里的 TagKey 只是声明，真正的成员由 JSON 文件定义。
 * 整合包作者可以通过数据包覆盖或追加（vanilla 数据包合并机制）。</p>
 */
public final class ModCropTags {
    private ModCropTags() {}

    // ===== 温带 =====
    public static final TagKey<Block> TEMPERATE_SPRING = create("crop/temperate/spring");
    public static final TagKey<Block> TEMPERATE_SUMMER = create("crop/temperate/summer");
    public static final TagKey<Block> TEMPERATE_AUTUMN = create("crop/temperate/autumn");
    public static final TagKey<Block> TEMPERATE_WINTER = create("crop/temperate/winter");

    // ===== 寒带 =====
    public static final TagKey<Block> COLD_SPRING   = create("crop/cold/spring");
    public static final TagKey<Block> COLD_SUMMER   = create("crop/cold/summer");
    public static final TagKey<Block> COLD_AUTUMN   = create("crop/cold/autumn");
    public static final TagKey<Block> COLD_WINTER   = create("crop/cold/winter");

    // ===== 干旱带 =====
    public static final TagKey<Block> ARID_SPRING   = create("crop/arid/spring");
    public static final TagKey<Block> ARID_SUMMER   = create("crop/arid/summer");
    public static final TagKey<Block> ARID_AUTUMN   = create("crop/arid/autumn");
    public static final TagKey<Block> ARID_WINTER   = create("crop/arid/winter");

    // ===== 热带 =====
    public static final TagKey<Block> TROPICAL_SPRING = create("crop/tropical/spring");
    public static final TagKey<Block> TROPICAL_SUMMER = create("crop/tropical/summer");
    public static final TagKey<Block> TROPICAL_AUTUMN = create("crop/tropical/autumn");
    public static final TagKey<Block> TROPICAL_WINTER = create("crop/tropical/winter");

    // ===== 高山带 =====
    public static final TagKey<Block> ALPINE_SPRING = create("crop/alpine/spring");
    public static final TagKey<Block> ALPINE_SUMMER = create("crop/alpine/summer");
    public static final TagKey<Block> ALPINE_AUTUMN = create("crop/alpine/autumn");
    public static final TagKey<Block> ALPINE_WINTER = create("crop/alpine/winter");

    // ===== 海洋带 =====
    public static final TagKey<Block> OCEANIC_SPRING = create("crop/oceanic/spring");
    public static final TagKey<Block> OCEANIC_SUMMER = create("crop/oceanic/summer");
    public static final TagKey<Block> OCEANIC_AUTUMN = create("crop/oceanic/autumn");
    public static final TagKey<Block> OCEANIC_WINTER = create("crop/oceanic/winter");

    /**
     * 2D 查询表，用 [zone.ordinal()][season.ordinal()] 直接索引对应标签。
     * 供 {@code CropSeasonResolver} 使用，避免 switch/if-else。
     */
    public static final TagKey<Block>[][] ALL_TAGS = buildTable();

    @SuppressWarnings("unchecked")
    private static TagKey<Block>[][] buildTable() {
        TagKey<Block>[][] table = new TagKey[6][4];
        // 温带 (ordinal 0)
        table[0][0] = TEMPERATE_SPRING; table[0][1] = TEMPERATE_SUMMER;
        table[0][2] = TEMPERATE_AUTUMN; table[0][3] = TEMPERATE_WINTER;
        // 寒带 (ordinal 1)
        table[1][0] = COLD_SPRING; table[1][1] = COLD_SUMMER;
        table[1][2] = COLD_AUTUMN; table[1][3] = COLD_WINTER;
        // 干旱带 (ordinal 2)
        table[2][0] = ARID_SPRING; table[2][1] = ARID_SUMMER;
        table[2][2] = ARID_AUTUMN; table[2][3] = ARID_WINTER;
        // 热带 (ordinal 3)
        table[3][0] = TROPICAL_SPRING; table[3][1] = TROPICAL_SUMMER;
        table[3][2] = TROPICAL_AUTUMN; table[3][3] = TROPICAL_WINTER;
        // 高山带 (ordinal 4)
        table[4][0] = ALPINE_SPRING; table[4][1] = ALPINE_SUMMER;
        table[4][2] = ALPINE_AUTUMN; table[4][3] = ALPINE_WINTER;
        // 海洋带 (ordinal 5)
        table[5][0] = OCEANIC_SPRING; table[5][1] = OCEANIC_SUMMER;
        table[5][2] = OCEANIC_AUTUMN; table[5][3] = OCEANIC_WINTER;
        return table;
    }

    private static TagKey<Block> create(String path) {
        return TagKey.create(
                Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(TheTurningoftheSeasons.MODID, path)
        );
    }
}
