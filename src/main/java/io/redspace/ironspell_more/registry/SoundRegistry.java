package io.redspace.ironspell_more.registry;

import io.redspace.ironspell_more.IronSpellMore;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, IronSpellMore.MODID);

    public static final RegistryObject<SoundEvent> VENOMOUS_BLOSSOMFALL_CHARGE_1 = registerSoundEvent("venomous_blossomfall_charge_1");
    public static final RegistryObject<SoundEvent> VENOMOUS_BLOSSOMFALL_CHARGE_2 = registerSoundEvent("venomous_blossomfall_charge_2");
    public static final RegistryObject<SoundEvent> VENOMOUS_BLOSSOMFALL_CHARGE = VENOMOUS_BLOSSOMFALL_CHARGE_1;

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(IronSpellMore.id(name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
