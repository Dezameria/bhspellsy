package io.redspace.ironspell_more.spells.fire;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket;
import io.redspace.ironsspellbooks.setup.Messages;
import io.redspace.ironsspellbooks.spells.TargetAreaCastData;
import io.redspace.ironsspellbooks.spells.TargetedTargetAreaCastData;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import mod.chloeprime.aaaparticles.api.common.AAALevel;
import mod.chloeprime.aaaparticles.api.common.ParticleEmitterInfo;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class PureWhiteFlameBurstSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(IronSpellMore.MODID,
            "pure_white_flame_burst");
    private static final ParticleEmitterInfo PURE_WHITE_FLAME_FX = new ParticleEmitterInfo(
            ResourceLocation.fromNamespaceAndPath(IronSpellMore.MODID, "pure_white_flame"));

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(15)
            .build();

    public PureWhiteFlameBurstSpell() {
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 80;
        this.spellPowerPerLevel = 10;
        this.castTime = 25; // 25 ticks (20-30 ticks charge)
        this.baseManaCost = 50;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float damage = getDamage(spellLevel, caster);
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(damage, 1)),
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(damage * 0.8f, 1)),
                Component.translatable("ui.irons_spellbooks.distance", "20"),
                Component.translatable("ui.irons_spellbooks.cast_time",
                        Utils.stringTruncation(getCastTime(spellLevel) / 20f, 1)));
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BLAZE_SHOOT);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.GENERIC_EXPLODE);
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.CHARGE_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.OVERHEAD_MELEE_SWING_ANIMATION;
    }

    /**
     * Detection for entities strictly inside the 1-block circle area in front of
     * caster
     */
    private LivingEntity getTargetInFrontArea(Level level, LivingEntity caster, Vec3 areaCenter, float radius) {
        float radiusSqr = radius * radius;
        AABB box = new AABB(
                areaCenter.x - radius, areaCenter.y - 1.5, areaCenter.z - radius,
                areaCenter.x + radius, areaCenter.y + 2.5, areaCenter.z + radius);

        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (e == caster || !e.isAlive() || e.isSpectator() || DamageSources.isFriendlyFireBetween(caster, e)) {
                return false;
            }
            // Check horizontal distance to circle center
            double dx = e.getX() - areaCenter.x;
            double dz = e.getZ() - areaCenter.z;
            if (dx * dx + dz * dz > radiusSqr) {
                return false;
            }
            // Check vertical height difference relative to circle center (within 2 blocks)
            if (Math.abs(e.getY() - areaCenter.y) > 2.0) {
                return false;
            }
            return true;
        });

        if (entities.isEmpty()) {
            return null;
        }

        // Return entity closest to the center of the circle
        entities.sort((a, b) -> Double.compare(a.distanceToSqr(areaCenter), b.distanceToSqr(areaCenter)));
        return entities.get(0);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        // 1. Create 1-block target area indicator directly 1.2 blocks in front of
        // caster
        Vec3 lookVec = entity.getLookAngle().normalize();
        Vec3 areaCenter = entity.position().add(lookVec.x * 1.2, 0, lookVec.z * 1.2);

        float radius = 1.0f;
        TargetedAreaEntity visualArea = TargetedAreaEntity.createTargetAreaEntity(level, areaCenter, radius, 0xFFFFFF);
        visualArea.setDuration(getEffectiveCastTime(spellLevel, entity) + 10);

        // 2. Detect target strictly inside the 1-block circle in front of caster
        LivingEntity target = getTargetInFrontArea(level, entity, areaCenter, radius);
        if (target != null) {
            TargetedTargetAreaCastData castData = new TargetedTargetAreaCastData(target, visualArea);
            playerMagicData.setAdditionalCastData(castData);

            if (entity instanceof ServerPlayer serverPlayer) {
                Messages.sendToPlayer(new SyncTargetingDataPacket(target, this), serverPlayer);
            }
        } else {
            // Track the 1-block zone with TargetAreaCastData
            playerMagicData.setAdditionalCastData(new TargetAreaCastData(areaCenter, visualArea));
        }

        return true;
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        // Caster Modifier: Apply Slowness IV and lock movement during charging
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 3, false, false, false));
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.05, 1.0, 0.05));

        if (!(level instanceof ServerLevel serverLevel)) {
            super.onServerCastTick(level, spellLevel, entity, playerMagicData);
            return;
        }

        serverLevel.sendParticles(ParticleTypes.FLAME,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                2, 0.25, 0.25, 0.25, 0.02);

        Vec3 lookVec = entity.getLookAngle().normalize();
        Vec3 currentCenter = entity.position().add(lookVec.x * 1.2, 0, lookVec.z * 1.2);

        // Dynamically update circle position to stay 1.2 blocks in front of caster
        // (follows in mid-air and on ground)
        TargetedAreaEntity visualArea = null;
        LivingEntity lockedTarget = null;

        if (playerMagicData.getAdditionalCastData() instanceof TargetedTargetAreaCastData targetedData) {
            visualArea = targetedData.getAreaEntity();
            lockedTarget = targetedData.getTarget(serverLevel);
        } else if (playerMagicData.getAdditionalCastData() instanceof TargetAreaCastData areaData) {
            visualArea = areaData.getCastingEntity();
        }

        if (visualArea != null && visualArea.isAlive()) {
            visualArea.teleportTo(currentCenter.x, currentCenter.y, currentCenter.z);
        }

        // Check if target died, was removed, or if we need to acquire one
        float radius = visualArea != null ? visualArea.getRadius() : 1.0f;
        if (lockedTarget == null || !lockedTarget.isAlive() || lockedTarget.isRemoved()) {
            LivingEntity foundTarget = getTargetInFrontArea(serverLevel, entity, currentCenter, radius + 0.5f);
            if (foundTarget != null) {
                lockedTarget = foundTarget;
                if (visualArea != null) {
                    TargetedTargetAreaCastData castData = new TargetedTargetAreaCastData(foundTarget, visualArea);
                    playerMagicData.setAdditionalCastData(castData);
                }
                if (entity instanceof ServerPlayer serverPlayer) {
                    Messages.sendToPlayer(new SyncTargetingDataPacket(foundTarget, this), serverPlayer);
                }
            } else if (visualArea != null) {
                playerMagicData.setAdditionalCastData(new TargetAreaCastData(currentCenter, visualArea));
            }
        }

        // CONTINUOUS SUCTION: If target is acquired, continuously suck target directly
        // in front of the caster
        if (lockedTarget != null && lockedTarget.isAlive()) {
            Vec3 holdPos = entity.position().add(lookVec.scale(1.2));
            Vec3 toHold = holdPos.subtract(lockedTarget.position());
            double dist = toHold.length();

            // Pull target smoothly towards the front of caster
            if (dist > 0.05) {
                double pullSpeed = Math.min(dist * 0.45, 0.85);
                Vec3 pullVel = toHold.normalize().scale(pullSpeed);
                lockedTarget.setDeltaMovement(pullVel);
                lockedTarget.resetFallDistance();
                lockedTarget.hurtMarked = true;
            }

            // Apply movement restriction to prevent target from sprinting away while being
            // sucked in
            lockedTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 2, false, false, false));

            // Suction visual effects: flames & smoke streaming from target towards caster
            if (entity.tickCount % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        lockedTarget.getX(), lockedTarget.getY() + lockedTarget.getBbHeight() * 0.5,
                        lockedTarget.getZ(),
                        3, 0.15, 0.15, 0.15, 0.02);
                serverLevel.sendParticles(ParticleTypes.SMOKE,
                        lockedTarget.getX(), lockedTarget.getY() + lockedTarget.getBbHeight() * 0.5,
                        lockedTarget.getZ(),
                        2, 0.1, 0.1, 0.1, 0.01);
                MagicManager.spawnParticles(serverLevel, ParticleHelper.FIRE,
                        lockedTarget.getX(), lockedTarget.getY() + lockedTarget.getBbHeight() * 0.5,
                        lockedTarget.getZ(),
                        1, 0.1, 0.1, 0.1, 0.02, false);
            }
        }

        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource,
            MagicData playerMagicData) {
        if (!(level instanceof ServerLevel serverLevel)) {
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }

        Vec3 lookVec = entity.getLookAngle().normalize();

        // 1. Resolve circle position and visual indicator
        TargetedAreaEntity visualArea = null;
        Vec3 circleCenter = null;
        float radius = 1.0f;

        if (playerMagicData.getAdditionalCastData() instanceof TargetedTargetAreaCastData targetedData) {
            visualArea = targetedData.getAreaEntity();
        } else if (playerMagicData.getAdditionalCastData() instanceof TargetAreaCastData areaData) {
            visualArea = areaData.getCastingEntity();
            circleCenter = areaData.getCenter();
        }

        if (visualArea != null && visualArea.isAlive()) {
            circleCenter = visualArea.position();
            radius = visualArea.getRadius();
            visualArea.discard();
        }

        if (circleCenter == null) {
            circleCenter = entity.position().add(lookVec.x * 1.2, 0, lookVec.z * 1.2);
        }

        // 2. CHECK STRICTLY IF A TARGET IS INSIDE THE 1-BLOCK CIRCLE IN FRONT OF
        // CASTER!
        LivingEntity primaryTarget = getTargetInFrontArea(serverLevel, entity, circleCenter, radius + 0.3f);

        // CONDITION A: MISS (No entity detected inside the circle area in front)
        if (primaryTarget == null) {
            // Stun / Complete Immobilization for 5.0 seconds (100 ticks)
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 255, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 255, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.JUMP, 100, 200, false, false, false));
            entity.setDeltaMovement(0, entity.getDeltaMovement().y < 0 ? entity.getDeltaMovement().y : 0, 0);
            entity.hurtMarked = true;

            // Miss VFX & Sound
            serverLevel.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.0, entity.getZ(), 25, 0.3,
                    0.4, 0.3, 0.05);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS, 1.5f, 0.8f);

            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }

        // CONDITION B: HIT (Target confirmed in 1-block circle)

        // Sub-step 1: Pull target directly in front of caster, then fling forward!
        Vec3 pullPos = entity.position().add(lookVec.scale(1.2));
        primaryTarget.teleportTo(pullPos.x, pullPos.y, pullPos.z);

        Vec3 flingVelocity = lookVec.scale(1.8).add(0, 0.35, 0);
        primaryTarget.setDeltaMovement(flingVelocity);
        primaryTarget.hurtMarked = true;

        // Sub-step 2: Phase 1 Explosion (Primary Impact Damage & Effects)
        float damage = getDamage(spellLevel, entity);
        DamageSources.applyDamage(primaryTarget, damage, getDamageSource(entity));
        primaryTarget.setRemainingFireTicks(100); // 5.0 seconds burn
        primaryTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2)); // Slowness III for 10.0s

        // Phase 1 VFX & SFX
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, primaryTarget.getX(), primaryTarget.getY() + 0.5,
                primaryTarget.getZ(), 1, 0, 0, 0, 0);
        serverLevel.sendParticles(ParticleTypes.FLAME, primaryTarget.getX(), primaryTarget.getY() + 0.5,
                primaryTarget.getZ(), 40, 0.4, 0.4, 0.4, 0.15);

        // Effekseer VFX: pure_white_flame run once upon explosion on grabbed target (scaled to 0.5, 2.5 blocks forward from caster)
        Vec3 effekPos = entity.position().add(0, entity.getEyeHeight() * 0.65, 0).add(lookVec.scale(2.5));
        AAALevel.addParticle(serverLevel, 64.0, PURE_WHITE_FLAME_FX.clone()
                .position(effekPos)
                .rotationFromForward(lookVec)
                .scale(0.5f));

        level.playSound(null, primaryTarget.getX(), primaryTarget.getY(), primaryTarget.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0f, 0.9f);
        level.playSound(null, primaryTarget.getX(), primaryTarget.getY(), primaryTarget.getZ(),
                SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 1.8f, 1.1f);

        // Sub-step 3: Phase 2 Explosion (Rectangular Box AoE delayed by 8 ticks) -
        // Length 20, Height 7, Width 7
        final Vec3 impactOrigin = primaryTarget.position();
        final Vec3 forwardDir = lookVec;
        final float phase2Damage = damage * 0.8F;
        final double boxLength = 20.0;
        final double boxWidth = 7.0;
        final double boxHeight = 7.0;

        serverLevel.getServer().tell(new TickTask(serverLevel.getServer().getTickCount() + 8, () -> {
            Vec3 f = forwardDir.normalize();
            Vec3 upRef = Math.abs(f.y) > 0.95 ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
            Vec3 right = f.cross(upRef).normalize();
            Vec3 up = right.cross(f).normalize();

            double halfWidth = boxWidth / 2.0;
            double halfHeight = boxHeight / 2.0;

            List<LivingEntity> boxTargets = serverLevel.getEntitiesOfClass(LivingEntity.class,
                    new AABB(impactOrigin, impactOrigin).inflate(boxLength + 3.0),
                    e -> e != entity && e.isAlive() && !e.isSpectator()
                            && !DamageSources.isFriendlyFireBetween(entity, e));

            for (LivingEntity targetInBox : boxTargets) {
                Vec3 targetCenter = targetInBox.getBoundingBox().getCenter();
                Vec3 toTarget = targetCenter.subtract(impactOrigin);
                double localForward = toTarget.dot(f);
                double localRight = toTarget.dot(right);
                double localUp = toTarget.dot(up);

                double extraW = targetInBox.getBbWidth() / 2.0;
                double extraH = targetInBox.getBbHeight() / 2.0;

                if (localForward >= -0.5 && localForward <= boxLength + 0.5
                        && Math.abs(localRight) <= (halfWidth + extraW)
                        && Math.abs(localUp) <= (halfHeight + extraH)) {
                    DamageSources.applyDamage(targetInBox, phase2Damage, getDamageSource(entity));
                    targetInBox.setRemainingFireTicks(100);

                    // Powerful directional knockback pushed outward along forward direction
                    targetInBox.setDeltaMovement(f.scale(1.8).add(0, 0.45, 0));
                    targetInBox.hurtMarked = true;
                }
            }

            // Spawn explosive burst and rectangular box fire particles (20x7x7)
            spawnBoxImpactParticles(serverLevel, impactOrigin.x, impactOrigin.y + 0.5, impactOrigin.z, forwardDir,
                    boxLength, boxWidth, boxHeight);
        }));

        // Sub-step 4: Caster Backfire Penalty
        float hpCost = entity.getHealth() * 0.20F;
        entity.setHealth(Math.max(1.0F, entity.getHealth() - hpCost));
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2)); // Slowness III for 10s
        entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0)); // Wither I for 5s
        entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 0)); // Weakness I for 30s

        serverLevel.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.0, entity.getZ(), 25, 0.3, 0.5,
                0.3, 0.05);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.WITHER_SHOOT,
                SoundSource.PLAYERS, 1.0f, 0.8f);

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /**
     * Spawns massive fiery explosion particles in a corridor extending forward
     * (length 20, width 7, height 7), using high-velocity rushing fire particles
     * (FIRE_EMITTER) based on directional rotation and spread vector physics.
     */
    public void spawnBoxImpactParticles(ServerLevel level, double x, double y, double z, Vec3 forwardDir,
            double length, double width, double height) {
        Vec3 origin = new Vec3(x, y, z);
        Vec3 rotation = forwardDir.normalize();
        Vec3 upRef = Math.abs(rotation.y) > 0.95 ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
        Vec3 right = rotation.cross(upRef).normalize();
        Vec3 up = right.cross(rotation).normalize();

        // 1. Epicenter Explosion Blast & Heavy Sounds
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 2, 0.3, 0.3, 0.3, 0);
        level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 12, 0.8, 0.5, 0.8, 0.1);
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.0f, 0.7f);
        level.playSound(null, x, y, z, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 2.5f, 0.8f);

        // 2. High-speed Forward-Floating Fire Surge (Floats & rushes forward 20 blocks,
        // expanding to 7 width x 7 height)
        // All particles are propelled forward from the launch point with velocity so
        // they visibly float and stream forward
        int totalParticles = 240;
        double angularness = 0.5; // (tan(0.5 / 3.0) * 20m = 3.33m radius -> 7m wide & 7m high at 20m)

        for (int i = 0; i < totalParticles; i++) {
            // Small offset in the launch nozzle area
            double forwardOffset = Utils.random.nextDouble() * 1.5;
            double sideOffset = (Utils.random.nextDouble() - 0.5) * 0.8;
            double upOffset = (Utils.random.nextDouble() - 0.5) * 0.8;

            Vec3 pPos = origin.add(rotation.scale(forwardOffset))
                    .add(right.scale(sideOffset))
                    .add(up.scale(upOffset));

            // Varied speeds from 0.8 to 2.5 blocks/tick so particles continuously float and
            // surge forward to 20 blocks
            double speed = 0.8 + Utils.random.nextDouble() * 1.7;

            Vec3 randomVec = new Vec3(
                    (Math.random() * 2.0 - 1.0) * angularness,
                    (Math.random() * 2.0 - 1.0) * angularness,
                    (Math.random() * 2.0 - 1.0) * angularness);

            Vec3 result = (rotation.scale(3.0).add(randomVec)).normalize().scale(speed);

            // FIRE_EMITTER flies forward along the result vector (creates the floating fire
            // wave effect)
            level.sendParticles(ParticleHelper.FIRE_EMITTER, pPos.x, pPos.y, pPos.z, 0, result.x, result.y, result.z,
                    1.0);

            if (i % 2 == 0) {
                // High-speed fiery sparks streaking ahead of the wave
                level.sendParticles(ParticleHelper.FIERY_SPARKS, pPos.x, pPos.y, pPos.z, 0, result.x * 1.25,
                        result.y * 1.25, result.z * 1.25, 1.0);
            }

            if (i % 3 == 0) {
                // Flame particles floating forward with the wave
                level.sendParticles(ParticleTypes.FLAME, pPos.x, pPos.y, pPos.z, 0, result.x * 0.9, result.y * 0.9,
                        result.z * 0.9, 1.0);
            }

            if (i % 4 == 0) {
                // Trailing embers floating forward
                level.sendParticles(ParticleHelper.EMBERS, pPos.x, pPos.y, pPos.z, 0, result.x * 0.65, result.y * 0.65,
                        result.z * 0.65, 1.0);
            }
        }
    }

    /**
     * Spawns forward-rushing fire burst particles matching the principle:
     * (rotation.scale(3).add(randomVec)).normalize().scale(speed)
     * rushing forward 20 blocks with height 7 and width 7.
     */
    public void spawnParticles(Level level, LivingEntity owner) {
        if (owner == null) {
            return;
        }
        Vec3 rotation = owner.getLookAngle().normalize();
        Vec3 pos = owner.position().add(rotation.scale(1.6));
        double x = pos.x;
        double y = pos.y + owner.getEyeHeight() * 0.9f;
        double z = pos.z;

        if (level instanceof ServerLevel serverLevel) {
            spawnBoxImpactParticles(serverLevel, x, y, z, rotation, 20.0, 7.0, 7.0);
        } else if (level.isClientSide) {
            for (int i = 0; i < 120; i++) {
                double offset = 0.25;
                double ox = Math.random() * 2 * offset - offset;
                double oy = Math.random() * 2 * offset - offset;
                double oz = Math.random() * 2 * offset - offset;

                double speed = 0.8 + Utils.random.nextDouble() * 1.7;
                double angularness = 0.5;
                Vec3 randomVec = new Vec3(
                        (Math.random() * 2.0 - 1.0) * angularness,
                        (Math.random() * 2.0 - 1.0) * angularness,
                        (Math.random() * 2.0 - 1.0) * angularness);
                Vec3 result = (rotation.scale(3.0).add(randomVec)).normalize().scale(speed);
                level.addParticle(ParticleHelper.FIRE_EMITTER, x + ox, y + oy, z + oz, result.x, result.y, result.z);
            }
        }
    }

    public void spawnConeImpactParticles(ServerLevel level, double x, double y, double z, Vec3 forwardDir,
            double range) {
        spawnBoxImpactParticles(level, x, y, z, forwardDir, range, 7.0, 7.0);
    }

    public void impactParticles(double x, double y, double z) {
        // Overload helper for impact particle integration
    }

    public float getDamage(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster);
    }
}
