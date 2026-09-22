package io.redspace.ironspell_more.entity.spells.rapturous_bloom;

import io.redspace.ironspell_more.client.particle.ShockwaveParticleOptionCustom;
import io.redspace.ironspell_more.registry.EntityRegistry;
import io.redspace.ironspell_more.registry.ParticleRegistry;
import io.redspace.ironspell_more.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.entity.spells.AoeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.joml.Vector3f;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RapturousBloomEntity extends AoeEntity implements AntiMagicSusceptible {
    public static final int PHASE_RIPPLE = 0;
    public static final int PHASE_BLOOM = 1;
    public static final int PHASE_BURST = 2;

    public static final int RIPPLE_DURATION_TICKS = 40; // 2 seconds
    public static final int BLOOM_DURATION_TICKS = 80;   // 4 seconds bloom
    public static final int BURST_TICK = RIPPLE_DURATION_TICKS + BLOOM_DURATION_TICKS; // 120 ticks
    public static final int BURST_VISUAL_TICKS = 10;    // 0.5s visual shell to render burst animation
    public static final int TOTAL_LIFETIME_TICKS = BURST_TICK + BURST_VISUAL_TICKS;

    public static final float DEFAULT_RADIUS = 3.0F;
    public static final float MIN_NON_OVERLAP_DISTANCE = DEFAULT_RADIUS * 2.0F; // 6.0 blocks

    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(RapturousBloomEntity.class, EntityDataSerializers.INT);

    private static final Set<RapturousBloomEntity> ACTIVE_BLOOMS =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private UUID casterUUID;
    private UUID lockedTargetUUID;
    private float burstDamage = 24.0F;
    private int spellLevel = 1;
    private boolean burstHandled = false;

    public RapturousBloomEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.setCircular();
        this.setRadius(DEFAULT_RADIUS);
        this.setDuration(TOTAL_LIFETIME_TICKS + 5);
        this.reapplicationDelay = 20;
        this.setNoGravity(true);
    }

    public RapturousBloomEntity(Level level) {
        this(EntityRegistry.RAPTUROUS_BLOOM.get(), level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, PHASE_RIPPLE);
    }

    public int getPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    public void setPhase(int phase) {
        this.entityData.set(DATA_PHASE, phase);
    }

    public void setBurstDamage(float damage) {
        this.burstDamage = damage;
    }

    public float getBurstDamage() {
        return this.burstDamage;
    }

    public void setSpellLevel(int level) {
        this.spellLevel = level;
    }

    public int getSpellLevel() {
        return this.spellLevel;
    }

    public void setCasterUUID(@Nullable UUID casterUUID) {
        this.casterUUID = casterUUID;
    }

    @Nullable
    public UUID getCasterUUID() {
        return this.casterUUID;
    }

    public void setLockedTargetUUID(@Nullable UUID targetUUID) {
        this.lockedTargetUUID = targetUUID;
    }

    @Nullable
    public UUID getLockedTargetUUID() {
        return this.lockedTargetUUID;
    }

    public boolean isBloomActive() {
        return this.isAlive() && !this.isRemoved() && !this.burstHandled && getPhase() != PHASE_BURST;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!this.level().isClientSide) {
            ACTIVE_BLOOMS.add(this);
        }
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (!this.level().isClientSide) {
            ACTIVE_BLOOMS.remove(this);
        }
    }

    public static int getActiveBloomCount(UUID casterId) {
        if (casterId == null) return 0;
        int count = 0;
        for (RapturousBloomEntity bloom : ACTIVE_BLOOMS) {
            if (bloom.isBloomActive()) {
                UUID ownerId = bloom.getCasterUUID();
                if (ownerId == null && bloom.getOwner() != null) {
                    ownerId = bloom.getOwner().getUUID();
                }
                if (casterId.equals(ownerId)) {
                    count++;
                }
            }
        }
        return count;
    }

    public static boolean isTargetOrAreaOccupied(Level level, @Nullable UUID targetUUID, Vec3 targetPos, float minDistance) {
        float minDistanceSqr = minDistance * minDistance;
        for (RapturousBloomEntity bloom : ACTIVE_BLOOMS) {
            if (bloom.level() == level && bloom.isBloomActive()) {
                if (targetUUID != null && targetUUID.equals(bloom.getLockedTargetUUID())) {
                    return true;
                }
                double dx = bloom.getX() - targetPos.x;
                double dz = bloom.getZ() - targetPos.z;
                if (dx * dx + dz * dz < minDistanceSqr && Math.abs(bloom.getY() - targetPos.y) < 4.0) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    public static Vec3 findGroundPosition(Level level, Vec3 pos, int maxSearchDown) {
        BlockPos startPos = BlockPos.containing(pos);
        Vec3 from = new Vec3(pos.x, pos.y + 0.5, pos.z);
        Vec3 to = new Vec3(pos.x, pos.y - maxSearchDown, pos.z);
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getLocation();
        }
        // Fallback: check block collision steps
        for (int y = 0; y <= maxSearchDown; y++) {
            BlockPos checkPos = startPos.below(y);
            if (!level.getBlockState(checkPos).getCollisionShape(level, checkPos).isEmpty()) {
                double topY = checkPos.getY() + level.getBlockState(checkPos).getCollisionShape(level, checkPos).max(net.minecraft.core.Direction.Axis.Y);
                return new Vec3(pos.x, topY, pos.z);
            }
        }
        return null;
    }

    public boolean isValidEnemyTarget(LivingEntity target) {
        if (target == null || !target.isAlive() || target.isRemoved() || target.isSpectator()) {
            return false;
        }
        Entity owner = getOwner();
        if (owner instanceof LivingEntity livingOwner) {
            if (target == livingOwner || target.isAlliedTo(livingOwner) || DamageSources.isFriendlyFireBetween(livingOwner, target)) {
                return false;
            }
        } else if (this.casterUUID != null) {
            if (target.getUUID().equals(this.casterUUID)) {
                return false;
            }
            if (target.getTeam() != null && this.level().getScoreboard() != null) {
                PlayerTeam casterTeam = this.level().getScoreboard().getPlayersTeam(this.casterUUID.toString());
                if (casterTeam != null && casterTeam.isAlliedTo(target.getTeam())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide) {
            // Lifecycle transitions
            if (this.tickCount < RIPPLE_DURATION_TICKS) {
                if (getPhase() != PHASE_RIPPLE) {
                    setPhase(PHASE_RIPPLE);
                }
            } else if (this.tickCount < BURST_TICK) {
                if (getPhase() != PHASE_BLOOM) {
                    setPhase(PHASE_BLOOM);
                    this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.FLOWERING_AZALEA_PLACE, SoundSource.PLAYERS, 1.2F, 0.9F);
                    this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHERRY_SAPLING_PLACE, SoundSource.PLAYERS, 1.0F, 1.1F);
                }

                // Authoritative periodic debuffs every 20 ticks (1.0s)
                if ((this.tickCount - RIPPLE_DURATION_TICKS) % 20 == 0) {
                    applyPeriodicDebuffs();
                }
            } else if (this.tickCount == BURST_TICK) {
                if (!burstHandled) {
                    triggerBurst();
                }
            } else if (this.tickCount >= TOTAL_LIFETIME_TICKS) {
                this.discard();
                return;
            }
        }

        super.tick();
    }

    private void applyPeriodicDebuffs() {
        float radius = getRadius();
        double radiusSqr = radius * radius;
        AABB aoeBox = new AABB(
                getX() - radius, getY() - 2.5, getZ() - radius,
                getX() + radius, getY() + 2.5, getZ() + radius
        );

        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, aoeBox, this::isValidEnemyTarget);
        for (LivingEntity target : targets) {
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            if (dx * dx + dz * dz <= radiusSqr) {
                // Poison I (8s / 160 ticks)
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 0, false, true, true));
                // Wither I (8s / 160 ticks)
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, 0, false, true, true));
                // Slowness III (5s / 100 ticks, amplifier 2)
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2, false, true, true));
            }
        }
    }

    private void triggerBurst() {
        this.burstHandled = true;
        setPhase(PHASE_BURST);

        float radius = getRadius();
        double radiusSqr = radius * radius;
        Entity owner = getOwner();
        LivingEntity caster = owner instanceof LivingEntity living ? living : null;

        AABB hitBox = new AABB(
                getX() - radius, getY() - 2.5, getZ() - radius,
                getX() + radius, getY() + 2.5, getZ() + radius
        );

        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, hitBox, this::isValidEnemyTarget);
        for (LivingEntity target : targets) {
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            if (dx * dx + dz * dz <= radiusSqr) {
                DamageSources.applyDamage(target, this.burstDamage,
                        SpellRegistry.RAPTUROUS_BLOOM_SPELL.get().getDamageSource(this, caster));
            }
        }

        // Burst audio
        this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2F, 1.3F);
        this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHERRY_LEAVES_BREAK, SoundSource.PLAYERS, 2.0F, 0.8F);

        // Burst VFX on server
        if (this.level() instanceof ServerLevel serverLevel) {
            Vector3f shockwaveColor = new Vector3f(0.890F, 0.071F, 0.275F); // Bright Red #E31246
            Vector3f direction = new Vector3f(0, 1, 0);

            for (ServerPlayer player : serverLevel.players()) {
                serverLevel.sendParticles(player,
                        new ShockwaveParticleOptionCustom(shockwaveColor, radius * 1.3F, true, direction),
                        true, getX(), getY() + 0.15, getZ(), 1, 0, 0, 0, 0);
            }

            // Custom plum petals and spores flying outwards
            for (int i = 0; i < 40; i++) {
                double speed = 0.25 + this.random.nextDouble() * 0.35;
                double angle = this.random.nextDouble() * Math.PI * 2.0;
                double vx = Math.cos(angle) * speed;
                double vz = Math.sin(angle) * speed;
                double vy = 0.10 + this.random.nextDouble() * 0.25;

                // count=0 preserves the supplied directional velocity exactly.
                serverLevel.sendParticles(ParticleRegistry.RED_PLUM.get(),
                        getX(), getY() + 0.4, getZ(),
                        0, vx, vy, vz, 1.0);
                if ((i & 1) == 0) {
                    serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE,
                            getX(), getY() + 0.4, getZ(),
                            1, vx * 0.8, vy * 0.8, vz * 0.8, 0.1);
                }
            }
        }
    }

    @Override
    public void applyEffect(LivingEntity target) {
        // Controlled directly by applyPeriodicDebuffs() in tick() for exact cylindrical bounds
    }

    @Override
    protected boolean canHitEntity(Entity pTarget) {
        return false; // Inherited AoeEntity hit-testing bypassed in favor of explicit cylindrical sweep
    }

    @Override
    public void ambientParticles() {
        if (!this.level().isClientSide) {
            return;
        }

        int phase = getPhase();
        float radius = getRadius();

        if (phase == PHASE_RIPPLE) {
            // Water droplet & ripple particles around ground ring
            int count = 3;
            for (int i = 0; i < count; i++) {
                float r = 0.4F + this.random.nextFloat() * (radius * 0.9F);
                float angle = this.random.nextFloat() * ((float) Math.PI * 2.0F);
                double px = getX() + r * Mth.cos(angle);
                double py = getY() + 0.05;
                double pz = getZ() + r * Mth.sin(angle);

                this.level().addParticle(ParticleTypes.SPLASH, px, py, pz, 0, 0.02, 0);
                if (this.random.nextFloat() < 0.4F) {
                    this.level().addParticle(ParticleTypes.FALLING_WATER, px, py + 0.2, pz, 0, 0, 0);
                }
            }

            // Begin with a compact orbit. Its spawn radius and outward drift
            // expand gradually as the telegraph approaches the bloom phase.
            if ((this.tickCount & 1) == 0) {
                float lifecycleProgress = Mth.clamp(
                        this.tickCount / (float) BURST_TICK, 0.0F, 1.0F);
                spawnSwirlingPlumPetals(radius, lifecycleProgress, 1);
            }
        } else if (phase == PHASE_BLOOM) {
            // Continue the same orbit while progressively widening it toward the
            // spell perimeter. Vanilla blossom/cherry sprites are not used.
            int count = 2 + (this.tickCount % 3 == 0 ? 1 : 0);
            float lifecycleProgress = Mth.clamp(
                    this.tickCount / (float) BURST_TICK, 0.0F, 1.0F);
            spawnSwirlingPlumPetals(radius, lifecycleProgress, count);

            // Nectar remains as a restrained fragrance accent.
            if (this.random.nextFloat() < 0.35F) {
                this.level().addParticle(ParticleTypes.FALLING_NECTAR,
                        getX() + (this.random.nextDouble() - 0.5) * 0.6,
                        getY() + 0.8 + this.random.nextDouble() * 0.4,
                        getZ() + (this.random.nextDouble() - 0.5) * 0.6,
                        0, 0, 0);
            }
        }
    }

    private void spawnSwirlingPlumPetals(float radius, float lifecycleProgress, int count) {
        float maxSpawnRadius = Mth.lerp(lifecycleProgress, 0.20F, radius * 0.82F);
        double baseRadialSpeed = Mth.lerp(lifecycleProgress, 0.004F, 0.028F);
        double baseHeight = Mth.lerp(lifecycleProgress, 0.18F, 0.65F);

        for (int i = 0; i < count; i++) {
            float r = 0.06F + this.random.nextFloat() * maxSpawnRadius;
            float angle = this.random.nextFloat() * Mth.TWO_PI;
            double px = getX() + r * Mth.cos(angle);
            double py = getY() + baseHeight + this.random.nextDouble() * 0.35;
            double pz = getZ() + r * Mth.sin(angle);

            double radialSpeed = baseRadialSpeed + this.random.nextDouble() * 0.010D;
            double tangentSpeed = 0.048D + this.random.nextDouble() * 0.024D;
            double vx = Mth.cos(angle) * radialSpeed - Mth.sin(angle) * tangentSpeed;
            double vz = Mth.sin(angle) * radialSpeed + Mth.cos(angle) * tangentSpeed;
            double vy = 0.018D + lifecycleProgress * 0.022D + this.random.nextDouble() * 0.018D;
            this.level().addParticle(ParticleRegistry.RED_PLUM.get(), px, py, pz, vx, vy, vz);
        }
    }

    @Override
    public float getParticleCount() {
        return 1.0F;
    }

    @Override
    public java.util.Optional<net.minecraft.core.particles.ParticleOptions> getParticle() {
        return java.util.Optional.empty();
    }

    @Override
    public void onAntiMagic(MagicData magicData) {
        this.burstHandled = true; // Prevent burst damage
        this.discard();
    }


    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.casterUUID != null) {
            tag.putUUID("CasterUUID", this.casterUUID);
        }
        if (this.lockedTargetUUID != null) {
            tag.putUUID("LockedTargetUUID", this.lockedTargetUUID);
        }
        tag.putFloat("BurstDamage", this.burstDamage);
        tag.putInt("SpellLevel", this.spellLevel);
        tag.putInt("Phase", getPhase());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("CasterUUID")) {
            this.casterUUID = tag.getUUID("CasterUUID");
        }
        if (tag.hasUUID("LockedTargetUUID")) {
            this.lockedTargetUUID = tag.getUUID("LockedTargetUUID");
        }
        this.burstDamage = tag.getFloat("BurstDamage");
        this.spellLevel = tag.getInt("SpellLevel");
        setPhase(tag.getInt("Phase"));
    }
}
