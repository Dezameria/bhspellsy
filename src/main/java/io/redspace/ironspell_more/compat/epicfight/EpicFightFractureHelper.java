package io.redspace.ironspell_more.compat.epicfight;

import io.redspace.ironspell_more.IronSpellMore;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

/**
 * Safe common-code boundary for the optional Epic Fight integration.
 */
public final class EpicFightFractureHelper {
    private static final boolean EPIC_FIGHT_LOADED = ModList.get().isLoaded("epicfight");
    private static boolean linkageFailed;

    private EpicFightFractureHelper() {
    }

    public static boolean trySpawnFracture(LivingEntity source, Level level, Vec3 samplePosition,
            int searchUp, int searchDown, double radius) {
        if (!EPIC_FIGHT_LOADED || linkageFailed || level.isClientSide || source == null) {
            return false;
        }

        try {
            return EpicFightLoadedBridge.trySpawnFracture(source, level, samplePosition, searchUp, searchDown,
                    radius);
        } catch (LinkageError error) {
            linkageFailed = true;
            IronSpellMore.LOGGER.error("Epic Fight fracture integration was disabled after a linkage failure", error);
            return false;
        }
    }
}
