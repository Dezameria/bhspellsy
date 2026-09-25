package io.redspace.ironspell_more.spells.aqua;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.config.SpellConfig;
import io.redspace.ironspell_more.entity.spells.toxic_salvation.ToxicSalvationAoe;
import io.redspace.ironspell_more.registry.EntityRegistry;
import io.redspace.ironspell_more.registry.MobEffectsRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import com.gametechbc.traveloptics.api.init.TravelopticsSchools;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class ToxicSalvationSpell extends AbstractSpell {
    // ==========================================
    // SPELL TUNING CONSTANTS (Code Defaults)
    // ==========================================
    public static final float BASE_DAMAGE = 4.0F;
    public static final float DAMAGE_PER_LEVEL = 1.0F;
    public static final float BASE_HEAL = 2.0F;
    public static final float HEAL_PER_LEVEL = 0.5F;
    public static final int BASE_MANA_COST = 40;
    public static final int MANA_COST_PER_LEVEL = 5;
    public static final double COOLDOWN_SECONDS = 22.0D;
    public static final int BASE_DURATION_SECONDS = 10;
    public static final double DURATION_PER_LEVEL_SECONDS = 1.5D;
    public static final float BASE_RADIUS = 5.0F;

    public static final ResourceLocation SPELL_ID = IronSpellMore.id("toxic_salvation");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(TravelopticsSchools.AQUA_RESOURCE)
            .setMaxLevel(8)
            .setCooldownSeconds(COOLDOWN_SECONDS)
            .build();

    public ToxicSalvationSpell() {
        this.baseManaCost = BASE_MANA_COST;
        this.manaCostPerLevel = MANA_COST_PER_LEVEL;
        this.baseSpellPower = (int) BASE_DAMAGE;
        this.spellPowerPerLevel = (int) DAMAGE_PER_LEVEL;
        this.castTime = 25;
    }

    @Override
    public int getManaCost(int spellLevel) {
        return SpellConfig.ToxicSalvation.getBaseMana() + (spellLevel - 1) * SpellConfig.ToxicSalvation.getManaPerLevel();
    }

    @Override
    public int getSpellCooldown() {
        return (int) (SpellConfig.ToxicSalvation.getCooldown() * 20);
    }

    @Override
    public ResourceLocation getSpellResource() {
        return SPELL_ID;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.CONE_OF_COLD_LOOP.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.ICE_CAST.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_LONG_CAST;
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        Utils.preCastTargetHelper(level, entity, playerMagicData, this, 32, 0.15f, false);
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 spawn = null;
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData castTargetingData) {
            var target = castTargetingData.getTarget((ServerLevel) level);
            if (target != null) {
                spawn = target.position();
            }
        }
        if (spawn == null) {
            spawn = RaycastBuilder.begin(level, entity)
                    .range(32)
                    .checkForBlocks(true)
                    .bbInflation(0.15f)
                    .build()
                    .getLocation();
        }
        spawn = Utils.moveToRelativeGroundLevel(level, spawn, 6);

        // 1. แปลงพิษในตัวซงหลิน ให้ระเหยกลายเป็นหมอกพิษ: ล้างเอฟเฟกต์พิษออกจากตัวซงหลิน
        entity.removeEffect(MobEffects.POISON);
        if (MobEffectsRegistry.AZURE_VENOMOUS.isPresent()) {
            entity.removeEffect(MobEffectsRegistry.AZURE_VENOMOUS.get());
        }

        // 2. ซงหลินได้รับการฟื้นฟูเลือดทันที
        float instantHeal = getHealAmount(spellLevel, entity);
        entity.heal(instantHeal);

        // 3. ปล่อยหมอกพิษ ToxicSalvationAoe
        int duration = getDurationTicks(spellLevel, entity);
        float radius = getRadius(spellLevel, entity);
        float damage = getDamage(spellLevel, entity);

        ToxicSalvationAoe aoe = new ToxicSalvationAoe(EntityRegistry.TOXIC_SALVATION_AOE.get(), level);
        aoe.moveTo(spawn);
        aoe.setOwner(entity);
        aoe.setRadius(radius);
        aoe.setDuration(duration);
        aoe.setDamage(damage);
        aoe.setHealAmount(instantHeal * 0.5F); // การฟื้นฟูเลือดต่อเนื่องเล็กน้อยเมื่ออยู่ในหมอก
        aoe.setDeltaMovement(entity.getForward().multiply(1, 0, 1).normalize().scale(0.05f));
        level.addFreshEntity(aoe);

        // เสียงการระเหยของพิษ
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.2F, 1.2F);
        level.playSound(null, spawn.x, spawn.y, spawn.z,
                SoundEvents.SPLASH_POTION_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public int getDurationTicks(int spellLevel, LivingEntity caster) {
        double seconds = SpellConfig.ToxicSalvation.getBaseDuration() + (spellLevel - 1) * SpellConfig.ToxicSalvation.getDurationPerLevel();
        return (int) (20 * seconds);
    }

    public float getRadius(int spellLevel, LivingEntity caster) {
        return SpellConfig.ToxicSalvation.getBaseRadius() * getEntityPowerMultiplier(caster);
    }

    public float getDamage(int spellLevel, LivingEntity caster) {
        float base = SpellConfig.ToxicSalvation.getBaseDamage();
        float perLevel = SpellConfig.ToxicSalvation.getDamagePerLevel();
        return (base + (spellLevel - 1) * perLevel) * getEntityPowerMultiplier(caster);
    }

    public float getHealAmount(int spellLevel, LivingEntity caster) {
        float base = SpellConfig.ToxicSalvation.getBaseHeal();
        float perLevel = SpellConfig.ToxicSalvation.getHealPerLevel();
        return (base + (spellLevel - 1) * perLevel) * getEntityPowerMultiplier(caster);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.healing", Utils.stringTruncation(getHealAmount(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(getRadius(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getDurationTicks(spellLevel, caster), 1))
        );
    }
}
