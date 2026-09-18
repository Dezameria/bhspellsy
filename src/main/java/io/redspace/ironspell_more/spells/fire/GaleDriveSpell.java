package io.redspace.ironspell_more.spells.fire;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.registry.MobEffectsRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.ImpulseCastData;
import io.redspace.ironsspellbooks.player.SpinAttackType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class GaleDriveSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(IronSpellMore.MODID, "gale_drive");

    public static final SpinAttackType GALE_SPIN = new SpinAttackType(
            ResourceLocation.fromNamespaceAndPath(IronSpellMore.MODID, "textures/entity/gale_riptide.png"), true);

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(20)
            .build();

    public GaleDriveSpell() {
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 40;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(10.0, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(140, 1)));
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
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ELYTRA_FLYING);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.TRIDENT_RIPTIDE_3);
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
            player.startAutoSpinAttack(12);
        }
        super.onClientCast(level, spellLevel, entity, castData);
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource,
            MagicData playerMagicData) {
        // 1. ผู้ร่ายได้รับบัฟ: Speed 2 (7 วินาที) และ Strength 1 (7 วินาที)
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 140, 1, false, false, true));
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 140, 0, false, false, true));

        // 2. คำนวณแรงพุ่ง Spin Attack ไปข้างหน้าระยะ 15 บล็อก
        entity.hasImpulse = true;
        Vec3 forward = entity.getLookAngle();
        double upwardness = forward.dot(new Vec3(0, 1, 0));
        double remap = 1 - (Math.max(0, upwardness) * 0.6);
        Vec3 impulse = forward.scale(2.4).multiply(1, remap, 1);
        if (entity.onGround()) {
            impulse = impulse.add(0, 0.2, 0);
        }

        // ซิงค์การพุ่งไปยัง Client
        playerMagicData.setAdditionalCastData(
                new ImpulseCastData((float) impulse.x, (float) impulse.y, (float) impulse.z, true));
        playerMagicData.getSyncedData().setSpinAttackType(GALE_SPIN);

        entity.setDeltaMovement(impulse);
        entity.hurtMarked = true;

        if (entity instanceof Player player) {
            player.startAutoSpinAttack(12);
        }

        // 3. มอบ GaleDriveDashEffect เพื่อขับเคลื่อนการพุ่งและตรวจจับ 1 เป้าหมาย
        entity.addEffect(new MobEffectInstance(MobEffectsRegistry.GALE_DRIVE_DASH.get(), 12,
                (int) getSpellPower(spellLevel, entity), false, false, false));

        world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.0f, 1.0f);

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }
}
