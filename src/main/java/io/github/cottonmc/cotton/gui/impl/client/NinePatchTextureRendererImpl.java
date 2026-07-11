package io.github.cottonmc.cotton.gui.impl.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance; // ShaderProgram -> ShaderInstance
import net.minecraft.client.gui.GuiGraphics; // DrawContext -> GuiGraphics
import com.mojang.blaze3d.vertex.*;
import net.minecraft.resources.ResourceLocation; // Identifier -> ResourceLocation

import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import juuxel.libninepatch.ContextualTextureRenderer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * An implementation of LibNinePatch's {@link ContextualTextureRenderer} for identifiers.
 */
public enum NinePatchTextureRendererImpl implements ContextualTextureRenderer<ResourceLocation, GuiGraphics> { // Identifier, DrawContext updated
	INSTANCE;

	@Override
	public void draw(ResourceLocation texture, GuiGraphics context, int x, int y, int width, int height, float u1, float v1, float u2, float v2) {
		ScreenDrawing.texturedRect(context, x, y, width, height, texture, u1, v1, u2, v2, 0xFF_FFFFFF);
	}

	@Override
	public void drawTiled(ResourceLocation texture, GuiGraphics context, int x, int y, int regionWidth, int regionHeight, int tileWidth, int tileHeight, float u1, float v1, float u2, float v2) {
		RenderSystem.setShader(LibGuiShaders::getTiledRectangle);
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		Matrix4f positionMatrix = context.pose().last().pose(); // getMatrices().peek().getPositionMatrix() -> pose().last().pose()

		onRenderThread(() -> {
			@Nullable ShaderInstance program = RenderSystem.getShader(); // ShaderProgram -> ShaderInstance
			if (program != null) {
				// 1.21.1 safe uniform handling using safe set() overrides
				if (program.safeGetUniform("LibGuiRectanglePos") != null) program.safeGetUniform("LibGuiRectanglePos").set((float) x, (float) y);
				if (program.safeGetUniform("LibGuiTileDimensions") != null) program.safeGetUniform("LibGuiTileDimensions").set((float) tileWidth, (float) tileHeight);
				if (program.safeGetUniform("LibGuiTileUvs") != null) program.safeGetUniform("LibGuiTileUvs").set(u1, v1, u2, v2);
				if (program.safeGetUniform("LibGuiPositionMatrix") != null) program.safeGetUniform("LibGuiPositionMatrix").set(positionMatrix);
			}
		});

		Tesselator tessellator = Tesselator.getInstance();
		// 1.21.1 requires using the explicit begin call on the modern Tessellator layout instance
		BufferBuilder buffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION); // DrawMode.QUADS -> Mode.QUADS, VertexFormats.POSITION -> DefaultVertexFormat.POSITION

		RenderSystem.enableBlend();
		buffer.addVertex(positionMatrix, (float) x, (float) y, 0.0F);
		buffer.addVertex(positionMatrix, (float) x, (float) (y + regionHeight), 0.0F);
		buffer.addVertex(positionMatrix, (float) (x + regionWidth), (float) (y + regionHeight), 0.0F);
		buffer.addVertex(positionMatrix, (float) (x + regionWidth), (float) y, 0.0F);

		BufferUploader.drawWithShader(buffer.buildOrThrow()); // BufferRenderer.drawWithGlobalProgram -> BufferUploader.drawWithShader
		RenderSystem.disableBlend();
	}

	private static void onRenderThread(Runnable renderCall) { // RenderCall -> Runnable
		if (RenderSystem.isOnRenderThread()) {
			renderCall.run();
		} else {
			RenderSystem.recordRenderCall(renderCall::run);
		}
	}
}
