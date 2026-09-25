package io.redspace.ironspell_more.entity.spells.toxic_salvation;

import io.redspace.ironspell_more.registry.EntityRegistry;
import io.redspace.ironspell_more.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AoeEntity;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class ToxicSalvationAoe extends AoeEntity {
    public static final float HEIGHT = 4.0F;

    protected float healAmount = 2.0F;
    protected int poisonDuration = 100; // 5 seconds

    public ToxicSalvationAoe(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setCircular();
        this.reapplicationDelay = 20; // ticks every 1 second
    }

    public ToxicSalvationAoe(Level level) {
        this(EntityRegistry.TOXIC_SALVATION_AOE.get(), level);
    }

    public void setHealAmount(float healAmount) {
        this.healAmount = healAmount;
    }

    public float getHealAmount() {
        return this.healAmount;
    }

    public void setPoisonDuration(int ticks) {
        this.poisonDuration = ticks;
    }

    public int getPoisonDuration() {
        return this.poisonDuration;
    }

    @Override
    public float getParticleCount() {
        return 1.8F;
    }

    @Override
    public Optional<ParticleOptions> getParticle() {
        return Optional.of(ParticleHelper.POISON_CLOUD);
    }

    @Override
    protected boolean canHitEntity(Entity pTarget) {
        if (pTarget instanceof LivingEntity living) {
            if (living == getOwner()) {
                return true; // allow owner to receive healing from mist
            }
            return super.canHitEntity(pTarget) && !DamageSources.isFriendlyFireBetween(this.getOwner(), pTarget);
        }
        return false;
    }

    @Override
    public void applyEffect(LivingEntity target) {
        if (target == getOwner()) {
            // ซงหลินได้รับ การฟื้นฟูเลือดเล็กน้อย
            float heal = this.healAmount > 0 ? this.healAmount : 2.0F;
            target.heal(heal);
        } else {
            // ผลของพลัง ทำให้ผู้ที่อยู่ในระยะหมอกติด effect poison 1
            target.addEffect(new MobEffectInstance(MobEffects.POISON, this.poisonDuration, 0, false, true, true));
            if (this.damage > 0) {
                var spell = SpellRegistry.TOXIC_SALVATION_SPELL.get();
                DamageSource damageSource = spell.getDamageSource(this, getOwner());
                DamageSources.applyDamage(target, this.damage, damageSource);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Gentle inward vortex / suction drift on enemies inside mist
        var entities = level().getEntities(this, this.getBoundingBox(), this::canHitEntity);
        var radius = getRadius();
        double strength = 0.02D;
        for (Entity entity : entities) {
            if (entity != getOwner() && entity.distanceToSqr(this) < radius * radius) {
                Vec3 offset = entity.position().subtract(this.position());
                if (offset.horizontalDistanceSqr() < 1.0D) {
                    continue;
                }
                Vec3 radial = new Vec3(offset.x, 0, offset.z).normalize();
                Vec3 tangent = new Vec3(-radial.z, 0, radial.x);
                Vec3 push = tangent.scale(strength * Mth.PI).add(radial.scale(-strength));
                if (entity instanceof LivingEntity living) {
                    entity.setDeltaMovement(entity.getDeltaMovement().add(push.scale(Utils.clampedKnockbackResistanceFactor(living, 0.4F, 1.0F))));
                }
            }
        }
        this.move(MoverType.SELF, getDeltaMovement());

        // Ambient mist bubbling sound
        if ((tickCount - 1) % 20 == 0) {
            playSound(SoundEvents.BREWING_STAND_BREW, 1.2F, 0.9F + random.nextFloat() * 0.2F);
        }

        // Terrain snap like BlizzardAoe
        if (tickCount % 20 == 0) {
            if (level().collidesWithSuffocatingBlock(this, AABB.ofSize(this.position().add(0, 0.5D, 0), 1.5D, 0.5D, 1.5D))) {
                Vec3 ground = Utils.moveToRelativeGroundLevel(level(), this.position().add(0, 1.0D, 0), 1);
                this.move(MoverType.SELF, ground.subtract(this.position()));
            } else {
                if (Utils.raycastForBlock(level(), position(), position().add(0, -0.5D, 0), ClipContext.Fluid.NONE).getType() == HitResult.Type.MISS) {
                    this.move(MoverType.SELF, new Vec3(0, -0.5D, 0));
                }
            }
        }
    }

    @Override
    public void ambientParticles() {
        super.ambientParticles();
        if (!level().isClientSide) {
            return;
        }
        // Subtle water droplet splash
        Vec3 pos = position();
        float radius = getRadius();
        for (int i = 0; i < 2; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double dist = Math.sqrt(random.nextDouble()) * radius;
            double px = pos.x + Math.cos(angle) * dist;
            double pz = pos.z + Math.sin(angle) * dist;
            double py = pos.y + 0.1D + random.nextDouble() * 1.5D;
            level().addParticle(ParticleTypes.SPLASH, px, py, pz, 0, 0.02D, 0);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("healAmount", this.healAmount);
        tag.putInt("poisonDuration", this.poisonDuration);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("healAmount")) {
            this.healAmount = tag.getFloat("healAmount");
        }
        if (tag.contains("poisonDuration")) {
            this.poisonDuration = tag.getInt("poisonDuration");
        }
    }
}
