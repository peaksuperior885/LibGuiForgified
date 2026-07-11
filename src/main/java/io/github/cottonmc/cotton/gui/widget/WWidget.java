package io.github.cottonmc.cotton.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;

import io.github.cottonmc.cotton.gui.GuiDescription;
import io.github.cottonmc.cotton.gui.client.LibGui;
import io.github.cottonmc.cotton.gui.impl.VisualLogger;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import io.github.cottonmc.cotton.gui.widget.data.ObservableProperty;
import io.github.cottonmc.cotton.gui.widget.focus.FocusModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The base class for all widgets.
 *
 */
public class WWidget {
	private static final VisualLogger LOGGER = new VisualLogger(WWidget.class);

	/**
	 * The containing panel of this widget.
	 * Can be null if this widget is the root panel or a HUD widget.
	 */
	@Nullable
	protected WPanel parent;

	/** The X coordinate of this widget relative to its parent. */
	protected int x = 0;
	/** The Y coordinate of this widget relative to its parent. */
	protected int y = 0;
	/** The width of this widget, defaults to 18 pixels. */
	protected int width = 18;
	/** The height of this widget, defaults to 18 pixels. */
	protected int height = 18;

	/**
	 * The containing {@link GuiDescription} of this widget.
	 * Can be null if this widget is a {@linkplain io.github.cottonmc.cotton.gui.client.CottonHud HUD} widget.
	 */
	@Nullable
	protected GuiDescription host;

	private final ObservableProperty<Boolean> hovered = ObservableProperty.of(false).nonnull().name("WWidget.hovered").build();

