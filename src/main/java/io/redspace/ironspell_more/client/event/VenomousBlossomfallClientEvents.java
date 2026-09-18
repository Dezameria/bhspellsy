package io.redspace.ironspell_more.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.github.L_Ender.cataclysm.client.particle.StormParticle;
import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.entity.spells.venomous_blossomfall.AzureVenomNeedleModel;
import io.redspace.ironspell_more.client.particle.ShockwaveParticleOptionCustom;
import io.redspace.ironspell_more.entity.spells.venomous_blossomfall.AzureVenomNeedleRenderer;
import io.redspace.ironspell_more.spells.nature.VenomousBlossomfallSpell;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.network.casting.CancelCastPacket;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = IronSpellMore.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VenomousBlossomfallClientEvents {
    private static final String SPELL_ID = "ironspell_more:venomous_blossomfall";
    private static final DustParticleOptions GREEN_DUST =
            new DustParticleOptions(new Vector3f(0.25F, 1.0F, 0.35F), 0.8F);
    private static final DustParticleOptions AZURE_DUST =
            new DustParticleOptions(new Vector3f(0.05F, 0.95F, 0.82F), 1.0F);
    private static final Map<UUID, Integer> CLIENT_CAST_START_TICKS = new HashMap<>();

    private static AzureVenomNeedleModel previewModel;

    private VenomousBlossomfallClientEvents() {
    }

    /**
     * Casts started by /cast or another external system do not put the player in
     * vanilla's use-item state, so Iron's normal releaseUsing path cannot finish a
     * held LONG cast. Reuse Iron's native cancel packet as an explicit release
     * request; the spell's server completion handler turns that cancellation into
     * the charged shot.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || !isLocalPlayerCasting()) {
            return;
        }

        PacketDistributor.sendToServer(new CancelCastPacket(false));
        event.setSwingHand(false);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getEntity() == minecraft.player && isLocalPlayerCasting()) {
            event.getInput().forwardImpulse = 0.0F;
            event.getInput().leftImpulse = 0.0F;
            event.getInput().jumping = false;
        }
    }

    @SubscribeEvent
    public static void onUsingItemTick(LivingEntityUseItemEvent.Tick event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getEntity() == minecraft.player
                && isLocalPlayerCasting()
                && VenomousBlossomfallSpell.isFullCharge(getLocalChargeProgress())
                && event.getDuration() <= VenomousBlossomfallSpell.FULL_CHARGE_HOLD_REFRESH_THRESHOLD) {
            event.setDuration(VenomousBlossomfallSpell.FULL_CHARGE_HOLD_USE_DURATION);
        }
    }

    private static net.minecraft.client.resources.sounds.SimpleSoundInstance charge1Sound = null;
    private static net.minecraft.client.resources.sounds.SimpleSoundInstance charge2Sound = null;
    private static int localChargeTickCount = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            CLIENT_CAST_START_TICKS.clear();
            stopChargeSound(minecraft);
            localChargeTickCount = 0;
            return;
        }

        // จัดการเสียง Charge สำหรับผู้เล่น Local
        if (isLocalPlayerCasting() && minecraft.player != null) {
            localChargeTickCount++;
            var soundManager = minecraft.getSoundManager();

            // เริ่มเล่นเสียง charge_1 ครั้งแรกเมื่อเริ่มชาร์จ แล้วปล่อยให้เล่นจบ
            if (localChargeTickCount == 1) {
                stopChargeSound(minecraft);
                charge1Sound = net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        io.redspace.ironspell_more.registry.SoundRegistry.VENOMOUS_BLOSSOMFALL_CHARGE_1.get(), 1.0F, 1.0F);
                soundManager.play(charge1Sound);
            }

            // หลังเริ่ม charge ไปได้ประมาณ 21 วินาที (420 ticks) เริ่มเล่นเสียง charge_2
            // แล้วรัน loop เสียง charge_2 ซ้ำทุกๆ 11 วินาที (220 ticks)
            if (localChargeTickCount >= 420 && (localChargeTickCount - 420) % 220 == 0) {
                if (charge2Sound != null) {
                    soundManager.stop(charge2Sound);
                }
                charge2Sound = net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        io.redspace.ironspell_more.registry.SoundRegistry.VENOMOUS_BLOSSOMFALL_CHARGE_2.get(), 1.0F, 1.0F);
                soundManager.play(charge2Sound);
            }
        } else {
            // เมื่อปล่อย หรือไม่ได้ทำการร่ายแล้ว ให้ทำการ stopSound ทันที
            if (charge1Sound != null || charge2Sound != null || localChargeTickCount > 0) {
                stopChargeSound(minecraft);
            }
            localChargeTickCount = 0;
        }

        for (AbstractClientPlayer player : level.players()) {
            if (isCasting(player)) {
                CLIENT_CAST_START_TICKS.putIfAbsent(player.getUUID(), player.tickCount);
                spawnChargeParticles(level, player, getChargeProgress(player));
            }
        }

        Iterator<Map.Entry<UUID, Integer>> iterator = CLIENT_CAST_START_TICKS.entrySet().iterator();
        while (iterator.hasNext()) {
            UUID playerId = iterator.next().getKey();
            var foundPlayer = level.getPlayerByUUID(playerId);
            if (!(foundPlayer instanceof AbstractClientPlayer player) || !isCasting(player)) {
                iterator.remove();
            }
        }
    }

    private static void stopChargeSound(Minecraft minecraft) {
        var soundManager = minecraft.getSoundManager();
        if (charge1Sound != null) {
            soundManager.stop(charge1Sound);
            charge1Sound = null;
        }
        if (charge2Sound != null) {
            soundManager.stop(charge2Sound);
            charge2Sound = null;
        }
        // สั่ง stopSound ระดับ SoundEvent ทุก category เพื่อความสะอาดหมดจด
        soundManager.stop(
                io.redspace.ironspell_more.registry.SoundRegistry.VENOMOUS_BLOSSOMFALL_CHARGE_1.get().getLocation(),
                null);
        soundManager.stop(
                io.redspace.ironspell_more.registry.SoundRegistry.VENOMOUS_BLOSSOMFALL_CHARGE_2.get().getLocation(),
                null);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            previewModel = null;
            return;
        }

        if (previewModel == null) {
            previewModel = new AzureVenomNeedleModel(
                    minecraft.getEntityModels().bakeLayer(AzureVenomNeedleModel.LAYER_LOCATION));
        }

        float partialTick = event.getPartialTick();
        Vec3 cameraPosition = event.getCamera().getPosition();
        var bufferSource = minecraft.renderBuffers().bufferSource();

        for (AbstractClientPlayer player : level.players()) {
            if (!isCasting(player)) {
                continue;
            }

            float charge = getChargeProgress(player);
            Vec3 look = player.getViewVector(partialTick).normalize();
            Vec3 position = getPreviewPosition(player, look, partialTick);
            float rollSpeed = 8.0F + charge * 34.0F;
            float roll = (player.tickCount + partialTick) * rollSpeed;

            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            poseStack.translate(position.x - cameraPosition.x, position.y - cameraPosition.y,
                    position.z - cameraPosition.z);
            AzureVenomNeedleRenderer.renderNeedle(previewModel, poseStack, bufferSource, look, roll,
                    0.75F + charge * 0.2F, 0.95F);
            poseStack.popPose();
        }

        bufferSource.endBatch(AzureVenomNeedleRenderer.RENDER_TYPE);
    }

    private static boolean isLocalPlayerCasting() {
        return ClientMagicData.isCasting() && SPELL_ID.equals(ClientMagicData.getCastingSpellId());
    }

    private static boolean isCasting(AbstractClientPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == minecraft.player) {
            return isLocalPlayerCasting();
        }
        SyncedSpellData syncedData = ClientMagicData.getSyncedSpellData(player);
        return syncedData.isCasting() && SPELL_ID.equals(syncedData.getCastingSpellId());
    }

    private static float getChargeProgress(AbstractClientPlayer player) {
        if (player == Minecraft.getInstance().player && isLocalPlayerCasting()) {
            return getLocalChargeProgress();
        }
        int startTick = CLIENT_CAST_START_TICKS.getOrDefault(player.getUUID(), player.tickCount);
        return VenomousBlossomfallSpell.normalizeCharge(
                (player.tickCount - startTick) / (float) VenomousBlossomfallSpell.FULL_CHARGE_TICKS);
    }

    private static float getLocalChargeProgress() {
        return VenomousBlossomfallSpell.chargeProgress(
                ClientMagicData.getCastDuration(), ClientMagicData.getCastDurationRemaining());
    }

    private static Vec3 getPreviewPosition(AbstractClientPlayer player, Vec3 look, float partialTick) {
        Vec3 interpolatedBase = new Vec3(
                Mth.lerp(partialTick, player.xo, player.getX()),
                Mth.lerp(partialTick, player.yo, player.getY()) + player.getEyeHeight(),
                Mth.lerp(partialTick, player.zo, player.getZ()));
        Vec3 upReference = Math.abs(look.y) > 0.95D
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = look.cross(upReference).normalize();
        double handSide = player.getMainArm() == HumanoidArm.RIGHT ? 1.0D : -1.0D;
        return interpolatedBase.add(look.scale(0.9D)).add(right.scale(0.2D * handSide)).add(0.0D, -0.16D, 0.0D);
    }

    private static void spawnChargeParticles(ClientLevel level, AbstractClientPlayer player, float charge) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 center = getPreviewPosition(player, look, 1.0F);
        Vec3 upReference = Math.abs(look.y) > 0.95D
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = look.cross(upReference).normalize();
        Vec3 up = right.cross(look).normalize();

        VenomousBlossomfallSpell.ChargeStage chargeStage =
                VenomousBlossomfallSpell.chargeStage(charge);
        spawnStageAura(level, player, chargeStage);
        int particleCount = switch (chargeStage) {
            case SHORT -> 3;
            case MEDIUM -> 7;
            case FULL -> 14;
        };
        double radius = 0.13D + charge * 0.32D;
        double time = player.tickCount * (0.25D + charge * 0.55D);

        for (int index = 0; index < particleCount; index++) {
            double phase = time + Mth.TWO_PI * index / particleCount;
            double axialOffset = ((index / (double) particleCount) - 0.5D) * (0.6D + charge * 0.8D);
            Vec3 radial = right.scale(Math.cos(phase) * radius).add(up.scale(Math.sin(phase) * radius));
            Vec3 particlePosition = center.add(radial).add(look.scale(axialOffset));
            Vec3 inwardMotion = center.subtract(particlePosition).scale(0.08D + charge * 0.08D);
            level.addParticle(index % 2 == 0 ? AZURE_DUST : GREEN_DUST,
                    particlePosition.x, particlePosition.y, particlePosition.z,
                    inwardMotion.x, inwardMotion.y, inwardMotion.z);

            if (chargeStage != VenomousBlossomfallSpell.ChargeStage.SHORT && index % 3 == 0) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        particlePosition.x, particlePosition.y, particlePosition.z,
                        inwardMotion.x, inwardMotion.y, inwardMotion.z);
            }
        }

        if (chargeStage == VenomousBlossomfallSpell.ChargeStage.FULL) {
            level.addParticle(ParticleTypes.BUBBLE_POP, center.x, center.y, center.z,
                    -look.x * 0.04D, -look.y * 0.04D, -look.z * 0.04D);
        }
    }

    private static void spawnStageAura(ClientLevel level, AbstractClientPlayer player,
            VenomousBlossomfallSpell.ChargeStage chargeStage) {
        if (chargeStage != VenomousBlossomfallSpell.ChargeStage.SHORT && player.tickCount % 8 == 0) {
            level.addParticle(new ShockwaveParticleOptionCustom(
                            new Vector3f(0.08F, 0.95F, 0.78F),
                            chargeStage == VenomousBlossomfallSpell.ChargeStage.FULL ? 3.2F : 2.2F,
                            true, new Vector3f(0.0F, 1.0F, 0.0F)),
                    player.getX(), player.getY() + 0.12D, player.getZ(), 0.0D, 0.0D, 0.0D);
        }

        if (chargeStage == VenomousBlossomfallSpell.ChargeStage.FULL && player.tickCount % 3 == 0) {
            float radius = 1.25F + player.getRandom().nextFloat() * 0.75F;
            float height = 0.35F + player.getRandom().nextFloat() * 0.9F;
            level.addParticle(new StormParticle.OrbData(
                            0.05F, 0.95F, 0.78F, radius, height, player.getId()),
                    player.getX(), player.getY(), player.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }
}
