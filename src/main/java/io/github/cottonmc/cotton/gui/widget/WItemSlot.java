package io.github.cottonmc.cotton.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.world.entity.player.Inventory; // PlayerInventory -> Inventory
import net.minecraft.world.Container;             // Inventory -> Container
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu; // ScreenHandler -> AbstractContainerMenu
import net.minecraft.world.inventory.ClickType;           // SlotActionType -> ClickType
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import io.github.cottonmc.cotton.gui.GuiDescription;
import io.github.cottonmc.cotton.gui.ValidatedSlot;
import io.github.cottonmc.cotton.gui.client.BackgroundPainter;
import io.github.cottonmc.cotton.gui.impl.LibGuiCommon;
import io.github.cottonmc.cotton.gui.impl.VisualLogger;
import io.github.cottonmc.cotton.gui.impl.client.NarrationMessages;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import io.github.cottonmc.cotton.gui.widget.data.Rect2i;
import io.github.cottonmc.cotton.gui.widget.focus.Focus;
import io.github.cottonmc.cotton.gui.widget.focus.FocusModel;
import io.github.cottonmc.cotton.gui.widget.icon.Icon;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A widget that displays an item that can be interacted with.
 */
public class WItemSlot extends WWidget {
	/**
	 * The default texture of item slots and {@link BackgroundPainter#SLOT}.
	 *
	 * @since 6.2.0
	 */
	public static final ResourceLocation SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(LibGuiCommon.MOD_ID, "textures/widget/item_slot.png");

	private static final VisualLogger LOGGER = new VisualLogger(WItemSlot.class);
	private final List<ValidatedSlot> peers = new ArrayList<>();
	@Nullable
	@OnlyIn(Dist.CLIENT)
	private BackgroundPainter backgroundPainter;
	@Nullable
	private Icon icon = null;
	private Container inventory;
	private int startIndex = 0;
	private int slotsWide = 1;
	private int slotsHigh = 1;
	private boolean big = false;
	private boolean insertingAllowed = true;
	private boolean takingAllowed = true;
	private int focusedSlot = -1;
	private int hoveredSlot = -1;
	private Predicate<ItemStack> inputFilter = ValidatedSlot.DEFAULT_ITEM_FILTER;
	private Predicate<ItemStack> outputFilter = ValidatedSlot.DEFAULT_ITEM_FILTER;
	private final Set<ChangeListener> listeners = new HashSet<>();
	private final FocusModel<Integer> focusModel = new FocusModel<>() {
		@Override
		public boolean isFocused(Focus<Integer> focus) {
			return focusedSlot == focus.key();
		}

		@Override
		public void setFocused(Focus<Integer> focus) {
			focusedSlot = focus.key();
		}

		@Override
		public Stream<Focus<Integer>> foci() {
			Stream.Builder<Focus<Integer>> builder = Stream.builder();
			int index = 0;

			for (int y = 0; y < slotsHigh; y++) {
				for (int x = 0; x < slotsWide; x++) {
					int slotX = x * 18;
					int slotY = y * 18;
					int size = 18;

					if (big) {
						slotX -= 4;
						slotY -= 4;
						size = 26;
					}

					builder.add(new Focus<>(index, new Rect2i(slotX, slotY, size, size)));
					index++;
				}
			}

			return builder.build();
		}
	};

	public WItemSlot(Container inventory, int startIndex, int slotsWide, int slotsHigh, boolean big) {
		this();
		this.inventory = inventory;
		this.startIndex = startIndex;
		this.slotsWide = slotsWide;
		this.slotsHigh = slotsHigh;
		this.big = big;
	}

	private WItemSlot() {
		hoveredProperty().addListener((property, from, to) -> {
			assert to != null;
			if (!to) hoveredSlot = -1;
		});
	}

	public static WItemSlot of(Container inventory, int index) {
		WItemSlot w = new WItemSlot();
		w.inventory = inventory;
		w.startIndex = index;

		return w;
	}

