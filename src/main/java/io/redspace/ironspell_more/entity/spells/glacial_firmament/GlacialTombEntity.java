package io.redspace.ironspell_more.entity.spells.glacial_firmament;

import io.redspace.ironspell_more.registry.EntityRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.spells.AoeEntity;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;

public class GlacialTombEntity extends AoeEntity {

    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(GlacialTombEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_WAIT_TIME = SynchedEntityData.defineId(GlacialTombEntity.class, EntityDataSerializers.INT);
    public static final int RISE_TIME = 8;
    public static final int REST_TIME = 55;

    public GlacialTombEntity(EntityType<? extends AoeEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public GlacialTombEntity(Level level, LivingEntity owner) {
        this(EntityRegistry.GLACIAL_TOMB.get(), level);
        setOwner(owner);
    }

    @Override
    public void applyEffect(LivingEntity target) {
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SIZE, 1f);
        this.entityData.define(DATA_WAIT_TIME, 10);
    }

    public float getTombSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public void setTombSize(float f) {
        this.entityData.set(DATA_SIZE, f);
        this.refreshDimensions();
    }

    public int getWaitTime() {
        return this.entityData.get(DATA_WAIT_TIME);
    }

    public void setWaitTime(int i) {
        this.entityData.set(DATA_WAIT_TIME, i);
    }

    public float getPositionOffset(float partialTick) {
        float f = this.tickCount + partialTick;
        int waitTime = getWaitTime();
        if (f < waitTime) {
            return -1;
        } else if (f < waitTime + RISE_TIME) {
            f = (f - waitTime) / RISE_TIME;
            return (Mth.sin(f * Mth.PI) / Mth.PI) + f - 1f;
        } else {
            return 0f;
        }
    }

    @Override
    public void tick() {
        this.refreshDimensions();
        int waitTime = getWaitTime();
        if (tickCount == waitTime) {
            if (!level().isClientSide) {
                float f = getTombSize();
                if (!this.isSilent()) {
                    level().playSound(null, this.blockPosition(), SoundRegistry.ICE_SPIKE_EMERGE.get(),
                            SoundSource.NEUTRAL, 1.5f * getTombSize(),
                            Mth.randomBetweenInclusive(Utils.random, 4, 8) * .1f);
                }
                MagicManager.spawnParticles(level(), ParticleHelper.SNOWFLAKE, getX(),
                        level().clip(new ClipContext(position().add(0, 2, 0), position(), ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE, null)).getLocation().y() + 0.1,
                        getZ(), (int) (20 * f * f), 0.2 * f, 0.2 * f, 0.2 * f, 0.15 * f, false);
                MagicManager.spawnParticles(level(), ParticleHelper.SNOW_DUST, getX(),
                        level().clip(new ClipContext(position().add(0, 2, 0), position(), ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE, null)).getLocation().y() + 0.1,
                        getZ(), (int) (25 * f * f), 0.2 * f, 0.2 * f, 0.2 * f, 0.1 * f, false);
            }
        } else if (tickCount >= waitTime + RISE_TIME + REST_TIME) {
            shatter();
        }
    }

    public void shatter() {
        if (!level().isClientSide) {
            float f = getTombSize();
            double centerY = getY() + (getDimensions(getPose()).height * 0.5);
            if (level() instanceof ServerLevel serverLevel) {
                // Giant ice monolith shattering into ice blocks and crystal shards
                serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
                        getX(), centerY, getZ(), (int) (40 * Math.min(f, 3.0f)), 0.45 * f, 0.8 * f, 0.45 * f, 0.25);
                serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                        getX(), centerY, getZ(), (int) (30 * Math.min(f, 3.0f)), 0.45 * f, 0.8 * f, 0.45 * f, 0.2);
                serverLevel.sendParticles(ParticleHelper.SNOWFLAKE,
                        getX(), centerY, getZ(), (int) (30 * Math.min(f, 3.0f)), 0.5 * f, 0.7 * f, 0.5 * f, 0.18);
                serverLevel.sendParticles(ParticleHelper.SNOW_DUST,
                        getX(), centerY, getZ(), (int) (25 * Math.min(f, 3.0f)), 0.5 * f, 0.7 * f, 0.5 * f, 0.12);
            }
            level().playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS,
                    1.0f + (f * 0.2f), Mth.randomBetweenInclusive(Utils.random, 7, 11) * 0.1f);
            level().playSound(null, this.blockPosition(), SoundRegistry.ICE_IMPACT.get(), SoundSource.BLOCKS,
                    1.2f + (f * 0.2f), Mth.randomBetweenInclusive(Utils.random, 6, 9) * 0.1f);
        }
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("waitTime", this.getWaitTime());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setWaitTime(pCompound.getInt("waitTime"));
    }

    @Override
    public EntityDimensions getDimensions(Pose pPose) {
        return EntityDimensions.scalable(this.getTombSize() * 0.8f,
                this.getTombSize() * 2.0f * (this.getPositionOffset(1) + 1));
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public void ambientParticles() {
    }

    @Override
    public float getParticleCount() {
        return 0;
    }

    @Override
    public Optional<ParticleOptions> getParticle() {
        return Optional.empty();
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket pPacket) {
        super.recreateFromPacket(pPacket);
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
    }
}
