package io.redspace.ironspell_more.entity.spells.resonant_knell;

import io.redspace.ironspell_more.registry.EntityRegistry;
import io.redspace.ironspell_more.spells.fire.ResonantKnellSpell;
import io.redspace.ironsspellbooks.entity.spells.AoeEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resonant Knell — Stationary/Following Fiery Gold & Red Dome AOE entity.
 * Centers directly on caster. Manages Open / Exploding / Inactive states.
 */
public class ResonantKnellDomeAoe extends AoeEntity {
    public static final int STATE_INACTIVE = 0;
    public static final int STATE_OPEN = 1;
    public static final int STATE_EXPLODING = 2;
    public static final int SHOCKWAVE_DURATION_TICKS = 24;

    private static final EntityDataAccessor<Integer> DATA_DOME_STATE =
            SynchedEntityData.defineId(ResonantKnellDomeAoe.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STAGE =
            SynchedEntityData.defineId(ResonantKnellDomeAoe.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SHOCKWAVE_TICK =
            SynchedEntityData.defineId(ResonantKnellDomeAoe.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SHOCKWAVE_RADIUS =
            SynchedEntityData.defineId(ResonantKnellDomeAoe.class, EntityDataSerializers.FLOAT);

    private static final Set<ResonantKnellDomeAoe> ACTIVE_DOMES =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Client-side tracking
    private int clientShockwaveStartTick = -100;
    private int clientDomeOpenStartTick = 0;
    private boolean discardAfterShockwave;

    public ResonantKnellDomeAoe(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.setRadius(8.0f);
    }

    public ResonantKnellDomeAoe(Level level) {
        this(EntityRegistry.RESONANT_KNELL_DOME.get(), level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_DOME_STATE, STATE_OPEN);
        this.entityData.define(DATA_STAGE, 1);
        this.entityData.define(DATA_SHOCKWAVE_TICK, -100);
        this.entityData.define(DATA_SHOCKWAVE_RADIUS, 15.0f);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (this.level().isClientSide()) {
            int state = this.entityData.get(DATA_DOME_STATE);
            if (key == DATA_DOME_STATE && state == STATE_OPEN) {
                this.clientDomeOpenStartTick = this.tickCount;
            }
            if ((key == DATA_SHOCKWAVE_TICK || key == DATA_DOME_STATE) && state == STATE_EXPLODING) {
                this.clientShockwaveStartTick = this.tickCount;
            }
        }
    }

    public static ResonantKnellDomeAoe getActiveDomeFor(LivingEntity caster) {
        if (caster == null) return null;
        ResonantKnellDomeAoe newestDome = null;
        for (ResonantKnellDomeAoe dome : ACTIVE_DOMES) {
            if (isActiveDomeFor(dome, caster)
                    && (newestDome == null || dome.getId() > newestDome.getId())) {
                newestDome = dome;
            }
        }
        return newestDome;
    }

    public static void discardActiveDomesFor(LivingEntity caster) {
        if (caster == null) return;
        for (ResonantKnellDomeAoe dome : ACTIVE_DOMES) {
            if (isActiveDomeFor(dome, caster)) {
                dome.discard();
            }
        }
    }

    public static void finishActiveDomesFor(LivingEntity caster) {
        if (caster == null) return;
        for (ResonantKnellDomeAoe dome : ACTIVE_DOMES) {
            if (isActiveDomeFor(dome, caster)) {
                dome.finishOrDiscard();
            }
        }
    }

    private static boolean isActiveDomeFor(ResonantKnellDomeAoe dome, LivingEntity caster) {
        if (dome.level() != caster.level() || dome.isRemoved() || !dome.isAlive()) {
            return false;
        }
        UUID casterId = caster.getUUID();
        Entity owner = dome.getOwner();
        return owner == caster || (owner != null && casterId.equals(owner.getUUID()));
    }

    public void activateDome(int stage) {
        this.entityData.set(DATA_STAGE, stage);
        this.entityData.set(DATA_DOME_STATE, STATE_OPEN);
        this.clientShockwaveStartTick = -100;
        if (this.level().isClientSide()) {
            this.clientDomeOpenStartTick = this.tickCount;
        }
    }

    public void triggerShockwave(float radius) {
        this.entityData.set(DATA_SHOCKWAVE_RADIUS, radius);
        this.entityData.set(DATA_SHOCKWAVE_TICK, this.tickCount);
        this.entityData.set(DATA_DOME_STATE, STATE_EXPLODING);
        if (this.level().isClientSide()) {
            this.clientShockwaveStartTick = this.tickCount;
        }
    }

    public void finishOrDiscard() {
        if (isExploding()) {
            this.discardAfterShockwave = true;
        } else {
            this.discard();
        }
    }

    public int getDomeState() {
        return this.entityData.get(DATA_DOME_STATE);
    }

    public boolean isDomeOpen() {
        return this.entityData.get(DATA_DOME_STATE) == STATE_OPEN;
    }

    public boolean isExploding() {
        return this.entityData.get(DATA_DOME_STATE) == STATE_EXPLODING;
    }

    public int getStage() {
        return this.entityData.get(DATA_STAGE);
    }

    public int getShockwaveStartTick() {
        if (this.level().isClientSide()) {
            if (this.clientShockwaveStartTick < 0) {
                this.clientShockwaveStartTick = this.tickCount;
            }
            return this.clientShockwaveStartTick;
        }
        return this.entityData.get(DATA_SHOCKWAVE_TICK);
    }

    public int getDomeOpenStartTick() {
        return this.clientDomeOpenStartTick;
    }

    public float getShockwaveRadius() {
        return this.entityData.get(DATA_SHOCKWAVE_RADIUS);
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
        ACTIVE_DOMES.add(this);
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        ACTIVE_DOMES.remove(this);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isRemoved()) {
            return;
        }

        Entity owner = this.getOwner();
        if (owner instanceof LivingEntity caster && caster.isAlive()) {
            // Center strictly on caster position
            this.setPos(caster.getX(), caster.getY(), caster.getZ());
            this.setDeltaMovement(Vec3.ZERO);
        } else if (!this.level().isClientSide && this.tickCount > 2) {
            this.discard();
            return;
        }

        int state = getDomeState();

        if (!this.level().isClientSide) {
            // Continuously buff allies inside the open dome
            if (state == STATE_OPEN && this.tickCount % 10 == 0) {
                buffAlliesInDome();
            }

            // Server side state transitions
            if (state == STATE_EXPLODING) {
                int startTick = this.entityData.get(DATA_SHOCKWAVE_TICK);
                int elapsed = this.tickCount - startTick;
                if (elapsed >= SHOCKWAVE_DURATION_TICKS) {
                    if (this.discardAfterShockwave || getStage() >= 3) {
                        this.discard();
                        return;
                    } else {
                        this.entityData.set(DATA_DOME_STATE, STATE_INACTIVE);
                    }
                }
            }
        } else {
            // Client side particles
            if (state == STATE_OPEN && this.tickCount % 3 == 0) {
                // Subtle soul fire wisps & flame around talismans
                double angle = this.random.nextDouble() * Math.PI * 2.0;
                double r = 8.1 + this.random.nextDouble() * 0.3;
                double px = this.getX() + Math.cos(angle) * r;
                double pz = this.getZ() + Math.sin(angle) * r;
                double py = this.getY() + 0.5 + this.random.nextDouble() * 5.5;

                if (this.random.nextBoolean()) {
                    this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 0.0, 0.015, 0.0);
                } else {
                    this.level().addParticle(ParticleTypes.FLAME, px, py, pz, 0.0, 0.01, 0.0);
                }
            } else if (state == STATE_EXPLODING && this.tickCount % 2 == 0) {
                double blastAngle = this.random.nextDouble() * Math.PI * 2.0;
                double blastR = 2.0 + this.random.nextDouble() * (getShockwaveRadius() * 0.8);
                this.level().addParticle(ParticleTypes.LAVA,
                        this.getX() + Math.cos(blastAngle) * blastR,
                        this.getY() + 0.2 + this.random.nextDouble() * 2.0,
                        this.getZ() + Math.sin(blastAngle) * blastR,
                        0.0, 0.05, 0.0);
            }
        }
    }

    private void buffAlliesInDome() {
        Entity ownerEntity = this.getOwner();
        if (!(ownerEntity instanceof LivingEntity caster) || !caster.isAlive()) {
            return;
        }

        float radius = this.getRadius();
        AABB aabb = this.getBoundingBox().inflate(radius);
        List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class, aabb,
                e -> e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand)
                        && ResonantKnellSpell.isAlly(caster, e)
                        && this.distanceToSqr(e) <= (double) (radius * radius));

        for (LivingEntity ally : list) {
            ResonantKnellSpell.applyBarrierBuffs(ally, 100);
        }
    }

    @Override
    public void applyEffect(LivingEntity target) {
    }

    @Override
    public float getParticleCount() {
        return 0.0f;
    }

    @Override
    public Optional<ParticleOptions> getParticle() {
        return Optional.empty();
    }
}
