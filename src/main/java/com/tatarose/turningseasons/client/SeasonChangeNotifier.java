package com.tatarose.turningseasons.client;

import com.tatarose.turningseasons.TheTurningoftheSeasons;
import com.tatarose.turningseasons.common.registry.ModSounds;
import com.tatarose.turningseasons.common.season.Season;
import com.tatarose.turningseasons.config.ClientConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;

/**
 * 客户端季节变更提示。
 *
 * <p>由 {@link com.tatarose.turningseasons.network.ModNetwork} 在收到
 * {@code SeasonChangedPayload} 时调用。</p>
 *
 * <p>表现：</p>
 * <ol>
 *     <li>屏幕中央 Title / Subtitle，使用 vanilla title 动画系统；</li>
 *     <li>停止当前在播的 vanilla 背景音乐，播放对应季节的 BGM。
 *         BGM 在 {@code sounds.json} 中挂了多个变体，引擎自动随机选取。</li>
 * </ol>
 *
 * <p>所有显示均受 {@link ClientConfig#SHOW_SEASON_TITLE}/{@link ClientConfig#PLAY_SEASON_SOUND}
 * 控制，玩家可在客户端设置中关闭。</p>
 */
public final class SeasonChangeNotifier {
    private SeasonChangeNotifier() {}

    // ===== Title 动画时长（tick = 1/20 s）=====
    private static final int TITLE_FADE_IN  = 10;  // 0.5s
    private static final int TITLE_STAY     = 60;  // 3.0s
    private static final int TITLE_FADE_OUT = 20;  // 1.0s

    /**
     * 跟踪本模块当前正在播放的 BGM 实例。
     * 用途：如果玩家在一首季节 BGM 没放完时再次触发季节切换（命令测试常见），
     * 先停掉旧的，避免叠音。
     */
    private static SoundInstance currentBgm = null;

    /**
     * 网络包到达后由 main thread 调用（PayloadRegistrar 已经把回调调度到主线程）。
     */
    public static void show(Season previous, Season next, int year) {
        TheTurningoftheSeasons.LOGGER.debug(
                "[SeasonChangeNotifier] show(): {} -> {} (year={}), showTitle={}, playSound={}",
                previous, next, year,
                ClientConfig.SHOW_SEASON_TITLE.get(),
                ClientConfig.PLAY_SEASON_SOUND.get());

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return; // 包到达时玩家应已登录，作防御处理

        // 1) 屏幕标题
        if (ClientConfig.SHOW_SEASON_TITLE.get()) {
            mc.gui.resetTitleTimes(); // 先清掉上一次的残留计时
            mc.gui.setTimes(TITLE_FADE_IN, TITLE_STAY, TITLE_FADE_OUT);

            Component title = Component.translatable(next.getTranslationKey())
                    .withStyle(seasonColor(next));

            Component subtitle;
            if (previous == Season.WINTER && next == Season.SPRING) {
                subtitle = Component.translatable(
                        "notification.theturningoftheseasons.year_advance", year);
            } else {
                subtitle = Component.translatable(
                        "notification.theturningoftheseasons.season_advance");
            }

            mc.gui.setTitle(title);
            mc.gui.setSubtitle(subtitle);
        }

        // 2) 季节 BGM
        if (ClientConfig.PLAY_SEASON_SOUND.get()) {
            playSeasonMusic(mc, next);
        }
    }

    /**
     * 播放季节 BGM。
     *
     * <p>策略：</p>
     * <ol>
     *     <li>停掉 vanilla MusicManager 当前正在挑选/播放的曲子，
     *         避免我们的 BGM 与它叠在一起。</li>
     *     <li>停掉本模块上次播放的曲子（如果还在播）。</li>
     *     <li>用 {@link SimpleSoundInstance#forMusic(SoundEvent)} 播放
     *         — 这会创建一个 MUSIC 类别的非 3D 声音，受"音乐"音量条控制。</li>
     *     <li>BGM 自然结束后 vanilla MusicManager 会按其正常逻辑（带几分钟随机延迟）
     *         重新挑曲，所以不需要我们去"恢复"。</li>
     * </ol>
     */
    private static void playSeasonMusic(Minecraft mc, Season season) {
        // 停掉 vanilla 的 MusicManager
        mc.getMusicManager().stopPlaying();

        // 停掉我们自己上次播放的（如果存在）
        if (currentBgm != null) {
            mc.getSoundManager().stop(currentBgm);
            currentBgm = null;
        }

        SoundEvent event = ModSounds.musicFor(season);
        SoundInstance instance = SimpleSoundInstance.forMusic(event);
        mc.getSoundManager().play(instance);
        currentBgm = instance;

        TheTurningoftheSeasons.LOGGER.debug(
                "[SeasonChangeNotifier] playSeasonMusic: season={}, soundEvent={}",
                season, event.getLocation());
    }

    private static ChatFormatting seasonColor(Season s) {
        return switch (s) {
            case SPRING -> ChatFormatting.GREEN;
            case SUMMER -> ChatFormatting.YELLOW;
            case AUTUMN -> ChatFormatting.GOLD;
            case WINTER -> ChatFormatting.AQUA;
        };
    }
}
