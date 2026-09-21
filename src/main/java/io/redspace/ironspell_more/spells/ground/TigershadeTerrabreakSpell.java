package io.redspace.ironspell_more.spells.ground;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.effect.TigershadeMarkEffect;
import io.redspace.ironspell_more.effect.TigershadeStanceEffect;
import io.redspace.ironspell_more.registry.MobEffectsRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironspell_more.client.particle.ShockwaveParticleOptionCustom;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

@AutoSpellConfig
public class TigershadeTerrabreakSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(IronSpellMore.MODID,
            "tigershade_terrabreak");

    // BHSpells Ground school with fallback to Evocation when bhspells is absent
    private static final ResourceLocation GROUND_SCHOOL_RESOURCE = ResourceLocation.fromNamespaceAndPath("bhspells",
            "ground");

    public static final float ACQUISITION_RANGE = 20.0F;
    public static final float EXECUTE_MAX_DISTANCE = 5.0F;
    public static final float EXECUTE_HEALTH_PERCENT = 0.10F; // 10%
    public static final float HEAL_ON_EXECUTE = 50.0F;
    public static final int MARK_DURATION_TICKS = 1200; // 60 seconds
    public static final int EXECUTE_COOLDOWN_TICKS = 600; // 30 seconds

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(GROUND_SCHOOL_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(0.0)
            .build();

    public TigershadeTerrabreakSpell() {
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 1;
        this.baseManaCost = 40;
        this.manaCostPerLevel = 0;
        this.castTime = 0;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public SchoolType getSchoolType() {
        SchoolType school = SchoolRegistry.getSchool(GROUND_SCHOOL_RESOURCE);
        return school != null ? school : SchoolRegistry.EVOCATION.get();
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.cooldown",
                        Utils.timeFromTicks(EXECUTE_COOLDOWN_TICKS, 1)),
                Component.translatable("ui.irons_spellbooks.distance",
                        Utils.stringTruncation(EXECUTE_MAX_DISTANCE, 1)),
                Component.translatable("ui.irons_spellbooks.duration",
                        Utils.timeFromTicks(MARK_DURATION_TICKS, 1)),
                Component.translatable("ui.irons_spellbooks.hp",
                        Utils.stringTruncation(HEAL_ON_EXECUTE, 1)));
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        boolean hasStance = entity.hasEffect(MobEffectsRegistry.TIGERSHADE_STANCE.get());

        if (!hasStance) {
            // Phase 1: Needs a living enemy target in sight to mark
            LivingEntity lookTarget = findLookTarget(level, entity, ACQUISITION_RANGE);
            if (lookTarget == null) {
                if (entity instanceof ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(
                            Component.translatable("ui.ironspell_more.tigershade_no_target"), true);
                }
                return false;
            }
            return true;
        } else {
            // Phase 2: Execute condition checks
            LivingEntity target = resolveMarkedTarget(level, entity);
            if (target == null || !target.isAlive()) {
                if (entity instanceof ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(
                            Component.translatable("ui.ironspell_more.tigershade_target_lost"), true);
                }
                return false;
            }

            double distanceSqr = entity.distanceToSqr(target);
            if (distanceSqr > (EXECUTE_MAX_DISTANCE * EXECUTE_MAX_DISTANCE)) {
                if (entity instanceof ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(
                            Component.translatable("ui.ironspell_more.tigershade_too_far"), true);
                }
                return false;
            }

            float hpThreshold = target.getMaxHealth() * EXECUTE_HEALTH_PERCENT;
            if (target.getHealth() > hpThreshold) {
                if (entity instanceof ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(
                            Component.translatable("ui.ironspell_more.tigershade_hp_too_high"), true);
                }
                return false;
            }

            return true;
        }
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        boolean hasStance = entity.hasEffect(MobEffectsRegistry.TIGERSHADE_STANCE.get());

        if (!hasStance) {
            // -------------------------------------------------------------
            // Phase 1: Mark target and activate Tigershade Stance
            // -------------------------------------------------------------
            LivingEntity target = findLookTarget(world, entity, ACQUISITION_RANGE);
            if (target != null && target.isAlive()) {
                // Apply stance effect to caster (Speed II, Strength I, aura)
                entity.addEffect(new MobEffectInstance(MobEffectsRegistry.TIGERSHADE_STANCE.get(),
                        MARK_DURATION_TICKS, 0, false, false, true));

                // Apply mark effect to target (triggers client-side glowing outline)
                target.addEffect(new MobEffectInstance(MobEffectsRegistry.TIGERSHADE_MARK.get(),
                        MARK_DURATION_TICKS, 0, false, false, true));

                // Store target UUID in caster persistent data
                entity.getPersistentData().putString(TigershadeStanceEffect.TARGET_UUID_TAG, target.getStringUUID());
                target.getPersistentData().putString(TigershadeMarkEffect.MARK_CASTER_UUID_TAG, entity.getStringUUID());

                // Sound & visual cue
                world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.2F, 1.2F);
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 0.8F, 1.4F);

                if (world instanceof ServerLevel serverLevel) {
                    // Yellow-orange mark burst on target
                    serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.65F, 0.15F), 1.5F),
                            target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(),
                            25, 0.4, 0.5, 0.4, 0.1);
                    serverLevel.sendParticles(ParticleTypes.FLAME,
                            target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(),
                            10, 0.25, 0.3, 0.25, 0.05);

                    // Yellow-orange aura pulse on caster
                    serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.55F, 0.10F), 1.2F),
                            entity.getX(), entity.getY() + 0.3, entity.getZ(),
                            15, 0.35, 0.2, 0.3, 0.02);
                }
            }
        } else {
            // -------------------------------------------------------------
            // Phase 2: Takedown Slam Execute
            // -------------------------------------------------------------
            LivingEntity target = resolveMarkedTarget(world, entity);
            if (target != null && target.isAlive()) {
                // 1. Dash / lunge impulse towards target
                Vec3 toTarget = target.position().subtract(entity.position());
                Vec3 lungeDir = new Vec3(toTarget.x, 0.15, toTarget.z).normalize().scale(1.25);
                entity.setDeltaMovement(lungeDir);
                entity.hurtMarked = true;

                // 2. Slam target down into ground
                target.setDeltaMovement(0, -1.2, 0);
                target.hurtMarked = true;

                // 3. Attributed lethal damage (instantly finishes fight)
                DamageSources.applyDamage(target, 999999.0F, this.getDamageSource(entity));
                if (target.isAlive()) {
                    target.setHealth(0.0F);
                    target.die(this.getDamageSource(entity));
                }

                // 4. Heal caster by 50 HP
                entity.heal(HEAL_ON_EXECUTE);

                // 5. Earth shatter / ground slam shockwave presentation
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.3F, 0.7F);
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.5F, 0.5F);

                if (world instanceof ServerLevel serverLevel) {
                    // Expanding custom shockwave ring in yellow-orange
                    serverLevel.sendParticles(new ShockwaveParticleOptionCustom(new Vector3f(1.0F, 0.65F, 0.15F), 5.0F, true, new Vector3f(0, 1, 0)),
                            target.getX(), target.getY() + 0.15, target.getZ(),
                            1, 0, 0, 0, 0);

                    // Ground impact burst particles (earth/dirt fragments)
                    serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COARSE_DIRT.defaultBlockState()),
                            target.getX(), target.getY() + 0.1, target.getZ(),
                            60, 1.2, 0.3, 1.2, 0.25);

                    // Yellow-orange dust and flame burst
                    serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.60F, 0.10F), 2.0F),
                            target.getX(), target.getY() + 0.8, target.getZ(),
                            40, 1.0, 0.8, 1.0, 0.25);
                    serverLevel.sendParticles(ParticleTypes.FLAME,
                            target.getX(), target.getY() + 0.6, target.getZ(),
                            25, 0.8, 0.6, 0.8, 0.15);
                    serverLevel.sendParticles(ParticleTypes.LAVA,
                            target.getX(), target.getY() + 0.5, target.getZ(),
                            6, 0.5, 0.5, 0.5, 0.1);

                    // Explosion center emitter
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                            target.getX(), target.getY() + 0.5, target.getZ(),
                            1, 0, 0, 0, 0);
                }

                // 6. Clean up stance, mark, and tracking tags
                entity.removeEffect(MobEffectsRegistry.TIGERSHADE_STANCE.get());
                target.removeEffect(MobEffectsRegistry.TIGERSHADE_MARK.get());
                entity.getPersistentData().remove(TigershadeStanceEffect.TARGET_UUID_TAG);
                target.getPersistentData().remove(TigershadeMarkEffect.MARK_CASTER_UUID_TAG);

                // 7. Apply 30-second cooldown on successful execution
                var cooldowns = MagicData.getPlayerMagicData(entity).getPlayerCooldowns();
                cooldowns.addCooldown(this, EXECUTE_COOLDOWN_TICKS);
                if (entity instanceof ServerPlayer serverPlayer) {
                    cooldowns.syncToPlayer(serverPlayer);
                }
            }
        }

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    @Nullable
    private LivingEntity findLookTarget(Level world, LivingEntity caster, float range) {
        // Direct crosshair raycast with generous hitbox inflation
        HitResult hitResult = Utils.raycastForEntity(world, caster, range, true, 0.6F);
        if (hitResult instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity livingTarget) {
            if (isValidTarget(caster, livingTarget)) {
                return livingTarget;
            }
        }

        // Cone search in front of caster within line of sight
        Vec3 eyePos = caster.getEyePosition();
        Vec3 lookVec = caster.getLookAngle();
        AABB searchBox = caster.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = world.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> isValidTarget(caster, e) && e.distanceToSqr(caster) <= (double) (range * range));

        LivingEntity bestTarget = null;
        double bestScore = 0.80D;

        for (LivingEntity candidate : candidates) {
            Vec3 toCandidate = candidate.getBoundingBox().getCenter().subtract(eyePos).normalize();
            double dot = lookVec.dot(toCandidate);
            if (dot > bestScore && Utils.hasLineOfSight(world, caster, candidate, true)) {
                bestScore = dot;
                bestTarget = candidate;
            }
        }
        return bestTarget;
    }

    @Nullable
    private LivingEntity resolveMarkedTarget(Level world, LivingEntity caster) {
        if (caster.getPersistentData().contains(TigershadeStanceEffect.TARGET_UUID_TAG)) {
            try {
                UUID targetUUID = UUID.fromString(caster.getPersistentData().getString(TigershadeStanceEffect.TARGET_UUID_TAG));
                if (world instanceof ServerLevel serverLevel) {
                    Entity entity = serverLevel.getEntity(targetUUID);
                    if (entity instanceof LivingEntity living && living.isAlive()) {
                        return living;
                    }
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Fallback: search nearby entities marked by this caster
        AABB searchBox = caster.getBoundingBox().inflate(32.0D);
        List<LivingEntity> markedList = world.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != caster && e.isAlive() && e.hasEffect(MobEffectsRegistry.TIGERSHADE_MARK.get()));

        for (LivingEntity marked : markedList) {
            if (marked.getPersistentData().contains(TigershadeMarkEffect.MARK_CASTER_UUID_TAG)) {
                if (caster.getStringUUID().equals(marked.getPersistentData().getString(TigershadeMarkEffect.MARK_CASTER_UUID_TAG))) {
                    return marked;
                }
            }
        }

        if (markedList.size() == 1) {
            return markedList.get(0);
        }

        return null;
    }

    private boolean isValidTarget(LivingEntity caster, LivingEntity target) {
        return target != caster
                && target.isAlive()
                && !target.isSpectator()
                && !DamageSources.isFriendlyFireBetween(caster, target);
    }
}
