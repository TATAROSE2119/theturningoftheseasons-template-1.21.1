package com.tatarose.turningseasons;

import com.mojang.logging.LogUtils;
import com.tatarose.turningseasons.common.registry.ModSounds;
import com.tatarose.turningseasons.config.ClientConfig;
import com.tatarose.turningseasons.config.CommonConfig;
import com.tatarose.turningseasons.config.ServerConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/**
 * 主 mod 入口（双端通用）。
 *
 * <p>阶段 0 的职责仅限于：</p>
 * <ol>
 *     <li>声明 mod ID 与 logger；</li>
 *     <li>注册 Common / Server / Client 三套配置；</li>
 * </ol>
 *
 * <p>注意：所有真正的业务逻辑都在子包里通过 {@code @EventBusSubscriber} 自动注册，
 * 这个类不直接订阅事件，保持轻量。</p>
 *
 * <p>客户端入口是 {@link com.tatarose.turningseasons.client.TheTurningoftheSeasonsClient}，
 * 通过 {@code dist = Dist.CLIENT} 限定仅在物理客户端加载。</p>
 */
@Mod(TheTurningoftheSeasons.MODID)
public class TheTurningoftheSeasons {

    /** Mod ID，必须与 neoforge.mods.toml / 资源包目录名严格一致。 */
    public static final String MODID = "theturningoftheseasons";

    /** 全局 logger，子模块统一通过它输出日志，方便过滤。 */
    public static final Logger LOGGER = LogUtils.getLogger();

    public TheTurningoftheSeasons(IEventBus modEventBus, ModContainer modContainer) {
        // 注册三套配置文件：
        //  - COMMON 同时存在于客户端 / 服务端的 config 目录
        //  - SERVER 在世界级别（保存在存档内），单机时由集成服务器加载
        //  - CLIENT 仅客户端
        modContainer.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);

        // CLIENT 配置只在物理客户端注册，避免专用服务器加载时报警
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        }

        // 注册 SoundEvent。即使在专用服务器上也要注册（registry 是 sided agnostic），
        // 实际播放只发生在客户端，但服务端持有 ID 才能广播给客户端。
        ModSounds.register(modEventBus);

        LOGGER.info("[{}] Loaded on {} side. Phase 0 bootstrap complete.",
                MODID, FMLEnvironment.dist);
    }
}
