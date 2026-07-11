package io.github.cottonmc.cotton.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics; // DrawContext -> GuiGraphics
import net.minecraft.client.gui.narration.NarrationElementOutput; // NarrationMessageBuilder -> NarrationElementOutput
import net.minecraft.client.gui.narration.NarratedElementType; // NarrationPart -> NarratedElementType
import net.minecraft.network.chat.Component; // Text -> Component
import net.minecraft.resources.ResourceLocation; // Identifier -> ResourceLocation
import com.mojang.math.Axis; // RotationAxis -> Axis

import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.impl.client.NarrationMessages;
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * A vanilla-style labeled slider widget.
 *
 * <p>In addition to the standard slider listeners,
 * labeled sliders also support "label updaters" that can update the label
 * when the value is changed.
 *
 * @see WAbstractSlider for more information about listeners
 */
public class WLabeledSlider extends WAbstractSlider {
	@Nullable private Component label = null; // Text -> Component
	@Nullable private LabelUpdater labelUpdater = null;
	private HorizontalAlignment labelAlignment = HorizontalAlignment.CENTER;

	/**
	 * Constructs a horizontal slider with no default label.
	 */
	public WLabeledSlider(int min, int max) {
		this(min, max, io.github.cottonmc.cotton.gui.widget.data.Axis.HORIZONTAL);
	}

	/**
	 * Constructs a slider with no default label.
	 */
	public WLabeledSlider(int min, int max, io.github.cottonmc.cotton.gui.widget.data.Axis axis) {
		super(min, max, axis);
	}

	/**
	 * Constructs a slider.
	 */
	public WLabeledSlider(int min, int max, io.github.cottonmc.cotton.gui.widget.data.Axis axis, @Nullable Component label) { // Text -> Component
		this(min, max, axis);
		this.label = label;
	}

	/**
	 * Constructs a horizontal slider.
	 */
	public WLabeledSlider(int min, int max, @Nullable Component label) { // Text -> Component
		this(min, max);
		this.label = label;
	}

	@Override
	public void setSize(int x, int y) {
		if (axis == io.github.cottonmc.cotton.gui.widget.data.Axis.HORIZONTAL) {
			super.setSize(x, 20);
		} else {
			super.setSize(20, y);
		}
	}

	/**
	 * Gets the current label of this slider.
	 */
	@Nullable
	public Component getLabel() { // Text -> Component
		return label;
	}

	/**
	 * Sets the label of this slider.
	 */
	public void setLabel(@Nullable Component label) { // Text -> Component
		this.label = label;
	}

	@Override
	protected void onValueChanged(int value) {
		super.onValueChanged(value);
		if (labelUpdater != null) {
			label = labelUpdater.updateLabel(value);
		}
	}

	public HorizontalAlignment getLabelAlignment() {
		return labelAlignment;
	}

	public void setLabelAlignment(HorizontalAlignment labelAlignment) {
		this.labelAlignment = labelAlignment;
	}

	@Nullable
	public LabelUpdater getLabelUpdater() {
		return labelUpdater;
	}

	public void setLabelUpdater(@Nullable LabelUpdater labelUpdater) {
		this.labelUpdater = labelUpdater;
	}

	@Override
	protected int getThumbWidth() {
		return 8;
	}

