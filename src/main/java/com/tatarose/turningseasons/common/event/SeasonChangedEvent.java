package com.tatarose.turningseasons.common.event;

import com.tatarose.turningseasons.common.season.Season;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

/**
 * 季节变更事件 —— 后续阶段所有模块（视觉、生物行为、作物、音乐）的统一接入点。
 *
 * <p>仅在服务端触发，发布到 {@code NeoForge.EVENT_BUS}（game bus）。
 * 触发时机：</p>
 * <ul>
 *     <li>自然推进：跨日导致 {@code dayOfSeason} 越界，季节切换；</li>
 *     <li>命令强制：{@code /season set <s>}（仅当目标 ≠ 当前才触发）；</li>
 *     <li>{@code /season advance N} 中跨过季节边界。</li>
 * </ul>
 *
 * <p>该事件不可取消（不影响游戏规则状态变更，仅作通知）。订阅者应当
 * 只读取事件数据，不修改 SeasonSavedData。</p>
 *
 * <p>设计上"按维度"触发：每个 ServerLevel 各自有独立季节，事件携带
 * {@link #getLevel()} 让订阅者能知道哪个维度变了。</p>
 */
public class SeasonChangedEvent extends Event {

    private final ServerLevel level;
    private final Season previousSeason;
    private final Season newSeason;
    private final int year;

    public SeasonChangedEvent(ServerLevel level, Season previousSeason, Season newSeason, int year) {
        this.level = level;
        this.previousSeason = previousSeason;
        this.newSeason = newSeason;
        this.year = year;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public Season getPreviousSeason() {
        return previousSeason;
    }

    public Season getNewSeason() {
        return newSeason;
    }

    /** 跨年（冬→春）时这里已经是新年的年份。 */
    public int getYear() {
        return year;
    }

    /** 是否是跨年（冬 → 春）。 */
    public boolean isYearTransition() {
        return previousSeason == Season.WINTER && newSeason == Season.SPRING;
    }
}
