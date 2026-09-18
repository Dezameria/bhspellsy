package io.redspace.ironspell_more;

import com.mojang.logging.LogUtils;
import io.redspace.ironspell_more.registry.ItemRegistry;
import io.redspace.ironspell_more.registry.MobEffectsRegistry;
import io.redspace.ironspell_more.registry.ParticleRegistry;
import io.redspace.ironspell_more.registry.SpellRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(IronSpellMore.MODID)
public class IronSpellMore {
    public static final String MODID = "ironspell_more";
    public static final Logger LOGGER = LogUtils.getLogger();

    public IronSpellMore() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        MobEffectsRegistry.register(modEventBus);
        ItemRegistry.register(modEventBus);
        SpellRegistry.register(modEventBus);
        ParticleRegistry.register(modEventBus);
        io.redspace.ironspell_more.registry.SoundRegistry.register(modEventBus);
        io.redspace.ironspell_more.registry.EntityRegistry.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("IronSpellMore COMMON SETUP");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("IronSpellMore SERVER STARTING");
    }

    public static ResourceLocation id(@NotNull String path) {
        return new ResourceLocation(MODID, path);
    }
}
