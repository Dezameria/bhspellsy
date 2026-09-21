package io.redspace.ironspell_more.registry;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.spells.fire.PureWhiteFlameBurstSpell;
import io.redspace.ironspell_more.spells.fire.SpinStrikeSpell;
import io.redspace.ironspell_more.spells.gold.ShackleofFearSpell;
import io.redspace.ironspell_more.spells.lightning.LightningStrikeSpell;
import io.redspace.ironspell_more.spells.lightning.ThunderStepSpell;
import io.redspace.ironspell_more.spells.nature.VenomousBlossomfallSpell;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class SpellRegistry {
    public static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister
            .create(io.redspace.ironsspellbooks.api.registry.SpellRegistry.SPELL_REGISTRY_KEY, IronSpellMore.MODID);

    // FIRE
    public static final RegistryObject<AbstractSpell> BLAZING_CHAKRA_SPELL = registerSpell(new io.redspace.ironspell_more.spells.fire.BlazingChakraSpell());
    public static final RegistryObject<AbstractSpell> SPIN_STRIKE_SPELL = registerSpell(new SpinStrikeSpell());
    public static final RegistryObject<AbstractSpell> PURE_WHITE_FLAME_BURST_SPELL = registerSpell(new PureWhiteFlameBurstSpell());
    public static final RegistryObject<AbstractSpell> GALE_DRIVE_SPELL = registerSpell(new io.redspace.ironspell_more.spells.fire.GaleDriveSpell());

    // LIGHTNING
    public static final RegistryObject<AbstractSpell> LIGHTNING_STRIKE_SPELL = registerSpell(new LightningStrikeSpell());
    public static final RegistryObject<AbstractSpell> THUNDER_STEP_SPELL = registerSpell(new ThunderStepSpell());

    // GOLD
    public static final RegistryObject<AbstractSpell> SHACKLE_OF_FEAR_SPELL = registerSpell(new ShackleofFearSpell());

    // GROUND
    public static final RegistryObject<AbstractSpell> TIGERSHADE_TERRABREAK_SPELL = registerSpell(new io.redspace.ironspell_more.spells.ground.TigershadeTerrabreakSpell());

    // NATURE
    public static final RegistryObject<AbstractSpell> WINGS_OF_TEMPEST_SPELL = registerSpell(new io.redspace.ironspell_more.spells.nature.WingsofTempestSpell());
    public static final RegistryObject<AbstractSpell> VENOMOUS_BLOSSOMFALL_SPELL = registerSpell(new VenomousBlossomfallSpell());
    public static final RegistryObject<AbstractSpell> GALE_PIERCER_SPELL = registerSpell(new io.redspace.ironspell_more.spells.nature.GalePiercerSpell());

    // AQUA
    public static final RegistryObject<AbstractSpell> CRIMSON_RAIN_BATHES_MOON_SPELL = registerSpell(new io.redspace.ironspell_more.spells.aqua.CrimsonRainBathesMoonSpell());
    public static final RegistryObject<AbstractSpell> GLACIAL_VEIL_SPELL = registerSpell(new io.redspace.ironspell_more.spells.aqua.GlacialVeilSpell());
    public static final RegistryObject<AbstractSpell> GLACIAL_FIRMAMENT_SPELL = registerSpell(new io.redspace.ironspell_more.spells.aqua.GlacialFirmamentSpell());

    public static RegistryObject<AbstractSpell> registerSpell(AbstractSpell spell) {
        return SPELLS.register(spell.getSpellName(), () -> spell);
    }

    public static void register(IEventBus eventBus) {
        SPELLS.register(eventBus);
    }
}
