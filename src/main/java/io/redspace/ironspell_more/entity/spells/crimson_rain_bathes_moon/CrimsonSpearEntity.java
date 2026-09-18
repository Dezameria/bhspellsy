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

        if (hitTarget && hitEntity instanceof LivingEntity livingTarget) {
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
