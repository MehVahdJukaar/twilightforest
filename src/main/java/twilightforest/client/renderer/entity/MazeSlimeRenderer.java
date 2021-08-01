package twilightforest.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.resources.ResourceLocation;
import twilightforest.TwilightForestMod;
import twilightforest.client.model.TFModelLayers;
import twilightforest.entity.MazeSlimeEntity;

public class MazeSlimeRenderer extends MobRenderer<MazeSlimeEntity, SlimeModel<MazeSlimeEntity>> {

	private static final ResourceLocation textureLoc = TwilightForestMod.getModelTexture("mazeslime.png");

	public MazeSlimeRenderer(EntityRendererProvider.Context manager, float shadowSize) {
		super(manager, new SlimeModel(manager.bakeLayer(TFModelLayers.MAZE_SLIME)), shadowSize);
		this.addLayer(new SlimeOuterLayer(this, manager.getModelSet()));
	}

	@Override
	public void render(MazeSlimeEntity entityIn, float entityYaw, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn) {
		this.shadowRadius = 0.25F * (float)entityIn.getSize();
		if(this.model.riding) matrixStackIn.translate(0, 0.25F, 0);
		super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
	}

	protected void scale(MazeSlimeEntity p_115983_, PoseStack p_115984_, float p_115985_) {
		p_115984_.scale(0.999F, 0.999F, 0.999F);
		p_115984_.translate(0.0D, 0.0010000000474974513D, 0.0D);
		float var5 = (float)p_115983_.getSize();
		float var6 = Mth.lerp(p_115985_, p_115983_.oSquish, p_115983_.squish) / (var5 * 0.5F + 1.0F);
		float var7 = 1.0F / (var6 + 1.0F);
		p_115984_.scale(var7 * var5, 1.0F / var7 * var5, var7 * var5);
	}

	@Override
	public ResourceLocation getTextureLocation(MazeSlimeEntity entity) {
		return textureLoc;
	}
}