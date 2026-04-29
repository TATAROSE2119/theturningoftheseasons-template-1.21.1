package com.tatarose.turningseasons.common.season;

import net.minecraft.util.StringRepresentable;

/**
 * 季节枚举。
 *
 * <p>顺序固定为 春 → 夏 → 秋 → 冬，与 ordinal() 返回值一一对应，
 * 这点在 NBT 序列化、网络传输（用 VAR_INT）以及配置文件中都被依赖，
 * 因此不能调整声明顺序。</p>
 *
 * <p>实现 {@link StringRepresentable} 主要是为了未来可以无缝接入 Minecraft 的
 * Codec 系统（例如数据包注册）。</p>
 */
public enum Season implements StringRepresentable {
    SPRING("spring"),
    SUMMER("summer"),
    AUTUMN("autumn"),
    WINTER("winter");

    private final String name;

    Season(String name) {
        this.name = name;
    }

    /** 推进到下一个季节，冬季之后回到春季。 */
    public Season next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /**
     * 通过序号取季节，使用 floorMod 保证负数也能正确循环，
     * 这在反序列化或玩家通过命令传入异常值时是一道防线。
     */
    public static Season byOrdinal(int ord) {
        Season[] vals = values();
        return vals[Math.floorMod(ord, vals.length)];
    }

    /** 通过名字（不区分大小写）查找季节，找不到返回 null。 */
    public static Season byName(String input) {
        if (input == null) return null;
        String lower = input.toLowerCase();
        for (Season s : values()) {
            if (s.name.equals(lower)) return s;
        }
        return null;
    }

    /** 用于翻译键的小写名字，例如 "spring"。 */
    public String getName() {
        return name;
    }

    /** 翻译键，统一前缀方便客户端 lang 文件管理。 */
    public String getTranslationKey() {
        return "season.theturningoftheseasons." + name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
