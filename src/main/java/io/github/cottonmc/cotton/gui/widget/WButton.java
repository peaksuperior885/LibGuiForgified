package io.github.cottonmc.cotton.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import io.github.cottonmc.cotton.gui.widget.icon.Icon;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public class WButton extends WWidget {
	private static final ResourceLocation DARK_WIDGETS_LOCATION = ResourceLocation.fromNamespaceAndPath("libgui", "textures/widget/dark_widgets.png");
	private static final ResourceLocation VANILLA_WIDGETS = ResourceLocation.fromNamespaceAndPath("libgui", "textures/widget/widgets.png");
	private static final int BUTTON_HEIGHT = 20;
	private static final int ICON_SPACING = 2;

	@Nullable private Component label;
	protected int color = WLabel.DEFAULT_TEXT_COLOR;
	protected int darkmodeColor = WLabel.DEFAULT_TEXT_COLOR;
	protected int iconSize = 16;
	private boolean active = true; // Renamed from enabled to avoid conflicts
	protected HorizontalAlignment alignment = HorizontalAlignment.CENTER;

	@Nullable private Runnable onClick;
	@Nullable private Icon icon = null;

	public WButton() {}
	public WButton(@Nullable Icon icon) { this.icon = icon; }
	public WButton(@Nullable Component label) { this.label = label; }

	@Override
	public boolean canResize() { return true; }
	@Override
	public boolean canFocus() { return true; }

	@OnlyIn(Dist.CLIENT)
	@Override
	public void paint(GuiGraphics context, int x, int y, int mouseX, int mouseY) {
		boolean hovered = (mouseX >= 0 && mouseY >= 0 && mouseX < getWidth() && mouseY < getHeight());
		int state = active ? (hovered || isFocused() ? 2 : 1) : 0;

		float px = 1/256f;
		float buttonLeft = 0 * px;
		float buttonTop = (46 + (state * 20)) * px;
		int halfWidth = Math.min(getWidth() / 2, 198);
		float buttonWidth = halfWidth * px;
		float buttonHeight = 20 * px;
		float buttonEndLeft = (200 - (getWidth() / 2)) * px;

		ResourceLocation texture = shouldRenderInDarkMode() ? DARK_WIDGETS_LOCATION : VANILLA_WIDGETS;
		ScreenDrawing.texturedRect(context, x, y, getWidth() / 2, 20, texture, buttonLeft, buttonTop, buttonLeft + buttonWidth, buttonTop + buttonHeight, 0xFFFFFFFF);
		ScreenDrawing.texturedRect(context, x + (getWidth() / 2), y, getWidth() / 2, 20, texture, buttonEndLeft, buttonTop, 200 * px, buttonTop + buttonHeight, 0xFFFFFFFF);

		if (icon != null) icon.paint(context, x + ICON_SPACING, y + (BUTTON_HEIGHT - iconSize) / 2, iconSize);
		if (label != null) {
			int textColor = active ? 0xE0E0E0 : 0xA0A0A0;
			int xOffset = (icon != null && alignment == HorizontalAlignment.LEFT) ? ICON_SPACING + iconSize + ICON_SPACING : 0;
			ScreenDrawing.drawStringWithShadow(context, label.getVisualOrderText(), alignment, x + xOffset, y + ((20 - 8) / 2), width, textColor);
		}
	}

	@Override
	public void setSize(int x, int y) { super.setSize(x, BUTTON_HEIGHT); }

	@OnlyIn(Dist.CLIENT)
	@Override
	public InputResult onClick(int x, int y, int button) {
		super.onClick(x, y, button);
		if (active && isWithinBounds(x, y)) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			if (onClick != null) onClick.run();
			return InputResult.PROCESSED;
		}
		return InputResult.IGNORED;
	}

	@OnlyIn(Dist.CLIENT)
	public void updateNarration(NarrationElementOutput builder) {
		if (label != null) builder.add(NarratedElementType.TITLE, label);
	}

	public WButton setOnClick(@Nullable Runnable onClick) {
		this.onClick = onClick;
		return this;
	}

	public static ResourceLocation getTexture(WWidget widget) {
		return widget.shouldRenderInDarkMode() ? DARK_WIDGETS_LOCATION : VANILLA_WIDGETS;
	}

	public boolean isEnabled() { return active; }
	public WButton setEnabled(boolean enabled) { this.active = enabled; return this; }
	public WButton setLabel(Component label) { this.label = label; return this; }
	public @Nullable Component getLabel() { return label; }
}
