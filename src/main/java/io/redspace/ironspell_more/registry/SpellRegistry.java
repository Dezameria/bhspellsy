package io.redspace.ironspell_more.registry;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.spells.fire.PureWhiteFlameBurstSpell;
import io.redspace.ironspell_more.spells.fire.SpinStrikeSpell;
import io.redspace.ironspell_more.spells.gold.ShackleofFearSpell;
import io.redspace.ironspell_more.spells.lightning.LightningStrikeSpell;
import io.redspace.ironspell_more.spells.lightning.ThunderStepSpell;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class SpellRegistry {
    public static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister
            .create(io.redspace.ironsspellbooks.api.registry.SpellRegistry.SPELL_REGISTRY_KEY, IronSpellMore.MODID);

    // FIRE
    public static final RegistryObject<AbstractSpell> SPIN_STRIKE_SPELL = registerSpell(new SpinStrikeSpell());
    public static final RegistryObject<AbstractSpell> PURE_WHITE_FLAME_BURST_SPELL = registerSpell(new PureWhiteFlameBurstSpell());

    // LIGHTNING
    public static final RegistryObject<AbstractSpell> LIGHTNING_STRIKE_SPELL = registerSpell(new LightningStrikeSpell());
    public static final RegistryObject<AbstractSpell> THUNDER_STEP_SPELL = registerSpell(new ThunderStepSpell());

    // GOLD
    public static final RegistryObject<AbstractSpell> SHACKLE_OF_FEAR_SPELL = registerSpell(new ShackleofFearSpell());

    // NATURE
    public static final RegistryObject<AbstractSpell> WINGS_OF_TEMPEST_SPELL = registerSpell(new io.redspace.ironspell_more.spells.nature.WingsofTempestSpell());

    public static RegistryObject<AbstractSpell> registerSpell(AbstractSpell spell) {
        return SPELLS.register(spell.getSpellName(), () -> spell);
    }

    public static void register(IEventBus eventBus) {
        SPELLS.register(eventBus);
    }
}
