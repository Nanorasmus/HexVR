package me.nanorasmus.nanodev.hex_vr.entity.custom;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ArmorStandEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Matrix4f;

public class TextEntityRenderer extends ArmorStandEntityRenderer {
    public static final float nameTagScaleMultiplier = 0.2f;

    public TextEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    protected void renderLabelIfPresent(ArmorStandEntity entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        double d = this.dispatcher.getSquaredDistanceToCamera((Entity)entity);
        if (d > 4096.0) {
            return;
        }
        boolean bl = !((Entity)entity).isSneaky();
        float f = ((Entity)entity).getHeight() + 0.5f;
        int i = "deadmau5".equals(text.getString()) ? -10 : 0;
        matrices.push();
        if (entity instanceof TextEntity) {
            matrices.scale(nameTagScaleMultiplier, nameTagScaleMultiplier, nameTagScaleMultiplier);
        }
        matrices.translate(0.0, f, 0.0);
        matrices.multiply(this.dispatcher.getRotation());;
        matrices.scale(-0.025f, -0.025f, 0.025f);
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        float g = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25f);
        int j = (int)(g * 255.0f) << 24;
        TextRenderer textRenderer = this.getTextRenderer();
        float h = -textRenderer.getWidth(text) / 2;
        textRenderer.draw(text, h, (float)i, 0x20FFFFFF, false, matrix4f, vertexConsumers, bl, j, light);

        if (bl) {
            textRenderer.draw(text, h, (float)i, -1, false, matrix4f, vertexConsumers, false, 0, light);
        }
        matrices.pop();
    }
}
