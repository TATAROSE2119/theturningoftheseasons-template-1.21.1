package com.tatarose.turningseasons.common.registry;

import com.tatarose.turningseasons.TheTurningoftheSeasons;
import com.tatarose.turningseasons.common.season.Season;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 模组自定义 SoundEvent 注册表。
 *
 * <p>为每个季节注册一个对应的 BGM SoundEvent。每个 SoundEvent 在
 * {@code assets/theturningoftheseasons/sounds.json} 中关联了多个 .ogg 变体，
 * Minecraft 在播放时会自动从变体中随机挑一个 — 这是引擎自带的随机机制，
 * 无需自己实现随机选取。</p>
 *
 * <p>使用 {@link SoundEvent#createVariableRangeEvent(ResourceLocation)} 而非
 * {@code createFixedRangeEvent}，这样后续如果想做"播放距离/衰减自定义"可以扩展；
 * 对于 MUSIC 类别声音其实距离不重要（不做 3D 衰减），但保留灵活性。</p>
 */
public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, TheTurningoftheSeasons.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_SEASON_SPRING =
            register("music.season.spring");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_SEASON_SUMMER =
            register("music.season.summer");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_SEASON_AUTUMN =
            register("music.season.autumn");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_SEASON_WINTER =
            register("music.season.winter");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                TheTurningoftheSeasons.MODID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    /**
     * 根据季节查 BGM SoundEvent。客户端在收到季节变更时调用。
     */
    public static SoundEvent musicFor(Season season) {
        return switch (season) {
            case SPRING -> MUSIC_SEASON_SPRING.value();
            case SUMMER -> MUSIC_SEASON_SUMMER.value();
            case AUTUMN -> MUSIC_SEASON_AUTUMN.value();
            case WINTER -> MUSIC_SEASON_WINTER.value();
        };
    }

    /**
     * 在 mod 主类构造器里调用，把 DeferredRegister 挂到 mod event bus。
     */
    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
