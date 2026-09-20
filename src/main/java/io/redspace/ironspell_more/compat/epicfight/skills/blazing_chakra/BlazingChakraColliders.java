package io.redspace.ironspell_more.compat.epicfight.skills.blazing_chakra;

import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.collider.OBBCollider;

/**
 * Dedicated hitboxes / colliders for Blazing Chakra skill.
 */
public final class BlazingChakraColliders {
    public static final Collider IMPACT = new OBBCollider(2.0D, 2.0D, 2.0D, 0.0D, 0.0D, 0.0D);

    private BlazingChakraColliders() {
    }
}
