package io.redspace.ironspell_more.entity.spells.gale_drive;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GaleDriveVortexRenderer extends GeoEntityRenderer<GaleDriveVortexEntity> {
    public GaleDriveVortexRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GaleDriveVortexModel());
        this.addRenderLayer(new GaleDriveVortexEmissiveLayer(this));
    }
}
