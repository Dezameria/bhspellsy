package io.redspace.ironspell_more.client.event;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.spells.nature.GalePiercerSpell;
import io.redspace.ironsspellbooks.network.casting.CancelCastPacket;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.player.KeyMappings;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IronSpellMore.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GalePiercerClientEvents {
    private static boolean hasHeldCastKey = false;

    private GalePiercerClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            hasHeldCastKey = false;
            return;
        }

        if (ClientMagicData.isCasting() && GalePiercerSpell.SPELL_ID.toString().equals(ClientMagicData.getCastingSpellId())) {
            boolean isKeyDown = isAnyCastKeyDown(mc);

            if (isKeyDown) {
                hasHeldCastKey = true;
            } else if (hasHeldCastKey) {
                // Key was held to charge and now released -> release the arrow!
                hasHeldCastKey = false;
                PacketDistributor.sendToServer(new CancelCastPacket(false));
            }
        } else {
            hasHeldCastKey = false;
        }
    }

    private static boolean isAnyCastKeyDown(Minecraft mc) {
        if (mc.options.keyUse.isDown()) {
            return true;
        }

        try {
            if (KeyMappings.SPELLBOOK_CAST_ACTIVE_KEYMAP.isDown()) {
                return true;
            }
            for (KeyMapping quickCastMapping : KeyMappings.QUICK_CAST_MAPPINGS) {
                if (quickCastMapping.isDown()) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }

        return false;
    }
}