	public static WItemSlot of(Container inventory, int startIndex, int slotsWide, int slotsHigh) {
		WItemSlot w = new WItemSlot();
		w.inventory = inventory;
		w.startIndex = startIndex;
		w.slotsWide = slotsWide;
		w.slotsHigh = slotsHigh;

		return w;
	}

	public static WItemSlot outputOf(Container inventory, int index) {
		WItemSlot w = new WItemSlot();
		w.inventory = inventory;
		w.startIndex = index;
		w.big = true;

		return w;
	}

	/**
	 * Creates a 9x3 slot widget from the "main" part of a player inventory.
	 */
	public static WItemSlot ofPlayerStorage(Container inventory) {
		WItemSlot w = new WItemSlot() {
			@Override
			protected Component getNarrationName() {
				return inventory instanceof Inventory inv ? inv.getDisplayName() : NarrationMessages.Vanilla.INVENTORY;
			}
		};
		w.inventory = inventory;
		w.startIndex = 9;
		w.slotsWide = 9;
		w.slotsHigh = 3;

		return w;
	}

	@Override
	public int getWidth() {
		return slotsWide * 18;
	}

	@Override
	public int getHeight() {
		return slotsHigh * 18;
	}

	@Override
	public boolean canFocus() {
		return true;
	}

	public boolean isBigSlot() {
		return big;
	}

	@Nullable
	public Icon getIcon() {
		return this.icon;
	}

	public WItemSlot setIcon(@Nullable Icon icon) {
		this.icon = icon;

		if (icon != null && (slotsWide * slotsHigh) > 1) {
			LOGGER.warn("Setting icon {} for item slot {} with more than 1 slot ({})", icon, this, slotsWide * slotsHigh);
		}

		return this;
	}

	public boolean isModifiable() {
		return takingAllowed || insertingAllowed;
	}

	public WItemSlot setModifiable(boolean modifiable) {
		this.insertingAllowed = modifiable;
		this.takingAllowed = modifiable;
		for (ValidatedSlot peer : peers) {
			peer.setInsertingAllowed(modifiable);
			peer.setTakingAllowed(modifiable);
		}
		return this;
	}

	public boolean isInsertingAllowed() {
		return insertingAllowed;
	}

	public WItemSlot setInsertingAllowed(boolean insertingAllowed) {
		this.insertingAllowed = insertingAllowed;
		for (ValidatedSlot peer : peers) {
			peer.setInsertingAllowed(insertingAllowed);
		}
		return this;
	}

	public boolean isTakingAllowed() {
		return takingAllowed;
	}

	public WItemSlot setTakingAllowed(boolean takingAllowed) {
		this.takingAllowed = takingAllowed;
		for (ValidatedSlot peer : peers) {
			peer.setTakingAllowed(takingAllowed);
		}
		return this;
	}

	public int getFocusedSlot() {
		return focusedSlot;
	}

