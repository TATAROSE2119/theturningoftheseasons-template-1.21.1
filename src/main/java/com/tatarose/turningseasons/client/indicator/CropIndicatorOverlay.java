package com.tatarose.turningseasons.client.indicator;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tatarose.turningseasons.TheTurningoftheSeasons;
import com.tatarose.turningseasons.client.ClientSeasonState;
import com.tatarose.turningseasons.common.season.ClimateZone;
import com.tatarose.turningseasons.common.season.ClimateZoneResolver;
import com.tatarose.turningseasons.common.season.SeasonState;
import com.tatarose.turningseasons.config.ClientConfig;
import com.tatarose.turningseasons.server.crop.CropSeasonResolver;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * 作物生长指示器 —— 世界空间 Billboard 渲染，使用 ByteBufferBuilder (1.21.1 API)。
 */
@EventBusSubscriber(modid = TheTurningoftheSeasons.MODID, value = Dist.CLIENT)
public final class CropIndicatorOverlay {
    private CropIndicatorOverlay() {}

    public static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "key.theturningoftheseasons.toggle_crop_indicator",
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
            org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.theturningoftheseasons"
    );

    private static boolean visible = true;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (!visible) return;
        if (!ClientConfig.SHOW_CROP_INDICATOR.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (mc.options.hideGui) return;

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
        } else {
            return;
        }

        SeasonState seasonState = ClientSeasonState.get();
        ClimateZone zone = ClimateZoneResolver.resolve(mc.level, pos);
        boolean inSeason = seasonState != null
                && CropSeasonResolver.isInSeason(block, seasonState.season(), zone);

        // 文字内容
        String cropName = block.getName().getString();
        int percent = maxAge > 0 ? age * 100 / maxAge : 0;
        String line1 = cropName + " \u00b7 " + percent + "%";

        String seasonName = seasonState != null
                ? Component.translatable(seasonState.season().getTranslationKey()).getString()
                : "?";
        String zoneName = Component.translatable(zone.getTranslationKey()).getString();
        String status = inSeason ? "\u2713" : "\u2717";
        String line2 = seasonName + " \u00b7 " + zoneName + " \u00b7 " + status;

        renderBillboard(event, pos, mc, line1, line2, inSeason);
    }

    private static void renderBillboard(RenderLevelStageEvent event, BlockPos pos,
            Minecraft mc, String line1, String line2, boolean inSeason) {

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        Vec3 targetPos = Vec3.atCenterOf(pos).add(0, 1.0, 0);

        Font font = mc.font;

        // 使用 ByteBufferBuilder (1.21.1 标准 API)
        ByteBufferBuilder byteBuf = new ByteBufferBuilder(1536);
        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(byteBuf);

        poseStack.pushPose();
        poseStack.translate(targetPos.x - camPos.x, targetPos.y - camPos.y, targetPos.z - camPos.z);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-0.025f, -0.025f, 0.025f);

        Matrix4f matrix = poseStack.last().pose();

        // 行1: 作物名 · 百分比
        float w1 = font.width(line1) / 2f;
        font.drawInBatch(line1, -w1, 0, 0xFFFFAA00, true, matrix, bufferSource,
                Font.DisplayMode.SEE_THROUGH, 0, 15728880);

        // 行2: 季节 · 气候带 · ✓/✗
        int line2Color = inSeason ? 0xFF55FF55 : 0xFFFF5555;
        float w2 = font.width(line2) / 2f;
        font.drawInBatch(line2, -w2, 14, line2Color, true, matrix, bufferSource,
                Font.DisplayMode.SEE_THROUGH, 0, 15728880);

        poseStack.popPose();

        // endBatch 内建 build + drawWithShader，无需额外调用
        bufferSource.endBatch();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
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
}
