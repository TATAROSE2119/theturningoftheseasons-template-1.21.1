package com.tatarose.turningseasons.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.tatarose.turningseasons.common.season.ClimateZone;
import com.tatarose.turningseasons.common.season.ClimateZoneResolver;
import com.tatarose.turningseasons.common.season.Season;
import com.tatarose.turningseasons.config.ServerConfig;
import com.tatarose.turningseasons.server.season.SeasonManager;
import com.tatarose.turningseasons.server.season.SeasonSavedData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;

import java.util.Arrays;

/**
 * /season 命令族。
 *
 * <ul>
 *     <li>{@code /season query}            —— 显示当前维度季节状态</li>
 *     <li>{@code /season set <season>}     —— 切换到指定季节（保留年份）</li>
 *     <li>{@code /season advance}          —— 推进到下一个季节</li>
 *     <li>{@code /season advance <days>}   —— 推进若干天</li>
 *     <li>{@code /season climate}          —— 显示玩家当前所在群系 + 气候带（调试用）</li>
 * </ul>
 *
 * <p>权限等级 2（默认 OP），避免普通玩家随意修改。</p>
 *
 * <p>所有"修改"类子命令都通过 {@link SeasonManager} 完成，保证事件 + 同步的一致性。</p>
 */
public final class SeasonCommand {
    private SeasonCommand() {}

    private static final SimpleCommandExceptionType ERR_DIM_DISABLED =
            new SimpleCommandExceptionType(Component.translatable("commands.theturningoftheseasons.season.dim_disabled"));

    private static final SimpleCommandExceptionType ERR_UNKNOWN_SEASON =
            new SimpleCommandExceptionType(Component.translatable("commands.theturningoftheseasons.season.unknown"));

    private static final SimpleCommandExceptionType ERR_NO_ENTITY =
            new SimpleCommandExceptionType(Component.translatable("commands.theturningoftheseasons.season.no_entity"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("season")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("query").executes(SeasonCommand::query))
                        .then(Commands.literal("set")
                                .then(Commands.argument("season", ResourceLocationArgument.id())
                                        // 用 ResourceLocation 是为了未来兼容 modid:season 形式；
                                        // 现阶段只匹配 path（spring/summer/autumn/winter）
                                        .suggests((ctx, builder) ->
                                                SharedSuggestionProvider.suggest(
                                                        Arrays.stream(Season.values()).map(Season::getName), builder))
                                        .executes(SeasonCommand::set)))
                        .then(Commands.literal("advance")
                                .executes(ctx -> advance(ctx, 0)) // 不传天数 → 推进到下一个季节
                                .then(Commands.argument("days", IntegerArgumentType.integer(1, 36500))
                                        .executes(ctx -> advance(ctx, IntegerArgumentType.getInteger(ctx, "days")))))
                        .then(Commands.literal("climate").executes(SeasonCommand::climate))
        );
    }

    // -----------------------------------------------------------------------
    // query
    // -----------------------------------------------------------------------
    private static int query(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        if (!SeasonManager.isDimensionEnabled(level)) throw ERR_DIM_DISABLED.create();

        SeasonSavedData data = SeasonSavedData.get(level);
        int daysPerSeason = ServerConfig.resolveDaysPerSeason();
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.theturningoftheseasons.season.query",
                Component.translatable(data.getSeason().getTranslationKey()),
                data.getYear(),
                data.getDayOfSeason() + 1, // 显示给玩家时从 1 开始更直观
                daysPerSeason
        ), false);
        return 1;
    }

    // -----------------------------------------------------------------------
    // set
    // -----------------------------------------------------------------------
    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        if (!SeasonManager.isDimensionEnabled(level)) throw ERR_DIM_DISABLED.create();

        var rl = ResourceLocationArgument.getId(ctx, "season");
        Season target = Season.byName(rl.getPath());
        if (target == null) throw ERR_UNKNOWN_SEASON.create();

        // 走 SeasonManager 统一入口：状态修改 + 同步 + 事件 + 客户端通知一并完成
        SeasonManager.forceSetSeason(level, target);

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.theturningoftheseasons.season.set",
                Component.translatable(target.getTranslationKey())
        ), true);
        return 1;
    }

    // -----------------------------------------------------------------------
    // advance
    // -----------------------------------------------------------------------
    /**
     * 推进若干天。days = 0 时表示"推进到下一个季节"。
     */
    private static int advance(CommandContext<CommandSourceStack> ctx, int days) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        if (!SeasonManager.isDimensionEnabled(level)) throw ERR_DIM_DISABLED.create();

        int daysPerSeason = ServerConfig.resolveDaysPerSeason();
        SeasonSavedData data = SeasonSavedData.get(level);

        int actualDays = days > 0 ? days : (daysPerSeason - data.getDayOfSeason());
        SeasonManager.advanceDays(level, actualDays);

        // 重新读取，因为 advanceDays 内部已经修改了 data
        final int advanced = actualDays;
        final Season nowSeason = data.getSeason();
        final int nowYear = data.getYear();
        final int nowDay = data.getDayOfSeason() + 1;
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.theturningoftheseasons.season.advance",
                advanced,
                Component.translatable(nowSeason.getTranslationKey()),
                nowYear,
                nowDay,
                daysPerSeason
        ), true);
        return 1;
    }

    // -----------------------------------------------------------------------
    // climate (debug)
    // -----------------------------------------------------------------------
    /**
     * 显示玩家脚下方块的群系 ID 和解析出的气候带，便于调试 tag 配置。
     */
    private static int climate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Entity entity = ctx.getSource().getEntity();
        if (entity == null) throw ERR_NO_ENTITY.create();

        ServerLevel level = ctx.getSource().getLevel();
        BlockPos pos = entity.blockPosition();
        Holder<Biome> biome = level.getBiome(pos);
        ClimateZone zone = ClimateZoneResolver.resolve(biome);

        // 取 biome ID。模组群系 + Vanilla 群系都有 ResourceKey；如果是程序化 biome 才会拿不到 key。
        String biomeId = biome.unwrapKey()
                .map(k -> k.location().toString())
                .orElse("<unknown>");

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.theturningoftheseasons.season.climate",
                biomeId,
                Component.translatable(zone.getTranslationKey())
        ), false);
        return 1;
    }
}
