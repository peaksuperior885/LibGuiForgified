package io.github.cottonmc.cotton.gui.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*; // BufferBuilder, Tessellator, DefaultVertexFormat, VertexFormat
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Minecraft; // MinecraftClient -> Minecraft
import net.minecraft.client.gui.Font; // TextRenderer -> Font
import net.minecraft.client.gui.GuiGraphics; // DrawContext -> GuiGraphics
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.FormattedCharSequence; // OrderedText -> FormattedCharSequence
import net.minecraft.network.chat.Style; // net.minecraft.text.Style -> net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation; // Identifier -> ResourceLocation

import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment;
import io.github.cottonmc.cotton.gui.widget.data.Texture;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * {@code ScreenDrawing} contains utility methods for drawing contents on a screen.
 */
public class ScreenDrawing {
	private ScreenDrawing() {}

	/**
	 * Draws a textured rectangle.
	 */
	public static void texturedRect(GuiGraphics context, int x, int y, int width, int height, ResourceLocation texture, int color) {
		texturedRect(context, x, y, width, height, texture, 0, 0, 1, 1, color, 1.0f);
	}

	/**
	 * Draws a textured rectangle.
	 */
	public static void texturedRect(GuiGraphics context, int x, int y, int width, int height, ResourceLocation texture, int color, float opacity) {
		texturedRect(context, x, y, width, height, texture, 0, 0, 1, 1, color, opacity);
	}

	/**
	 * Draws a textured rectangle.
	 */
	public static void texturedRect(GuiGraphics context, int x, int y, int width, int height, ResourceLocation texture, float u1, float v1, float u2, float v2, int color) {
		texturedRect(context, x, y, width, height, texture, u1, v1, u2, v2, color, 1.0f);
	}

	/**
	 * Draws a textured rectangle.
	 */
	public static void texturedRect(GuiGraphics context, int x, int y, int width, int height, Texture texture, int color) {
		texturedRect(context, x, y, width, height, texture, color, 1.0f);
	}

	/**
	 * Draws a textured rectangle.
	 */
	public static void texturedRect(GuiGraphics context, int x, int y, int width, int height, Texture texture, int color, float opacity) {
		texturedRect(context, x, y, width, height, texture.image(), texture.u1(), texture.v1(), texture.u2(), texture.v2(), color, opacity);
	}

	/**
	 * Draws a textured rectangle using modern 1.21.1 VertexConsumer layouts.
	 */
	public static void texturedRect(GuiGraphics context, int x, int y, int width, int height, ResourceLocation texture, float u1, float v1, float u2, float v2, int color, float opacity) {
		if (width <= 0) width = 1;
		if (height <= 0) height = 1;

		float r = (color >> 16 & 255) / 255.0F;
		float g = (color >> 8 & 255) / 255.0F;
		float b = (color & 255) / 255.0F;

		Matrix4f model = context.pose().last().pose(); // context.getMatrices().peek().getPositionMatrix() -> context.pose().last().pose()

		RenderSystem.enableBlend();
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.setShaderColor(r, g, b, opacity);
		RenderSystem.setShader(GameRenderer::getPositionTexShader); // getPositionTexProgram -> getPositionTexShader

		Tesselator tessellator = Tesselator.getInstance();
		// 1.21.1 requires using the built-in direct buffer builder allocation
		BufferBuilder buffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX); // VertexFormat.DrawMode -> VertexFormat.Mode, VertexFormats.POSITION_TEXTURE -> DefaultVertexFormat.POSITION_TEX

		// Fixed modern vertex chain builders for 1.21.1
		buffer.addVertex(model, (float) x,         (float) (y + height), 0.0F).setUv(u1, v2);
		buffer.addVertex(model, (float) (x + width), (float) (y + height), 0.0F).setUv(u2, v2);
		buffer.addVertex(model, (float) (x + width), (float) y,          0.0F).setUv(u2, v1);
		buffer.addVertex(model, (float) x,         (float) y,          0.0F).setUv(u1, v1);

