package io.redspace.ironspell_more.registry;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.entity.spells.gold_chain.ArcaneShackleProjectile;
import io.redspace.ironspell_more.entity.spells.gold_chain.GoldChain;
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

    public static final RegistryObject<EntityType<io.redspace.ironspell_more.entity.spells.WingofTempestAoe>> WING_OF_TEMPEST_AOE = ENTITIES.register("wing_of_tempest_aoe",
            () -> EntityType.Builder.<io.redspace.ironspell_more.entity.spells.WingofTempestAoe>of(io.redspace.ironspell_more.entity.spells.WingofTempestAoe::new, MobCategory.MISC)
                    .sized(4.0f, 1.0f)
                    .clientTrackingRange(64)
                    .build(IronSpellMore.MODID + ":wing_of_tempest_aoe"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