	@Override
	protected boolean isMouseInsideBounds(int x, int y) {
		return x >= 0 && x <= width && y >= 0 && y <= height;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void paint(GuiGraphics context, int x, int y, int mouseX, int mouseY) { // DrawContext -> GuiGraphics
		int aWidth = axis == io.github.cottonmc.cotton.gui.widget.data.Axis.HORIZONTAL ? width : height;
		int aHeight = axis == io.github.cottonmc.cotton.gui.widget.data.Axis.HORIZONTAL ? height : width;
		int rotMouseX = axis == io.github.cottonmc.cotton.gui.widget.data.Axis.HORIZONTAL
			? (direction == Direction.LEFT ? width - mouseX : mouseX)
			: (direction == Direction.UP ? height - mouseY : mouseY);
		int rotMouseY = axis == io.github.cottonmc.cotton.gui.widget.data.Axis.HORIZONTAL ? mouseY : mouseX;

		PoseStack matrices = context.pose(); // getMatrices() -> pose()
		matrices.pushPose(); // push() -> pushPose()
		matrices.translate(x, y, 0);
		if (axis == io.github.cottonmc.cotton.gui.widget.data.Axis.VERTICAL) {
			matrices.translate(0, height, 0);
			matrices.mulPose(Axis.ZP.rotationDegrees(270)); // RotationAxis.POSITIVE_Z -> Axis.ZP, multiply() -> mulPose()
		}
		drawButton(context, 0, 0, 0, aWidth);

		int thumbX = Math.round(coordToValueRatio * (value - min));
		int thumbY = 0;
		int thumbWidth = getThumbWidth();
		int thumbHeight = aHeight;
		boolean hovering = rotMouseX >= thumbX && rotMouseX <= thumbX + thumbWidth && rotMouseY >= thumbY && rotMouseY <= thumbY + thumbHeight;
		int thumbState = dragging || hovering ? 2 : 1;

		drawButton(context, thumbX, thumbY, thumbState, thumbWidth);

		if (thumbState == 1 && isFocused()) {
			float px = 1 / 32f;
			ScreenDrawing.texturedRect(context, thumbX, thumbY, thumbWidth, thumbHeight, WSlider.LIGHT_TEXTURE, 24*px, 0*px, 32*px, 20*px, 0xFFFFFFFF);
		}

		if (label != null) {
			int color = isMouseInsideBounds(mouseX, mouseY) ? 0xFFFFA0 : 0xE0E0E0;
			ScreenDrawing.drawStringWithShadow(context, label.getVisualOrderText(), labelAlignment, 2, aHeight / 2 - 4, aWidth - 4, color); // asOrderedText -> getVisualOrderText
		}
		matrices.popPose(); // pop() -> popPose()
	}

	@OnlyIn(Dist.CLIENT)
	private void drawButton(GuiGraphics context, int x, int y, int state, int width) { // DrawContext -> GuiGraphics
		float px = 1 / 256f;
		float buttonLeft = 0 * px;
		float buttonTop = (46 + (state * 20)) * px;
		int halfWidth = width / 2;
		if (halfWidth > 198) halfWidth = 198;
		float buttonWidth = halfWidth * px;
		float buttonHeight = 20 * px;
		float buttonEndLeft = (200 - halfWidth) * px;

		ResourceLocation texture = WButton.getTexture(this); // Identifier -> ResourceLocation
		ScreenDrawing.texturedRect(context, x, y, halfWidth, 20, texture, buttonLeft, buttonTop, buttonLeft + buttonWidth, buttonTop + buttonHeight, 0xFFFFFFFF);
		ScreenDrawing.texturedRect(context, x + halfWidth, y, halfWidth, 20, texture, buttonEndLeft, buttonTop, 200 * px, buttonTop + buttonHeight, 0xFFFFFFFF);
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void updateNarration(NarrationElementOutput builder) { // addNarrations -> updateNarration, NarrationMessageBuilder -> NarrationElementOutput
		if (getLabel() != null) {
			// put -> add, NarrationPart -> NarratedElementType, Text.translatable -> Component.translatable
			builder.add(NarratedElementType.TITLE, Component.translatable(NarrationMessages.LABELED_SLIDER_TITLE_KEY, getLabel(), value, min, max));
			builder.add(NarratedElementType.USAGE, NarrationMessages.SLIDER_USAGE);
		} else {
			super.updateNarration(builder);
		}
	}

	@FunctionalInterface
	public interface LabelUpdater {
		/**
		 * Gets the updated label for the new slider value.
		 *
		 * @param value the slider value
		 * @return the label
		 */
		Component updateLabel(int value); // Text -> Component
	}
}
