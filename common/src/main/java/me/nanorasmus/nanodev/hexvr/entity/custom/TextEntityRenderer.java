package me.nanorasmus.nanodev.hexvr.entity.custom;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ArmorStandEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

public class TextEntityRenderer extends ArmorStandEntityRenderer {
    public static final float nameTagScaleMultiplier = 0.2f;

    public TextEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    protected void renderLabelIfPresent(ArmorStandEntity entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        double d = this.dispatcher.getSquaredDistanceToCamera(entity);
        if (!(d > 4096.0)) {
            boolean bl = !entity.isSneaky();
            float f = entity.getNameLabelHeight();
            int i = "deadmau5".equals(text.getString()) ? -10 : 0;
            matrices.push();
            if (entity instanceof TextEntity) {
                matrices.scale(nameTagScaleMultiplier, nameTagScaleMultiplier, nameTagScaleMultiplier);

                OrderedText patterns = ((TextEntity) entity).patterns;

                matrices.translate(0.0F, f, 0.0F);
                matrices.multiply(this.dispatcher.getRotation());
                matrices.scale(-0.025F, -0.025F, 0.025F);
                Matrix4f matrix4f = matrices.peek().getPositionMatrix();
                float g = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F);
                int j = (int)(g * 255.0F) << 24;
                TextRenderer textRenderer = this.getTextRenderer();
                float h = (float)(-textRenderer.getWidth(patterns) / 2);
                textRenderer.draw(patterns, h, (float)i, 553648127, false, matrix4f, vertexConsumers, bl ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL, j, light);
                if (bl) {
                    textRenderer.draw(patterns, h, (float)i, -1, false, matrix4f, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, light);
                }
                matrices.pop();
                return;
            }
            matrices.translate(0.0F, f, 0.0F);
            matrices.multiply(this.dispatcher.getRotation());
            matrices.scale(-0.025F, -0.025F, 0.025F);
            Matrix4f matrix4f = matrices.peek().getPositionMatrix();
            float g = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F);
            int j = (int)(g * 255.0F) << 24;
            TextRenderer textRenderer = this.getTextRenderer();
            float h = (float)(-textRenderer.getWidth(text) / 2);
            textRenderer.draw(text, h, (float)i, 553648127, false, matrix4f, vertexConsumers, bl ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL, j, light);
            if (bl) {
                textRenderer.draw(text, h, (float)i, -1, false, matrix4f, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, light);
            }

            matrices.pop();
        }
    }
}
