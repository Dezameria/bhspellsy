package io.redspace.ironspell_more.registry;

import io.redspace.ironspell_more.IronSpellMore;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, IronSpellMore.MODID);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
