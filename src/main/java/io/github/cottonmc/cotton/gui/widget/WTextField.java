package io.github.cottonmc.cotton.gui.widget;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import io.github.cottonmc.cotton.gui.client.BackgroundPainter;
import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.impl.client.NarrationMessages;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class WTextField extends WWidget {
	public static final int TEXT_PADDING_X = 4;
	public static final int TEXT_PADDING_Y = 6;
	public static final int CURSOR_PADDING_Y = 4;
	public static final int CURSOR_HEIGHT = 12;

	@OnlyIn(Dist.CLIENT)
	private Font font;

	private String text = "";
	private int maxLength = 16;
	private boolean editable = true;
	private int tickCount = 0;

	private int disabledColor = 0x707070;
	private int enabledColor = 0xE0E0E0;
	private int suggestionColor = 0x808080;
	private int selectionColor = 0x4A90E2;

	private static final int BACKGROUND_COLOR = 0xFF000000;
	private static final int BORDER_COLOR_SELECTED = 0xFFFFFFA0;
	private static final int BORDER_COLOR_UNSELECTED = 0xFFA0A0A0;
	private static final int CURSOR_COLOR = 0xFFD0D0D0;

	@Nullable
	private Component suggestion = null;

	// Index of the leftmost character to be rendered.
	private int scrollOffset = 0;

	private int cursor = 0;
	/**
	 * If not -1, selectionStart is the "anchor point" of a selection. That is, if you hit shift+left with no
	 * existing selection, the selection will be anchored to where you were, but the cursor will move left,
	 * expanding the selection as you continue to move left. If you move to the right, eventually you'll overtake
	 * the anchor, drop the anchor at the same place and start expanding the selection rightwards instead.
	 */
	private int selectionStart = -1;

	private Consumer<String> onChanged;

	private Predicate<String> textPredicate;

	@OnlyIn(Dist.CLIENT)
	@Nullable
	private BackgroundPainter backgroundPainter;

	public WTextField() {
	}

	public WTextField(Component suggestion) {
		this.suggestion = suggestion;
	}

	/**
	 * Sets the text of this text field.
	 * If the text is more than the {@linkplain #getMaxLength() max length},
	 * it'll be shortened to the max length.
	 *
	 * @param newText the new text
	 */
	public void setText(String newText) {
		setTextWithResult(newText);
	}

	private boolean setTextWithResult(String newText) {
		if (this.textPredicate == null || this.textPredicate.test(newText)) {
			this.text = (newText.length() > maxLength) ? newText.substring(0, maxLength) : newText;
			// Call change listener
			if (onChanged != null) onChanged.accept(this.text);
			// Reset cursor if needed
			cursor = clampCursor(cursor);
			if (selectionStart > text.length()) selectionStart = -1;
			return true;
		}

		return false;
	}

	/**
	 * {@return the text in this text field}
	 */
	public String getText() {
		return this.text;
	}

	@Override
	public boolean canResize() {
		return true;
	}

	@Override
	public void tick() {
		super.tick();
		this.tickCount++;
	}

	@Override
	public void setSize(int x, int y) {
		super.setSize(x, 20);
	}

	private int clampCursor(int cursor) {
		return Mth.clamp(cursor, 0, text.length());
	}

	public void setCursorPos(int location) {
		this.cursor = clampCursor(location);
		scrollCursorIntoView();
	}

	public int getMaxLength() {
		return this.maxLength;
	}

	public int getCursor() {
		return this.cursor;
	}

	@OnlyIn(Dist.CLIENT)
	public void scrollCursorIntoView() {
		if (font == null) font = Minecraft.getInstance().font;

		if (scrollOffset > cursor) {
			scrollOffset = cursor;
		}
		if (scrollOffset < cursor && font.plainSubstrByWidth(text.substring(scrollOffset), width - TEXT_PADDING_X * 2).length() + scrollOffset < cursor) {
			scrollOffset = cursor;
		}

		checkScrollOffset();
	}

	@OnlyIn(Dist.CLIENT)
	private void checkScrollOffset() {
		if (font == null) font = Minecraft.getInstance().font;

		int rightMostScrollOffset = text.length() - font.plainSubstrByWidth(text, width - TEXT_PADDING_X * 2, true).length();
		scrollOffset = Math.min(rightMostScrollOffset, scrollOffset);
		scrollOffset = Math.max(0, scrollOffset);
	}

	@Nullable
	public String getSelection() {
		if (selectionStart < 0) return null;
		if (selectionStart == cursor) return null;

		// Tidy some things
		if (selectionStart > text.length()) selectionStart = text.length();
		cursor = clampCursor(cursor);

		int start = Math.min(selectionStart, cursor);
		int end = Math.max(selectionStart, cursor);

		return text.substring(start, end);
	}

	public boolean isEditable() {
		return this.editable;
	}

	@OnlyIn(Dist.CLIENT)
	protected void renderBox(GuiGraphics graphics, int x, int y) {
		int borderColor = this.isFocused() ? BORDER_COLOR_SELECTED : BORDER_COLOR_UNSELECTED;
		ScreenDrawing.coloredRect(graphics, x - 1, y - 1, width + 2, height + 2, borderColor);
		ScreenDrawing.coloredRect(graphics, x, y, width, height, BACKGROUND_COLOR);
	}

	@OnlyIn(Dist.CLIENT)
	protected void renderText(GuiGraphics graphics, int x, int y, String visibleText) {
		int textColor = this.editable ? this.enabledColor : this.disabledColor;
		graphics.drawString(font, visibleText, x + TEXT_PADDING_X, y + TEXT_PADDING_Y, textColor, true);
	}

	@OnlyIn(Dist.CLIENT)
	protected void renderCursor(GuiGraphics graphics, int x, int y, String visibleText) {
		if (this.tickCount / 6 % 2 == 0) return;
		if (this.cursor < this.scrollOffset) return;
		if (this.cursor > this.scrollOffset + visibleText.length()) return;
		int cursorOffset = this.font.width(visibleText.substring(0, this.cursor - this.scrollOffset));
		ScreenDrawing.coloredRect(graphics, x + TEXT_PADDING_X + cursorOffset, y + CURSOR_PADDING_Y, 1, CURSOR_HEIGHT, CURSOR_COLOR);
	}

	@OnlyIn(Dist.CLIENT)
	protected void renderSuggestion(GuiGraphics graphics, int x, int y) {
		if (this.suggestion == null) return;
		graphics.drawString(font, suggestion, x + TEXT_PADDING_X, y + TEXT_PADDING_Y, this.suggestionColor, true);
	}

	@OnlyIn(Dist.CLIENT)
	protected void renderSelection(GuiGraphics graphics, int x, int y, String visibleText) {
		if (selectionStart == cursor || selectionStart == -1) return;

		int textLength = visibleText.length();

		int left = Math.min(cursor, selectionStart);
		int right = Math.max(cursor, selectionStart);

		if (right < scrollOffset || left > scrollOffset + textLength) return;

		int normalizedLeft = Math.max(scrollOffset, left) - scrollOffset;
		int normalizedRight = Math.min(scrollOffset + textLength, right) - scrollOffset;

		int leftCaret = font.width(visibleText.substring(0, normalizedLeft));
		int selectionWidth = font.width(visibleText.substring(normalizedLeft, normalizedRight));

		invertedRect(graphics, x + TEXT_PADDING_X + leftCaret, y + CURSOR_PADDING_Y, selectionWidth, CURSOR_HEIGHT);
	}

	@OnlyIn(Dist.CLIENT)
	protected void renderTextField(GuiGraphics graphics, int x, int y) {
		if (this.font == null) this.font = Minecraft.getInstance().font;

		checkScrollOffset();
		String visibleText = font.plainSubstrByWidth(this.text.substring(this.scrollOffset), this.width - 2 * TEXT_PADDING_X);
		renderBox(graphics, x, y);
		renderText(graphics, x, y, visibleText);
		if (this.text.isEmpty() && !this.isFocused()) {
			renderSuggestion(graphics, x, y);
		}
		if (this.isFocused()) {
			renderCursor(graphics, x, y, visibleText);
		}
		renderSelection(graphics, x, y, visibleText);
	}

	@OnlyIn(Dist.CLIENT)
	private void invertedRect(GuiGraphics graphics, int x, int y, int width, int height) {
		Matrix4f model = graphics.pose().last().pose();
		RenderSystem.setShaderColor(0.0F, 0.0F, 1.0F, 1.0F);
		RenderSystem.setShader(GameRenderer::getPositionShader);
		RenderSystem.enableColorLogicOp();
		RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
		buffer.addVertex(model, x, y + height, 0);
		buffer.addVertex(model, x + width, y + height, 0);
		buffer.addVertex(model, x + width, y, 0);
		buffer.addVertex(model, x, y, 0);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		RenderSystem.disableColorLogicOp();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	public WTextField setTextPredicate(Predicate<String> predicate) {
		this.textPredicate = predicate;
		return this;
	}

	public WTextField setChangedListener(Consumer<String> listener) {
		this.onChanged = listener;
		return this;
	}

	public WTextField setMaxLength(int max) {
		this.maxLength = max;
		if (this.text.length() > max) {
			setText(this.text.substring(0, max));
		}
		return this;
	}

	public WTextField setEnabledColor(int col) {
		this.enabledColor = col;
		return this;
	}

	public WTextField setSuggestionColor(int suggestionColor) {
		this.suggestionColor = suggestionColor;
		return this;
	}

	public WTextField setDisabledColor(int col) {
		this.disabledColor = col;
		return this;
	}

	public WTextField setSelectionColor(int selectionColor) {
		this.selectionColor = selectionColor;
		return this;
	}

	public WTextField setEditable(boolean editable) {
		this.editable = editable;
		return this;
	}

	@Nullable
	public Component getSuggestion() {
		return suggestion;
	}

	public WTextField setSuggestion(@Nullable Component suggestion) {
		this.suggestion = suggestion;
		return this;
	}

	@OnlyIn(Dist.CLIENT)
	public WTextField setBackgroundPainter(BackgroundPainter painter) {
		this.backgroundPainter = painter;
		return this;
	}

	@Override
	public boolean canFocus() {
		return true;
	}

	@Override
	public void onFocusGained() {
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void paint(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
		renderTextField(graphics, x, y);
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public InputResult onClick(int x, int y, int button) {
		requestFocus();
		cursor = getCaretPosition(x - TEXT_PADDING_X);
		selectionStart = -1;
		scrollCursorIntoView();
		return InputResult.PROCESSED;
	}

	@OnlyIn(Dist.CLIENT)
	public int getCaretPosition(int clickX) {
		if (font == null) font = Minecraft.getInstance().font;
		if (clickX < 0) return 0;
		int lastPos = 0;
		checkScrollOffset();
		String string = text.substring(scrollOffset);
		for (int i = 0; i < string.length(); i++) {
			int w = font.width(String.valueOf(string.charAt(i)));
			if (lastPos + w >= clickX) {
				if (clickX - lastPos < w / 2) {
					return i + scrollOffset;
				}
			}
			lastPos += w;
		}
		return string.length() + scrollOffset;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public InputResult onCharTyped(char ch) {
		insertText(String.valueOf(ch));
		return InputResult.PROCESSED;
	}

	@OnlyIn(Dist.CLIENT)
	private void insertText(String toInsert) {
		String before, after;
		if (selectionStart != -1 && selectionStart != cursor) {
			int left = Math.min(cursor, selectionStart);
			int right = Math.max(cursor, selectionStart);
			before = this.text.substring(0, left);
			after = this.text.substring(right);
		} else {
			before = this.text.substring(0, cursor);
			after = this.text.substring(cursor);
		}
		if (before.length() + after.length() + toInsert.length() > maxLength) return;
		if (setTextWithResult(before + toInsert + after)) {
			selectionStart = -1;
			cursor = (before + toInsert).length();
			scrollCursorIntoView();
		}
	}

	@OnlyIn(Dist.CLIENT)
	private void copySelection() {
		String selection = getSelection();
		if (selection != null) {
			Minecraft.getInstance().keyboardHandler.setClipboard(selection);
		}
	}

	@OnlyIn(Dist.CLIENT)
	private void paste() {
		String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
		insertText(clip);
	}

	@OnlyIn(Dist.CLIENT)
	private void deleteSelection() {
		int left = Math.min(cursor, selectionStart);
		int right = Math.max(cursor, selectionStart);
		if (setTextWithResult(text.substring(0, left) + text.substring(right))) {
			selectionStart = -1;
			cursor = clampCursor(left);
			scrollCursorIntoView();
		}
	}

	@OnlyIn(Dist.CLIENT)
	private void delete(int modifiers, boolean backwards) {
		if (selectionStart == -1 || selectionStart == cursor) {
			selectionStart = skipCharacters((GLFW.GLFW_MOD_CONTROL & modifiers) != 0, backwards ? -1 : 1);
		}
		deleteSelection();
	}

	@OnlyIn(Dist.CLIENT)
	private int skipCharacters(boolean skipMany, int direction) {
		if (direction != -1 && direction != 1) return cursor;
		int position = cursor;
		while (true) {
			position += direction;
			if (position < 0) {
				return 0;
			}
			if (position > text.length()) {
				return text.length();
			}
			if (!skipMany) return position;
			if (position < text.length() && Character.isWhitespace(text.charAt(position))) {
				return position;
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	public void onDirectionalKey(int direction, int modifiers) {
		if ((GLFW.GLFW_MOD_SHIFT & modifiers) != 0) {
			if (selectionStart == -1 || selectionStart == cursor) selectionStart = cursor;
			cursor = skipCharacters((GLFW.GLFW_MOD_CONTROL & modifiers) != 0, direction);
		} else {
			if (selectionStart != -1) {
				cursor = direction < 0 ? Math.min(cursor, selectionStart) : Math.max(cursor, selectionStart);
				selectionStart = -1;
			} else {
				cursor = skipCharacters((GLFW.GLFW_MOD_CONTROL & modifiers) != 0, direction);
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public InputResult onKeyPressed(int keyCode, int scanCode, int modifiers) {
		if (!isEditable()) return InputResult.IGNORED;

		if (Screen.isCopy(keyCode)) {
			copySelection();
			return InputResult.PROCESSED;
		} else if (Screen.isPaste(keyCode)) {
			paste();
			return InputResult.PROCESSED;
		} else if (Screen.isSelectAll(keyCode)) {
			selectionStart = 0;
			cursor = text.length();
			return InputResult.PROCESSED;
		}

		switch (keyCode) {
			case GLFW.GLFW_KEY_DELETE -> delete(modifiers, false);
			case GLFW.GLFW_KEY_BACKSPACE -> delete(modifiers, true);
			case GLFW.GLFW_KEY_LEFT -> onDirectionalKey(-1, modifiers);
			case GLFW.GLFW_KEY_RIGHT -> onDirectionalKey(1, modifiers);
			case GLFW.GLFW_KEY_HOME, GLFW.GLFW_KEY_UP -> {
				if ((GLFW.GLFW_MOD_SHIFT & modifiers) == 0) {
					selectionStart = -1;
				} else if (selectionStart == -1) {
					selectionStart = cursor;
				}
				cursor = 0;
			}
			case GLFW.GLFW_KEY_END, GLFW.GLFW_KEY_DOWN -> {
				if ((GLFW.GLFW_MOD_SHIFT & modifiers) == 0) {
					selectionStart = -1;
				} else if (selectionStart == -1) {
					selectionStart = cursor;
				}
				cursor = text.length();
			}
			default -> {
				return InputResult.IGNORED;
			}
		}
		scrollCursorIntoView();

		return InputResult.PROCESSED;
	}

	@Override
	public void addNarrations(NarrationElementOutput builder) {
		builder.add(NarratedElementType.TITLE, Component.translatable(NarrationMessages.TEXT_FIELD_TITLE_KEY, text));

		if (suggestion != null) {
			builder.add(NarratedElementType.HINT, Component.translatable(NarrationMessages.TEXT_FIELD_SUGGESTION_KEY, suggestion));
		}
	}
}
