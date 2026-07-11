package io.github.cottonmc.cotton.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.cottonmc.cotton.gui.client.BackgroundPainter;
import io.github.cottonmc.cotton.gui.impl.LibGuiCommon;
import io.github.cottonmc.cotton.gui.impl.client.NarrationMessages;
import io.github.cottonmc.cotton.gui.impl.client.NinePatchTextureRendererImpl;
import io.github.cottonmc.cotton.gui.widget.data.Axis;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import juuxel.libninepatch.NinePatch;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.resources.ResourceLocation;

import static io.github.cottonmc.cotton.gui.client.BackgroundPainter.createNinePatch;

public class WScrollBar extends WWidget {
	private static final int SCROLLING_SPEED = 4;

	protected Axis axis = Axis.HORIZONTAL;
	protected int value;
	protected int maxValue = 100;
	protected int window = 16;

	protected int anchor = -1;
	protected int anchorValue = -1;
	protected boolean sliding = false;

	public WScrollBar() {
	}

	public WScrollBar(Axis axis) {
		this.axis = axis;
	}

	@Override
	public void paint(GuiGraphics gui, int x, int y, int mouseX, int mouseY) {
		PoseStack pose = gui.pose();

		boolean darkMode = shouldRenderInDarkMode();

		Painters.BACKGROUND.paintBackground(gui, x, y, this);

		NinePatch<ResourceLocation> painter =
			darkMode ? Painters.SCROLL_BAR_DARK : Painters.SCROLL_BAR;

		if (maxValue <= 0) {
			return;
		}

		if (sliding) {
			painter = darkMode
				? Painters.SCROLL_BAR_PRESSED_DARK
				: Painters.SCROLL_BAR_PRESSED;
		} else if (isWithinBounds(mouseX, mouseY)) {
			painter = darkMode
				? Painters.SCROLL_BAR_HOVERED_DARK
				: Painters.SCROLL_BAR_HOVERED;
		}

		pose.pushPose();

		if (axis == Axis.HORIZONTAL) {
			pose.translate(x + 1 + getHandlePosition(), y + 1, 0);

			painter.draw(
				NinePatchTextureRendererImpl.INSTANCE,
				gui,
				getHandleSize(),
				height - 2
			);

			if (isFocused()) {
				Painters.FOCUS.draw(
					NinePatchTextureRendererImpl.INSTANCE,
					gui,
					getHandleSize(),
					height - 2
				);
			}
		} else {
			pose.translate(x + 1, y + 1 + getHandlePosition(), 0);

			painter.draw(
				NinePatchTextureRendererImpl.INSTANCE,
				gui,
				width - 2,
				getHandleSize()
			);

			if (isFocused()) {
				Painters.FOCUS.draw(
					NinePatchTextureRendererImpl.INSTANCE,
					gui,
					width - 2,
					getHandleSize()
				);
			}
		}

		pose.popPose();
	}

	@Override
	public boolean canResize() {
		return true;
	}

	@Override
	public boolean canFocus() {
		return true;
	}

	public int getHandleSize() {
		float percentage = (window >= maxValue)
			? 1f
			: window / (float) maxValue;

		int bar = axis == Axis.HORIZONTAL
			? width - 2
			: height - 2;

		int result = (int) (percentage * bar);

		if (result < 6) {
			result = 6;
		}

		return result;
	}

	public int getMovableDistance() {
		int bar = axis == Axis.HORIZONTAL
			? width - 2
			: height - 2;

		return bar - getHandleSize();
	}

	public int pixelsToValues(int pixels) {
		int bar = getMovableDistance();

		if (bar <= 0) {
			return 0;
		}

		float percent = pixels / (float) bar;

		return (int) (percent * (maxValue - window));
	}

	public int getHandlePosition() {
		float percent =
			value / (float) Math.max(maxValue - window, 1);

		return (int) (percent * getMovableDistance());
	}

	public int getMaxScrollValue() {
		return maxValue - window;
	}

	protected void adjustSlider(int x, int y) {
		int delta;

		if (axis == Axis.HORIZONTAL) {
			delta = x - anchor;
		} else {
			delta = y - anchor;
		}

		int valueDelta = pixelsToValues(delta);
		int valueNew = anchorValue + valueDelta;

		if (valueNew > getMaxScrollValue()) {
			valueNew = getMaxScrollValue();
		}

		if (valueNew < 0) {
			valueNew = 0;
		}

		this.value = valueNew;
	}

	@Override
	public InputResult onMouseDown(int x, int y, int button) {
		requestFocus();

		if (axis == Axis.HORIZONTAL) {
			anchor = x;
			anchorValue = value;
		} else {
			anchor = y;
			anchorValue = value;
		}

		sliding = true;
		return InputResult.PROCESSED;
	}

	@Override
	public InputResult onMouseDrag(
		int x,
		int y,
		int button,
		double deltaX,
		double deltaY
	) {
		adjustSlider(x, y);
		return InputResult.PROCESSED;
	}

