package io.redspace.ironspell_more.registry;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.spells.LightningStrikeSpell;
import io.redspace.ironspell_more.spells.SpinStrikeSpell;
import io.redspace.ironspell_more.spells.ThunderStepSpell;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import io.redspace.ironspell_more.effect.SpinStrikeEffect;

public class SpellRegistry {
        public static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister
                        .create(io.redspace.ironsspellbooks.api.registry.SpellRegistry.SPELL_REGISTRY_KEY,
                                        IronSpellMore.MODID);

        public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(
                        ForgeRegistries.MOB_EFFECTS,
                        IronSpellMore.MODID);

        public static void register(IEventBus eventBus) {
                SPELLS.register(eventBus);
                MOB_EFFECTS.register(eventBus);
        }

        // Define your custom effect here
        public static final RegistryObject<MobEffect> SPIN_STRIKE = MOB_EFFECTS.register("spin_strike",
                        () -> new SpinStrikeEffect(MobEffectCategory.BENEFICIAL, 0x4dd9eb));

        public static RegistryObject<AbstractSpell> registerSpell(AbstractSpell spell) {
                return SPELLS.register(spell.getSpellName(), () -> spell);
        }

        public static final RegistryObject<AbstractSpell> LIGHTNING_STRIKE_SPELL = registerSpell(
                        new LightningStrikeSpell());
        public static final RegistryObject<AbstractSpell> THUNDER_STEP_SPELL = registerSpell(new ThunderStepSpell());
        public static final RegistryObject<AbstractSpell> SPIN_STRIKE_SPELL = registerSpell(new SpinStrikeSpell());
}
