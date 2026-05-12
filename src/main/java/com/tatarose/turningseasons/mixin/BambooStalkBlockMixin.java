package com.tatarose.turningseasons.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 为竹子的 randomTick() 注入 {@link CropGrowEvent.Pre}，
 * 使其受季节作物限制管控。
 */
@Mixin(BambooStalkBlock.class)
public abstract class BambooStalkBlockMixin {

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void theturningoftheseasons$onRandomTick(BlockState state, ServerLevel level,
            BlockPos pos, RandomSource random, CallbackInfo ci) {
        CropGrowEvent.Pre event = new CropGrowEvent.Pre(level, pos, state);
        NeoForge.EVENT_BUS.post(event);
        if (event.getResult() == CropGrowEvent.Pre.Result.DO_NOT_GROW) {
            ci.cancel();
        }
    }
}
