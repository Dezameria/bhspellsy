package io.redspace.ironspell_more.entity.spells.glacial_firmament;

import io.redspace.ironspell_more.registry.EntityRegistry;
import io.redspace.ironspell_more.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractShieldEntity;
import io.redspace.ironsspellbooks.entity.spells.AoeEntity;
import io.redspace.ironsspellbooks.entity.spells.ShieldPart;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GlacialSpikeEntity extends AoeEntity {

    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(GlacialSpikeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_WAIT_TIME = SynchedEntityData.defineId(GlacialSpikeEntity.class, EntityDataSerializers.INT);
    public static final int RISE_TIME = 6;
    public static final int REST_TIME = 55;
    private final List<Entity> victims;

    public GlacialSpikeEntity(EntityType<? extends AoeEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.victims = new ArrayList<>();
    }

    public GlacialSpikeEntity(Level level, LivingEntity owner) {
        this(EntityRegistry.GLACIAL_SPIKE.get(), level);
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

    public float getSpikeSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public void setSpikeSize(float f) {
        this.entityData.set(DATA_SIZE, f);
        this.refreshDimensions();
    }

    public int getWaitTime() {
        return this.entityData.get(DATA_WAIT_TIME);
    }

    public void setWaitTime(int i) {
        this.entityData.set(DATA_WAIT_TIME, i);
    }

    /**
     * @return [-1,0] based on whether it should be underground or at full height
     */
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
                float f = getSpikeSize();
                if (!this.isSilent()) {
                    level().playSound(null, this.blockPosition(), SoundRegistry.ICE_SPIKE_EMERGE.get(),
                            SoundSource.NEUTRAL, 1.25f * getSpikeSize(),
                            Mth.randomBetweenInclusive(Utils.random, 6, 12) * .1f);
                }
                MagicManager.spawnParticles(level(), ParticleHelper.SNOWFLAKE, getX(),
                        level().clip(new ClipContext(position().add(0, 2, 0), position(), ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE, null)).getLocation().y() + 0.1,
                        getZ(), (int) (10 * f * f), 0.1 * f, 0.1 * f, 0.1f * f, 0.12 * f, false);
                MagicManager.spawnParticles(level(), ParticleHelper.SNOW_DUST, getX(),
                        level().clip(new ClipContext(position().add(0, 2, 0), position(), ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE, null)).getLocation().y() + 0.1,
                        getZ(), (int) (15 * f * f), 0.1 * f, 0.1 * f, 0.1f * f, 0.08 * f, false);
            }
        } else if (tickCount > waitTime && tickCount < waitTime + RISE_TIME) {
            AABB damager = this.getBoundingBox();
            damager.setMaxY(this.getY() + (damager.getYsize() * (getPositionOffset(0) + 1)));
            for (Entity entity : level().getEntities(this, damager).stream()
                    .filter(target -> canHitEntity(target) && !victims.contains(target)).collect(Collectors.toSet())) {
                if (DamageSources.applyDamage(entity, damage,
                        SpellRegistry.GLACIAL_FIRMAMENT_SPELL.get().getDamageSource(this, getOwner()))) {
                    entity.setDeltaMovement(entity.getDeltaMovement().add(0, this.getSpikeSize() * 0.3, 0));
                    entity.hurtMarked = true;
                    entity.setTicksFrozen(entity.getTicksFrozen() + (int) (40 * getSpikeSize()));
                }
                victims.add(entity);
                if (entity instanceof ShieldPart || entity instanceof AbstractShieldEntity) {
                    shatter();
                    return;
                }
            }
        } else if (tickCount >= waitTime + RISE_TIME + REST_TIME) {
            shatter();
        }
    }

    public void shatter() {
        if (!level().isClientSide) {
            float f = getSpikeSize();
            double centerY = getY() + (getDimensions(getPose()).height * 0.5);
            if (level() instanceof ServerLevel serverLevel) {
                // Flying ice block shards and snow fragments
                serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
                        getX(), centerY, getZ(), (int) (30 * Math.min(f, 3.5f)), 0.35 * f, 0.6 * f, 0.35 * f, 0.22);
                serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                        getX(), centerY, getZ(), (int) (20 * Math.min(f, 3.5f)), 0.35 * f, 0.6 * f, 0.35 * f, 0.18);
                serverLevel.sendParticles(ParticleHelper.SNOWFLAKE,
                        getX(), centerY, getZ(), (int) (25 * Math.min(f, 3.5f)), 0.4 * f, 0.5 * f, 0.4 * f, 0.15);
                serverLevel.sendParticles(ParticleHelper.SNOW_DUST,
                        getX(), centerY, getZ(), (int) (20 * Math.min(f, 3.5f)), 0.4 * f, 0.5 * f, 0.4 * f, 0.1);
            }
            level().playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS,
                    0.8f + (f * 0.15f), Mth.randomBetweenInclusive(Utils.random, 9, 13) * 0.1f);
            level().playSound(null, this.blockPosition(), SoundRegistry.ICE_IMPACT.get(), SoundSource.BLOCKS,
                    0.9f + (f * 0.15f), Mth.randomBetweenInclusive(Utils.random, 8, 11) * 0.1f);
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
        return EntityDimensions.scalable(this.getSpikeSize() * 0.4f,
                this.getSpikeSize() * 1.25f * (this.getPositionOffset(1) + 1));
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
