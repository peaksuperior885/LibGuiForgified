package io.github.cottonmc.cotton.gui.widget;

import net.minecraft.client.gui.GuiGraphics; // DrawContext -> GuiGraphics
import net.minecraft.resources.ResourceLocation; // Identifier -> ResourceLocation

import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.widget.data.Texture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class WSprite extends WWidget {
	protected int currentFrame = 0;
	protected long currentFrameTime = 0;
	protected Texture[] frames;
	protected int frameTime;
	protected long lastFrame;
	protected boolean singleImage = false;
	protected int tint = 0xFFFFFFFF;

	/**
	 * Create a new sprite with a single image.
	 */
	public WSprite(Texture texture) {
		this.frames = new Texture[]{texture};
		this.singleImage = true;
	}

	/**
	 * Create a new sprite with a single image.
	 */
	public WSprite(ResourceLocation image) { // Identifier -> ResourceLocation
		this(new Texture(image));
	}

	/**
	 * Create a new sprite with a single image and custom UV values.
	 */
	public WSprite(ResourceLocation image, float u1, float v1, float u2, float v2) { // Identifier -> ResourceLocation
		this(new Texture(image, u1, v1, u2, v2));
	}

	/**
	 * Create a new animated sprite.
	 */
	public WSprite(int frameTime, ResourceLocation... frames) { // Identifier -> ResourceLocation
		this.frameTime = frameTime;
		this.frames = new Texture[frames.length];

		for (int i = 0; i < frames.length; i++) {
			this.frames[i] = new Texture(frames[i]);
		}

		if (frames.length == 1) this.singleImage = true;
	}

	/**
	 * Create a new animated sprite.
	 */
	public WSprite(int frameTime, Texture... frames) {
		this.frameTime = frameTime;
		this.frames = frames;
		if (frames.length == 1) this.singleImage = true;
	}

	/**
	 * Sets the image of this sprite.
	 */
	public WSprite setImage(ResourceLocation image) { // Identifier -> ResourceLocation
		return setImage(new Texture(image));
	}

	/**
	 * Sets the animation frames of this sprite.
	 */
	public WSprite setFrames(ResourceLocation... frames) { // Identifier -> ResourceLocation
		Texture[] textures = new Texture[frames.length];
		for (int i = 0; i < frames.length; i++) {
			textures[i] = new Texture(frames[i]);
		}
		return setFrames(textures);
	}

	public WSprite setImage(Texture image) {
		this.frames = new Texture[]{image};
		this.singleImage = true;
		this.currentFrame = 0;
		this.currentFrameTime = 0;
		return this;
	}

	public WSprite setFrames(Texture... frames) {
		this.frames = frames;
		if (frames.length == 1) singleImage = true;
		if (currentFrame >= frames.length) {
			currentFrame = 0;
			currentFrameTime = 0;
		}
		return this;
	}

	public WSprite setTint(int tint) {
		this.tint = tint;
		return this;
	}

	public WSprite setOpaqueTint(int tint) {
		this.tint = tint | 0xFF000000;
		return this;
	}

	public WSprite setUv(float u1, float v1, float u2, float v2) {
		Texture[] newFrames = new Texture[frames.length];
		for (int i = 0; i < frames.length; i++) {
			newFrames[i] = frames[i].withUv(u1, v1, u2, v2);
		}
		return setFrames(newFrames);
	}

	@Override
	public boolean canResize() {
		return true;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void paint(GuiGraphics context, int x, int y, int mouseX, int mouseY) { // DrawContext -> GuiGraphics
		if (singleImage) {
			paintFrame(context, x, y, frames[0]);
		} else {
			long now = System.nanoTime() / 1_000_000L;

			boolean inBounds = (currentFrame >= 0) && (currentFrame < frames.length);
			if (!inBounds) currentFrame = 0;
			Texture currentFrameTex = frames[currentFrame];
			paintFrame(context, x, y, currentFrameTex);

			long elapsed = now - lastFrame;
			currentFrameTime += elapsed;
			if (currentFrameTime >= frameTime) {
				currentFrame++;
				if (currentFrame >= frames.length - 1) {
					currentFrame = 0;
				}
				currentFrameTime = 0;
			}
			this.lastFrame = now;
		}
	}

	@OnlyIn(Dist.CLIENT)
	protected void paintFrame(GuiGraphics context, int x, int y, Texture texture) { // DrawContext -> GuiGraphics
		ScreenDrawing.texturedRect(context, x, y, getWidth(), getHeight(), texture, tint);
	}
}
