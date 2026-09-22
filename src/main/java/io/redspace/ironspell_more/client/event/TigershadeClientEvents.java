package io.redspace.ironspell_more.client.event;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.registry.MobEffectsRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = IronSpellMore.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TigershadeClientEvents {
    @Nullable
    private static UUID markedTargetUuid;
    @Nullable
    private static Entity glowingEntity;
    @Nullable
    private static ClientLevel activeLevel;
    private static boolean appliedGlow;

    private TigershadeClientEvents() {
    }

    public static void setMarkedTarget(@Nullable UUID targetUuid) {
        if (targetUuid == null || !targetUuid.equals(markedTargetUuid)) {
            clearAppliedGlow();
        }
        markedTargetUuid = targetUuid;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;

        if (player == null || level == null) {
            clearAppliedGlow();
            markedTargetUuid = null;
            activeLevel = null;
            return;
        }

        if (activeLevel != level) {
            clearAppliedGlow();
            activeLevel = level;
        }

        if (!player.hasEffect(MobEffectsRegistry.TIGERSHADE_STANCE.get()) || markedTargetUuid == null) {
            clearAppliedGlow();
            return;
        }

        LivingEntity markedTarget = null;
        for (Entity candidate : level.entitiesForRendering()) {
            if (candidate instanceof LivingEntity living
                    && candidate.getUUID().equals(markedTargetUuid)
                    && living.isAlive()) {
                markedTarget = living;
                break;
            }
        }

        if (glowingEntity != markedTarget) {
            clearAppliedGlow();
        }

        if (markedTarget != null) {
            glowingEntity = markedTarget;
            if (!markedTarget.isCurrentlyGlowing()) {
                markedTarget.setGlowingTag(true);
                appliedGlow = true;
            }
        }
    }

    private static void clearAppliedGlow() {
        if (appliedGlow && glowingEntity != null && !glowingEntity.isRemoved()) {
            glowingEntity.setGlowingTag(false);
        }
        glowingEntity = null;
        appliedGlow = false;
    }
}
