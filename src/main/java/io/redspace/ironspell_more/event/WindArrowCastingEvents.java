package io.redspace.ironspell_more.event;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.spells.nature.WindArrowSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IronSpellMore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WindArrowCastingEvents {
    private WindArrowCastingEvents() {
    }

    @SubscribeEvent
    public static void onUsingItemTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        MagicData magicData = MagicData.getPlayerMagicData(serverPlayer);
        if (magicData.isCasting()
                && WindArrowSpell.SPELL_ID.toString().equals(magicData.getCastingSpellId())) {
            int chargeTicks = magicData.getCastDuration() - magicData.getCastDurationRemaining();
            if (chargeTicks >= WindArrowSpell.FULL_CHARGE_TICKS
                    && event.getDuration() <= WindArrowSpell.FULL_CHARGE_HOLD_REFRESH_THRESHOLD) {
                event.setDuration(WindArrowSpell.FULL_CHARGE_HOLD_USE_DURATION);
            }
        }
    }
}