		BufferUploader.drawWithShader(buffer.buildOrThrow()); // BufferRenderer.drawWithGlobalProgram -> BufferUploader.drawWithShader
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // Reset shader colors safely
	}

	/**
	 * Draws a textured rectangle with UV values based on the width and height.
	 */
	public static void texturedGuiRect(GuiGraphics context, int x, int y, int width, int height, ResourceLocation texture, int textureX, int textureY, int color) {
		float px = 1/256f;
		texturedRect(context, x, y, width, height, texture, textureX*px, textureY*px, (textureX+width)*px, (textureY+height)*px, color);
	}

	/**
	 * Draws a textured rectangle with UV values based on the width and height.
	 */
	public static void texturedGuiRect(GuiGraphics context, int left, int top, int width, int height, ResourceLocation texture, int color) {
		texturedGuiRect(context, left, top, width, height, texture, 0, 0, color);
	}

	/**
	 * Draws an untextured rectangle of the specified RGB color.
	 */
	public static void coloredRect(GuiGraphics context, int left, int top, int width, int height, int color) {
		if (width <= 0) width = 1;
		if (height <= 0) height = 1;

		context.fill(left, top, left + width, top + height, color);
	}

	/**
	 * Draws a beveled, round rectangle that is substantially similar to default Minecraft UI panels.
	 */
	public static void drawGuiPanel(GuiGraphics context, int x, int y, int width, int height) {
		if (LibGui.isDarkMode()) drawGuiPanel(context, x, y, width, height, 0xFF0B0B0B, 0xFF2F2F2F, 0xFF414141, 0xFF000000);
		else drawGuiPanel(context, x, y, width, height, 0xFF555555, 0xFFC6C6C6, 0xFFFFFFFF, 0xFF000000);
	}

	/**
	 * Draws a beveled, round, and colored rectangle that is substantially similar to default Minecraft UI panels.
	 */
	public static void drawGuiPanel(GuiGraphics context, int x, int y, int width, int height, int panelColor) {
		int shadowColor = multiplyColor(panelColor, 0.50f);
		int hilightColor = multiplyColor(panelColor, 1.25f);

		drawGuiPanel(context, x, y, width, height, shadowColor, panelColor, hilightColor, 0xFF000000);
	}

	/**
	 * Draws a beveled, round rectangle with custom edge colors that is substantially similar to default Minecraft UI panels.
	 */
	public static void drawGuiPanel(GuiGraphics context, int x, int y, int width, int height, int shadow, int panel, int hilight, int outline) {
		coloredRect(context, x + 3,         y + 3,          width - 6, height - 6, panel);

		coloredRect(context, x + 2,         y + 1,          width - 4, 2,          hilight);
		coloredRect(context, x + 2,         y + height - 3, width - 4, 2,          shadow);
		coloredRect(context, x + 1,         y + 2,          2,         height - 4, hilight);
		coloredRect(context, x + width - 3, y + 2,          2,         height - 4, shadow);
		coloredRect(context, x + width - 3, y + 2,          1,         1,          panel);
		coloredRect(context, x + 2,         y + height - 3, 1,         1,          panel);
		coloredRect(context, x + 3,         y + 3,          1,         1,          hilight);
		coloredRect(context, x + width - 4, y + height - 4, 1,         1,          shadow);

		coloredRect(context, x + 2,         y,              width - 4, 1,          outline);
		coloredRect(context, x,             y + 2,          1,         height - 4, outline);
		coloredRect(context, x + width - 1, y + 2,          1,         height - 4, outline);
		coloredRect(context, x + 2,         y + height - 1, width - 4, 1,          outline);
		coloredRect(context, x + 1,         y + 1,          1,         1,          outline);
		coloredRect(context, x + 1,         y + height - 2, 1,         1,          outline);
		coloredRect(context, x + width - 2, y + 1,          1,         1,          outline);
		coloredRect(context, x + width - 2, y + height - 2, 1,         1,          outline);
	}

	/**
	 * Draws a default-sized recessed itemslot panel
	 */
	public static void drawBeveledPanel(GuiGraphics context, int x, int y) {
		drawBeveledPanel(context, x, y, 18, 18, 0xFF373737, 0xFF8b8b8b, 0xFFFFFFFF);
	}

	/**
	 * Draws a default-color recessed itemslot panel of variable size
	 */
	public static void drawBeveledPanel(GuiGraphics context, int x, int y, int width, int height) {
		drawBeveledPanel(context, x, y, width, height, 0xFF373737, 0xFF8b8b8b, 0xFFFFFFFF);
	}

	/**
	 * Draws a generalized-case beveled panel. Can be inset or outset depending on arguments.
	 */
	public static void drawBeveledPanel(GuiGraphics context, int x, int y, int width, int height, int topleft, int panel, int bottomright) {
		coloredRect(context, x,             y,              width,     height,     panel);
		coloredRect(context, x,             y,              width - 1, 1,          topleft);
		coloredRect(context, x,             y + 1,          1,         height - 2, topleft);
		coloredRect(context, x + width - 1, y + 1,          1,         height - 1, bottomright);
		coloredRect(context, x + 1,         y + height - 1, width - 1, 1,          bottomright);
	}

	/**
	 * Draws a string with a custom alignment.
	 */
	public static void drawString(GuiGraphics context, String s, HorizontalAlignment align, int x, int y, int width, int color) {
		Font font = Minecraft.getInstance().font; // textRenderer -> font
		switch (align) {
			case LEFT -> {
				context.drawString(font, s, x, y, color, false); // drawText -> drawString
			}

			case CENTER -> {
				int wid = font.width(s); // getWidth -> width
				int l = (width / 2) - (wid / 2);
				context.drawString(font, s, x + l, y, color, false);
			}

			case RIGHT -> {
				int wid = font.width(s);
				int l = width - wid;
				context.drawString(font, s, x + l, y, color, false);
			}
		}
	}

	/**
	 * Draws a text component with a custom alignment.
	 */
	public static void drawString(GuiGraphics context, FormattedCharSequence text, HorizontalAlignment align, int x, int y, int width, int color) { // OrderedText -> FormattedCharSequence
		Font font = Minecraft.getInstance().font;
		switch (align) {
			case LEFT -> {
				context.drawString(font, text, x, y, color, false);
			}

			case CENTER -> {
				int wid = font.width(text);
				int l = (width / 2) - (wid / 2);
				context.drawString(font, text, x + l, y, color, false);
			}

			case RIGHT -> {
				int wid = font.width(text);
				int l = width - wid;
				context.drawString(font, text, x + l, y, color, false);
			}
		}
	}

	/**
	 * Draws a shadowed string.
	 */
	public static void drawStringWithShadow(GuiGraphics context, String s, HorizontalAlignment align, int x, int y, int width, int color) {
		Font font = Minecraft.getInstance().font;
		switch (align) {
			case LEFT -> {
				context.drawString(font, s, x, y, color, true);
			}

			case CENTER -> {
				int wid = font.width(s);
				int l = (width / 2) - (wid / 2);
				context.drawString(font, s, x + l, y, color, true);
			}

			case RIGHT -> {
				int wid = font.width(s);
				int l = width - wid;
				context.drawString(font, s, x + l, y, color, true);
			}
		}
	}

	/**
	 * Draws a shadowed text component.
	 */
	public static void drawStringWithShadow(GuiGraphics context, FormattedCharSequence text, HorizontalAlignment align, int x, int y, int width, int color) { // OrderedText -> FormattedCharSequence
		Font font = Minecraft.getInstance().font;
		switch (align) {
			case LEFT -> {
				context.drawString(font, text, x, y, color, true);
			}

			case CENTER -> {
				int wid = font.width(text);
				int l = (width / 2) - (wid / 2);
				context.drawString(font, text, x + l, y, color, true);
			}

			case RIGHT -> {
				int wid = font.width(text);
				int l = width - wid;
				context.drawString(font, text, x + l, y, color, true);
			}
		}
	}

	/**
	 * Draws a left-aligned string.
	 */
	public static void drawString(GuiGraphics context, String s, int x, int y, int color) {
		context.drawString(Minecraft.getInstance().font, s, x, y, color, false);
	}

	/**
	 * Draws a left-aligned text component.
	 */
	public static void drawString(GuiGraphics context, FormattedCharSequence text, int x, int y, int color) { // OrderedText -> FormattedCharSequence
		context.drawString(Minecraft.getInstance().font, text, x, y, color, false);
	}

	/**
	 * Draws the text hover effects for a text style.
	 */
	public static void drawTextHover(GuiGraphics context, @Nullable Style textStyle, int x, int y) {
		if (textStyle != null) {
			context.renderComponentHoverEffect(Minecraft.getInstance().font, textStyle, x, y); // drawHoverEvent -> renderComponentHoverEffect
		}
	}

	public static int colorAtOpacity(int opaque, float opacity) {
		if (opacity<0.0f) opacity=0.0f;
		if (opacity>1.0f) opacity=1.0f;

		int a = (int)(opacity * 255.0f);

		return (opaque & 0xFFFFFF) | (a << 24);
	}

	public static int multiplyColor(int color, float amount) {
		int a = color & 0xFF000000;
		float r = (color >> 16 & 255) / 255.0F;
		float g = (color >> 8  & 255) / 255.0F;
		float b = (color       & 255) / 255.0F;

		r = Math.min(r*amount, 1.0f);
		g = Math.min(g*amount, 1.0f);
		b = Math.min(b*amount, 1.0f);

		int ir = (int)(r*255);
		int ig = (int)(g*255);
		int ib = (int)(b*255);

		return
			a |
				(ir << 16) |
				(ig <<  8) |
				ib;
	}
}
