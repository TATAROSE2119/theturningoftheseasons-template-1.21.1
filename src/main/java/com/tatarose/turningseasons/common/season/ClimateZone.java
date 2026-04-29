package com.tatarose.turningseasons.common.season;

import net.minecraft.util.StringRepresentable;

/**
 * 气候带分类（方案 §四 模块 2）。
 *
 * <p>这是横在"群系"之上的一层抽象：每个 vanilla / modded 群系
 * 都会被映射到下面 6 个气候带之一，季节系统据此决定：</p>
 * <ul>
 *     <li>温度基础值 / 修正</li>
 *     <li>降雨 / 降雪概率</li>
 *     <li>雾、风暴出现概率</li>
 *     <li>植物颜色变化幅度</li>
 *     <li>动物季节行为强度</li>
 * </ul>
 *
 * <p>分配方式：通过 biome tag（{@link com.tatarose.turningseasons.common.tags.ModBiomeTags}）
 * 数据驱动，整合包作者可以覆盖或追加。</p>
 */
public enum ClimateZone implements StringRepresentable {
    /** 温带：四季明显，本 mod 的"基准"气候。 */
    TEMPERATE("temperate"),
    /** 寒带：冬季极长 / 表现强，夏季短暂。 */
    COLD("cold"),
    /** 干旱：少雨，昼夜温差大。 */
    ARID("arid"),
    /** 热带：无明显冬季，雨季 / 旱季更明显。 */
    TROPICAL("tropical"),
    /** 高山：低温、积雪、风暴；优先级最高，覆盖其它带的判定。 */
    ALPINE("alpine"),
    /** 海洋性：雾、雨、风暴较多，温度波动小。 */
    OCEANIC("oceanic");

    private final String name;

    ClimateZone(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /** 翻译键，统一前缀方便 lang 文件管理。 */
    public String getTranslationKey() {
        return "climate.theturningoftheseasons." + name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
