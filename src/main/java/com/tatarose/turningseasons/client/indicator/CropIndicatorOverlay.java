package com.tatarose.turningseasons.client.indicator;

import com.tatarose.turningseasons.client.ClientSeasonState;
import com.tatarose.turningseasons.common.season.ClimateZone;
import com.tatarose.turningseasons.common.season.ClimateZoneResolver;
import com.tatarose.turningseasons.common.season.SeasonState;
import com.tatarose.turningseasons.config.ClientConfig;
import com.tatarose.turningseasons.server.crop.CropSeasonResolver;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 作物生长指示器 —— 屏幕空间 HUD 层。
 *
 * <p>与 {@code SeasonHudOverlay} 完全相同的渲染管道（{@link LayeredDraw.Layer}），
 * 100% 可靠，不依赖世界空间渲染。</p>
 *
 * <p>准星对准作物时，在屏幕顶部居中显示作物信息。</p>
 */
public final class CropIndicatorOverlay implements LayeredDraw.Layer {

    public static final CropIndicatorOverlay INSTANCE = new CropIndicatorOverlay();

    public static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "key.theturningoftheseasons.toggle_crop_indicator",
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
            org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.theturningoftheseasons"
    );

    static boolean visible = true;

    private static final int BG_COLOR = 0xAA000000;
    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 10;

    private CropIndicatorOverlay() {}

    public static void onClientTick() {
        while (TOGGLE_KEY.consumeClick()) {
            visible = !visible;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        Component.translatable(visible
                                ? "indicator.theturningoftheseasons.shown"
                                : "indicator.theturningoftheseasons.hidden"),
                        true);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!visible) return;
        if (!ClientConfig.SHOW_CROP_INDICATOR.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        if (mc.level == null || mc.player == null) return;
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult hit = (BlockHitResult) mc.hitResult;
        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        Block block = state.getBlock();

        int age;
        int maxAge;
        if (block instanceof CropBlock cropBlock) {
            age = cropBlock.getAge(state);
            maxAge = cropBlock.getMaxAge();
        } else if (block instanceof StemBlock) {
            age = state.getValue(StemBlock.AGE);
            maxAge = StemBlock.MAX_AGE;
        } else if (block instanceof SugarCaneBlock) {
            age = state.getValue(BlockStateProperties.AGE_15);
            maxAge = 15;
        } else if (block instanceof BambooStalkBlock) {
            age = 0;
            maxAge = 0; // 竹子不显示百分比
        } else {
            return;
        }

        SeasonState seasonState = ClientSeasonState.get();
        ClimateZone zone = ClimateZoneResolver.resolve(mc.level, pos);
        boolean inSeason = seasonState != null
                && CropSeasonResolver.isInSeason(block, seasonState.season(), zone);

        String cropName = block.getName().getString();
        // 竹子不显示百分比
        String percentText = maxAge > 0
                ? (" \u00b7 " + (age * 100 / maxAge) + "%") : "";

        // 生长倍率：当季 = 配置值，非当季 = 0
        double speed = inSeason && seasonState != null
                ? seasonState.growthMultiplier() : 0.0;

        String seasonName = seasonState != null
                ? Component.translatable(seasonState.season().getTranslationKey()).getString()
                : "?";
        String zoneName = Component.translatable(zone.getTranslationKey()).getString();
        String status = inSeason ? "\u2713" : "\u2717";

        Font font = mc.font;
        String speedStr = speed == 1.0 ? "x1" : String.format("x%.1f", speed);
        Component line1 = Component.literal(cropName + percentText + " \u00b7 " + speedStr);
        Component line2 = Component.literal(seasonName + " \u00b7 " + zoneName);
        String line2Suffix = " \u00b7 " + status;

        int line2Width = font.width(line2) + font.width(line2Suffix);
        int boxW = Math.max(font.width(line1), line2Width) + PADDING * 2;
        int boxH = LINE_HEIGHT * 2 + PADDING * 2 + 2;

        // 屏幕顶部居中
        int screenW = graphics.guiWidth();
        int x = (screenW - boxW) / 2;
        int y = 40;

        // 半透明背景
        graphics.fill(x, y, x + boxW, y + boxH, BG_COLOR);

        int textX = x + PADDING;
        int textY = y + PADDING;

        // 第一行：作物名 · 85% · x1.5
        graphics.drawString(font, line1, textX, textY, 0xFFFFAA00, true);

        // 第二行：季节 · 气候带 · ✓/✗
        textY += LINE_HEIGHT;
        graphics.drawString(font, line2, textX, textY, 0xFFFFFFFF, true);
        graphics.drawString(font, line2Suffix, textX + font.width(line2), textY,
                inSeason ? 0xFF55FF55 : 0xFFFF5555, true);
    }
}