	@Override
	public InputResult onMouseUp(int x, int y, int button) {
		anchor = -1;
		anchorValue = -1;
		sliding = false;

		return InputResult.PROCESSED;
	}

	@Override
	public InputResult onKeyPressed(int ch, int key, int modifiers) {
		WAbstractSlider.Direction direction =
			axis == Axis.HORIZONTAL
				? WAbstractSlider.Direction.RIGHT
				: WAbstractSlider.Direction.DOWN;

		if (WAbstractSlider.isIncreasingKey(ch, direction)) {
			if (value < getMaxScrollValue()) {
				value++;
			}
			return InputResult.PROCESSED;
		}

		if (WAbstractSlider.isDecreasingKey(ch, direction)) {
			if (value > 0) {
				value--;
			}
			return InputResult.PROCESSED;
		}

		return InputResult.IGNORED;
	}

	@Override
	public InputResult onMouseScroll(int x, int y, double amount) {
		setValue(getValue() + (int) -amount * SCROLLING_SPEED);
		return InputResult.PROCESSED;
	}

	public int getValue() {
		return value;
	}

	public WScrollBar setValue(int value) {
		this.value = value;
		checkValue();
		return this;
	}

	public int getMaxValue() {
		return maxValue;
	}

	public WScrollBar setMaxValue(int max) {
		this.maxValue = max;
		checkValue();
		return this;
	}

	public int getWindow() {
		return window;
	}

	public WScrollBar setWindow(int window) {
		this.window = window;
		return this;
	}

	protected void checkValue() {
		if (value > maxValue - window) {
			value = maxValue - window;
		}

		if (value < 0) {
			value = 0;
		}
	}

	public void updateNarration(NarrationElementOutput output) {
		output.add(
			NarratedElementType.TITLE,
			NarrationMessages.SCROLL_BAR_TITLE
		);

		output.add(
			NarratedElementType.USAGE,
			NarrationMessages.SLIDER_USAGE
		);
	}

	static final class Painters {
		static final NinePatch<ResourceLocation> SCROLL_BAR =
			NinePatch.builder(
				ResourceLocation.fromNamespaceAndPath(
					LibGuiCommon.MOD_ID,
					"textures/widget/scroll_bar/scroll_bar_light.png"
				)
			).cornerSize(4).cornerUv(0.25f).build();

		static final NinePatch<ResourceLocation> SCROLL_BAR_DARK =
			NinePatch.builder(
				ResourceLocation.fromNamespaceAndPath(
					LibGuiCommon.MOD_ID,
					"textures/widget/scroll_bar/scroll_bar_dark.png"
				)
			).cornerSize(4).cornerUv(0.25f).build();

		static final NinePatch<ResourceLocation> SCROLL_BAR_PRESSED =
			NinePatch.builder(
				ResourceLocation.fromNamespaceAndPath(
					LibGuiCommon.MOD_ID,
					"textures/widget/scroll_bar/scroll_bar_pressed_light.png"
				)
			).cornerSize(4).cornerUv(0.25f).build();

		static final NinePatch<ResourceLocation> SCROLL_BAR_PRESSED_DARK =
			NinePatch.builder(
				ResourceLocation.fromNamespaceAndPath(
					LibGuiCommon.MOD_ID,
					"textures/widget/scroll_bar/scroll_bar_pressed_dark.png"
				)
			).cornerSize(4).cornerUv(0.25f).build();

		static final NinePatch<ResourceLocation> SCROLL_BAR_HOVERED =
			NinePatch.builder(
				ResourceLocation.fromNamespaceAndPath(
					LibGuiCommon.MOD_ID,
					"textures/widget/scroll_bar/scroll_bar_hovered_light.png"
				)
			).cornerSize(4).cornerUv(0.25f).build();

		static final NinePatch<ResourceLocation> SCROLL_BAR_HOVERED_DARK =
			NinePatch.builder(
				ResourceLocation.fromNamespaceAndPath(
					LibGuiCommon.MOD_ID,
					"textures/widget/scroll_bar/scroll_bar_hovered_dark.png"
				)
			).cornerSize(4).cornerUv(0.25f).build();

		static final BackgroundPainter BACKGROUND =
			BackgroundPainter.createLightDarkVariants(
				createNinePatch(
					ResourceLocation.fromNamespaceAndPath(
						LibGuiCommon.MOD_ID,
						"textures/widget/scroll_bar/background_light.png"
					)
				),
				createNinePatch(
					ResourceLocation.fromNamespaceAndPath(
						LibGuiCommon.MOD_ID,
						"textures/widget/scroll_bar/background_dark.png"
					)
				)
			);

		static final NinePatch<ResourceLocation> FOCUS =
			NinePatch.builder(
				ResourceLocation.fromNamespaceAndPath(
					LibGuiCommon.MOD_ID,
					"textures/widget/scroll_bar/focus.png"
				)
			).cornerSize(4).cornerUv(0.25f).build();
	}
}
