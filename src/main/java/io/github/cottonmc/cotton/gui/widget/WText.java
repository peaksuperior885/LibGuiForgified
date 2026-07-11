package io.github.cottonmc.cotton.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;

import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment;
import io.github.cottonmc.cotton.gui.widget.data.VerticalAlignment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Objects;

/**
 * Ported WText for Forge 1.21.1 (Parchment mappings).
 */
public class WText extends WWidget {

	protected Component text;
	protected int color;
	protected int darkmodeColor;
	protected HorizontalAlignment horizontalAlignment = HorizontalAlignment.LEFT;
	protected VerticalAlignment verticalAlignment = VerticalAlignment.TOP;

	@OnlyIn(Dist.CLIENT)
	private List<FormattedCharSequence> wrappedLines;
	private boolean wrappingScheduled = false;

	public WText(Component text) {
		this(text, WLabel.DEFAULT_TEXT_COLOR);
	}

	public WText(Component text, int color) {
		this.text = Objects.requireNonNull(text, "text must not be null");
		this.color = color;
		this.darkmodeColor = (color == WLabel.DEFAULT_TEXT_COLOR) ? WLabel.DEFAULT_DARKMODE_TEXT_COLOR : color;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void paint(GuiGraphics context, int x, int y, int mouseX, int mouseY) {
		Font font = Minecraft.getInstance().font;
		if (wrappedLines == null || wrappingScheduled) {
			wrappedLines = font.split(text, width);
			wrappingScheduled = false;
		}

		int yOffset = switch (verticalAlignment) {
			case CENTER -> height / 2 - font.lineHeight * wrappedLines.size() / 2;
			case BOTTOM -> height - font.lineHeight * wrappedLines.size();
			case TOP -> 0;
		};

		for (int i = 0; i < wrappedLines.size(); i++) {
			int c = shouldRenderInDarkMode() ? darkmodeColor : color;
			ScreenDrawing.drawString(context, wrappedLines.get(i), horizontalAlignment, x, y + yOffset + i * font.lineHeight, width, c);
		}
	}

	@OnlyIn(Dist.CLIENT)
	public void updateNarration(NarrationElementOutput builder) {
		builder.add(NarratedElementType.TITLE, text);
	}

	public WText setText(Component text) {
		this.text = text;
		this.wrappingScheduled = true;
		return this;
	}

	public int getColor() { return color; }

	public WText setColor(int color) {
		this.color = color;
		return this;
	}

	public int getDarkmodeColor() { return darkmodeColor; }

	public WText setDarkmodeColor(int darkmodeColor) {
		this.darkmodeColor = darkmodeColor;
		return this;
	}

	public WText setColor(int color, int darkmodeColor) {
		this.color = color;
		this.darkmodeColor = darkmodeColor;
		return this;
	}

	public WText disableDarkmode() {
		this.darkmodeColor = this.color;
		return this;
	}

	public HorizontalAlignment getHorizontalAlignment() { return horizontalAlignment; }

	public WText setHorizontalAlignment(HorizontalAlignment horizontalAlignment) {
		this.horizontalAlignment = horizontalAlignment;
		return this;
	}

	public VerticalAlignment getVerticalAlignment() { return verticalAlignment; }

	public WText setVerticalAlignment(VerticalAlignment verticalAlignment) {
		this.verticalAlignment = verticalAlignment;
		return this;
	}
}
