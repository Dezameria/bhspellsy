package io.redspace.ironspell_more.registry;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.effect.SpinStrikeEffect;
import io.redspace.ironspell_more.effect.AzureVenomousEffect;
import io.redspace.ironspell_more.effect.CooldownEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MobEffectsRegistry {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, IronSpellMore.MODID);

    public static final RegistryObject<MobEffect> SPIN_STRIKE = MOB_EFFECTS.register("spin_strike",
            () -> new SpinStrikeEffect(MobEffectCategory.BENEFICIAL, 0x4dd9eb));

    public static final RegistryObject<MobEffect> WHITE_FLAME_BURN = MOB_EFFECTS.register("white_flame_burn",
            () -> new io.redspace.ironspell_more.effect.WhiteFlameBurnEffect(MobEffectCategory.HARMFUL, 0xFFFFFF));

    public static final RegistryObject<MobEffect> GALE_DRIVE_DASH = MOB_EFFECTS.register("gale_drive_dash",
            () -> new io.redspace.ironspell_more.effect.GaleDriveDashEffect(MobEffectCategory.BENEFICIAL, 0x76d7ea));

    public static final RegistryObject<MobEffect> GALE_FALL_IMPACT = MOB_EFFECTS.register("gale_fall_impact",
            () -> new io.redspace.ironspell_more.effect.GaleFallImpactEffect(MobEffectCategory.HARMFUL, 0x4a7c59));

    public static final RegistryObject<MobEffect> AZURE_VENOMOUS = MOB_EFFECTS.register("azure_venomous",
            () -> new AzureVenomousEffect(MobEffectCategory.HARMFUL, 0x16E6C1));

    public static final RegistryObject<MobEffect> COOLDOWN = MOB_EFFECTS.register("cooldown",
            () -> new CooldownEffect(MobEffectCategory.NEUTRAL, 0x16E6C1));

    public static final RegistryObject<MobEffect> TIGERSHADE_STANCE = MOB_EFFECTS.register("tigershade_stance",
            () -> new io.redspace.ironspell_more.effect.TigershadeStanceEffect(MobEffectCategory.BENEFICIAL, 0x8B5A2B));

    public static final RegistryObject<MobEffect> TIGERSHADE_MARK = MOB_EFFECTS.register("tigershade_mark",
            () -> new io.redspace.ironspell_more.effect.TigershadeMarkEffect(MobEffectCategory.HARMFUL, 0xD2691E));

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
