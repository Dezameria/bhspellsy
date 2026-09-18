package io.redspace.ironspell_more.client.event;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.spells.nature.WindArrowSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.ClientSpellTargetingData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IronSpellMore.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WindArrowClientEvents {
    private static final String SPELL_ID = WindArrowSpell.SPELL_ID.toString();

    private WindArrowClientEvents() {
    }

    private static boolean isLocalPlayerCasting() {
        return ClientMagicData.isCasting() && SPELL_ID.equals(ClientMagicData.getCastingSpellId());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }

        if (isLocalPlayerCasting()) {
            HitResult hit = Utils.raycastForEntity(minecraft.level, player, (float) WindArrowSpell.MAX_TARGET_RANGE, true, 0.35F);
            if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living && living.isAlive() && player.hasLineOfSight(living)) {
                ClientMagicData.setTargetingData(new ClientSpellTargetingData(SPELL_ID, living.getUUID()));
            } else {
                ClientSpellTargetingData current = ClientMagicData.getTargetingData();
                if (current != null && SPELL_ID.equals(current.spellId)) {
                    ClientMagicData.resetTargetingData();
                }
            }
        } else {
            ClientSpellTargetingData current = ClientMagicData.getTargetingData();
            if (current != null && SPELL_ID.equals(current.spellId)) {
                ClientMagicData.resetTargetingData();
            }
        }
    }

    @SubscribeEvent
    public static void onUsingItemTick(LivingEntityUseItemEvent.Tick event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getEntity() == minecraft.player && isLocalPlayerCasting()) {
            int durationRemaining = ClientMagicData.getCastDurationRemaining();
            int castDuration = ClientMagicData.getCastDuration();
            int chargeTicks = castDuration - durationRemaining;
            if (chargeTicks >= WindArrowSpell.FULL_CHARGE_TICKS
                    && event.getDuration() <= WindArrowSpell.FULL_CHARGE_HOLD_REFRESH_THRESHOLD) {
                event.setDuration(WindArrowSpell.FULL_CHARGE_HOLD_USE_DURATION);
            }
        }
    }
}
