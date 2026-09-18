package io.redspace.ironspell_more.entity.spells.gale_drive;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GaleDriveVortexModel extends GeoModel<GaleDriveVortexEntity> {
    private static final ResourceLocation MODEL = ResourceLocation
            .fromNamespaceAndPath(io.redspace.ironspell_more.IronSpellMore.MODID, "geo/gale_vortex.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation
            .fromNamespaceAndPath(io.redspace.ironspell_more.IronSpellMore.MODID, "textures/entity/gale_vortex.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
            io.redspace.ironspell_more.IronSpellMore.MODID, "animations/gale_vortex.animation.json");

    @Override
    public ResourceLocation getModelResource(GaleDriveVortexEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(GaleDriveVortexEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(GaleDriveVortexEntity animatable) {
        return ANIMATION;
    }
}
