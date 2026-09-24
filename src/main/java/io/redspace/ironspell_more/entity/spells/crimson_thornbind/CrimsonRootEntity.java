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
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
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
    public static final int MAX_PATH_SEGMENTS = 12;
    public static final float SEGMENT_SPACING = 1.35F;
    public static final int PATH_HOLD_TICKS = 6;

    private static final EntityDataAccessor<Integer> DATA_WARMUP = SynchedEntityData.defineId(CrimsonRootEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SCALE = SynchedEntityData.defineId(CrimsonRootEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(CrimsonRootEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_REMOVAL_GAME_TIME = SynchedEntityData.defineId(CrimsonRootEntity.class, EntityDataSerializers.LONG);

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
            if (activeTicks < 15) {
                if (isPathMode()) {
                    clientPathParticles();
                } else {
                    clientDiggingParticles(this);
                }
            }
            activeTicks++;
        }
    }

    public void initCoordinator(Vec3 startPos, int maxSegments, float damage, float rendDamage) {
        this.isCoordinator = true;
        this.coordinatorActive = true;
        this.stepTimer = STEP_INTERVAL;
        this.currentSegmentIndex = 1;
        this.maxSegments = maxSegments;
        this.currentTip = startPos;
        this.damage = damage;
        this.rendDamage = rendDamage;
        this.spawnedSegmentUUIDs.clear();
        this.spawnedSegmentUUIDs.add(this.getUUID());
        long fallbackRemoval = level().getGameTime() + ((long) maxSegments * STEP_INTERVAL) + PATH_HOLD_TICKS + 40L;
        this.setRemovalGameTime(fallbackRemoval);
    }

    private void performPropagationStep() {
        LivingEntity rootOwner = getOwner();
        if (rootOwner == null || !rootOwner.isAlive() || rootOwner.isRemoved()) {
            terminatePropagation();
            return;
        }

        Vec3 look = rootOwner.getLookAngle();
        Vec3 flatDir = new Vec3(look.x, 0.0D, look.z).normalize();
        if (flatDir.lengthSqr() < 1e-4) {
            float yawRad = rootOwner.getYRot() * Mth.DEG_TO_RAD;
            flatDir = new Vec3(-Mth.sin(yawRad), 0.0D, Mth.cos(yawRad));
        }

        Vec3 nextPoint = currentTip.add(flatDir.scale(SEGMENT_SPACING));

        BlockHitResult blockHit = level().clip(new ClipContext(currentTip, nextPoint,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, rootOwner));
        Vec3 clippedEnd = blockHit.getType() == HitResult.Type.BLOCK
                ? blockHit.getLocation().subtract(flatDir.scale(0.1D))
                : nextPoint;

        CrimsonThornbindSpell.TargetHit targetHit = CrimsonThornbindSpell.findTargetAlongSegment(level(), rootOwner, currentTip, clippedEnd);
        if (targetHit != null) {
            spawnBindRootAndTerminate(targetHit.target());
            return;
        }

        if (blockHit.getType() == HitResult.Type.BLOCK) {
            terminatePropagation();
            return;
        }

        Vec3 stepVec = nextPoint.subtract(currentTip);
        float stepYaw = (float) (Mth.atan2(stepVec.z, stepVec.x) * (180.0D / Math.PI)) - 90.0F;

        CrimsonRootEntity nextSegment = new CrimsonRootEntity(level(), rootOwner);
        nextSegment.setMode(RootMode.PATH);
        nextSegment.moveTo(nextPoint.x, nextPoint.y, nextPoint.z, stepYaw, 0.0F);
        nextSegment.setWarmup(0);
        nextSegment.setBaseScale(DEFAULT_PATH_SCALE);
        long fallbackRemoval = level().getGameTime() + ((long) (maxSegments - currentSegmentIndex + 1) * STEP_INTERVAL) + PATH_HOLD_TICKS + 40L;
        nextSegment.setRemovalGameTime(fallbackRemoval);
        level().addFreshEntity(nextSegment);

        spawnedSegmentUUIDs.add(nextSegment.getUUID());
        currentTip = nextPoint;
        currentSegmentIndex++;

        level().playSound(null, nextPoint.x, nextPoint.y, nextPoint.z,
                SoundRegistry.ROOT_EMERGE.get(), SoundSource.PLAYERS, 0.6F, 1.15F + random.nextFloat() * 0.15F);

        if (currentSegmentIndex >= maxSegments) {
            terminatePropagation();
        }
    }

    private void spawnBindRootAndTerminate(LivingEntity hitTarget) {
        terminatePropagation();
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

    public void terminatePropagation() {
        this.coordinatorActive = false;
        long cleanupTime = level().getGameTime() + PATH_HOLD_TICKS;
        this.setRemovalGameTime(cleanupTime);
        if (level() instanceof ServerLevel serverLevel) {
            for (UUID uuid : spawnedSegmentUUIDs) {
                Entity entity = serverLevel.getEntity(uuid);
                if (entity instanceof CrimsonRootEntity root && root.isPathMode()) {
                    root.setRemovalGameTime(cleanupTime);
                }
            }
        }
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
        if (this.random.nextBoolean()) {
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
        if (level().isClientSide) {
            for (int i = 0; i < 6; i++) {
                level().addParticle(ParticleHelper.ROOT_FOG, getX() + Utils.getRandomScaled(.3f), getY() + Utils.getRandomScaled(.3f), getZ() + Utils.getRandomScaled(.3f), Utils.getRandomScaled(1.5f), -random.nextFloat() * .3f, Utils.getRandomScaled(1.5f));
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
        pCompound.putDouble("CurrentTipX", currentTip.x);
        pCompound.putDouble("CurrentTipY", currentTip.y);
        pCompound.putDouble("CurrentTipZ", currentTip.z);
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
