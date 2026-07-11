package io.github.cottonmc.cotton.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation; // Yarn Identifier -> Mojmap/Parchment ResourceLocation

import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.widget.data.Texture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * A sprite whose texture will be tiled.
 */
public class WTiledSprite extends WSprite {
	private int tileWidth;
	private int tileHeight;

	public WTiledSprite(int tileWidth, int tileHeight, ResourceLocation image) {
		super(image);
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
	}

	public WTiledSprite(int tileWidth, int tileHeight, int frameTime, ResourceLocation... frames) {
		super(frameTime, frames);
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
	}

	public WTiledSprite(int tileWidth, int tileHeight, Texture image) {
		super(image);
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
	}

	public WTiledSprite(int tileWidth, int tileHeight, int frameTime, Texture... frames) {
		super(frameTime, frames);
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
	}

	public void setTileSize(int width, int height) {
		this.tileWidth = width;
		this.tileHeight = height;
	}

	public int getTileWidth() { return tileWidth; }
	public int getTileHeight() { return tileHeight; }

	public WTiledSprite setTileWidth(int tileWidth) {
		this.tileWidth = tileWidth;
		return this;
	}

	public WTiledSprite setTileHeight(int tileHeight) {
		this.tileHeight = tileHeight;
		return this;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void paintFrame(GuiGraphics graphics, int x, int y, Texture texture) {
		// Y Direction (down)
		for (int tileYOffset = 0; tileYOffset < height; tileYOffset += tileHeight) {
			// X Direction (right)
			for (int tileXOffset = 0; tileXOffset < width; tileXOffset += tileWidth) {

				// Calculate how much to draw, so we don't render tiles outside the widget bounds
				int currentWidth = Math.min(tileWidth, width - tileXOffset);
				int currentHeight = Math.min(tileHeight, height - tileYOffset);

				// Use ScreenDrawing with the GuiGraphics instance
				ScreenDrawing.texturedRect(
					graphics,
					x + tileXOffset,
					y + tileYOffset,
					currentWidth,
					currentHeight,
					texture,
					tint
				);
			}
		}
	}
}
