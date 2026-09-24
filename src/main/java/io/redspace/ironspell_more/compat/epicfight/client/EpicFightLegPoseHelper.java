package io.redspace.ironspell_more.compat.epicfight.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Isolated client-side helper for sampling Epic Fight biped leg joint transforms.
 * Only loaded when CompatMods.isEpicFightLoaded() is true.
 */
public final class EpicFightLegPoseHelper {
    private EpicFightLegPoseHelper() {
    }

    public static boolean isBattleMode(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        try {
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
            if (patch instanceof yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch<?> playerPatch) {
                return playerPatch.isEpicFightMode();
            }
            return patch != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static Vec3 getLegJointWorldPos(LivingEntity entity, boolean isLeft) {
        if (entity == null) {
            return null;
        }

        try {
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
            if (patch == null) {
                return null;
            }

            Armature armature = patch.getArmature();
            if (armature == null) {
                return null;
            }

            HumanoidArmature humanoidArmature = (HumanoidArmature) Armatures.BIPED.get();
            if (humanoidArmature == null) {
                return null;
            }

            Joint targetJoint = isLeft ? humanoidArmature.legL : humanoidArmature.legR;
            Pose pose = patch.getAnimator().getPose(0.0F);
            OpenMatrix4f transform = armature.getBoundTransformFor(pose, targetJoint);

            OpenMatrix4f rot = new OpenMatrix4f();
            rot.rotate((float) -Math.toRadians(entity.getYRot() + 180.0F), new Vec3f(0.0F, 1.0F, 0.0F));
            OpenMatrix4f.mul(transform, rot, transform);

            double wx = transform.m30 + entity.getX();
            double wy = transform.m31 + entity.getY();
            double wz = transform.m32 + entity.getZ();

            return new Vec3(wx, wy, wz);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
