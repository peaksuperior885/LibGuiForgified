package io.github.cottonmc.cotton.gui.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.stream.Collectors;

/**
 * Contains a stack for GL scissors for restricting the drawn area of a widget.
 *
 * @since 2.0.0
 */
@OnlyIn(Dist.CLIENT)
public final class Scissors {
	private static final ArrayDeque<Frame> STACK = new ArrayDeque<>();

	private Scissors() {
	}

	/**
	 * Pushes a new scissor frame onto the stack and refreshes the scissored area.
	 */
	public static Frame push(int x, int y, int width, int height) {
		Frame frame = new Frame(x, y, width, height);
		STACK.push(frame);
		refreshScissors();

		return frame;
	}

	/**
	 * Pops the topmost scissor frame and refreshes the scissored area.
	 */
	public static void pop() {
		if (STACK.isEmpty()) {
			throw new IllegalStateException("No scissors on the stack!");
		}

		STACK.pop();
		refreshScissors();
	}

	static void refreshScissors() {
		Minecraft mc = Minecraft.getInstance();

		if (STACK.isEmpty()) {
			// No scissors left? Disable the test entirely so Minecraft renders normally
			GL11.glDisable(GL11.GL_SCISSOR_TEST);
			return;
		}

		// Turn the scissor test on
		GL11.glEnable(GL11.GL_SCISSOR_TEST);

		// Start with the boundary of the first frame
		Frame first = STACK.peekFirst();
		int x = first.x;
		int y = first.y;
		int maxX = x + first.width;
		int maxY = y + first.height;

		// Intersect with all other frames down the stack to find the overlap area
		for (Frame frame : STACK) {
			x = Math.max(x, frame.x);
			y = Math.max(y, frame.y);
			maxX = Math.min(maxX, frame.x + frame.width);
			maxY = Math.min(maxY, frame.y + frame.height);
		}

		// Prevent negative widths/heights if things don't intersect
		int width = Math.max(0, maxX - x);
		int height = Math.max(0, maxY - y);

		int windowHeight = mc.getWindow().getHeight();
		double scale = mc.getWindow().getGuiScale(); // getScaleFactor() -> getGuiScale()

		int scaledX = (int) (x * scale);
		int scaledY = (int) (windowHeight - (y * scale) - (height * scale));
		int scaledWidth = (int) (width * scale);
		int scaledHeight = (int) (height * scale);

		GL11.glScissor(scaledX, scaledY, scaledWidth, scaledHeight);
	}

	/**
	 * Internal method. Throws an {@link IllegalStateException} if the scissor stack is not empty.
	 */
	static void checkStackIsEmpty() {
		if (!STACK.isEmpty()) {
			throw new IllegalStateException("Unpopped scissor frames: " + STACK.stream().map(Frame::toString).collect(Collectors.joining(", ")));
		}
	}

	/**
	 * A single scissor frame in the stack.
	 */
	public static final class Frame implements AutoCloseable {
		private final int x;
		private final int y;
		private final int width;
		private final int height;

		private Frame(int x, int y, int width, int height) {
			if (width < 0) throw new IllegalArgumentException("Negative width for a stack frame");
			if (height < 0) throw new IllegalArgumentException("Negative height for a stack frame");

			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		@Override
		public void close() {
			// FIXED: Use peekFirst() instead of peekLast() because ArrayDeque#push acts on the head!
			if (STACK.peekFirst() != this) {
				if (STACK.contains(this)) {
					throw new IllegalStateException(this + " is not on top of the stack!");
				} else {
					throw new IllegalStateException(this + " is not on the stack!");
				}
			}

			pop();
		}

		@Override
		public String toString() {
			return "Frame{ at = (" + x + ", " + y + "), size = (" + width + ", " + height + ") }";
		}
	}
}
