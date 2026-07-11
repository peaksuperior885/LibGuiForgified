package io.github.cottonmc.cotton.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import io.github.cottonmc.cotton.gui.client.BackgroundPainter;
import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.impl.LibGuiCommon;
import io.github.cottonmc.cotton.gui.impl.client.NarrationMessages;
import io.github.cottonmc.cotton.gui.widget.data.Axis;
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import io.github.cottonmc.cotton.gui.widget.icon.Icon;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

// TODO: Different tab positions

/**
 * A panel that contains creative inventory-style tabs on the top.
 *
 * @since 3.0.0
 */
public class WTabPanel extends WPanel {
	private static final int TAB_PADDING = 4;
	private static final int TAB_WIDTH = 28;
	private static final int TAB_HEIGHT = 30;
	private static final int ICON_SIZE = 16;

	private final WBox tabRibbon = new WBox(Axis.HORIZONTAL).setSpacing(1);
	private final List<WTab> tabWidgets = new ArrayList<>();
	private final Map<Tab, WTab> tabWidgetsByData = new HashMap<>();
	private final WCardPanel mainPanel = new WCardPanel();

	public WTabPanel() {
		add(tabRibbon, 0, 0);
		add(mainPanel, 0, TAB_HEIGHT);
	}

	private void add(WWidget widget, int x, int y) {
		children.add(widget);
		widget.setParent(this);
		widget.setLocation(x, y);
		expandToFit(widget);
	}

	public void add(Tab tab) {
		WTab tabWidget = new WTab(tab);

		if (tabWidgets.isEmpty()) {
			tabWidget.selected = true;
		}

		tabWidgets.add(tabWidget);
		tabWidgetsByData.put(tab, tabWidget);
		tabRibbon.add(tabWidget, TAB_WIDTH, TAB_HEIGHT + TAB_PADDING);
		mainPanel.add(tab.getWidget());
	}

	public void add(WWidget widget, Consumer<Tab.Builder> configurator) {
		Tab.Builder builder = new Tab.Builder(widget);
		configurator.accept(builder);
		add(builder.build());
	}

	public Tab getSelectedTab() {
		return ((WTab) mainPanel.getSelectedCard()).data;
	}

	@Contract("null -> fail; _ -> this")
	public WTabPanel setSelectedTab(Tab tab) {
		Objects.requireNonNull(tab, "tab");
		WTab widget = tabWidgetsByData.get(tab);

		if (widget == null) {
			throw new NoSuchElementException("Trying to select unknown tab " + tab);
		}

		return setSelectedIndex(tabWidgets.indexOf(widget));
	}

	public int getSelectedIndex() {
		return mainPanel.getSelectedIndex();
	}

	@Contract("_ -> this")
	public WTabPanel setSelectedIndex(int tabIndex) {
		mainPanel.setSelectedIndex(tabIndex);

		for (int i = 0; i < getTabCount(); i++) {
			tabWidgets.get(i).selected = (i == tabIndex);
		}

		layout();
		return this;
	}

	public int getTabCount() {
		return tabWidgets.size();
	}

