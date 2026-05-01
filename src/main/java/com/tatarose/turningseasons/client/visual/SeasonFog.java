package com.tatarose.turningseasons.client.visual;

import com.tatarose.turningseasons.TheTurningoftheSeasons;
import com.tatarose.turningseasons.client.ClientSeasonState;
import com.tatarose.turningseasons.common.season.ClimateZone;
import com.tatarose.turningseasons.common.season.ClimateZoneResolver;
import com.tatarose.turningseasons.common.season.Season;
import com.tatarose.turningseasons.common.season.SeasonState;
import com.tatarose.turningseasons.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * 季节迷雾。
 *
 * <p>在 vanilla 雾的基础上做后处理（拉近 fog 距离 + 改色），
 * 触发条件按"什么时候应该有雾"的常识来：</p>
 *
 * <ul>
 *     <li><b>冬季</b>：任何时段都有冷蓝色雾，整体压抑感。</li>
 *     <li><b>秋季清晨</b>：游戏时刻 0-2000（约 sunrise 到 7am）暖米色晨雾。</li>
 *     <li><b>海洋性气候带（OCEANIC，非夏季）</b>：常驻浅灰白雾。</li>
 * </ul>
 *
 * <p>三个条件按优先级判断，互斥：冬季 > 秋晨 > 海洋。
 * 不在条件内时直接 return，让 vanilla 完全控制 → 不会污染其它季节 / 维度。</p>
 *
 * <p>实现方式：监听 NeoForge 两个 viewport 事件：</p>
 * <ul>
 *     <li>{@link ViewportEvent.RenderFog}：拉近 near / far plane，让雾"更近"</li>
 *     <li>{@link ViewportEvent.ComputeFogColor}：把当前 RGB lerp 到季节色</li>
 * </ul>
 *
 * <p>这两个事件是 GAME 总线，框架根据事件类型自动路由，不需要显式 bus=。</p>
 */
@EventBusSubscriber(modid = TheTurningoftheSeasons.MODID, value = Dist.CLIENT)
public final class SeasonFog {
    private SeasonFog() {}

    // ===== 触发参数 =====

    /** 秋晨雾覆盖的游戏时刻范围（vanilla day = 24000 tick）。0 = sunrise。 */
    private static final long MORNING_START = 0L;
    private static final long MORNING_END   = 2000L;

    // ===== 雾配方 =====

    /**
     * 雾上下文：每个触发条件给一组参数。
     *
     * @param farMul     vanilla 远端 fog 距离的乘子（< 1 → 雾更近）
     * @param nearMul    近端乘子（一般 1.0 即可）
     * @param tintBlend  与季节色混合权重 [0,1]
     * @param tintR/G/B  季节雾色（线性 0-1）
     */
    private record Recipe(float farMul, float nearMul,
                          float tintBlend,
                          float tintR, float tintG, float tintB) {}

    /** 冬季：冷蓝雾 + 中等近化。 */
    private static final Recipe WINTER = new Recipe(
            0.80f, 1.0f,
            0.45f,
            0.70f, 0.78f, 0.92f);

    /** 秋晨：暖米色雾 + 较强近化（"晨光透不过来"的感觉）。 */
    private static final Recipe AUTUMN_MORNING = new Recipe(
            0.65f, 1.0f,
            0.40f,
            0.85f, 0.75f, 0.55f);

    /** 海洋带（非夏季）：浅灰白雾 + 轻微近化。 */
    private static final Recipe OCEANIC = new Recipe(
            0.85f, 1.0f,
            0.30f,
            0.85f, 0.88f, 0.92f);

    // ===== 事件 =====

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!ClientConfig.SHOW_SEASON_FOG.get()) return;
        Recipe r = currentRecipe();
        if (r == null) return;
        event.setNearPlaneDistance(event.getNearPlaneDistance() * r.nearMul);
        event.setFarPlaneDistance (event.getFarPlaneDistance()  * r.farMul);
        // 不要 setCanceled —— 取消会让 vanilla 跳过它的"应用"逻辑；
        // 我们只想覆盖数值，让 vanilla 后续流程正常走。
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!ClientConfig.SHOW_SEASON_FOG.get()) return;
        Recipe r = currentRecipe();
        if (r == null) return;
        event.setRed  (lerp(event.getRed(),   r.tintR, r.tintBlend));
        event.setGreen(lerp(event.getGreen(), r.tintG, r.tintBlend));
        event.setBlue (lerp(event.getBlue(),  r.tintB, r.tintBlend));
    }

    // ===== 决策 =====

    /**
     * 当前应该用哪种雾。按优先级判断，返回 null 表示不干预。
     */
    private static Recipe currentRecipe() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) return null;

        SeasonState state = ClientSeasonState.get();
        if (state == null) return null;

        Season season = state.season();

        // 1) 冬季：所有冬季时段都触发（暂不区分气候带，下雪比飘雪更明显）
        if (season == Season.WINTER) return WINTER;

        // 2) 秋晨：用 dayTime mod 24000 判断时刻
        if (season == Season.AUTUMN) {
            long timeOfDay = level.getDayTime() % 24000L;
            // 防御 dayTime 负数（rare，但 / commands 可能给）
            if (timeOfDay < 0) timeOfDay += 24000L;
            if (timeOfDay >= MORNING_START && timeOfDay < MORNING_END) {
                return AUTUMN_MORNING;
            }
        }

        // 3) 海洋带（任何非夏季）—— 玩家所在 biome 解析 1 次/帧
        if (season != Season.SUMMER) {
            ClimateZone zone = ClimateZoneResolver.resolve(level, player.blockPosition());
            if (zone == ClimateZone.OCEANIC) return OCEANIC;
        }

        return null;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
