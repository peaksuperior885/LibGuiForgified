package io.github.cottonmc.cotton.gui.widget;

import net.minecraft.client.gui.narration.NarrationElementOutput; // NarrationMessageBuilder -> NarrationElementOutput
import net.minecraft.client.gui.narration.NarratedElementType; // NarrationPart -> NarratedElementType
import net.minecraft.network.chat.Component; // Text -> Component
import net.minecraft.util.Mth; // MathHelper -> Mth

import io.github.cottonmc.cotton.gui.impl.client.NarrationMessages;
import io.github.cottonmc.cotton.gui.widget.data.Axis;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.IntConsumer;

/**
 * A base class for slider widgets that can be used to select int values.
 */
public abstract class WAbstractSlider extends WWidget {
	/**
	 * The minimum time between two draggingFinished events caused by scrolling ({@link #onMouseScroll}).
	 */
	private static final int DRAGGING_FINISHED_RATE_LIMIT_FOR_SCROLLING = 10;

	protected int min, max;
	protected final Axis axis;
	protected Direction direction;

	protected int value;

	/**
	 * True if the user is currently dragging the thumb.
	 * Used for visuals.
	 */
	protected boolean dragging = false;

	/**
	 * A value:coordinate ratio. Used for converting user input into values.
	 */
	protected float valueToCoordRatio;

	/**
	 * A coordinate:value ratio. Used for rendering the thumb.
	 */
	protected float coordToValueRatio;

	/**
	 * True if there is a pending dragging finished event caused by the keyboard.
	 */
	private boolean pendingDraggingFinishedFromKeyboard = false;
	private int draggingFinishedFromScrollingTimer = 0;
	private boolean pendingDraggingFinishedFromScrolling = false;

	@Nullable private IntConsumer valueChangeListener = null;
	@Nullable private IntConsumer draggingFinishedListener = null;

	protected WAbstractSlider(int min, int max, Axis axis) {
		if (max <= min) throw new IllegalArgumentException("Minimum value must be smaller than the maximum!");

		this.min = min;
		this.max = max;
		this.axis = axis;
		this.value = min;
		this.direction = (axis == Axis.HORIZONTAL) ? Direction.RIGHT : Direction.UP;
	}

	/**
	 * {@return the thumb size along the slider axis}
	 */
	protected abstract int getThumbWidth();

	/**
	 * Checks if the mouse cursor is close enough to the slider that the user can start dragging.
	 *
	 * @param x the mouse x position
	 * @param y the mouse y position
	 * @return if the cursor is inside dragging bounds
	 */
	protected abstract boolean isMouseInsideBounds(int x, int y);

	/**
	 * Updates {@link #coordToValueRatio} and {@link #valueToCoordRatio}.
	 * This method should be called whenever this widget resizes or changes it min/max value boundaries.
	 *
	 * @since 5.1.0
	 */
	protected void updateValueCoordRatios() {
		int trackHeight = (axis == Axis.HORIZONTAL ? getWidth() : getHeight()) - getThumbWidth();
		valueToCoordRatio = (float) (max - min) / trackHeight;
		coordToValueRatio = 1 / valueToCoordRatio;
	}

	@Override
	public void setSize(int x, int y) {
		super.setSize(x, y);
		updateValueCoordRatios();
	}

	@Override
	public boolean canResize() {
		return true;
	}

