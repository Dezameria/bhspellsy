package io.redspace.ironspell_more.entity.spells.crimson_thornbind;

import io.redspace.ironspell_more.registry.EntityRegistry;
import io.redspace.ironspell_more.registry.SpellRegistry;
import io.redspace.ironspell_more.spells.fire.CrimsonThornbindSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.entity.spells.root.PreventDismount;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CrimsonRootEntity extends LivingEntity implements GeoEntity, PreventDismount, AntiMagicSusceptible {
    public enum RootMode {
        BIND,
        PATH
    }

    public static final float DEFAULT_PATH_SCALE = 1.85F;
    public static final float DEFAULT_BIND_SCALE = 1.8F;
    public static final int STEP_INTERVAL = 3;
    public static final int MAX_PATH_SEGMENTS = 20;
    public static final float SEGMENT_SPACING = 1.35F;
    public static final int PATH_HOLD_TICKS = 6;
    public static final int PATH_LINGER_TICKS = 60; // 3 seconds lingering hazard trail
    public static final int DISSOLVE_INTERVAL_TICKS = 2; // 2 ticks between each segment dissolving

    private static final EntityDataAccessor<Integer> DATA_WARMUP = SynchedEntityData.defineId(CrimsonRootEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SCALE = SynchedEntityData.defineId(CrimsonRootEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(CrimsonRootEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_REMOVAL_GAME_TIME = SynchedEntityData.defineId(CrimsonRootEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> DATA_YAW = SynchedEntityData.defineId(CrimsonRootEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PITCH = SynchedEntityData.defineId(CrimsonRootEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_IS_ORIGIN = SynchedEntityData.defineId(CrimsonRootEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private LivingEntity owner;
    @Nullable
    private UUID ownerUUID;
    @Nullable
    private UUID targetUUID;
    private int duration = 200;
    private float rendDamage = 6.0F;
    private float damage = 12.0F;
    private boolean playSound = true;
    private boolean bindActivated;
    @Nullable
    private LivingEntity target;
    private int activeTicks = 0;

    private boolean isCoordinator = false;
    private boolean coordinatorActive = false;
    private int stepTimer = STEP_INTERVAL;
    private int currentSegmentIndex = 1;
    private int maxSegments = MAX_PATH_SEGMENTS;
    private Vec3 currentSegmentTail = Vec3.ZERO;
    private Vec3 currentTip = Vec3.ZERO;
    private final List<UUID> spawnedSegmentUUIDs = new ArrayList<>();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final RawAnimation EMERGE_ANIMATION = RawAnimation.begin().thenPlayAndHold("emerge");

    private final AnimationController<CrimsonRootEntity> controller = new AnimationController<>(this, "crimson_root_controller", 0, this::animationPredicate);

    public CrimsonRootEntity(EntityType<? extends CrimsonRootEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public CrimsonRootEntity(Level level, LivingEntity owner) {
        this(EntityRegistry.CRIMSON_ROOT.get(), level);
        setOwner(owner);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_WARMUP, 0);
        this.entityData.define(DATA_SCALE, DEFAULT_BIND_SCALE);
        this.entityData.define(DATA_MODE, RootMode.BIND.ordinal());
        this.entityData.define(DATA_REMOVAL_GAME_TIME, -1L);
        this.entityData.define(DATA_YAW, 0.0F);
        this.entityData.define(DATA_PITCH, 0.0F);
        this.entityData.define(DATA_IS_ORIGIN, false);
    }

    public boolean isOrigin() {
        return this.entityData.get(DATA_IS_ORIGIN);
    }

    public void setIsOrigin(boolean origin) {
        this.entityData.set(DATA_IS_ORIGIN, origin);
    }

    public float getPathYaw() {
        return this.entityData.get(DATA_YAW);
    }

    public void setPathYaw(float yaw) {
        this.entityData.set(DATA_YAW, yaw);
        this.setYRot(yaw);
        this.yRotO = yaw;
    }

    public float getPathPitch() {
        return this.entityData.get(DATA_PITCH);
    }

    public void setPathPitch(float pitch) {
        this.entityData.set(DATA_PITCH, pitch);
        this.setXRot(pitch);
        this.xRotO = pitch;
    }

    public float getPitch() {
        return getPathPitch();
    }

    public void setPitch(float pitch) {
        setPathPitch(pitch);
    }

    public int getWarmup() {
        return this.entityData.get(DATA_WARMUP);
    }

    public void setWarmup(int warmup) {
        this.entityData.set(DATA_WARMUP, warmup);
    }

    public float getBaseScale() {
        return this.entityData.get(DATA_SCALE);
    }

    public void setBaseScale(float scale) {
        this.entityData.set(DATA_SCALE, scale);
    }

    public RootMode getMode() {
        int ordinal = Mth.clamp(this.entityData.get(DATA_MODE), 0, RootMode.values().length - 1);
        return RootMode.values()[ordinal];
    }

    public void setMode(RootMode mode) {
        this.entityData.set(DATA_MODE, mode.ordinal());
        this.refreshDimensions();
    }

    public boolean isPathMode() {
        return getMode() == RootMode.PATH;
    }

    public boolean isBindMode() {
        return getMode() == RootMode.BIND;
    }

    public void setRemovalGameTime(long removalGameTime) {
        this.entityData.set(DATA_REMOVAL_GAME_TIME, removalGameTime);
    }

    public long getRemovalGameTime() {
        return this.entityData.get(DATA_REMOVAL_GAME_TIME);
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getDamage() {
        return this.damage;
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
        for (int y = 0; y <= maxSearchDown; y++) {
            BlockPos checkPos = startPos.below(y);
            if (!level.getBlockState(checkPos).getCollisionShape(level, checkPos).isEmpty()) {
                double topY = checkPos.getY() + level.getBlockState(checkPos).getCollisionShape(level, checkPos).max(net.minecraft.core.Direction.Axis.Y);
                return new Vec3(pos.x, topY, pos.z);
            }
        }
        return null;
    }

    @Override
    public float getScale() {
        return !isBindMode() || target == null ? getBaseScale() : target.getScale() * getBaseScale();
    }

    public LivingEntity getTarget() {
        return this.target;
    }

    public void setTarget(LivingEntity target) {
        this.target = target;
        this.targetUUID = target.getUUID();
    }

    public void setRendDamage(float rendDamage) {
        this.rendDamage = rendDamage;
    }

    public float getRendDamage() {
        return this.rendDamage;
    }

    @Override
    public boolean canCollideWith(@NotNull Entity pEntity) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected void doPush(@NotNull Entity pEntity) {
    }

    @Override
    public void push(@NotNull Entity pEntity) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean dismountsUnderwater() {
        return false;
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.0D;
    }

    @Override
    public boolean shouldRiderFaceForward(@NotNull Player player) {
        return false;
    }

    @Override
    public EntityDimensions getDimensions(Pose pPose) {
        if (isPathMode()) {
            return EntityDimensions.fixed(0.35F, 0.35F);
        }
        float scale = getBaseScale();
        var rooted = getFirstPassenger();
        if (rooted != null) {
            return EntityDimensions.fixed(Math.max(1.0F * scale, rooted.getBbWidth() * 1.25F), 1.0F * scale);
        }
        return EntityDimensions.fixed(1.0F * scale, 0.9F * scale);
    }

    @Override
    public boolean canRiderInteract() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide && isPathMode()) {
            long removalGameTime = getRemovalGameTime();
            if (removalGameTime >= 0L && level().getGameTime() >= removalGameTime) {
                removeRoot();
                return;
            }
            if (this.tickCount % 2 == 0) {
                checkHazardSnare();
            }
        }

        int currentWarmup = getWarmup();
        if (currentWarmup > 0) {
            if (!level().isClientSide) {
                setWarmup(currentWarmup - 1);
            }
            return;
        }

        if (playSound) {
            this.refreshDimensions();
            playSound(SoundRegistry.ROOT_EMERGE.get(), isPathMode() ? 0.55F : 1.5F,
                    isPathMode() ? 1.15F + random.nextFloat() * 0.15F : 0.9F + random.nextFloat() * 0.2F);
            playSound = false;
        }

        if (!level().isClientSide) {
            if (isCoordinator && coordinatorActive) {
                this.stepTimer--;
                if (this.stepTimer <= 0) {
                    this.stepTimer = STEP_INTERVAL;
                    performPropagationStep();
                }
            }

            if (isBindMode() && !bindActivated) {
                activateBind();
            }

            if (isBindMode() && bindActivated && this.target == null) {
                resolveTarget();
            }
            if (isBindMode() && (this.tickCount >= this.duration || (bindActivated
                    && (this.target == null || this.target.isDeadOrDying() || this.target.isRemoved()
                    || this.target.getVehicle() != this)))) {
                this.removeRoot();
            }
        } else {
            if (isPathMode()) {
                clientPathParticles();
            } else if (activeTicks < 15) {
                clientDiggingParticles(this);
            }
            activeTicks++;
        }
    }

    public void initCoordinator(Vec3 startPos, Vec3 initialDirection, int maxSegments, float damage, float rendDamage) {
        this.isCoordinator = true;
        this.coordinatorActive = true;
        this.stepTimer = STEP_INTERVAL;
        this.currentSegmentIndex = 1;
        this.maxSegments = maxSegments;
        Vec3 direction = initialDirection.lengthSqr() < 1e-4
                ? Vec3.directionFromRotation(getXRot(), getYRot())
                : initialDirection.normalize();
        this.currentSegmentTail = startPos;
        this.currentTip = startPos.add(direction.scale(SEGMENT_SPACING));
        this.damage = damage;
        this.rendDamage = rendDamage;
        this.spawnedSegmentUUIDs.clear();
        this.spawnedSegmentUUIDs.add(this.getUUID());
        this.setIsOrigin(true);
        long fallbackRemoval = level().getGameTime() + ((long) maxSegments * STEP_INTERVAL)
                + PATH_LINGER_TICKS + ((long) maxSegments * DISSOLVE_INTERVAL_TICKS) + 80L;
        this.setRemovalGameTime(fallbackRemoval);
    }

    private void performPropagationStep() {
        LivingEntity rootOwner = getOwner();
        if (rootOwner == null || !rootOwner.isAlive() || rootOwner.isRemoved()) {
            terminatePropagationAndScheduleDissolve();
            return;
        }

        // Resolve the segment that has just finished growing before creating the next one.
        BlockHitResult blockHit = level().clip(new ClipContext(currentSegmentTail, currentTip,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, rootOwner));
        Vec3 segmentDirection = currentTip.subtract(currentSegmentTail).normalize();
        Vec3 clippedEnd = blockHit.getType() == HitResult.Type.BLOCK
                ? blockHit.getLocation().subtract(segmentDirection.scale(0.1D))
                : currentTip;

        CrimsonThornbindSpell.TargetHit targetHit = CrimsonThornbindSpell.findTargetAlongSegment(
                level(), rootOwner, currentSegmentTail, clippedEnd);
        if (targetHit != null) {
            spawnBindRootOnTarget(targetHit.target());
            terminatePropagationAndScheduleDissolve();
            return;
        }

        if (blockHit.getType() == HitResult.Type.BLOCK || currentSegmentIndex >= maxSegments) {
            terminatePropagationAndScheduleDissolve();
            return;
        }

        Vec3 lookDir = rootOwner.getLookAngle().normalize();
        if (lookDir.lengthSqr() < 1e-4) {
            float yawRad = rootOwner.getYRot() * Mth.DEG_TO_RAD;
            float pitchRad = rootOwner.getXRot() * Mth.DEG_TO_RAD;
            lookDir = new Vec3(
                    -Mth.sin(yawRad) * Mth.cos(pitchRad),
                    -Mth.sin(pitchRad),
                    Mth.cos(yawRad) * Mth.cos(pitchRad)
            ).normalize();
        }

        Vec3 nextTail = currentTip;
        Vec3 nextTip = nextTail.add(lookDir.scale(SEGMENT_SPACING));
        Vec3 stepVec = nextTip.subtract(nextTail).normalize();
        double horizDist = Math.sqrt(stepVec.x * stepVec.x + stepVec.z * stepVec.z);
        float stepYaw = horizDist > 1e-4
                ? (float) (Mth.atan2(-stepVec.x, stepVec.z) * (180.0D / Math.PI))
                : rootOwner.getYRot();
        float stepPitch = (float) (-Mth.atan2(stepVec.y, horizDist) * (180.0D / Math.PI));

        CrimsonRootEntity nextSegment = new CrimsonRootEntity(level(), rootOwner);
        nextSegment.setMode(RootMode.PATH);
        nextSegment.moveTo(nextTail.x, nextTail.y, nextTail.z, stepYaw, stepPitch);
        nextSegment.setPathYaw(stepYaw);
        nextSegment.setPathPitch(stepPitch);
        nextSegment.setWarmup(0);
        nextSegment.setBaseScale(DEFAULT_PATH_SCALE);
        nextSegment.setIsOrigin(false);
        nextSegment.setDamage(this.damage);
        nextSegment.setRendDamage(this.rendDamage);
        long fallbackRemoval = level().getGameTime() + ((long) (maxSegments - currentSegmentIndex + 1) * STEP_INTERVAL)
                + PATH_LINGER_TICKS + ((long) maxSegments * DISSOLVE_INTERVAL_TICKS) + 80L;
        nextSegment.setRemovalGameTime(fallbackRemoval);
        level().addFreshEntity(nextSegment);

        spawnedSegmentUUIDs.add(nextSegment.getUUID());
        currentSegmentTail = nextTail;
        currentTip = nextTip;
        currentSegmentIndex++;

        level().playSound(null, nextTail.x, nextTail.y, nextTail.z,
                SoundRegistry.ROOT_EMERGE.get(), SoundSource.PLAYERS, 0.6F, 1.15F + random.nextFloat() * 0.15F);
    }

    public void spawnBindRootOnTarget(LivingEntity hitTarget) {
        LivingEntity rootOwner = getOwner();
        double groundY = CrimsonThornbindSpell.findGroundHeight(level(), hitTarget.getX(), hitTarget.getY(), hitTarget.getZ());
        CrimsonRootEntity bindRoot = new CrimsonRootEntity(level(), rootOwner);
        bindRoot.setMode(RootMode.BIND);
        bindRoot.moveTo(hitTarget.getX(), groundY, hitTarget.getZ(), hitTarget.getYRot(), 0.0F);
        bindRoot.setTarget(hitTarget);
        bindRoot.setDamage(this.damage);
        bindRoot.setRendDamage(this.rendDamage);
        bindRoot.setWarmup(0);
        bindRoot.setBaseScale(DEFAULT_BIND_SCALE);
        level().addFreshEntity(bindRoot);

        level().playSound(null, hitTarget.getX(), hitTarget.getY(), hitTarget.getZ(),
                SoundRegistry.ROOT_EMERGE.get(), SoundSource.PLAYERS, 1.2F, 0.8F);
        level().playSound(null, hitTarget.getX(), hitTarget.getY(), hitTarget.getZ(),
                SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.PLAYERS, 1.2F, 0.8F);
    }

    private void checkHazardSnare() {
        LivingEntity rootOwner = getOwner();
        if (rootOwner == null || !rootOwner.isAlive() || rootOwner.isRemoved()) {
            return;
        }
        AABB box = this.getBoundingBox().inflate(0.65D, 0.5D, 0.65D);
        List<LivingEntity> victims = level().getEntitiesOfClass(LivingEntity.class, box,
                e -> CrimsonThornbindSpell.isValidTarget(rootOwner, e)
                        && !(e.getVehicle() instanceof CrimsonRootEntity));
        for (LivingEntity victim : victims) {
            if (victim.getVehicle() instanceof CrimsonRootEntity) {
                continue;
            }
            spawnBindRootOnTarget(victim);
        }
    }

    public void terminatePropagationAndScheduleDissolve() {
        this.coordinatorActive = false;
        long baseDissolveTime = level().getGameTime() + PATH_LINGER_TICKS;
        if (level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < spawnedSegmentUUIDs.size(); i++) {
                UUID uuid = spawnedSegmentUUIDs.get(i);
                Entity entity = serverLevel.getEntity(uuid);
                if (entity instanceof CrimsonRootEntity root && root.isPathMode()) {
                    long segmentRemoval = baseDissolveTime + ((long) i * DISSOLVE_INTERVAL_TICKS);
                    root.setRemovalGameTime(segmentRemoval);
                }
            }
        } else {
            this.setRemovalGameTime(baseDissolveTime);
        }
    }

    public void terminatePropagation() {
        terminatePropagationAndScheduleDissolve();
    }

    private void activateBind() {
        bindActivated = true;
        LivingEntity candidate = resolveTarget();
        LivingEntity rootOwner = getOwner();
        if (candidate == null || rootOwner == null || !candidate.isAlive() || candidate.isRemoved()
                || candidate.isSpectator() || candidate.isAlliedTo(rootOwner)
                || DamageSources.isFriendlyFireBetween(rootOwner, candidate)
                || candidate.distanceToSqr(this) > 25.0D) {
            removeRoot();
            return;
        }

        DamageSources.applyDamage(candidate, this.damage,
                SpellRegistry.CRIMSON_THORNBIND_SPELL.get().getDamageSource(this, rootOwner));

        if (!candidate.isAlive() || candidate.getType().is(ModTags.CANT_ROOT)) {
            removeRoot();
            return;
        }

        this.target = candidate;
        this.targetUUID = candidate.getUUID();
        this.duration = this.tickCount + 200;
        candidate.stopRiding();
        if (!candidate.startRiding(this, true)) {
            removeRoot();
            return;
        }
        candidate.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0, false, true, true));
        candidate.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2, false, true, true));
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.PLAYERS, 1.2F, 0.8F);
    }

    @Nullable
    private LivingEntity resolveTarget() {
        if (this.target != null && !this.target.isRemoved()) {
            return this.target;
        }
        if (this.targetUUID != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.targetUUID);
            if (entity instanceof LivingEntity livingEntity) {
                this.target = livingEntity;
                return livingEntity;
            }
        }
        return null;
    }

    private void clientPathParticles() {
        if (this.random.nextFloat() < 0.45F) {
            this.level().addParticle(ParticleTypes.CRIMSON_SPORE,
                    getX() + Utils.getRandomScaled(0.25F),
                    getY() + Utils.getRandomScaled(0.18F),
                    getZ() + Utils.getRandomScaled(0.25F),
                    0.0D, 0.01D, 0.0D);
        }
    }

    protected void clientDiggingParticles(LivingEntity livingEntity) {
        RandomSource randomsource = livingEntity.getRandom();
        BlockState blockstate = livingEntity.getBlockStateOn();
        if (blockstate.getRenderShape() != RenderShape.INVISIBLE) {
            for (int i = 0; i < 8; ++i) {
                double d0 = livingEntity.getX() + (double) Mth.randomBetween(randomsource, -0.6F, 0.6F);
                double d1 = livingEntity.getY();
                double d2 = livingEntity.getZ() + (double) Mth.randomBetween(randomsource, -0.6F, 0.6F);
                livingEntity.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockstate), d0, d1, d2, 0.0D, 0.06D, 0.0D);
            }
        }
        for (int i = 0; i < 4; ++i) {
            double d0 = livingEntity.getX() + (double) Mth.randomBetween(randomsource, -0.5F, 0.5F);
            double d1 = livingEntity.getY() + (double) Mth.randomBetween(randomsource, 0.1F, 0.8F);
            double d2 = livingEntity.getZ() + (double) Mth.randomBetween(randomsource, -0.5F, 0.5F);
            livingEntity.level().addParticle(ParticleTypes.CRIMSON_SPORE, d0, d1, d2, 0.0D, 0.03D, 0.0D);
        }
    }

    public void setOwner(@Nullable LivingEntity pOwner) {
        this.owner = pOwner;
        this.ownerUUID = pOwner == null ? null : pOwner.getUUID();
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Nullable
    public LivingEntity getOwner() {
        if (this.owner == null && this.ownerUUID != null && this.level() instanceof ServerLevel) {
            Entity entity = ((ServerLevel) this.level()).getEntity(this.ownerUUID);
            if (entity instanceof LivingEntity living) {
                this.owner = living;
            }
        }
        return this.owner;
    }

    public void removeRoot() {
        if (level() instanceof ServerLevel serverLevel) {
            if (isPathMode()) {
                // Red crimson burst particles for sequential dissolve wave
                serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE, getX(), getY() + 0.25D, getZ(),
                        18, 0.3D, 0.25D, 0.3D, 0.04D);
                serverLevel.sendParticles(new DustParticleOptions(new Vector3f(0.88F, 0.06F, 0.14F), 1.4F),
                        getX(), getY() + 0.25D, getZ(), 16, 0.35D, 0.25D, 0.35D, 0.05D);
                serverLevel.playSound(null, getX(), getY(), getZ(),
                        SoundEvents.SWEET_BERRY_BUSH_BREAK, SoundSource.PLAYERS, 0.75F, 1.25F + random.nextFloat() * 0.3F);
            }
        }
        if (level().isClientSide) {
            for (int i = 0; i < 4; i++) {
                level().addParticle(ParticleTypes.CRIMSON_SPORE, getX() + Utils.getRandomScaled(.3f), getY() + Utils.getRandomScaled(.3f), getZ() + Utils.getRandomScaled(.3f), Utils.getRandomScaled(0.5f), 0.02D, Utils.getRandomScaled(0.5f));
            }
        }
        this.ejectPassengers();
        this.discard();
    }

    @Override
    public void die(@NotNull DamageSource damageSource) {
        removeRoot();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("Age", this.tickCount);
        if (this.ownerUUID != null) {
            pCompound.putUUID("Owner", this.ownerUUID);
        }
        pCompound.putInt("Duration", duration);
        pCompound.putFloat("RendDamage", rendDamage);
        pCompound.putFloat("DirectDamage", damage);
        pCompound.putInt("Warmup", getWarmup());
        pCompound.putFloat("BaseScale", getBaseScale());
        pCompound.putInt("Mode", getMode().ordinal());
        pCompound.putLong("RemovalGameTime", getRemovalGameTime());
        pCompound.putBoolean("BindActivated", bindActivated);
        if (this.targetUUID != null) {
            pCompound.putUUID("Target", this.targetUUID);
        }
        pCompound.putBoolean("IsCoordinator", isCoordinator);
        pCompound.putBoolean("CoordinatorActive", coordinatorActive);
        pCompound.putInt("StepTimer", stepTimer);
        pCompound.putInt("CurrentSegmentIndex", currentSegmentIndex);
        pCompound.putInt("MaxSegments", maxSegments);
        pCompound.putDouble("CurrentSegmentTailX", currentSegmentTail.x);
        pCompound.putDouble("CurrentSegmentTailY", currentSegmentTail.y);
        pCompound.putDouble("CurrentSegmentTailZ", currentSegmentTail.z);
        pCompound.putDouble("CurrentTipX", currentTip.x);
        pCompound.putDouble("CurrentTipY", currentTip.y);
        pCompound.putDouble("CurrentTipZ", currentTip.z);
        pCompound.putFloat("PathYaw", getPathYaw());
        pCompound.putFloat("PathPitch", getPathPitch());
        net.minecraft.nbt.ListTag uuidList = new net.minecraft.nbt.ListTag();
        for (UUID uuid : spawnedSegmentUUIDs) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("UUID", uuid);
            uuidList.add(tag);
        }
        pCompound.put("SpawnedUUIDs", uuidList);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.tickCount = pCompound.getInt("Age");
        if (pCompound.hasUUID("Owner")) {
            this.ownerUUID = pCompound.getUUID("Owner");
        }
        if (pCompound.contains("PathYaw")) {
            setPathYaw(pCompound.getFloat("PathYaw"));
        }
        if (pCompound.contains("PathPitch")) {
            setPathPitch(pCompound.getFloat("PathPitch"));
        } else if (pCompound.contains("Pitch")) {
            setPathPitch(pCompound.getFloat("Pitch"));
        }
        if (pCompound.contains("Duration")) {
            this.duration = pCompound.getInt("Duration");
        }
        if (pCompound.contains("RendDamage")) {
            this.rendDamage = pCompound.getFloat("RendDamage");
        }
        if (pCompound.contains("DirectDamage")) {
            this.damage = pCompound.getFloat("DirectDamage");
        }
        if (pCompound.contains("Warmup")) {
            setWarmup(pCompound.getInt("Warmup"));
        }
        if (pCompound.contains("BaseScale")) {
            setBaseScale(pCompound.getFloat("BaseScale"));
        }
        if (pCompound.contains("Mode")) {
            int mode = Mth.clamp(pCompound.getInt("Mode"), 0, RootMode.values().length - 1);
            setMode(RootMode.values()[mode]);
        }
        if (pCompound.contains("RemovalGameTime")) {
            setRemovalGameTime(pCompound.getLong("RemovalGameTime"));
        }
        this.bindActivated = pCompound.getBoolean("BindActivated");
        if (pCompound.hasUUID("Target")) {
            this.targetUUID = pCompound.getUUID("Target");
        }
        if (pCompound.contains("IsCoordinator")) {
            this.isCoordinator = pCompound.getBoolean("IsCoordinator");
        }
        if (pCompound.contains("CoordinatorActive")) {
            this.coordinatorActive = pCompound.getBoolean("CoordinatorActive");
        }
        if (pCompound.contains("StepTimer")) {
            this.stepTimer = pCompound.getInt("StepTimer");
        }
        if (pCompound.contains("CurrentSegmentIndex")) {
            this.currentSegmentIndex = pCompound.getInt("CurrentSegmentIndex");
        }
        if (pCompound.contains("MaxSegments")) {
            this.maxSegments = pCompound.getInt("MaxSegments");
        }
        if (pCompound.contains("CurrentTipX")) {
            this.currentTip = new Vec3(pCompound.getDouble("CurrentTipX"), pCompound.getDouble("CurrentTipY"), pCompound.getDouble("CurrentTipZ"));
        }
        if (pCompound.contains("CurrentSegmentTailX")) {
            this.currentSegmentTail = new Vec3(
                    pCompound.getDouble("CurrentSegmentTailX"),
                    pCompound.getDouble("CurrentSegmentTailY"),
                    pCompound.getDouble("CurrentSegmentTailZ"));
        } else {
            // Compatibility with saves made before the explicit tail/head chain state.
            this.currentSegmentTail = this.currentTip;
        }
        if (pCompound.contains("SpawnedUUIDs", net.minecraft.nbt.Tag.TAG_LIST)) {
            net.minecraft.nbt.ListTag uuidList = pCompound.getList("SpawnedUUIDs", net.minecraft.nbt.Tag.TAG_COMPOUND);
            this.spawnedSegmentUUIDs.clear();
            for (int i = 0; i < uuidList.size(); i++) {
                CompoundTag tag = uuidList.getCompound(i);
                if (tag.hasUUID("UUID")) {
                    this.spawnedSegmentUUIDs.add(tag.getUUID("UUID"));
                }
            }
        }
    }

    @Override
    public boolean hasIndirectPassenger(Entity pEntity) {
        return isBindMode();
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        this.removeRoot();
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    @Override
    public boolean isDamageSourceBlocked(DamageSource pDamageSource) {
        return true;
    }

    @Override
    public boolean showVehicleHealth() {
        return false;
    }

    @Override
    public void knockback(double pStrength, double pX, double pZ) {
    }

    @Override
    public void positionRider(Entity passenger, Entity.MoveFunction pCallback) {
        double x = this.getX() - passenger.getX();
        double y = this.getY() - passenger.getY();
        double z = this.getZ() - passenger.getZ();
        if (x * x + y * y + z * z > 25.0D) {
            this.removeRoot();
        } else {
            passenger.setPos(this.getX(), this.getY(), this.getZ());
        }
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public void travel(@NotNull Vec3 pTravelVector) {
    }

    @Override
    protected boolean isImmobile() {
        return true;
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    public boolean isPushedByFluid(FluidType type) {
        return false;
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (pSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            this.removeRoot();
            return true;
        }
        return false;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return Collections.singleton(ItemStack.EMPTY);
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot pSlot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot pSlot, ItemStack pStack) {
    }

    private PlayState animationPredicate(software.bernie.geckolib.core.animation.AnimationState<CrimsonRootEntity> event) {
        if (getWarmup() > 0) {
            return PlayState.STOP;
        }
        return event.setAndContinue(EMERGE_ANIMATION);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
