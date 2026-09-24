package io.redspace.ironspell_more.event;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.client.particle.GildedHareVfx;
import io.redspace.ironspell_more.effect.GildedHareEffect;
import io.redspace.ironspell_more.effect.GildedHareMarkEffect;
import io.redspace.ironspell_more.registry.MobEffectsRegistry;
import io.redspace.ironspell_more.spells.gold.GildedHareSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IronSpellMore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GildedHareCombatEvents {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getAmount() <= 0.0F) {
            return;
        }

        DamageSource source = event.getSource();
        LivingEntity victim = event.getEntity();
        Entity directEntity = source.getDirectEntity();
        Entity attackerEntity = source.getEntity();

        // 1. Must be direct melee damage from a living attacker (exclude projectiles, thorns, magic, explosions)
        if (!(attackerEntity instanceof LivingEntity attacker) || directEntity != attacker) {
            return;
        }

        if (source.is(DamageTypeTags.IS_PROJECTILE)
                || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypeTags.BYPASSES_ARMOR)
                || source.is(DamageTypes.THORNS)
                || source.is(DamageTypes.MAGIC)) {
            return;
        }

        // 2. Attacker must have active Gilded Hare buff
        if (!attacker.hasEffect(MobEffectsRegistry.GILDED_HARE.get())) {
            return;
        }

        // 3. Reject self-damage, dead victim, or friendly fire
        if (victim == attacker || !victim.isAlive() || DamageSources.isFriendlyFireBetween(attacker, victim)) {
            return;
        }

        // 4. If victim is on finisher cooldown (10s after completing full 5-hit combo), do not build stacks
        long finisherCooldown = victim.getPersistentData().getLong(GildedHareMarkEffect.FINISHER_COOLDOWN_TICK_TAG);
        if (victim.level().getGameTime() < finisherCooldown) {
            return;
        }

        // If victim is currently in Cocoon Stun (amplifier >= 4), do not add combo hits
        var activeMark = victim.getEffect(MobEffectsRegistry.GILDED_HARE_MARK.get());
        if (activeMark != null && activeMark.getAmplifier() >= 4) {
            return;
        }

        // 5. Evaluate combo state with strict single-caster ownership
        String storedOwner = victim.getPersistentData().getString(GildedHareMarkEffect.OWNER_UUID_TAG);
        String attackerUUID = attacker.getStringUUID();
        String lastTargetUUID = attacker.getPersistentData().getString(GildedHareEffect.CASTER_ACTIVE_TARGET_TAG);
        String victimUUID = victim.getStringUUID();

        boolean isOwnerMatch = attackerUUID.equals(storedOwner);
        boolean isSameTarget = victimUUID.equals(lastTargetUUID);
        boolean hasActiveMark = activeMark != null; // Active mark guarantees inside combo window

        int currentCombo = victim.getPersistentData().getInt(GildedHareMarkEffect.COMBO_COUNT_TAG);
        int nextCombo = (isOwnerMatch && isSameTarget && hasActiveMark) ? currentCombo + 1 : 1;

        if (nextCombo < 5) {
            // Consecutive hit (1 to 4) -> maps to amplifier 0 to 3 (displaying 1 to 4 ribbon strips)
            attacker.getPersistentData().putString(GildedHareEffect.CASTER_ACTIVE_TARGET_TAG, victimUUID);
            victim.getPersistentData().putString(GildedHareMarkEffect.OWNER_UUID_TAG, attackerUUID);
            victim.getPersistentData().putInt(GildedHareMarkEffect.LAST_HIT_TICK_TAG, victim.tickCount);
            victim.getPersistentData().putInt(GildedHareMarkEffect.COMBO_COUNT_TAG, nextCombo);

            // Remove existing mark before re-adding to cleanly advance amplifier without hidden effect retention
            victim.removeEffect(MobEffectsRegistry.GILDED_HARE_MARK.get());

            // Apply ribbon binding mark with amplifier (nextCombo - 1) for 100 ticks (5 seconds)
            victim.addEffect(new MobEffectInstance(MobEffectsRegistry.GILDED_HARE_MARK.get(),
                    GildedHareSpell.COMBO_WINDOW_TICKS, nextCombo - 1, false, false, true));

            // Apply Slowness I for 20 ticks (1 second)
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    GildedHareSpell.SLOWNESS_DURATION_TICKS, 0, false, false, true));

            // Impact VFX
            if (victim.level() instanceof ServerLevel serverLevel) {
                GildedHareVfx.spawnKickImpactVfx(serverLevel, attacker, victim);
            }
        } else {
            // 5th Hit Finisher: Full Cocoon Stun & Blindness (amplifier = 4)
            attacker.getPersistentData().remove(GildedHareEffect.CASTER_ACTIVE_TARGET_TAG);
            GildedHareMarkEffect.clearComboData(victim);
            // Lock out building new combo stacks on this victim for 10 seconds (200 ticks)
            victim.getPersistentData().putLong(GildedHareMarkEffect.FINISHER_COOLDOWN_TICK_TAG,
                    victim.level().getGameTime() + GildedHareSpell.FINISHER_COOLDOWN_TICKS);

            // Remove existing mark before applying finisher mark to prevent vanilla hidden effect restoration
            victim.removeEffect(MobEffectsRegistry.GILDED_HARE_MARK.get());

            // Apply Full Cocoon Mark (amplifier 4) for 20 ticks (1 second)
            victim.addEffect(new MobEffectInstance(MobEffectsRegistry.GILDED_HARE_MARK.get(),
                    GildedHareSpell.STUN_DURATION_TICKS, 4, false, false, true));

            // Complete Immobilization Stun for 20 ticks (1 second)
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    GildedHareSpell.STUN_DURATION_TICKS, 255, false, false, true));
            victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,
                    GildedHareSpell.STUN_DURATION_TICKS, 255, false, false, true));
            victim.addEffect(new MobEffectInstance(MobEffects.JUMP,
                    GildedHareSpell.STUN_DURATION_TICKS, 200, false, false, false));
            victim.setDeltaMovement(0, 0, 0);
            victim.hurtMarked = true;

            // Blindness for 60 ticks (3 seconds)
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,
                    GildedHareSpell.BLINDNESS_DURATION_TICKS, 0, false, false, true));

            // Cocoon burst VFX & audio
            if (victim.level() instanceof ServerLevel serverLevel) {
                GildedHareVfx.spawnCocoonStunBurst(serverLevel, victim);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        LivingEntity entity = event.getEntity();
        var mark = entity.getEffect(MobEffectsRegistry.GILDED_HARE_MARK.get());
        if (mark != null && mark.getAmplifier() >= 4) {
            // Completely cancel knockback while stunned in full cocoon
            event.setCanceled(true);
            entity.setDeltaMovement(0, 0, 0);
            entity.hurtMarked = true;
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.getPersistentData().contains(GildedHareEffect.CASTER_ACTIVE_TARGET_TAG)) {
            entity.getPersistentData().remove(GildedHareEffect.CASTER_ACTIVE_TARGET_TAG);
        }
        if (entity.getPersistentData().contains(GildedHareMarkEffect.OWNER_UUID_TAG)
                || entity.getPersistentData().contains(GildedHareMarkEffect.COMBO_COUNT_TAG)) {
            GildedHareMarkEffect.clearComboData(entity);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        cleanupPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        cleanupPlayer(event.getEntity());
    }

    private static void cleanupPlayer(Player player) {
        if (player.getPersistentData().contains(GildedHareEffect.CASTER_ACTIVE_TARGET_TAG)) {
            player.getPersistentData().remove(GildedHareEffect.CASTER_ACTIVE_TARGET_TAG);
        }
        if (player.getPersistentData().contains(GildedHareMarkEffect.OWNER_UUID_TAG)
                || player.getPersistentData().contains(GildedHareMarkEffect.COMBO_COUNT_TAG)) {
            GildedHareMarkEffect.clearComboData(player);
        }
    }
}
