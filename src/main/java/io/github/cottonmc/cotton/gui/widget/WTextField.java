package io.github.cottonmc.cotton.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class WTextField extends WWidget {
	public static final int TEXT_PADDING_X = 4;
	public static final int TEXT_PADDING_Y = 6;
	public static final int CURSOR_PADDING_Y = 4;
	public static final int CURSOR_HEIGHT = 12;

	private String text = "";
	private int maxLength = 16;
	private boolean editable = true;
	private int tickCount = 0;

	private int disabledColor = 0x707070;
	private int enabledColor = 0xE0E0E0;
	private int suggestionColor = 0x808080;

	private static final int BACKGROUND_COLOR = 0xFF000000;
	private static final int BORDER_COLOR_SELECTED = 0xFFFFFFA0;
	private static final int BORDER_COLOR_UNSELECTED = 0xFFA0A0A0;
	private static final int CURSOR_COLOR = 0xFFD0D0D0;

	@Nullable
	private Component suggestion = null;
	private int scrollOffset = 0;
	private int cursor = 0;
	private int select = -1;

	private Consumer<String> onChanged;
	private Predicate<String> textPredicate;

	public WTextField() {}

	public WTextField(Component suggestion) {
		this.suggestion = suggestion;
	}

	public void setText(String s) {
		setTextWithResult(s);
	}

	private boolean setTextWithResult(String s) {
		if (this.textPredicate == null || this.textPredicate.test(s)) {
			this.text = (s.length() > maxLength) ? s.substring(0, maxLength) : s;
			if (onChanged != null) onChanged.accept(this.text);
			cursor = Mth.clamp(cursor, 0, text.length());
			return true;
		}
		return false;
	}

	public String getText() { return this.text; }

	@Override
	public void tick() {
		super.tick();
		this.tickCount++;
	}

	@Override
	public void setSize(int x, int y) {
		this.width = x;
		this.height = y;
	}

	public void setCursorPos(int location) {
		this.cursor = Mth.clamp(location, 0, text.length());
		scrollCursorIntoView();
	}

	@OnlyIn(Dist.CLIENT)
	public void scrollCursorIntoView() {
		Font font = Minecraft.getInstance().font;
		int widthLimit = this.width - TEXT_PADDING_X * 2;
		if (scrollOffset > cursor) scrollOffset = cursor;
		if (scrollOffset < cursor && font.plainSubstrByWidth(text.substring(scrollOffset), widthLimit).length() + scrollOffset < cursor) {
			scrollOffset = cursor;
		}
		checkScrollOffset();
	}

	@OnlyIn(Dist.CLIENT)
	private void checkScrollOffset() {
		Font font = Minecraft.getInstance().font;
		int rightMostScrollOffset = text.length() - font.plainSubstrByWidth(text, width - TEXT_PADDING_X * 2).length();
		scrollOffset = Math.min(rightMostScrollOffset, scrollOffset);
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void paint(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
		Font font = Minecraft.getInstance().font;
		checkScrollOffset();
		String visibleText = font.plainSubstrByWidth(this.text.substring(this.scrollOffset), this.width - 2 * TEXT_PADDING_X);

		ScreenDrawing.coloredRect(graphics, x - 1, y - 1, width + 2, height + 2, BORDER_COLOR_UNSELECTED);
		ScreenDrawing.coloredRect(graphics, x, y, width, height, BACKGROUND_COLOR);

		int textColor = this.editable ? this.enabledColor : this.disabledColor;
		graphics.drawString(font, visibleText, x + TEXT_PADDING_X, y + TEXT_PADDING_Y, textColor, false);

		if (this.isFocused() && (this.tickCount / 6 % 2 == 0)) {
			int cursorOffset = font.width(visibleText.substring(0, Math.min(this.cursor - this.scrollOffset, visibleText.length())));
			ScreenDrawing.coloredRect(graphics, x + TEXT_PADDING_X + cursorOffset, y + CURSOR_PADDING_Y, 1, CURSOR_HEIGHT, CURSOR_COLOR);
		}

		if (this.isFocused()) {
			ScreenDrawing.coloredRect(graphics, x - 1, y - 1, width + 2, height + 2, BORDER_COLOR_SELECTED);
		}
	}
	public boolean isEditable() {
		return this.editable;
	}

	public void updateNarration(NarrationElementOutput builder) {
		builder.add(NarratedElementType.TITLE, Component.literal("Text Field: " + text));
	}

	public InputResult onCharTyped(char ch, int modifiers) {
		insertText(String.valueOf(ch));
		return InputResult.PROCESSED;
	}

	private void insertText(String toInsert) {
		String before = this.text.substring(0, cursor);
		String after = this.text.substring(cursor);
		if (before.length() + after.length() + toInsert.length() > maxLength) return;
		if (setTextWithResult(before + toInsert + after)) {
			cursor = (before + toInsert).length();
			scrollCursorIntoView();
		}
	}

	@Override
	public InputResult onKeyPressed(int keyCode, int scanCode, int modifiers) {
		if (!isEditable()) return InputResult.IGNORED;

		switch (keyCode) {
			case GLFW.GLFW_KEY_BACKSPACE -> {
				if (cursor > 0) {
					setText(text.substring(0, cursor - 1) + text.substring(cursor));
					cursor--;
					scrollCursorIntoView();
				}
			}
			case GLFW.GLFW_KEY_LEFT -> setCursorPos(cursor - 1);
			case GLFW.GLFW_KEY_RIGHT -> setCursorPos(cursor + 1);
			default -> { return InputResult.IGNORED; }
		}
		return InputResult.PROCESSED;
	}

	public WTextField setMaxLength(int max) { this.maxLength = max; return this; }
	public WTextField setEditable(boolean editable) { this.editable = editable; return this; }
}
