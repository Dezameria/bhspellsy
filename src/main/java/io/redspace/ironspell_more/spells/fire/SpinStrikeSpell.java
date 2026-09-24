package io.redspace.ironspell_more.spells.fire;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.registry.MobEffectsRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.capabilities.magic.ImpulseCastData;
import io.redspace.ironsspellbooks.player.SpinAttackType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import io.redspace.ironspell_more.config.SpellConfig;
import io.redspace.ironsspellbooks.api.util.Utils;

import java.util.List;

@AutoSpellConfig
public class SpinStrikeSpell extends AbstractSpell {
    // ==========================================
    // SPELL TUNING CONSTANTS (Code Defaults)
    // ==========================================
    public static final float BASE_DAMAGE = 10.0F;
    public static final float DAMAGE_PER_LEVEL = 1.0F;
    public static final int BASE_MANA_COST = 30;
    public static final int MANA_COST_PER_LEVEL = 5;
    public static final double COOLDOWN_SECONDS = 10.0;

    private final ResourceLocation spellId = new ResourceLocation(IronSpellMore.MODID, "spin_strike");

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)));
    }

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(COOLDOWN_SECONDS)
            .build();

    public SpinStrikeSpell() {
        this.manaCostPerLevel = MANA_COST_PER_LEVEL;
        this.baseSpellPower = (int) BASE_DAMAGE;
        this.spellPowerPerLevel = (int) DAMAGE_PER_LEVEL;
        this.castTime = 0;
        this.baseManaCost = BASE_MANA_COST;
    }

    @Override
    public int getManaCost(int spellLevel) {
        return SpellConfig.SpinStrike.getBaseMana() + (spellLevel - 1) * SpellConfig.SpinStrike.getManaPerLevel();
    }

    @Override
    public int getSpellCooldown() {
        return (int) (SpellConfig.SpinStrike.getCooldown() * 20);
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new ImpulseCastData();
    }

    @Override
    public void onClientCast(Level level, int spellLevel, LivingEntity entity, ICastData castData) {
        if (castData instanceof ImpulseCastData impulseData) {
            entity.hasImpulse = impulseData.hasImpulse;
            entity.setDeltaMovement(impulseData.x, impulseData.y, impulseData.z);
        }
        if (entity instanceof Player player) {
            player.startAutoSpinAttack(10);
        }
        super.onClientCast(level, spellLevel, entity, castData);
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource,
            MagicData playerMagicData) {
        entity.hasImpulse = true;
        float multiplier = (15 + getSpellPower(spellLevel, entity)) / 20f;

        Vec3 forward = entity.getLookAngle();
        var upwardness = forward.dot(new Vec3(0, 1, 0));
        var remap = 1 - (Math.max(0, upwardness) * 0.6f);
        var impulse = forward.scale(2.5 * multiplier).multiply(1, remap, 1);
        if (entity.onGround()) {
            impulse = impulse.add(0, 0.2, 0);
        }

        // ซิงค์การพุ่งไปยัง Client
        playerMagicData.setAdditionalCastData(
                new ImpulseCastData((float) impulse.x, (float) impulse.y, (float) impulse.z, true));
        playerMagicData.getSyncedData().setSpinAttackType(SpinAttackType.FIRE);

        entity.setDeltaMovement(impulse);
        entity.hurtMarked = true;
        entity.invulnerableTime = 20;

        if (entity instanceof Player player) {
            player.startAutoSpinAttack(10);
        }

        // ส่ง Effect ไปคอยผลักดันให้พุ่งทะลุและทำดาเมจอย่างต่อเนื่องทุก Tick
        int damageValue = (int) getDamage(spellLevel, entity);
        entity.addEffect(new MobEffectInstance(MobEffectsRegistry.SPIN_STRIKE.get(), 12,
                damageValue, false, false, false));

        world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 2.0f, 1.0f);

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    // กำหนดค่า Damage ตรงนี้ (จะอิงจาก baseSpellPower / SpellConfig อัตโนมัติ)
    public float getDamage(int spellLevel, LivingEntity caster) {
        float base = SpellConfig.SpinStrike.getBaseDamage();
        float perLevel = SpellConfig.SpinStrike.getDamagePerLevel();
        return (base + (spellLevel - 1) * perLevel) * getEntityPowerMultiplier(caster);
    }
}
