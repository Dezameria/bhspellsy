package io.redspace.ironspell_more.event;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.effect.TigershadeMarkEffect;
import io.redspace.ironspell_more.effect.TigershadeStanceEffect;
import io.redspace.ironspell_more.registry.MobEffectsRegistry;
import io.redspace.ironspell_more.spells.ground.TigershadeTerrabreakSpell;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IronSpellMore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TigershadeLifecycleEvents {
    private TigershadeLifecycleEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TigershadeTerrabreakSpell.syncHuntTarget(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        clearPlayerLinks(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        clearPlayerLinks(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity deceased = event.getEntity();
        if (deceased.hasEffect(MobEffectsRegistry.TIGERSHADE_STANCE.get())
                || deceased.getPersistentData().contains(TigershadeStanceEffect.TARGET_UUID_TAG)) {
            TigershadeTerrabreakSpell.clearHunt(deceased);
        }
        if (deceased.hasEffect(MobEffectsRegistry.TIGERSHADE_MARK.get())
                || deceased.getPersistentData().contains(TigershadeMarkEffect.MARK_CASTER_UUID_TAG)) {
            TigershadeTerrabreakSpell.onMarkRemoved(deceased);
        }
    }

    private static void clearPlayerLinks(LivingEntity player) {
        if (player.hasEffect(MobEffectsRegistry.TIGERSHADE_MARK.get())
                || player.getPersistentData().contains(TigershadeMarkEffect.MARK_CASTER_UUID_TAG)) {
            TigershadeTerrabreakSpell.onMarkRemoved(player);
        }
        if (player.hasEffect(MobEffectsRegistry.TIGERSHADE_STANCE.get())
                || player.getPersistentData().contains(TigershadeStanceEffect.TARGET_UUID_TAG)) {
            TigershadeTerrabreakSpell.clearHunt(player);
        }
    }
}
