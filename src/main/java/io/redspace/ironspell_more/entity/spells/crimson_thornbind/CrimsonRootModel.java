package io.redspace.ironspell_more.entity.spells.crimson_thornbind;

import io.redspace.ironspell_more.IronSpellMore;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CrimsonRootModel extends GeoModel<CrimsonRootEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(IronSpellMore.MODID, "textures/entity/crimson_root.png");
    private static final ResourceLocation MODEL = new ResourceLocation(IronSpellMore.MODID, "geo/crimson_root.geo.json");
    public static final ResourceLocation ANIMS = new ResourceLocation(IronSpellMore.MODID, "animations/crimson_root_animations.json");

    @Override
    public ResourceLocation getTextureResource(CrimsonRootEntity object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getModelResource(CrimsonRootEntity object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getAnimationResource(CrimsonRootEntity animatable) {
        return ANIMS;
    }
}
