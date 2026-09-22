package io.redspace.ironspell_more.spells.fire;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.entity.spells.resonant_knell.ResonantKnellDomeAoe;
import io.redspace.ironspell_more.registry.MobEffectsRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerRecasts;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Resonant Knell — Fire School Spell.
 * 6-count Recast system (3 cycles of Open Barrier & Nuclear Blast Push).
 * Grants Strength II, Resistance II, and Fire Resistance I during cast.
 * Pushes and launches enemies into the air, causing standard fall damage.
 */
public class ResonantKnellSpell extends AbstractSpell {
    private static final ResourceLocation SPELL_ID = IronSpellMore.id("resonant_knell");

    public static final int TOTAL_RECAST_COUNT = 6;
    public static final int RECAST_WINDOW_TICKS = 300; // 15 seconds per push window
    public static final int COOLDOWN_DURATION_TICKS = 600; // 30 seconds cooldown effect

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(0) // Gated by CooldownEffect
            .build();

    public ResonantKnellSpell() {
        this.baseManaCost = 75;
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return SPELL_ID;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return this.defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public int getRecastCount(int spellLevel, LivingEntity entity) {
        return TOTAL_RECAST_COUNT;
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (entity.hasEffect(MobEffectsRegistry.COOLDOWN.get())) {
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("ui.ironspell_more.spell_on_cooldown")
                                .withStyle(ChatFormatting.RED), true);
            }
            return false;
        }

