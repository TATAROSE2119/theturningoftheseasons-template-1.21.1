package com.tatarose.turningseasons.server.season;

import com.tatarose.turningseasons.common.season.Season;
import com.tatarose.turningseasons.common.season.SeasonState;
import com.tatarose.turningseasons.config.ServerConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/**
 * 季节系统的持久化数据 —— 每个维度（ServerLevel）一份。
 *
 * <p>使用 Vanilla 的 {@link SavedData} 机制，存盘到
 * {@code <world>/<dimension>/data/turningseasons_season.dat}，
 * 关闭世界后下次进入时自动加载。</p>
 *
 * <p>之所以选择"按维度存"而不是"按世界全局存"：</p>
 * <ul>
 *     <li>方案中允许不同维度独立启用 / 禁用季节；</li>
 *     <li>未来可能允许不同维度有不同节奏（比如末地永远不变化）。</li>
 * </ul>
 *
 * <p>如果将来需要"全局季节"，再叠一层 ServerLevel#getServer() 级别的存储即可，
 * 当前结构不会成为障碍。</p>
 */
public class SeasonSavedData extends SavedData {

    /** 存储文件名（不含扩展名），不能含斜杠。 */
    public static final String NAME = "turningseasons_season";

    private Season season;
    private int year;
    /** 当前季节内的累计天数，0 = 季节第 1 天的开始。 */
    private int dayOfSeason;
    /** 上一次推进时记录的"游戏内总天数"，用于检测跨日。 */
    private long lastTickedDay;

    /** 默认构造：用于 SavedData 第一次创建时初始化。 */
    public SeasonSavedData() {
        this.season = ServerConfig.INITIAL_SEASON.get();
        this.year = 1;
        this.dayOfSeason = 0;
        this.lastTickedDay = -1L; // -1 表示尚未与世界天数对齐，第一次 tick 时会校准
    }

    /** 从 NBT 反序列化。签名必须匹配 SavedData.Factory 中传入的方法引用。 */
    public static SeasonSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        SeasonSavedData data = new SeasonSavedData();
        data.season = Season.byOrdinal(tag.getInt("season"));
        data.year = Math.max(1, tag.getInt("year"));
        data.dayOfSeason = Math.max(0, tag.getInt("dayOfSeason"));
        data.lastTickedDay = tag.contains("lastTickedDay") ? tag.getLong("lastTickedDay") : -1L;
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("season", season.ordinal());
        tag.putInt("year", year);
        tag.putInt("dayOfSeason", dayOfSeason);
        tag.putLong("lastTickedDay", lastTickedDay);
        return tag;
    }

    /**
     * 取（或创建）某个 ServerLevel 的季节数据。
     * 必须在服务端调用，使用 {@link ServerLevel#getDataStorage()} 进入对应维度的 data 目录。
     */
    public static SeasonSavedData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(
                new SavedData.Factory<>(SeasonSavedData::new, SeasonSavedData::load),
                NAME
        );
    }

    // ===== Getter / Setter =====
    // 任何修改都会调用 setDirty()，以便 Vanilla 的存盘调度感知数据变化。

    public Season getSeason() { return season; }

    public int getYear() { return year; }

    public int getDayOfSeason() { return dayOfSeason; }

    public long getLastTickedDay() { return lastTickedDay; }

    public void setSeason(Season season) {
        this.season = season;
        setDirty();
    }

    public void setYear(int year) {
        this.year = Math.max(1, year);
        setDirty();
    }

    public void setDayOfSeason(int dayOfSeason) {
        this.dayOfSeason = Math.max(0, dayOfSeason);
        setDirty();
    }

    public void setLastTickedDay(long day) {
        this.lastTickedDay = day;
        setDirty();
    }

    /**
     * 推进一天。如果超过当前模式的"每季天数"，自动进入下一个季节，
     * 冬季结束后年份 +1 并回到春季。
     */
    public void advanceOneDay(int daysPerSeason) {
        this.dayOfSeason++;
        if (this.dayOfSeason >= daysPerSeason) {
            this.dayOfSeason = 0;
            Season previous = this.season;
            this.season = previous.next();
            // 冬 → 春 时跨年
            if (previous == Season.WINTER) {
                this.year++;
            }
        }
        setDirty();
    }

    /**
     * 把当前数据快照成 SeasonState，供网络同步 / HUD 显示使用。
     * 参数由调用方从 ServerConfig 传入，避免数据类反向依赖配置。
     */
    public SeasonState toState(int daysPerSeason, double growthMultiplier) {
        return new SeasonState(season, year, dayOfSeason, daysPerSeason, growthMultiplier);
    }
}
