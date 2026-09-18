package io.redspace.ironspell_more.client;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.client.particle.ShockwaveParticleCustom;
import io.redspace.ironspell_more.client.particle.WhiteEmberParticle;
import io.redspace.ironspell_more.client.particle.WhiteFireParticle;
import io.redspace.ironspell_more.client.particle.ZapParticleCustom;
import io.redspace.ironspell_more.registry.ParticleRegistry;
import io.redspace.ironspell_more.entity.spells.gold_chain.ArcaneShackleRenderer;
import io.redspace.ironspell_more.entity.spells.gold_chain.GoldChainRenderer;
import io.redspace.ironspell_more.registry.EntityRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD, modid = IronSpellMore.MODID)
public class IronSpellMoreClient {

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleRegistry.ZAP_CUSTOM.get(), ZapParticleCustom.Provider::new);
        event.registerSpriteSet(ParticleRegistry.SHOCKWAVE_CUSTOM.get(), ShockwaveParticleCustom.Provider::new);
        event.registerSpriteSet(ParticleRegistry.WHITE_FIRE.get(), WhiteFireParticle.Provider::new);
        event.registerSpriteSet(ParticleRegistry.WHITE_EMBER.get(), WhiteEmberParticle.Provider::new);
        event.registerSpriteSet(ParticleRegistry.WHITE_FIRE_EMITTER.get(), io.redspace.ironspell_more.client.particle.WhiteFireEmitterParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.ARCANE_SHACKLE.get(), ArcaneShackleRenderer::new);
        event.registerEntityRenderer(EntityRegistry.GOLD_CHAIN.get(), GoldChainRenderer::new);
        event.registerEntityRenderer(EntityRegistry.WING_OF_TEMPEST_AOE.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
    }
}