	@Override
	public void setSize(int x, int y) {
		super.setSize(x, y);
		tabRibbon.setSize(x, TAB_HEIGHT);
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void addPainters() {
		super.addPainters();
		mainPanel.setBackgroundPainter(BackgroundPainter.VANILLA);
	}

	// ==================== Tab Inner Class ====================

	public static class Tab {
		@Nullable
		private final Component title;
		@Nullable
		private final Icon icon;
		private final WWidget widget;
		@Nullable
		private final Consumer<TooltipBuilder> tooltip;

		private Tab(@Nullable Component title, @Nullable Icon icon, WWidget widget, @Nullable Consumer<TooltipBuilder> tooltip) {
			if (title == null && icon == null) {
				throw new IllegalArgumentException("A tab must have a title or an icon");
			}

			this.title = title;
			this.icon = icon;
			this.widget = Objects.requireNonNull(widget, "widget");
			this.tooltip = tooltip;
		}

		@Nullable
		public Component getTitle() {
			return title;
		}

		@Nullable
		public Icon getIcon() {
			return icon;
		}

		public WWidget getWidget() {
			return widget;
		}

		@OnlyIn(Dist.CLIENT)
		public void addTooltip(TooltipBuilder tooltip) {
			if (this.tooltip != null) {
				this.tooltip.accept(tooltip);
			}
		}

		public static final class Builder {
			@Nullable
			private Component title;
			@Nullable
			private Icon icon;
			private final WWidget widget;
			private final List<Component> tooltip = new ArrayList<>();

			public Builder(WWidget widget) {
				this.widget = Objects.requireNonNull(widget, "widget");
			}

			public Builder title(Component title) {
				this.title = Objects.requireNonNull(title, "title");
				return this;
			}

			public Builder icon(Icon icon) {
				this.icon = Objects.requireNonNull(icon, "icon");
				return this;
			}

			public Builder tooltip(Component... lines) {
				Objects.requireNonNull(lines, "lines");
				Collections.addAll(tooltip, lines);
				return this;
			}

			public Builder tooltip(Collection<? extends Component> lines) {
				Objects.requireNonNull(lines, "lines");
				tooltip.addAll(lines);
				return this;
			}

			public Tab build() {
				Consumer<TooltipBuilder> tooltipConsumer = null;

				if (!this.tooltip.isEmpty()) {
					tooltipConsumer = builder -> builder.add(this.tooltip.toArray(new Component[0]));
				}

				return new Tab(title, icon, widget, tooltipConsumer);
			}
		}
	}

	// ==================== WTab Inner Class ====================

	private final class WTab extends WWidget {
		private final Tab data;
		boolean selected = false;

		WTab(Tab data) {
			this.data = data;
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
		public InputResult onClick(int x, int y, int button) {
			super.onClick(x, y, button);

			Minecraft.getInstance().getSoundManager().play(
				SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
			);

			setSelectedIndex(tabWidgets.indexOf(this));
			return InputResult.PROCESSED;
		}

		@OnlyIn(Dist.CLIENT)
		@Override
		public InputResult onKeyPressed(int ch, int key, int modifiers) {
			if (isActivationKey(ch)) {
				onClick(0, 0, 0);
				return InputResult.PROCESSED;
			}
			return InputResult.IGNORED;
		}

		@OnlyIn(Dist.CLIENT)
		@Override
		public void paint(GuiGraphics context, int x, int y, int mouseX, int mouseY) {
			Font renderer = Minecraft.getInstance().font;
			Component title = data.getTitle();
			Icon icon = data.getIcon();

			if (title != null) {
				int width = TAB_WIDTH + renderer.width(title);
				if (icon == null) width = Math.max(TAB_WIDTH, width - ICON_SIZE);

				if (this.width != width) {
					setSize(width, this.height);
					getParent().layout();
				}
			}

			(selected ? Painters.SELECTED_TAB : Painters.UNSELECTED_TAB).paintBackground(context, x, y, this);

			if (isFocused()) {
				(selected ? Painters.SELECTED_TAB_FOCUS_BORDER : Painters.UNSELECTED_TAB_FOCUS_BORDER)
					.paintBackground(context, x, y, this);
			}

			int iconX = 6;

			if (title != null) {
				int titleX = (icon != null) ? iconX + ICON_SIZE + 1 : 0;
				int titleY = (height - TAB_PADDING - renderer.lineHeight) / 2 + 1;
				int width = (icon != null) ? this.width - iconX - ICON_SIZE : this.width;
				HorizontalAlignment align = (icon != null) ? HorizontalAlignment.LEFT : HorizontalAlignment.CENTER;

				int color = shouldRenderInDarkMode()
					? WLabel.DEFAULT_DARKMODE_TEXT_COLOR
					: (selected ? WLabel.DEFAULT_TEXT_COLOR : 0xEEEEEE);

				ScreenDrawing.drawString(context, title.getVisualOrderText(), align, x + titleX, y + titleY, width, color);
			}

			if (icon != null) {
				icon.paint(context, x + iconX, y + (height - TAB_PADDING - ICON_SIZE) / 2, ICON_SIZE);
			}
		}

		@OnlyIn(Dist.CLIENT)
		@Override
		public void addTooltip(TooltipBuilder tooltip) {
			data.addTooltip(tooltip);
		}

		@OnlyIn(Dist.CLIENT)
		@Override
		public void addNarrations(NarrationElementOutput builder) {
			Component label = data.getTitle();

			if (label != null) {
				builder.add(NarratedElementType.TITLE, Component.translatable(NarrationMessages.TAB_TITLE_KEY, label));
			}

			builder.add(NarratedElementType.POSITION,
				Component.translatable(NarrationMessages.TAB_POSITION_KEY, tabWidgets.indexOf(this) + 1, tabWidgets.size()));
		}
	}

	// ==================== Painters ====================

	@OnlyIn(Dist.CLIENT)
	static final class Painters {
		// Use ResourceLocation.fromNamespaceAndPath instead of the 'new' operator
		static final BackgroundPainter SELECTED_TAB = BackgroundPainter.createLightDarkVariants(
			BackgroundPainter.createNinePatch(ResourceLocation.fromNamespaceAndPath(LibGuiCommon.MOD_ID, "textures/widget/tab/selected_light.png")).setTopPadding(2),
			BackgroundPainter.createNinePatch(ResourceLocation.fromNamespaceAndPath(LibGuiCommon.MOD_ID, "textures/widget/tab/selected_dark.png")).setTopPadding(2)
		);

		static final BackgroundPainter UNSELECTED_TAB = BackgroundPainter.createLightDarkVariants(
			BackgroundPainter.createNinePatch(ResourceLocation.fromNamespaceAndPath(LibGuiCommon.MOD_ID, "textures/widget/tab/unselected_light.png")),
			BackgroundPainter.createNinePatch(ResourceLocation.fromNamespaceAndPath(LibGuiCommon.MOD_ID, "textures/widget/tab/unselected_dark.png"))
		);

		static final BackgroundPainter SELECTED_TAB_FOCUS_BORDER = BackgroundPainter.createNinePatch(
			ResourceLocation.fromNamespaceAndPath(LibGuiCommon.MOD_ID, "textures/widget/tab/focus.png")).setTopPadding(2);

		static final BackgroundPainter UNSELECTED_TAB_FOCUS_BORDER = BackgroundPainter.createNinePatch(
			ResourceLocation.fromNamespaceAndPath(LibGuiCommon.MOD_ID, "textures/widget/tab/focus.png"));
	}
}
