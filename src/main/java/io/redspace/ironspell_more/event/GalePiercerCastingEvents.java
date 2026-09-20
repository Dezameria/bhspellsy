package io.redspace.ironspell_more.event;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.spells.nature.GalePiercerSpell;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = IronSpellMore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GalePiercerCastingEvents {
    private static final Map<UUID, UUID> PLAYER_TARGET_LOCKS = new HashMap<>();

    private GalePiercerCastingEvents() {
    }

    @Nullable
    public static LivingEntity updateLock(ServerPlayer player, AbstractSpell spell, float maxRange) {
        Level world = player.level();
        LivingEntity lookTarget = findLookTargetWithLOS(world, player, maxRange);

        UUID currentLockedUUID = PLAYER_TARGET_LOCKS.get(player.getUUID());
        LivingEntity activeTarget = null;

        if (lookTarget != null) {
            // New target acquired or switched with clear line-of-sight
            PLAYER_TARGET_LOCKS.put(player.getUUID(), lookTarget.getUUID());
            activeTarget = lookTarget;
            PacketDistributor.sendToPlayer(player, new SyncTargetingDataPacket(activeTarget, spell));
        } else if (currentLockedUUID != null) {
            // Check if previously locked target is still valid in range and dimension
            Entity existing = player.serverLevel().getEntity(currentLockedUUID);
            if (existing instanceof LivingEntity living && isValidTarget(player, living) && living.distanceToSqr(player) <= (maxRange * maxRange)) {
                activeTarget = living;
            } else {
                clearLock(player, spell);
            }
        }

        return activeTarget;
    }

    @Nullable
    public static LivingEntity getLockedTarget(ServerPlayer player, float maxRange) {
        UUID lockedUUID = PLAYER_TARGET_LOCKS.get(player.getUUID());
        if (lockedUUID == null) {
            return null;
        }
        Entity existing = player.serverLevel().getEntity(lockedUUID);
        if (existing instanceof LivingEntity living && isValidTarget(player, living) && living.distanceToSqr(player) <= (maxRange * maxRange)) {
            return living;
        }
        PLAYER_TARGET_LOCKS.remove(player.getUUID());
        return null;
    }

    public static void clearLock(ServerPlayer player, @Nullable AbstractSpell spell) {
        PLAYER_TARGET_LOCKS.remove(player.getUUID());
        if (spell != null) {
            try {
                PacketDistributor.sendToPlayer(player, new SyncTargetingDataPacket(spell, Collections.emptyList()));
            } catch (Exception ignored) {
            }
        }
    }

    @Nullable
    private static LivingEntity findLookTargetWithLOS(Level world, LivingEntity caster, float range) {
        Vec3 eyePos = caster.getEyePosition();
        Vec3 lookVec = caster.getLookAngle().normalize();

        AABB searchBox = caster.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = world.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> isValidTarget(caster, e) && e.distanceToSqr(caster) <= (range * range));

        LivingEntity bestTarget = null;
        double bestScore = 0.85D; // ~31 degree cone around crosshair

        for (LivingEntity candidate : candidates) {
            Vec3 toCandidate = candidate.getBoundingBox().getCenter().subtract(eyePos).normalize();
            double dot = lookVec.dot(toCandidate);
            // Must be within cone AND have clear, unobstructed Line of Sight!
            if (dot > bestScore && Utils.hasLineOfSight(world, caster, candidate, true)) {
                bestScore = dot;
                bestTarget = candidate;
            }
        }
        return bestTarget;
    }

    private static boolean isValidTarget(LivingEntity caster, LivingEntity target) {
        return target != caster
                && target.isAlive()
                && !target.isRemoved()
                && !target.isSpectator()
                && !DamageSources.isFriendlyFireBetween(caster, target)
                && !target.isAlliedTo(caster);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PLAYER_TARGET_LOCKS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PLAYER_TARGET_LOCKS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity deceased = event.getEntity();
        if (deceased instanceof ServerPlayer player) {
            PLAYER_TARGET_LOCKS.remove(player.getUUID());
        } else {
            // Remove locks on dead target
            PLAYER_TARGET_LOCKS.values().removeIf(targetUUID -> targetUUID.equals(deceased.getUUID()));
        }
    }
}
