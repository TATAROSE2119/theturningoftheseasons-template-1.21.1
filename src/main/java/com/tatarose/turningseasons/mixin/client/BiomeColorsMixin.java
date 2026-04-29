package com.tatarose.turningseasons.mixin.client;

import com.tatarose.turningseasons.client.visual.SeasonTint;
import net.minecraft.client.renderer.BiomeColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在 {@link BiomeColors} 的三个静态算色方法返回前，叠一层季节 tint。
 *
 * <p>vanilla 的算法是：在 BlockPos 周围采样若干群系颜色取平均，
 * 我们这里只关心"采样平均后"的最终 RGB，用通道乘法做后处理。
 * 这样：</p>
 * <ul>
 *     <li>不影响 vanilla 的群系平滑过渡（仍然平均原色再 tint）；</li>
 *     <li>不与其它 mod 的 BiomeColors 修改冲突（我们只改返回值）。</li>
 * </ul>
 *
 * <p>触发条件：</p>
 * <ul>
 *     <li>方块渲染时调用 BiomeColors.getAverageGrassColor / FoliageColor —— 草地、树叶、藤蔓、睡莲、糖蘂等所有"自然色"方块；</li>
 *     <li>水、流体渲染调用 getAverageWaterColor。</li>
 * </ul>
 *
 * <p>客户端缓存季节为 null（主菜单 / 刚连入服务器尚未收到同步）时，
 * SeasonTint.apply* 会直接返回原值，等价于 no-op。</p>
 */
@Mixin(BiomeColors.class)
public abstract class BiomeColorsMixin {

    @Inject(
            method = "getAverageGrassColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void theturningoftheseasons$applyGrassTint(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SeasonTint.applyGrass(cir.getReturnValueI()));
    }

    @Inject(
            method = "getAverageFoliageColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void theturningoftheseasons$applyFoliageTint(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SeasonTint.applyFoliage(cir.getReturnValueI()));
    }

    @Inject(
            method = "getAverageWaterColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void theturningoftheseasons$applyWaterTint(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SeasonTint.applyWater(cir.getReturnValueI()));
    }
}