        ResonantKnellDomeAoe activeDome = ResonantKnellDomeAoe.getActiveDomeFor(entity);
        if (activeDome != null && activeDome.isExploding()) {
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("ui.ironspell_more.resonant_knell_shockwave_active")
                                .withStyle(ChatFormatting.GOLD), true);
            }
            return false;
        }
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        PlayerRecasts recasts = playerMagicData.getPlayerRecasts();
        boolean hasRecast = recasts.hasRecastForSpell(this);

        // Keep caster buffed with Strength II, Resistance II, Fire Resistance I
        applyCasterBuffs(entity);

        if (!hasRecast) {
            // === Press 1: Cycle 1 Open ===
            ResonantKnellDomeAoe.discardActiveDomesFor(entity);
            playChimeSound(level, entity);

            // Spawn barrier dome following caster
            ResonantKnellDomeAoe dome = new ResonantKnellDomeAoe(level);
            dome.setOwner(entity);
            dome.setPos(entity.getX(), entity.getY(), entity.getZ());
            dome.activateDome(1);
            level.addFreshEntity(dome);

            // Add recast instance with 6 counts
            // RecastInstance constructor automatically sets remainingRecasts = 6 - 1 = 5
            RecastInstance recastInstance = new RecastInstance(getSpellId(), spellLevel, TOTAL_RECAST_COUNT,
                    RECAST_WINDOW_TICKS, castSource, null);
            recasts.addRecast(recastInstance, playerMagicData);
        } else {
            RecastInstance recastInstance = recasts.getRecastInstance(getSpellId());
            int remaining = recastInstance.getRemainingRecasts();

            switch (remaining) {
                case 5 -> {
                    // === Press 2: Cycle 1 Push (Radius 15) ===
                    playPushSound(level, entity, 1.2f, 1.4f);
                    pushEnemies(level, entity, 15.0f, 1.8f, 1.2f, 6.0f);
                    triggerDomeShockwave(level, entity, 15.0f);
                }
                case 4 -> {
                    // === Press 3: Cycle 2 Open ===
                    playChimeSound(level, entity);
                    openOrCreateDome(level, entity, 2);
                }
                case 3 -> {
                    // === Press 4: Cycle 2 Push (Radius 20) ===
                    playPushSound(level, entity, 1.0f, 1.2f);
                    pushEnemies(level, entity, 20.0f, 2.5f, 1.5f, 10.0f);
                    triggerDomeShockwave(level, entity, 20.0f);
                }
                case 2 -> {
                    // === Press 5: Cycle 3 Open ===
                    playChimeSound(level, entity);
                    openOrCreateDome(level, entity, 3);
                }
                case 1 -> {
                    // === Press 6: Cycle 3 Push (Radius 30 - Final Nuclear Blast) ===
                    playNuclearBlastSound(level, entity);
                    pushEnemies(level, entity, 30.0f, 3.8f, 2.0f, 16.0f);
                    triggerDomeShockwave(level, entity, 30.0f);
                    applyCooldownEffect(entity);
                }
                default -> {
                    // Fallback
                }
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void openOrCreateDome(Level level, LivingEntity entity, int stage) {
        ResonantKnellDomeAoe dome = ResonantKnellDomeAoe.getActiveDomeFor(entity);
        if (dome == null || !dome.isAlive() || dome.isRemoved()) {
            dome = new ResonantKnellDomeAoe(level);
            dome.setOwner(entity);
            dome.setPos(entity.getX(), entity.getY(), entity.getZ());
            level.addFreshEntity(dome);
        }
        dome.activateDome(stage);
    }

    private void triggerDomeShockwave(Level level, LivingEntity entity, float radius) {
        ResonantKnellDomeAoe dome = ResonantKnellDomeAoe.getActiveDomeFor(entity);
        if (dome == null || !dome.isAlive() || dome.isRemoved()) {
            dome = new ResonantKnellDomeAoe(level);
            dome.setOwner(entity);
            dome.setPos(entity.getX(), entity.getY(), entity.getZ());
            level.addFreshEntity(dome);
        }
        dome.triggerShockwave(radius);
    }

    private void pushEnemies(Level level, LivingEntity caster, float radius, float horizontalPower, float verticalPower, float damage) {
        AABB aabb = caster.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, aabb,
                e -> e != caster && e.isAlive() && !e.isAlliedTo(caster) && !(e instanceof ArmorStand));

        for (LivingEntity target : targets) {
            double dx = target.getX() - caster.getX();
            double dz = target.getZ() - caster.getZ();
            double distSq = dx * dx + dz * dz;
            if (distSq <= radius * radius && distSq > 0.001) {
                double dist = Math.sqrt(distSq);
                double nx = dx / dist;
                double nz = dz / dist;

                // Launch enemy high into the air + outward knockback for fall damage
                target.setDeltaMovement(target.getDeltaMovement().x + nx * horizontalPower,
                        verticalPower,
                        target.getDeltaMovement().z + nz * horizontalPower);
                target.hurtMarked = true;

                // Direct spell damage
                DamageSources.applyDamage(target, damage, getDamageSource(caster));
            }
        }
    }

    private void applyCasterBuffs(LivingEntity caster) {
        // Strength II (amplifier 1)
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, RECAST_WINDOW_TICKS, 1, false, false, true));
        // Resistance II (amplifier 1)
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, RECAST_WINDOW_TICKS, 1, false, false, true));
        // Fire Resistance I (amplifier 0)
        caster.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, RECAST_WINDOW_TICKS, 0, false, false, true));
    }

    private void applyCooldownEffect(LivingEntity caster) {
        caster.addEffect(new MobEffectInstance(MobEffectsRegistry.COOLDOWN.get(), COOLDOWN_DURATION_TICKS, 0, false, false, true));
    }

    private void playChimeSound(Level level, LivingEntity entity) {
        // Magic Bell/Chime striking weapon sound
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.4f, 1.6f);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.3f);
    }

    private void playPushSound(Level level, LivingEntity entity, float explodePitch, float fireballPitch) {
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2f, explodePitch);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 1.0f, fireballPitch);
    }

    private void playNuclearBlastSound(Level level, LivingEntity entity) {
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.8f, 0.65f);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 1.5f, 0.85f);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.5f, 0.9f);
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult, ICastDataSerializable castData) {
        // When recast window times out without finishing or when finished
        applyCooldownEffect(serverPlayer);

        ResonantKnellDomeAoe.finishActiveDomesFor(serverPlayer);
        super.onRecastFinished(serverPlayer, recastInstance, recastResult, castData);
    }
}
