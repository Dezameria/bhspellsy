package io.redspace.ironspell_more.client.event;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.registry.MobEffectsRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IronSpellMore.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GaleDriveClientEvents {
    private static Float lockedYaw = null;
    private static Float lockedPitch = null;

    private GaleDriveClientEvents() {
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player != null && event.getEntity() == player) {
            if (player.hasEffect(MobEffectsRegistry.GALE_DRIVE_DASH.get())) {
                // ป้องกันการกดปุ่มเลี้ยว/ถอยหลังขณะกำลังพุ่งตรงดิ่ง
                event.getInput().forwardImpulse = 0.0F;
                event.getInput().leftImpulse = 0.0F;
                event.getInput().jumping = false;
                event.getInput().shiftKeyDown = false;
            }
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            lockedYaw = null;
            lockedPitch = null;
            return;
        }

        if (player.hasEffect(MobEffectsRegistry.GALE_DRIVE_DASH.get())) {
            if (lockedYaw == null || lockedPitch == null) {
                lockedYaw = player.getYRot();
                lockedPitch = player.getXRot();
            }
            // ล็อกมุมกล้องของผู้เล่นไม่ให้สะบัดหรือหันไปทิศทางอื่นได้
            event.setYaw(lockedYaw);
            event.setPitch(lockedPitch);
            player.setYRot(lockedYaw);
            player.setXRot(lockedPitch);
            player.yRotO = lockedYaw;
            player.xRotO = lockedPitch;
        } else {
            lockedYaw = null;
            lockedPitch = null;
        }
    }
}
