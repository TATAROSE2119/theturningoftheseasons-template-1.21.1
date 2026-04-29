package com.tatarose.turningseasons.client.hud;

import com.tatarose.turningseasons.client.ClientSeasonState;
import com.tatarose.turningseasons.common.season.SeasonState;
import com.tatarose.turningseasons.config.ClientConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;

/**
 * 季节调试 HUD。
 *
 * <p>实现 {@link LayeredDraw.Layer}，由 {@code RegisterGuiLayersEvent} 注册到
 * {@code VanillaGuiLayers.HOTBAR} 之上。</p>
 *
 * <p>显示三行文字：</p>
 * <ol>
 *     <li>季节 + 年份</li>
 *     <li>当前是该季节第几天 / 总天数</li>
 *     <li>进度条（用文字字符画的方式画一个简单条）</li>
 * </ol>
 *
 * <p>设计为"调试 HUD"——后续阶段会提供更精致的玩家 HUD（带图标、动画），
 * 这里优先满足功能可见性。</p>
 */
public final class SeasonHudOverlay implements LayeredDraw.Layer {

    /** 单例：HUD 是无状态的。 */
    public static final SeasonHudOverlay INSTANCE = new SeasonHudOverlay();

    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int SHADOW_COLOR = 0xFF000000;
    private static final int BAR_BG_COLOR = 0x80000000;
    private static final int BAR_FILL_COLOR = 0xFF7CFC00; // 春绿，后续可按季节变色
    private static final int PADDING = 2;
    private static final int LINE_HEIGHT = 10;

    private SeasonHudOverlay() {}

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        // F3 调试屏 / 截图模式 / 玩家关闭 HUD：不渲染
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        if (mc.getDebugOverlay().showDebugScreen()) return;

        // 配置开关
        if (!ClientConfig.SHOW_DEBUG_HUD.get()) return;

        SeasonState state = ClientSeasonState.get();
        if (state == null) return; // 还未收到服务端同步

        Font font = mc.font;

        // 三行文字
        Component line1 = Component.translatable(
                "hud.theturningoftheseasons.line1",
                Component.translatable(state.season().getTranslationKey())
                        .withStyle(seasonColor(state)),
                state.year()
        );
        Component line2 = Component.translatable(
                "hud.theturningoftheseasons.line2",
                state.dayOfSeason() + 1, // 玩家视角从 1 开始
                state.daysPerSeason()
        );

        int barWidth = 80;
        int boxWidth = Math.max(barWidth, Math.max(font.width(line1), font.width(line2))) + PADDING * 2;
        int boxHeight = LINE_HEIGHT * 2 + 4 /*bar*/ + PADDING * 3;

        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();
        ClientConfig.HudAnchor anchor = ClientConfig.HUD_ANCHOR.get();
        int offX = ClientConfig.HUD_OFFSET_X.get();
        int offY = ClientConfig.HUD_OFFSET_Y.get();




        int x;
        int y;
        switch (anchor) {
            case TOP_LEFT     -> { x = offX;                       y = offY; }
            case TOP_RIGHT    -> { x = screenW - boxWidth - offX;  y = offY; }
            case BOTTOM_LEFT  -> { x = offX;                       y = screenH - boxHeight - offY; }
            case BOTTOM_RIGHT -> { x = screenW - boxWidth - offX;  y = screenH - boxHeight - offY; }
            default           -> { x = offX;                       y = offY; }
        }

        // 半透明背景方便光影下也能看清
        graphics.fill(x, y, x + boxWidth, y + boxHeight, BAR_BG_COLOR);

        int textX = x + PADDING;
        int textY = y + PADDING;
        graphics.drawString(font, line1, textX, textY, TEXT_COLOR, true);
        textY += LINE_HEIGHT;
        graphics.drawString(font, line2, textX, textY, TEXT_COLOR, true);
        textY += LINE_HEIGHT;

        // 进度条
        int barX = textX;
        int barY = textY + 1;
        int filled = Math.round(barWidth * state.progress());
        graphics.fill(barX, barY, barX + barWidth, barY + 4, SHADOW_COLOR);
        graphics.fill(barX, barY, barX + filled, barY + 4, BAR_FILL_COLOR);
    }

    /** 根据季节给文字加点颜色，纯视觉差异，不影响逻辑。 */
    private static ChatFormatting seasonColor(SeasonState state) {
        return switch (state.season()) {
            case SPRING -> ChatFormatting.GREEN;
            case SUMMER -> ChatFormatting.YELLOW;
            case AUTUMN -> ChatFormatting.GOLD;
            case WINTER -> ChatFormatting.AQUA;
        };
    }
}
