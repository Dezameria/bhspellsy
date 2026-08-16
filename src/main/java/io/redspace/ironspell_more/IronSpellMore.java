package io.redspace.ironspell_more;

import com.mojang.logging.LogUtils;
import io.redspace.ironspell_more.particle.ShockwaveParticleCustom;
import io.redspace.ironspell_more.particle.ZapParticleCustom;
import io.redspace.ironspell_more.registry.ItemRegistry;
import io.redspace.ironspell_more.registry.ParticleRegistry;
import io.redspace.ironspell_more.registry.SpellRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(IronSpellMore.MODID)
public class IronSpellMore {
    public static final String MODID = "ironspell_more";
    public static final Logger LOGGER = LogUtils.getLogger();

    public IronSpellMore() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        SpellRegistry.register(modEventBus);
        ItemRegistry.register(modEventBus);
        ParticleRegistry.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ParticleRegistry.ZAP_CUSTOM.get(), ZapParticleCustom.Provider::new);
            event.registerSpriteSet(ParticleRegistry.SHOCKWAVE_CUSTOM.get(), ShockwaveParticleCustom.Provider::new);
        }
    }
}
