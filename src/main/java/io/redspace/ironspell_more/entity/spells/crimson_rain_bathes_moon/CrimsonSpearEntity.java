package io.redspace.ironspell_more.entity.spells.crimson_rain_bathes_moon;

import com.gametechbc.traveloptics.entity.extended_projectiles.ExtendedWaterSpearEntity;
import com.gametechbc.traveloptics.init.TravelopticsEffects;
import com.github.L_Ender.cataclysm.client.particle.StormParticle;
import com.github.L_Ender.cataclysm.entity.projectile.Water_Spear_Entity;
import io.redspace.ironspell_more.registry.EntityRegistry;
import io.redspace.ironspell_more.registry.SpellRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public class CrimsonSpearEntity extends ExtendedWaterSpearEntity {

    public CrimsonSpearEntity(EntityType<? extends Water_Spear_Entity> entityType, Level level) {
        super(entityType, level);
    }

    public CrimsonSpearEntity(LivingEntity shooter, Vec3 direction, Level level, float damage) {
        super(EntityRegistry.CRIMSON_SPEAR.get(), shooter, shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ(), direction, damage, level);
    }

    public CrimsonSpearEntity(EntityType<? extends Water_Spear_Entity> entityType, LivingEntity shooter, double x, double y, double z, Vec3 direction, float damage, Level level) {
        super(entityType, shooter, x, y, z, direction, damage, level);
    }

    private LivingEntity homingTarget;
    private int lifeTicks = 0;
    private static final int MAX_LIFE_TICKS = 80; // 4 seconds max lifetime to prevent lag
    private int hitCount = 0;
    private static final int MAX_HITS = 3; // 1 spear can hit entities up to 3 times
    private final Map<Integer, Integer> hitCooldowns = new HashMap<>();

    public void setHomingTarget(LivingEntity target) {
        this.homingTarget = target;
    }

    public LivingEntity getHomingTarget() {
        return this.homingTarget;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (!this.level().isClientSide && this.hitCooldowns.containsKey(entity.getId())) {
            return false;
        }
        return super.canHitEntity(entity);
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide) {
            this.lifeTicks++;
            if (this.lifeTicks >= MAX_LIFE_TICKS) {
                this.discard();
                return;
            }

            if (!this.hitCooldowns.isEmpty()) {
                this.hitCooldowns.entrySet().removeIf(entry -> entry.getValue() <= 1);
                this.hitCooldowns.replaceAll((id, cd) -> cd - 1);
            }

            if (this.homingTarget != null) {
                if (this.homingTarget.isAlive() && !this.homingTarget.isRemoved()) {
                    Vec3 targetCenter = this.homingTarget.getBoundingBox().getCenter();
                    Vec3 currentPos = this.position();
                    Vec3 currentMotion = this.getDeltaMovement();
                    double currentSpeed = currentMotion.length();
                    if (currentSpeed > 0.05D) {
                        Vec3 desiredDir = targetCenter.subtract(currentPos).normalize();
                        double turnRate = 0.22D;
                        Vec3 newDir = currentMotion.normalize().scale(1.0 - turnRate).add(desiredDir.scale(turnRate)).normalize();
                        this.setDeltaMovement(newDir.scale(currentSpeed));
                    }
                } else {
                    this.homingTarget = null;
                }
            }
        }
        super.tick();
    }

    @Override
    protected void SpawnParticle() {
        double px = this.getX() + 1.5 * (this.random.nextFloat() - 0.5);
        double py = this.getY() + 1.5 * (this.random.nextFloat() - 0.5);
        double pz = this.getZ() + 1.5 * (this.random.nextFloat() - 0.5);

        // Crimson Red StormParticle trail
        float red = (215 + this.random.nextInt(40)) / 255.0F;
        float green = (20 + this.random.nextInt(30)) / 255.0F;
        float blue = (30 + this.random.nextInt(35)) / 255.0F;

        this.level().addParticle(
                new StormParticle.OrbData(red, green, blue, 0.1F, this.getBbHeight() / 2.0F, this.getId()),
                px, py, pz, 0.0, 0.0, 0.0
        );
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        Entity hitEntity = hitResult.getEntity();
        boolean hitTarget = false;
        Entity owner = this.getOwner();

        if (owner instanceof LivingEntity livingOwner) {
            if (!hitEntity.isAlliedTo(livingOwner) && !livingOwner.equals(hitEntity) && !livingOwner.isAlliedTo(hitEntity)) {
                SpellDamageSource damageSource = SpellRegistry.CRIMSON_RAIN_BATHES_MOON_SPELL.get().getDamageSource(this, livingOwner);
                DamageSources.applyDamage(hitEntity, this.getDamage(), damageSource);
                hitTarget = true;
            }
        } else {
            DamageSources.applyDamage(hitEntity, 5.0F, this.damageSources().magic());
            hitTarget = true;
        }

        if (hitTarget) {
            this.hitCooldowns.put(hitEntity.getId(), 8); // 8 ticks cooldown before hitting the same entity again
            this.hitCount++;
            if (this.hitCount >= MAX_HITS) {
                this.discard();
                return;
            }

            if (hitEntity instanceof LivingEntity livingTarget) {
                MobEffectInstance effect = livingTarget.getEffect(TravelopticsEffects.WET.get());
                if (effect != null) {
                    if (this.random.nextBoolean()) {
                        livingTarget.addEffect(new MobEffectInstance(TravelopticsEffects.WET.get(), effect.getDuration() + 40, effect.getAmplifier(), false, false, true));
                    } else {
                        livingTarget.addEffect(new MobEffectInstance(TravelopticsEffects.WET.get(), effect.getDuration(), Math.min(10, effect.getAmplifier() + 1), false, false, true));
                    }
                } else {
                    livingTarget.addEffect(new MobEffectInstance(TravelopticsEffects.WET.get(), 40, 0, false, false, true));
                }
            }
        }
    }
}
