package io.github.cottonmc.cotton.gui.impl.client;

import net.minecraft.client.gui.components.events.ContainerEventHandler; // AbstractParentElement -> ContainerEventHandler
import net.minecraft.client.gui.components.events.GuiEventListener; // Element -> GuiEventListener
import net.minecraft.client.gui.navigation.ScreenRectangle; // ScreenRect -> ScreenRectangle
import net.minecraft.client.gui.navigation.FocusNavigationEvent; // Fixed package path
import net.minecraft.client.gui.ComponentPath; // GuiNavigationPath -> ComponentPath

import io.github.cottonmc.cotton.gui.widget.WPanel;
import io.github.cottonmc.cotton.gui.widget.WWidget;
import io.github.cottonmc.cotton.gui.widget.data.Rect2i;
import io.github.cottonmc.cotton.gui.widget.focus.Focus;
import io.github.cottonmc.cotton.gui.widget.focus.FocusModel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class FocusElements {
	public static PanelFocusElement ofPanel(WPanel panel) {
		PanelFocusElement result = new PanelFocusElement(panel);
		result.refreshChildren();
		return result;
	}

	public static Stream<FocusElement<?>> toElements(WWidget widget) {
		if (widget instanceof WPanel panel) {
			return Stream.of(ofPanel(panel));
		} else {
			return fromFoci(widget);
		}
	}

	private static Stream<FocusElement<?>> fromFoci(WWidget widget) {
		@Nullable FocusModel<?> focusModel = widget.getFocusModel();
		if (focusModel == null) return Stream.empty();

		return focusModel.foci().map(focus -> new LeafFocusElement(widget, focus));
	}

	public sealed interface FocusElement<W extends WWidget> extends GuiEventListener { // Element -> GuiEventListener
		W widget();
	}

	private record LeafFocusElement(WWidget widget, Focus<?> focus) implements FocusElement<WWidget> {
		@SuppressWarnings("unchecked")
		@Override
		public void setFocused(boolean focused) {
			if (focused) {
				Focus<?> focus = focus();

				if (focus != null) {
					widget.requestFocus();
					((FocusModel<Object>) widget.getFocusModel()).setFocused((Focus<Object>) focus);
				}
			} else {
				widget.releaseFocus();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean isFocused() {
			if (widget.isFocused()) {
				FocusModel<Object> focusModel = (FocusModel<Object>) widget.getFocusModel();
				if (focusModel != null) {
					return focusModel.isFocused((Focus<Object>) focus);
				}
			}

			return false;
		}

		@Override
		public ScreenRectangle getRectangle() {
			Rect2i area = focus.area();
			return new ScreenRectangle(
				widget.getAbsoluteX() + area.x(),
				widget.getAbsoluteY() + area.y(),
				area.width(), area.height()
			);
		}

		@Override
		public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigation) {
			return widget.canFocus() && !isFocused() ? ComponentPath.leaf(this) : null;
		}
	}

	private static final class PanelFocusElement implements ContainerEventHandler, FocusElement<WPanel> { // AbstractParentElement -> ContainerEventHandler
		private final List<FocusElement<?>> children = new ArrayList<>();
		private final WPanel widget;
		private List<WWidget> childWidgets;
		private @Nullable GuiEventListener focused; // Internal focused element tracking

		private PanelFocusElement(WPanel widget) {
			this.widget = widget;
		}

		private boolean dragging;

		@Override
		public boolean isDragging() {
			return dragging;
		}

		@Override
		public void setDragging(boolean dragging) {
			this.dragging = dragging;
		}

		private void refreshChildren() {
			boolean shouldRefresh = false;
			if (childWidgets == null) {
				childWidgets = widget.streamChildren().toList();
				shouldRefresh = true;
			} else {
				List<WWidget> currentChildren = widget.streamChildren().toList();
				if (!childWidgets.equals(currentChildren)) {
					childWidgets = currentChildren;
					shouldRefresh = true;
				}
			}

			if (shouldRefresh) {
				children.clear();
				fromFoci(widget).forEach(children::add);
				childWidgets.stream()
					.flatMap(FocusElements::toElements)
					.forEach(children::add);
				refreshFocus();
			}
		}

		@Override
		public List<? extends GuiEventListener> children() { // Returns your UI listeners
			refreshChildren();
			return children;
		}

		@Override
		public WPanel widget() {
			return widget;
		}

		@Override
		public @Nullable GuiEventListener getFocused() {
			refreshFocus();
			return this.focused;
		}

		@Override
		public void setFocused(@Nullable GuiEventListener listener) {
			this.focused = listener;
		}

		public void refreshFocus() {
			if (children.isEmpty()) return;

			boolean foundFocus = false;
			for (FocusElement<?> child : children) {
				if (child instanceof PanelFocusElement panel) {
					panel.refreshFocus();
				}

				if (!foundFocus && child.isFocused()) {
					setFocused(child);
					foundFocus = true;
				}
			}
		}

		@Override
		public void setFocused(boolean focused) {
			// Required interface mapping method stub
		}

		@Override
		public boolean isFocused() {
			return children.stream().anyMatch(GuiEventListener::isFocused);
		}
	}
}