	@Override
	public boolean canFocus() {
		return true;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public InputResult onMouseDown(int x, int y, int button) {
		if (isMouseInsideBounds(x, y)) {
			requestFocus();
			return InputResult.PROCESSED;
		}
		return InputResult.IGNORED;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public InputResult onMouseDrag(int x, int y, int button, double deltaX, double deltaY) {
		if (isFocused()) {
			dragging = true;
			moveSlider(x, y);
			return InputResult.PROCESSED;
		}

		return InputResult.IGNORED;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public InputResult onClick(int x, int y, int button) {
		moveSlider(x, y);
		if (draggingFinishedListener != null) draggingFinishedListener.accept(value);
		return InputResult.PROCESSED;
	}

	private void moveSlider(int x, int y) {
		int axisPos = switch (direction) {
			case UP -> height - y;
			case DOWN -> y;
			case LEFT -> width - x;
			case RIGHT -> x;
		};

		int pos = axisPos - getThumbWidth() / 2;
		int rawValue = min + Math.round(valueToCoordRatio * pos);
		int previousValue = value;
		value = Mth.clamp(rawValue, min, max); // MathHelper.clamp -> Mth.clamp
		if (value != previousValue) onValueChanged(value);
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public InputResult onMouseUp(int x, int y, int button) {
		dragging = false;
		if (draggingFinishedListener != null) draggingFinishedListener.accept(value);
		return InputResult.PROCESSED;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public InputResult onMouseScroll(int x, int y, double amount) {
		if (direction == Direction.LEFT || direction == Direction.DOWN) {
			amount = -amount;
		}

		int previous = value;
		// MathHelper -> Mth
		value = Mth.clamp(value + (int) Math.signum(amount) * Mth.ceil(valueToCoordRatio * Math.abs(amount) * 2), min, max);

		if (previous != value) {
			onValueChanged(value);
			pendingDraggingFinishedFromScrolling = true;
		}

		return InputResult.PROCESSED;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void tick() {
		if (draggingFinishedFromScrollingTimer > 0) {
			draggingFinishedFromScrollingTimer--;
		}

		if (pendingDraggingFinishedFromScrolling && draggingFinishedFromScrollingTimer <= 0) {
			if (draggingFinishedListener != null) draggingFinishedListener.accept(value);
			pendingDraggingFinishedFromScrolling = false;
			draggingFinishedFromScrollingTimer = DRAGGING_FINISHED_RATE_LIMIT_FOR_SCROLLING;
		}
	}

	public int getValue() {
		return value;
	}

	/**
	 * Sets the slider value without calling listeners.
	 * @param value the new value
	 */
	public void setValue(int value) {
		setValue(value, false);
	}

	/**
	 * Sets the slider value.
	 *
	 * @param value the new value
	 * @param callListeners if true, call all slider listeners
	 */
	public void setValue(int value, boolean callListeners) {
		int previous = this.value;
		this.value = Mth.clamp(value, min, max); // MathHelper -> Mth
		if (callListeners && previous != this.value) {
			onValueChanged(this.value);
			if (draggingFinishedListener != null) draggingFinishedListener.accept(value);
		}
	}

	@Nullable
	public IntConsumer getValueChangeListener() {
		return valueChangeListener;
	}

	public void setValueChangeListener(@Nullable IntConsumer valueChangeListener) {
		this.valueChangeListener = valueChangeListener;
	}

	@Nullable
	public IntConsumer getDraggingFinishedListener() {
		return draggingFinishedListener;
	}

	public void setDraggingFinishedListener(@Nullable IntConsumer draggingFinishedListener) {
		this.draggingFinishedListener = draggingFinishedListener;
	}

	public int getMinValue() {
		return min;
	}

	public int getMaxValue() {
		return max;
	}

	public void setMinValue(int min) {
		this.min = min;
		updateValueCoordRatios();
		if (this.value < min) {
			this.value = min;
			onValueChanged(this.value);
		}
	}

	public void setMaxValue(int max) {
		this.max = max;
		updateValueCoordRatios();
		if (this.value > max) {
			this.value = max;
			onValueChanged(this.value);
		}
	}

	public Axis getAxis() {
		return axis;
	}

	/**
	 * Gets the direction of this slider.
	 */
	public Direction getDirection() {
		return direction;
	}

	/**
	 * Sets the direction of this slider.
	 */
	public void setDirection(Direction direction) {
		if (direction.getAxis() != axis) {
			throw new IllegalArgumentException("Incorrect axis: " + axis);
		}

		this.direction = direction;
	}

	protected void onValueChanged(int value) {
		if (valueChangeListener != null) valueChangeListener.accept(value);
	}

	@Override
	public InputResult onKeyPressed(int ch, int key, int modifiers) {
		boolean valueChanged = false;
		if (modifiers == 0) {
			if (isDecreasingKey(ch, direction) && value > min) {
				value--;
				valueChanged = true;
			} else if (isIncreasingKey(ch, direction) && value < max) {
				value++;
				valueChanged = true;
			}
		} else if (modifiers == GLFW.GLFW_MOD_CONTROL) {
			if (isDecreasingKey(ch, direction) && value != min) {
				value = min;
				valueChanged = true;
			} else if (isIncreasingKey(ch, direction) && value != max) {
				value = max;
				valueChanged = true;
			}
		}

		if (valueChanged) {
			onValueChanged(value);
			pendingDraggingFinishedFromKeyboard = true;
		}

		return InputResult.of(valueChanged);
	}

	@Override
	public InputResult onKeyReleased(int ch, int key, int modifiers) {
		if (pendingDraggingFinishedFromKeyboard && (isDecreasingKey(ch, direction) || isIncreasingKey(ch, direction))) {
			if (draggingFinishedListener != null) draggingFinishedListener.accept(value);
			pendingDraggingFinishedFromKeyboard = false;
			return InputResult.PROCESSED;
		}

		return InputResult.IGNORED;
	}

	/**
	 * Tests whether the user is dragging this slider.
	 */
	public boolean isDragging() {
		return dragging;
	}

	public void updateNarration(NarrationElementOutput builder) { // addNarrations -> updateNarration, NarrationMessageBuilder -> NarrationElementOutput
		// put -> add, NarrationPart -> NarratedElementType, Text.translatable -> Component.translatable
		builder.add(NarratedElementType.TITLE, Component.translatable(NarrationMessages.SLIDER_MESSAGE_KEY, value, min, max));
		builder.add(NarratedElementType.USAGE, NarrationMessages.SLIDER_USAGE);
	}

	public static boolean isDecreasingKey(int ch, Direction direction) {
		return direction.isInverted()
			? (ch == GLFW.GLFW_KEY_RIGHT || ch == GLFW.GLFW_KEY_UP)
			: (ch == GLFW.GLFW_KEY_LEFT || ch == GLFW.GLFW_KEY_DOWN);
	}

	public static boolean isIncreasingKey(int ch, Direction direction) {
		return direction.isInverted()
			? (ch == GLFW.GLFW_KEY_LEFT || ch == GLFW.GLFW_KEY_DOWN)
			: (ch == GLFW.GLFW_KEY_RIGHT || ch == GLFW.GLFW_KEY_UP);
	}

	public enum Direction {
		UP(Axis.VERTICAL, false),
		DOWN(Axis.VERTICAL, true),
		LEFT(Axis.HORIZONTAL, true),
		RIGHT(Axis.HORIZONTAL, false);

		private final Axis axis;
		private final boolean inverted;

		Direction(Axis axis, boolean inverted) {
			this.axis = axis;
			this.inverted = inverted;
		}

		public Axis getAxis() {
			return axis;
		}

		public boolean isInverted() {
			return inverted;
		}
	}
}