	@Override
	public void validate(GuiDescription host) {
		super.validate(host);
		peers.clear();
		int index = startIndex;

		for (int y = 0; y < slotsHigh; y++) {
			for (int x = 0; x < slotsWide; x++) {
				ValidatedSlot slot = createSlotPeer(inventory, index, this.getAbsoluteX() + (x * 18) + 1, this.getAbsoluteY() + (y * 18) + 1);
				slot.setInsertingAllowed(insertingAllowed);
				slot.setTakingAllowed(takingAllowed);
				slot.setInputFilter(inputFilter);
				slot.setOutputFilter(outputFilter);
				for (ChangeListener listener : listeners) {
					slot.addChangeListener(this, listener);
				}
				peers.add(slot);
				host.addSlotPeer(slot);
				index++;
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public InputResult onKeyPressed(int ch, int key, int modifiers) {
		if (isActivationKey(ch) && host instanceof AbstractContainerMenu && focusedSlot >= 0) {
			AbstractContainerMenu handler = (AbstractContainerMenu) host;
			Minecraft client = Minecraft.getInstance();

			ValidatedSlot peer = peers.get(focusedSlot);
			if (client.gameMode != null && client.player != null) {
				client.gameMode.handleInventoryMouseClick(handler.containerId, peer.index, 0, ClickType.PICKUP, client.player);
			}
			return InputResult.PROCESSED;
		}

		return InputResult.IGNORED;
	}

	protected ValidatedSlot createSlotPeer(Container inventory, int index, int x, int y) {
		return new ValidatedSlot(inventory, index, x, y);
	}

	@Nullable
	@OnlyIn(Dist.CLIENT)
	public BackgroundPainter getBackgroundPainter() {
		return backgroundPainter;
	}

	@OnlyIn(Dist.CLIENT)
	public void setBackgroundPainter(@Nullable BackgroundPainter painter) {
		this.backgroundPainter = painter;
	}

	public Predicate<ItemStack> getInputFilter() {
		return inputFilter;
	}

	public WItemSlot setInputFilter(Predicate<ItemStack> inputFilter) {
		this.inputFilter = inputFilter;
		for (ValidatedSlot peer : peers) {
			peer.setInputFilter(inputFilter);
		}
		return this;
	}

	public Predicate<ItemStack> getOutputFilter() {
		return outputFilter;
	}

	public WItemSlot setOutputFilter(Predicate<ItemStack> outputFilter) {
		this.outputFilter = outputFilter;
		for (ValidatedSlot peer : peers) {
			peer.setOutputFilter(outputFilter);
		}
		return this;
	}

	@Deprecated(forRemoval = true)
	public Predicate<ItemStack> getFilter() {
		return inputFilter;
	}

	@Deprecated(forRemoval = true)
	public WItemSlot setFilter(Predicate<ItemStack> filter) {
		return setInputFilter(filter);
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void paint(GuiGraphics context, int x, int y, int mouseX, int mouseY) {
		if (backgroundPainter != null) {
			backgroundPainter.paintBackground(context, x, y, this);
		}

		if (icon != null) {
			icon.paint(context, x + 1, y + 1, 16);
		}
	}

	@Nullable
	@Override
	public FocusModel<?> getFocusModel() {
		return focusModel;
	}

	@Override
	public void onFocusLost() {
		focusedSlot = -1;
	}

	public void addChangeListener(ChangeListener listener) {
		Objects.requireNonNull(listener, "listener");
		listeners.add(listener);

		for (ValidatedSlot peer : peers) {
			peer.addChangeListener(this, listener);
		}
	}

	@Override
	public void onShown() {
		for (ValidatedSlot peer : peers) {
			peer.setVisible(true);
		}
	}

	@Override
	public InputResult onMouseMove(int x, int y) {
		int slotX = x / 18;
		int slotY = y / 18;
		hoveredSlot = slotX + slotY * slotsWide;
		return InputResult.PROCESSED;
	}

	@Override
	public void onHidden() {
		super.onHidden();

		for (ValidatedSlot peer : peers) {
			peer.setVisible(false);
		}
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void addPainters() {
		backgroundPainter = BackgroundPainter.SLOT;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void addNarrations(NarrationElementOutput builder) { // NarrationMessageBuilder -> NarrationElementOutput
		List<Component> parts = new ArrayList<>();
		Component name = getNarrationName();
		if (name != null) parts.add(name);

		if (focusedSlot >= 0) {
			parts.add(Component.translatable(NarrationMessages.ITEM_SLOT_TITLE_KEY, focusedSlot + 1, slotsWide * slotsHigh));
		} else if (hoveredSlot >= 0) {
			parts.add(Component.translatable(NarrationMessages.ITEM_SLOT_TITLE_KEY, hoveredSlot + 1, slotsWide * slotsHigh));
		}

		builder.add(NarratedElementType.TITLE, parts.toArray(new Component[0]));
	}

	@Nullable
	protected Component getNarrationName() {
		return null;
	}

	@FunctionalInterface
	public interface ChangeListener {
		void onStackChanged(WItemSlot slot, Container inventory, int index, ItemStack stack);
	}
}
