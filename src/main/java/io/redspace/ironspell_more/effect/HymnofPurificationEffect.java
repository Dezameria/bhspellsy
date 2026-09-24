package io.redspace.ironspell_more.effect;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.registry.MobEffectsRegistry;
import io.redspace.ironspell_more.spells.gold.HymnofPurificationSpell;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

public class HymnofPurificationEffect extends MobEffect {
    public static final String START_X = "HymnStartX";
    public static final String START_Y = "HymnStartY";
    public static final String START_Z = "HymnStartZ";
    public static final String START_YAW = "HymnStartYaw";
    public static final String START_PITCH = "HymnStartPitch";
    public static final String INTERRUPTED = "HymnInterrupted";
    public static final String TICKS_ACTIVE = "HymnTicksActive";

    public HymnofPurificationEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(entity, attributeMap, amplifier);
        entity.getPersistentData().putDouble(START_X, entity.getX());
        entity.getPersistentData().putDouble(START_Y, entity.getY());
        entity.getPersistentData().putDouble(START_Z, entity.getZ());
        entity.getPersistentData().putFloat(START_YAW, entity.getYRot());
        entity.getPersistentData().putFloat(START_PITCH, entity.getXRot());
        entity.getPersistentData().putBoolean(INTERRUPTED, false);
        entity.getPersistentData().putInt(TICKS_ACTIVE, 0);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true; // Tick every server tick
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        Level level = entity.level();
        if (level.isClientSide) {
            return;
        }

        if (entity.getPersistentData().getBoolean(INTERRUPTED)) {
            return;
        }

        int ticks = entity.getPersistentData().getInt(TICKS_ACTIVE) + 1;
        entity.getPersistentData().putInt(TICKS_ACTIVE, ticks);

        // Interruption Check 1: Movement displacement (threshold 0.2 blocks -> 0.04 sqr)
        double dx = entity.getX() - entity.getPersistentData().getDouble(START_X);
        double dy = entity.getY() - entity.getPersistentData().getDouble(START_Y);
        double dz = entity.getZ() - entity.getPersistentData().getDouble(START_Z);
        if (dx * dx + dy * dy + dz * dz > 0.04D) {
            interrupt(entity);
            return;
        }

        // Interruption Check 2: Camera / Look angle rotation (threshold 3.0 degrees)
        float yawDiff = Math.abs(Mth.wrapDegrees(entity.getYRot() - entity.getPersistentData().getFloat(START_YAW)));
        float pitchDiff = Math.abs(Mth.wrapDegrees(entity.getXRot() - entity.getPersistentData().getFloat(START_PITCH)));
        if (yawDiff > 3.0F || pitchDiff > 3.0F) {
            interrupt(entity);
            return;
        }

        // Interruption Check 3: Physical living entity collision
        List<LivingEntity> colliding = level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox(),
                e -> e != entity && !e.isSpectator() && !e.isPassengerOfSameVehicle(entity) && e.isPickable());
        if (!colliding.isEmpty()) {
            interrupt(entity);
            return;
        }

        // Periodic healing pulse every 20 ticks (1 second) during seconds 1–19 (ticks 20 to 380)
        if (ticks >= HymnofPurificationSpell.HEAL_INTERVAL_TICKS && ticks < HymnofPurificationSpell.DURATION_TICKS
                && ticks % HymnofPurificationSpell.HEAL_INTERVAL_TICKS == 0) {
            int spellLevel = amplifier + 1;
            float healAmount = HymnofPurificationSpell.getHealAmount(spellLevel, entity);
            float radiusSqr = HymnofPurificationSpell.RADIUS * HymnofPurificationSpell.RADIUS;

            List<LivingEntity> allies = level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(HymnofPurificationSpell.RADIUS),
                    target -> HymnofPurificationSpell.isAlly(entity, target) && entity.distanceToSqr(target) <= radiusSqr);

            for (LivingEntity ally : allies) {
                ally.heal(healAmount);
                if (level instanceof ServerLevel serverLevel) {
                    MagicManager.spawnParticles(serverLevel, ParticleTypes.HEART,
                            ally.getX(), ally.getY() + ally.getBbHeight() * 0.7D, ally.getZ(),
                            1, 0.2D, 0.2D, 0.2D, 0.02D, false);
                }
            }
        }

        // Golden soundwave and music particles during performance
        HymnofPurificationSpell.spawnChannelingParticles(level, entity, ticks);
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
        Level level = entity.level();
        if (level.isClientSide) {
            return;
        }

        boolean interrupted = entity.getPersistentData().getBoolean(INTERRUPTED);
        int ticks = entity.getPersistentData().getInt(TICKS_ACTIVE);

        if (!interrupted && ticks >= HymnofPurificationSpell.DURATION_TICKS - 5) {
            // Full 20-second completion: Cleanse all HARMFUL debuffs for allies within 20 blocks
            HymnofPurificationSpell.performDebuffCleanse(level, entity);
        } else if (!interrupted) {
            // Terminated early (e.g. death or external effect removal)
            interrupt(entity);
        }
    }

    public static void interrupt(LivingEntity entity) {
        if (entity.getPersistentData().getBoolean(INTERRUPTED)) {
            return;
        }
        entity.getPersistentData().putBoolean(INTERRUPTED, true);

        Level level = entity.level();

        // 1. Play discordant / missed note sounds
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.5F, 0.5F);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), SoundSource.PLAYERS, 1.2F, 0.6F);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);

        // 2. Apply Qi instability debuff: Dizziness (Nausea / Confusion) for 2 seconds (40 ticks) to caster & allies within circle
        float radiusSqr = HymnofPurificationSpell.RADIUS * HymnofPurificationSpell.RADIUS;
        List<LivingEntity> allies = level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(HymnofPurificationSpell.RADIUS),
                target -> HymnofPurificationSpell.isAlly(entity, target) && entity.distanceToSqr(target) <= radiusSqr);

        for (LivingEntity ally : allies) {
            ally.addEffect(new MobEffectInstance(MobEffects.CONFUSION, HymnofPurificationSpell.NAUSEA_DURATION_TICKS, 1, false, true, true));
        }

        // 3. Remove effect if still present to finalize termination
        if (entity.hasEffect(MobEffectsRegistry.HYMN_OF_PURIFICATION.get())) {
            entity.removeEffect(MobEffectsRegistry.HYMN_OF_PURIFICATION.get());
        }
    }

    /**
     * Forge Event Subscriber for interruption triggers like taking damage, player logout, or death.
     */
    @Mod.EventBusSubscriber(modid = IronSpellMore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {

        @SubscribeEvent
        public static void onLivingDamage(LivingDamageEvent event) {
            if (event.getAmount() <= 0.0F) {
                return;
            }

            if (event.getEntity().hasEffect(MobEffectsRegistry.HYMN_OF_PURIFICATION.get())) {
                interrupt(event.getEntity());
            }
        }

        @SubscribeEvent
        public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer && serverPlayer.hasEffect(MobEffectsRegistry.HYMN_OF_PURIFICATION.get())) {
                interrupt(serverPlayer);
            }
        }

        @SubscribeEvent
        public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer && serverPlayer.hasEffect(MobEffectsRegistry.HYMN_OF_PURIFICATION.get())) {
                interrupt(serverPlayer);
            }
        }

        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            if (event.getEntity().hasEffect(MobEffectsRegistry.HYMN_OF_PURIFICATION.get())) {
                interrupt(event.getEntity());
            }
        }
    }
}
