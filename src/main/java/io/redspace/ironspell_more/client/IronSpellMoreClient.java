package io.redspace.ironspell_more.client;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.client.particle.ShockwaveParticleCustom;
import io.redspace.ironspell_more.client.particle.RedPlumParticle;
import io.redspace.ironspell_more.client.particle.WhiteEmberParticle;
import io.redspace.ironspell_more.client.particle.WhiteFireParticle;
import io.redspace.ironspell_more.client.particle.ZapParticleCustom;
import io.redspace.ironspell_more.client.renderer.ResonantKnellDomeRenderer;
import io.redspace.ironspell_more.entity.spells.gold_chain.ArcaneShackleRenderer;
import io.redspace.ironspell_more.entity.spells.gold_chain.GoldChainRenderer;
import io.redspace.ironspell_more.entity.spells.venomous_blossomfall.AzureVenomNeedleModel;
import io.redspace.ironspell_more.entity.spells.venomous_blossomfall.AzureVenomNeedleRenderer;
import io.redspace.ironspell_more.registry.EntityRegistry;
import io.redspace.ironspell_more.registry.ParticleRegistry;
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
        event.registerSpriteSet(ParticleRegistry.RED_PLUM.get(), RedPlumParticle.Provider::new);
        event.registerSpriteSet(ParticleRegistry.GILDED_HARE.get(), io.redspace.ironspell_more.client.particle.GildedHareParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.ARCANE_SHACKLE.get(), ArcaneShackleRenderer::new);
        event.registerEntityRenderer(EntityRegistry.GOLD_CHAIN.get(), GoldChainRenderer::new);
        event.registerEntityRenderer(EntityRegistry.WING_OF_TEMPEST_AOE.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(EntityRegistry.CRIMSON_SPEAR.get(), io.redspace.ironspell_more.entity.spells.crimson_rain_bathes_moon.CrimsonSpearRenderer::new);
        event.registerEntityRenderer(EntityRegistry.GALE_DRIVE_VORTEX.get(), io.redspace.ironspell_more.entity.spells.gale_drive.GaleDriveVortexRenderer::new);
        event.registerEntityRenderer(EntityRegistry.AZURE_VENOM_NEEDLE.get(), AzureVenomNeedleRenderer::new);
        event.registerEntityRenderer(EntityRegistry.WIND_ARROW.get(), io.redspace.ironspell_more.entity.spells.gale_piercer.WindArrowRenderer::new);
        event.registerEntityRenderer(EntityRegistry.GALE_ARROW.get(), io.redspace.ironspell_more.entity.spells.gale_piercer.GaleArrowRenderer::new);
        event.registerEntityRenderer(EntityRegistry.GLACIAL_SPIKE.get(), io.redspace.ironspell_more.entity.spells.glacial_firmament.GlacialSpikeRenderer::new);
        event.registerEntityRenderer(EntityRegistry.GLACIAL_TOMB.get(), io.redspace.ironspell_more.entity.spells.glacial_firmament.GlacialTombRenderer::new);
        event.registerEntityRenderer(EntityRegistry.RESONANT_KNELL_DOME.get(), ResonantKnellDomeRenderer::new);
        event.registerEntityRenderer(EntityRegistry.RAPTUROUS_BLOOM.get(), io.redspace.ironspell_more.entity.spells.rapturous_bloom.RapturousBloomRenderer::new);
        event.registerEntityRenderer(EntityRegistry.CRIMSON_ROOT.get(), io.redspace.ironspell_more.entity.spells.crimson_thornbind.CrimsonRootRenderer::new);
        event.registerEntityRenderer(EntityRegistry.TOXIC_SALVATION_AOE.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(AzureVenomNeedleModel.LAYER_LOCATION, AzureVenomNeedleModel::createBodyLayer);
        event.registerLayerDefinition(io.redspace.ironspell_more.entity.spells.glacial_firmament.GlacialSpikeRenderer.GlacialSpikeModel.LAYER_LOCATION, io.redspace.ironspell_more.entity.spells.glacial_firmament.GlacialSpikeRenderer.GlacialSpikeModel::createBodyLayer);
        event.registerLayerDefinition(io.redspace.ironspell_more.entity.spells.glacial_firmament.GlacialTombRenderer.GlacialTombModel.LAYER_LOCATION, io.redspace.ironspell_more.entity.spells.glacial_firmament.GlacialTombRenderer.GlacialTombModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            var renderer = event.getSkin(skin);
            if (renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer playerRenderer) {
                playerRenderer.addLayer(new io.redspace.ironspell_more.client.renderer.GalePiercerChargeLayer(playerRenderer));
                playerRenderer.addLayer(new io.redspace.ironspell_more.client.renderer.GildedHarePlayerLayer(playerRenderer));
            }
        }
    }
}
