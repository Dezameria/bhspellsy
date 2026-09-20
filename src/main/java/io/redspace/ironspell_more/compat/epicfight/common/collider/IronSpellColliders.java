package io.redspace.ironspell_more.compat.epicfight.common.collider;

import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.collider.OBBCollider;

/**
 * Centralized custom hitbox/collider geometries for Iron's Spells attacks.
 */
public final class IronSpellColliders {
    public static final Collider FIST = new OBBCollider(0.4D, 0.4D, 0.4D, 0.0D, 0.0D, 0.4D);
    public static final Collider KICK = new OBBCollider(0.5D, 0.5D, 0.7D, 0.0D, 0.0D, 0.6D);
    public static final Collider SLAM = new OBBCollider(1.2D, 1.0D, 1.2D, 0.0D, -0.5D, 0.8D);
    public static final Collider SWEEP = new OBBCollider(1.5D, 0.6D, 1.5D, 0.0D, 0.2D, 1.0D);
    public static final Collider BLAZING_CHAKRA = new OBBCollider(2.0D, 2.0D, 2.0D, 0.0D, 0.0D, 0.0D);

    private IronSpellColliders() {
    }

    public static Collider createBox(double halfX, double halfY, double halfZ, double centerX, double centerY, double centerZ) {
        return new OBBCollider(halfX, halfY, halfZ, centerX, centerY, centerZ);
    }
}
