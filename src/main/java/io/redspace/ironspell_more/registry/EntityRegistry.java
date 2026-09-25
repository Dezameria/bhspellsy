package io.redspace.ironspell_more.registry;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.entity.spells.gold_chain.ArcaneShackleProjectile;
import io.redspace.ironspell_more.entity.spells.gold_chain.GoldChain;
import io.redspace.ironspell_more.entity.spells.rapturous_bloom.RapturousBloomEntity;
import io.redspace.ironspell_more.entity.spells.resonant_knell.ResonantKnellDomeAoe;
import io.redspace.ironspell_more.entity.spells.venomous_blossomfall.AzureVenomNeedleEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, IronSpellMore.MODID);

    public static final RegistryObject<EntityType<ArcaneShackleProjectile>> ARCANE_SHACKLE = ENTITIES.register("arcane_shackle",
            () -> EntityType.Builder.<ArcaneShackleProjectile>of(ArcaneShackleProjectile::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(64)
                    .build(IronSpellMore.MODID + ":arcane_shackle"));

    public static final RegistryObject<EntityType<GoldChain>> GOLD_CHAIN = ENTITIES.register("gold_chain",
            () -> EntityType.Builder.<GoldChain>of(GoldChain::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(64)
                    .build(IronSpellMore.MODID + ":gold_chain"));

    public static final RegistryObject<EntityType<io.redspace.ironspell_more.entity.spells.wings_of_tempest.WingofTempestAoe>> WING_OF_TEMPEST_AOE = ENTITIES.register("wing_of_tempest_aoe",
            () -> EntityType.Builder.<io.redspace.ironspell_more.entity.spells.wings_of_tempest.WingofTempestAoe>of(io.redspace.ironspell_more.entity.spells.wings_of_tempest.WingofTempestAoe::new, MobCategory.MISC)
                    .sized(4.0f, 1.0f)
                    .clientTrackingRange(64)
                    .build(IronSpellMore.MODID + ":wing_of_tempest_aoe"));

    public static final RegistryObject<EntityType<io.redspace.ironspell_more.entity.spells.crimson_rain_bathes_moon.CrimsonSpearEntity>> CRIMSON_SPEAR = ENTITIES.register("crimson_spear",
            () -> EntityType.Builder.<io.redspace.ironspell_more.entity.spells.crimson_rain_bathes_moon.CrimsonSpearEntity>of(io.redspace.ironspell_more.entity.spells.crimson_rain_bathes_moon.CrimsonSpearEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(IronSpellMore.MODID + ":crimson_spear"));

    public static final RegistryObject<EntityType<io.redspace.ironspell_more.entity.spells.gale_drive.GaleDriveVortexEntity>> GALE_DRIVE_VORTEX = ENTITIES.register("gale_drive_vortex",
            () -> EntityType.Builder.<io.redspace.ironspell_more.entity.spells.gale_drive.GaleDriveVortexEntity>of(io.redspace.ironspell_more.entity.spells.gale_drive.GaleDriveVortexEntity::new, MobCategory.MISC)
                    .sized(4.0f, 6.0f)
                    .clientTrackingRange(64)
                    .build(IronSpellMore.MODID + ":gale_drive_vortex"));

    public static final RegistryObject<EntityType<AzureVenomNeedleEntity>> AZURE_VENOM_NEEDLE = ENTITIES.register("azure_venom_needle",
            () -> EntityType.Builder.<AzureVenomNeedleEntity>of(AzureVenomNeedleEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(IronSpellMore.MODID + ":azure_venom_needle"));

    public static final RegistryObject<EntityType<io.redspace.ironspell_more.entity.spells.gale_piercer.WindArrowEntity>> WIND_ARROW = ENTITIES.register("wind_arrow",
            () -> EntityType.Builder.<io.redspace.ironspell_more.entity.spells.gale_piercer.WindArrowEntity>of(io.redspace.ironspell_more.entity.spells.gale_piercer.WindArrowEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(IronSpellMore.MODID + ":wind_arrow"));

    public static final RegistryObject<EntityType<io.redspace.ironspell_more.entity.spells.gale_piercer.GaleArrowEntity>> GALE_ARROW = ENTITIES.register("gale_arrow",
            () -> EntityType.Builder.<io.redspace.ironspell_more.entity.spells.gale_piercer.GaleArrowEntity>of(io.redspace.ironspell_more.entity.spells.gale_piercer.GaleArrowEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(IronSpellMore.MODID + ":gale_arrow"));

    public static final RegistryObject<EntityType<io.redspace.ironspell_more.entity.spells.glacial_firmament.GlacialSpikeEntity>> GLACIAL_SPIKE = ENTITIES.register("glacial_spike",
            () -> EntityType.Builder.<io.redspace.ironspell_more.entity.spells.glacial_firmament.GlacialSpikeEntity>of(io.redspace.ironspell_more.entity.spells.glacial_firmament.GlacialSpikeEntity::new, MobCategory.MISC)
                    .sized(0.8F, 2.0F)
                    .clientTrackingRange(64)
                    .build(IronSpellMore.MODID + ":glacial_spike"));

    public static final RegistryObject<EntityType<io.redspace.ironspell_more.entity.spells.glacial_firmament.GlacialTombEntity>> GLACIAL_TOMB = ENTITIES.register("glacial_tomb",
            () -> EntityType.Builder.<io.redspace.ironspell_more.entity.spells.glacial_firmament.GlacialTombEntity>of(io.redspace.ironspell_more.entity.spells.glacial_firmament.GlacialTombEntity::new, MobCategory.MISC)
                    .sized(1.2F, 2.5F)
                    .clientTrackingRange(64)
                    .build(IronSpellMore.MODID + ":glacial_tomb"));

    public static final RegistryObject<EntityType<ResonantKnellDomeAoe>> RESONANT_KNELL_DOME = ENTITIES.register("resonant_knell_dome",
            () -> EntityType.Builder.<ResonantKnellDomeAoe>of(ResonantKnellDomeAoe::new, MobCategory.MISC)
                    .sized(16.0F, 10.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(IronSpellMore.MODID + ":resonant_knell_dome"));

    public static final RegistryObject<EntityType<RapturousBloomEntity>> RAPTUROUS_BLOOM = ENTITIES.register("rapturous_bloom",
            () -> EntityType.Builder.<RapturousBloomEntity>of(RapturousBloomEntity::new, MobCategory.MISC)
                    .sized(6.0F, 2.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(IronSpellMore.MODID + ":rapturous_bloom"));

    public static final RegistryObject<EntityType<io.redspace.ironspell_more.entity.spells.toxic_salvation.ToxicSalvationAoe>> TOXIC_SALVATION_AOE = ENTITIES.register("toxic_salvation_aoe",
            () -> EntityType.Builder.<io.redspace.ironspell_more.entity.spells.toxic_salvation.ToxicSalvationAoe>of(io.redspace.ironspell_more.entity.spells.toxic_salvation.ToxicSalvationAoe::new, MobCategory.MISC)
                    .sized(4.0f, 1.0f)
                    .clientTrackingRange(64)
                    .build(IronSpellMore.MODID + ":toxic_salvation_aoe"));

    public static final RegistryObject<EntityType<io.redspace.ironspell_more.entity.spells.crimson_thornbind.CrimsonRootEntity>> CRIMSON_ROOT = ENTITIES.register("crimson_root",
            () -> EntityType.Builder.<io.redspace.ironspell_more.entity.spells.crimson_thornbind.CrimsonRootEntity>of(io.redspace.ironspell_more.entity.spells.crimson_thornbind.CrimsonRootEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(64)
                    .build(IronSpellMore.MODID + ":crimson_root"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }

    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = IronSpellMore.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onEntityAttributeCreation(net.minecraftforge.event.entity.EntityAttributeCreationEvent event) {
            event.put(CRIMSON_ROOT.get(), net.minecraft.world.entity.LivingEntity.createLivingAttributes().build());
        }
    }
}