	/**
	 * Sets the location of this widget relative to its parent.
	 *
	 * @param x the new X coordinate
	 * @param y the new Y coordinate
	 */
	public void setLocation(int x, int y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Sets the size of this widget.
	 *
	 * @param x the new width
	 * @param y the new height
	 */
	public void setSize(int x, int y) {
		this.width = x;
		this.height = y;
	}

	/**
	 * Gets the X coordinate of this widget relative to its parent.
	 *
	 * @return the X coordinate
	 */
	public int getX() {
		return x;
	}

	/**
	 * Gets the Y coordinate of this widget relative to its parent.
	 *
	 * @return the Y coordinate
	 */
	public int getY() {
		return y;
	}

	/**
	 * Gets the absolute X coordinate of this widget.
	 *
	 * @return the absolute X coordinate
	 */
	public int getAbsoluteX() {
		if (parent==null) {
			return getX();
		} else {
			return getX() + parent.getAbsoluteX();
		}
	}

	/**
	 * Gets the absolute Y coordinate of this widget.
	 *
	 * @return the absolute Y coordinate
	 */
	public int getAbsoluteY() {
		if (parent==null) {
			return getY();
		} else {
			return getY() + parent.getAbsoluteY();
		}
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	/**
	 * Checks whether this widget can be resized using {@link #setSize}.
	 *
	 * @return true if this widget can be resized, false otherwise
	 */
	public boolean canResize() {
		return false;
	}

	/**
	 * Gets the parent panel of this widget.
	 *
	 * @return the parent, or null if this widget has no parent
	 */
	@Nullable
	public WPanel getParent() {
		return parent;
	}

	/**
	 * Sets the parent panel of this widget.
	 *
	 * @param parent the new parent
	 */
	public void setParent(WPanel parent) {
		this.parent = parent;
	}

	@OnlyIn(Dist.CLIENT)
	public InputResult onMouseDown(int x, int y, int button) {
		return InputResult.IGNORED;
	}

	@OnlyIn(Dist.CLIENT)
	public InputResult onMouseDrag(int x, int y, int button, double deltaX, double deltaY) {
		return InputResult.IGNORED;
	}

	@OnlyIn(Dist.CLIENT)
	public InputResult onMouseUp(int x, int y, int button) {
		return InputResult.IGNORED;
	}

	@OnlyIn(Dist.CLIENT)
	public InputResult onClick(int x, int y, int button) {
		return InputResult.IGNORED;
	}

	@OnlyIn(Dist.CLIENT)
	public InputResult onMouseScroll(int x, int y, double amount) {
		return InputResult.IGNORED;
	}

	@OnlyIn(Dist.CLIENT)
	public InputResult onMouseMove(int x, int y) {
		return InputResult.IGNORED;
	}

	@OnlyIn(Dist.CLIENT)
	public InputResult onCharTyped(char ch) {
		return InputResult.IGNORED;
	}

	@OnlyIn(Dist.CLIENT)
	public InputResult onKeyPressed(int ch, int key, int modifiers) {
		return InputResult.IGNORED;
	}

	@OnlyIn(Dist.CLIENT)
	public InputResult onKeyReleased(int ch, int key, int modifiers) {
		return InputResult.IGNORED;
	}

	public void onFocusGained() {
	}

	public void onFocusLost() {
	}

	public boolean isFocused() {
		if (host==null) return false;
		return host.isFocused(this);
	}

	public void requestFocus() {
		if (host!=null) {
			host.requestFocus(this);
		} else {
			LOGGER.warn("Requesting focus for {}, but the host is null", this);
		}
	}

	public void releaseFocus() {
		if (host!=null) host.releaseFocus(this);
	}

	public boolean canFocus() {
		return false;
	}

	/**
	 * Paints this widget.
	 */
	@OnlyIn(Dist.CLIENT)
	public void paint(GuiGraphics context, int x, int y, int mouseX, int mouseY) {
	}

	public boolean isWithinBounds(int x, int y) {
		return x>=0 && y>=0 && x<this.width && y<this.height;
	}

	/**
	 * Internal method to render tooltip data.
	 */
	@OnlyIn(Dist.CLIENT)
	public void renderTooltip(GuiGraphics context, int x, int y, int tX, int tY) {
		TooltipBuilder builder = new TooltipBuilder();
		addTooltip(builder);

		if (builder.size() == 0) return;

		var client = Minecraft.getInstance();
		// In 1.21.1 GuiGraphics, drawTooltip has multiple variants; this uses the straightforward string list processor
		context.renderTooltip(client.font, builder.lines, tX + x, tY + y);
	}

	public void validate(GuiDescription host) {
		if (host != null) {
			this.host = host;
		} else {
			LOGGER.warn("Validating {} with a null host", this);
		}
	}

	@Nullable
	public final GuiDescription getHost() {
		return host;
	}

	public void setHost(GuiDescription host) {
		if (host != null) {
			this.host = host;
		} else {
			LOGGER.warn("Setting null host for {}", this);
		}
	}

	@OnlyIn(Dist.CLIENT)
	public void addTooltip(TooltipBuilder tooltip) {
	}

	public WWidget hit(int x, int y) {
		return this;
	}

	@OnlyIn(Dist.CLIENT)
	public void tick() {}

	public @Nullable FocusModel<?> getFocusModel() {
		return canFocus() ? FocusModel.simple(this) : null;
	}

	public void onShown() {
	}

	public void onHidden() {
		releaseFocus();
	}

	@OnlyIn(Dist.CLIENT)
	public void addPainters() {
	}

	public boolean canHover() {
		return true;
	}

	public ObservableProperty<Boolean> hoveredProperty() {
		return hovered;
	}

	public final boolean isHovered() {
		return hoveredProperty().get();
	}

	public final void setHovered(boolean hovered) {
		hoveredProperty().set(hovered);
	}

	public boolean isNarratable() {
		return true;
	}

	/**
	 * Adds the narrations of this widget to a narration builder.
	 */
	@OnlyIn(Dist.CLIENT)
	public void addNarrations(NarrationElementOutput builder) {
	}

	@OnlyIn(Dist.CLIENT)
	public static boolean isActivationKey(int ch) {
		return ch == GLFW.GLFW_KEY_ENTER || ch == GLFW.GLFW_KEY_KP_ENTER || ch == GLFW.GLFW_KEY_SPACE;
	}

	@OnlyIn(Dist.CLIENT)
	public boolean shouldRenderInDarkMode() {
		var globalDarkMode = LibGui.isDarkMode();

		if (host != null) {
			return host.isDarkMode().orElse(globalDarkMode);
		}

		return globalDarkMode;
	}
}
