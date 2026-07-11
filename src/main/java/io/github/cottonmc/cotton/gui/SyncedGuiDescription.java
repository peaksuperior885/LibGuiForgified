package io.github.cottonmc.cotton.gui;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.WorldlyContainerHolder; // Replacement for InventoryProvider if applicable, using standard checks below
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player; // PlayerEntity -> Player
import net.minecraft.world.entity.player.Inventory; // PlayerInventory -> Inventory
import net.minecraft.world.Container; // Inventory -> Container
import net.minecraft.world.SimpleContainer; // SimpleInventory -> SimpleContainer
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.SimpleContainerData; // ArrayPropertyDelegate -> SimpleContainerData
import net.minecraft.world.inventory.ContainerData; // PropertyDelegate -> ContainerData
import net.minecraft.world.inventory.AbstractContainerMenu; // ScreenHandler -> AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess; // ScreenHandlerContext -> ContainerLevelAccess
import net.minecraft.world.inventory.MenuType; // ScreenHandlerType -> MenuType
import net.minecraft.world.inventory.Slot;
import net.minecraft.server.level.ServerPlayer; // ServerPlayerEntity -> ServerPlayer
import net.minecraft.server.level.ServerLevel; // ServerWorld -> ServerLevel
import net.minecraft.world.level.Level; // World -> Level

import io.github.cottonmc.cotton.gui.client.BackgroundPainter;
import io.github.cottonmc.cotton.gui.client.LibGui;
import io.github.cottonmc.cotton.gui.networking.NetworkSide;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.WPanel;
import io.github.cottonmc.cotton.gui.widget.WPlayerInvPanel;
import io.github.cottonmc.cotton.gui.widget.WWidget;
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import io.github.cottonmc.cotton.gui.widget.data.Vec2i;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import com.peak885.libgui_forgified.network.LibGuiPacket;
import com.peak885.libgui_forgified.network.ModNetwork;

import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * A screen handler-based GUI description for GUIs with slots.
 */
public class SyncedGuiDescription extends AbstractContainerMenu implements GuiDescription { // ScreenHandler -> AbstractContainerMenu

	protected Container blockInventory; // Inventory -> Container
	protected Inventory playerInventory; // PlayerInventory -> Inventory
	protected Level world; // World -> Level
	protected ContainerData propertyDelegate; // PropertyDelegate -> ContainerData

	protected WPanel rootPanel = new WGridPanel().setInsets(Insets.ROOT_PANEL);
	protected int titleColor = WLabel.DEFAULT_TEXT_COLOR;
	protected int darkTitleColor = WLabel.DEFAULT_DARKMODE_TEXT_COLOR;
	protected boolean fullscreen = false;
	protected boolean titleVisible = true;
	protected HorizontalAlignment titleAlignment = HorizontalAlignment.LEFT;

	protected WWidget focus;
	private Vec2i titlePos = new Vec2i(8, 6);

	/**
	 * Constructs a new synced GUI description without a block inventory or a property delegate.
	 */
	public SyncedGuiDescription(MenuType<?> type, int syncId, Inventory playerInventory) { // ScreenHandlerType -> MenuType
		super(type, syncId);
		this.blockInventory = null;
		this.playerInventory = playerInventory;
		this.world = playerInventory.player.level(); // player.getWorld() -> player.level()
		this.propertyDelegate = null;
	}

	/**
	 * Constructs a new synced GUI description.
	 */
	public SyncedGuiDescription(MenuType<?> type, int syncId, Inventory playerInventory, @Nullable Container blockInventory, @Nullable ContainerData propertyDelegate) {
		super(type, syncId);
		this.blockInventory = blockInventory;
		this.playerInventory = playerInventory;
		this.world = playerInventory.player.level();
		this.propertyDelegate = propertyDelegate;
		if (propertyDelegate != null && propertyDelegate.getCount() > 0) this.addDataSlots(propertyDelegate); // addProperties -> addDataSlots, size() -> getCount()
		if (blockInventory != null) blockInventory.startOpen(playerInventory.player); // onOpen -> startOpen
	}

	public WPanel getRootPanel() {
		return rootPanel;
	}

	public int getTitleColor() {
		return (world.isClientSide() && isDarkMode().orElse(LibGui.isDarkMode())) ? darkTitleColor : titleColor; // isClient -> isClientSide()
	}

	public SyncedGuiDescription setRootPanel(WPanel panel) {
		this.rootPanel = panel;
		return this;
	}

	@Override
	public SyncedGuiDescription setTitleColor(int color) {
		this.titleColor = color;
		this.darkTitleColor = (color == WLabel.DEFAULT_TEXT_COLOR) ? WLabel.DEFAULT_DARKMODE_TEXT_COLOR : color;
		return this;
	}

	@Override
	public SyncedGuiDescription setTitleColor(int lightColor, int darkColor) {
		this.titleColor = lightColor;
		this.darkTitleColor = darkColor;
		return this;
	}

	@OnlyIn(Dist.CLIENT)
	public void addPainters() {
		if (this.rootPanel!=null && !fullscreen) {
			this.rootPanel.setBackgroundPainter(BackgroundPainter.VANILLA);
		}
	}

	public void addSlotPeer(ValidatedSlot slot) {
		this.addSlot(slot);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) { // quickMove -> quickMoveStack, PlayerEntity -> Player
		ItemStack result = ItemStack.EMPTY;
		Slot slot = slots.get(index);

		if (slot.hasItem()) { // hasStack() -> hasItem()
			ItemStack slotStack = slot.getItem(); // getStack() -> getItem()
			result = slotStack.copy();

			if (blockInventory!=null) {
				if (slot.container == blockInventory) { // inventory -> container
					//Try to transfer the item from the block into the player's inventory
					if (!this.insertItem(slotStack, this.playerInventory, true, player)) {
						return ItemStack.EMPTY;
					}
				} else if (!this.insertItem(slotStack, this.blockInventory, false, player)) { //Try to transfer the item from the player to the block
					return ItemStack.EMPTY;
				}
			} else {
				//There's no block, just swap between the player's storage and their hotbar
				if (!swapHotbar(slotStack, index, this.playerInventory, player)) {
					return ItemStack.EMPTY;
				}
			}

			if (slotStack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY); // setStack -> setByPlayer
			} else {
				slot.setChanged(); // markDirty -> setChanged
			}
		}

		return result;
	}

	/** WILL MODIFY toInsert! Returns true if anything was inserted. */
	private boolean insertIntoExisting(ItemStack toInsert, Slot slot, Player player) {
		ItemStack curSlotStack = slot.getItem();
		if (!curSlotStack.isEmpty() && ItemStack.isSameItemSameComponents(toInsert, curSlotStack) && slot.mayPlace(toInsert)) { // canCombine -> isSameItemSameComponents, canInsert -> mayPlace
			int combinedAmount = curSlotStack.getCount() + toInsert.getCount();
			int maxAmount = Math.min(toInsert.getMaxStackSize(), slot.getMaxStackSize(toInsert)); // getMaxCount -> getMaxStackSize, getMaxItemCount -> getMaxStackSize
			if (combinedAmount <= maxAmount) {
				toInsert.setCount(0);
				curSlotStack.setCount(combinedAmount);
				slot.setChanged();
				return true;
			} else if (curSlotStack.getCount() < maxAmount) {
				toInsert.shrink(maxAmount - curSlotStack.getCount()); // decrement -> shrink
				curSlotStack.setCount(maxAmount);
				slot.setChanged();
				return true;
			}
		}
		return false;
	}

	/** WILL MODIFY toInsert! Returns true if anything was inserted. */
	private boolean insertIntoEmpty(ItemStack toInsert, Slot slot) {
		ItemStack curSlotStack = slot.getItem();
		if (curSlotStack.isEmpty() && slot.mayPlace(toInsert)) {
			if (toInsert.getCount() > slot.getMaxStackSize(toInsert)) {
				slot.setByPlayer(toInsert.split(slot.getMaxStackSize(toInsert)));
			} else {
				slot.setByPlayer(toInsert.split(toInsert.getCount()));
			}

			slot.setChanged();
			return true;
		}

		return false;
	}

	private boolean insertItem(ItemStack toInsert, Container inventory, boolean walkBackwards, Player player) {
		//Make a unified list of slots *only from this inventory*
		ArrayList<Slot> inventorySlots = new ArrayList<>();
		for(Slot slot : slots) {
			if (slot.container==inventory) inventorySlots.add(slot);
		}
		if (inventorySlots.isEmpty()) return false;

		//Try to insert it on top of existing stacks
		boolean inserted = false;
		if (walkBackwards) {
			for(int i=inventorySlots.size()-1; i>=0; i--) {
				Slot curSlot = inventorySlots.get(i);
				if (insertIntoExisting(toInsert, curSlot, player)) inserted = true;
				if (toInsert.isEmpty()) break;
			}
		} else {
			for(int i=0; i<inventorySlots.size(); i++) {
				Slot curSlot = inventorySlots.get(i);
				if (insertIntoExisting(toInsert, curSlot, player)) inserted = true;
				if (toInsert.isEmpty()) break;
			}

		}

		//If we still have any, shove them into empty slots
		if (!toInsert.isEmpty()) {
			if (walkBackwards) {
				for(int i=inventorySlots.size()-1; i>=0; i--) {
					Slot curSlot = inventorySlots.get(i);
					if (insertIntoEmpty(toInsert, curSlot)) inserted = true;
					if (toInsert.isEmpty()) break;
				}
			} else {
				for(int i=0; i<inventorySlots.size(); i++) {
					Slot curSlot = inventorySlots.get(i);
					if (insertIntoEmpty(toInsert, curSlot)) inserted = true;
					if (toInsert.isEmpty()) break;
				}

			}
		}

		return inserted;
	}

	private boolean swapHotbar(ItemStack toInsert, int slotNumber, Container inventory, Player player) {
		//Feel out the slots to see what's storage versus hotbar
		ArrayList<Slot> storageSlots = new ArrayList<>();
		ArrayList<Slot> hotbarSlots = new ArrayList<>();
		boolean swapToStorage = true;
		boolean inserted = false;

		for(Slot slot : slots) {
			if (slot.container==inventory && slot instanceof ValidatedSlot) {
				int index = ((ValidatedSlot)slot).getInventoryIndex();
				if (index >= 0 && index < 9) {
					hotbarSlots.add(slot);
				} else {
					storageSlots.add(slot);
					if (slot.index==slotNumber) swapToStorage = false; // id -> index
				}
			}
		}
		if (storageSlots.isEmpty() || hotbarSlots.isEmpty()) return false;

		if (swapToStorage) {
			//swap from hotbar to storage
			for(int i=0; i<storageSlots.size(); i++) {
				Slot curSlot = storageSlots.get(i);
				if (insertIntoExisting(toInsert, curSlot, player)) inserted = true;
				if (toInsert.isEmpty()) break;
			}
			if (!toInsert.isEmpty()) {
				for(int i=0; i<storageSlots.size(); i++) {
					Slot curSlot = storageSlots.get(i);
					if (insertIntoEmpty(toInsert, curSlot)) inserted = true;
					if (toInsert.isEmpty()) break;
				}
			}
		} else {
			//swap from storage to hotbar
			for(int i=0; i<hotbarSlots.size(); i++) {
				Slot curSlot = hotbarSlots.get(i);
				if (insertIntoExisting(toInsert, curSlot, player)) inserted = true;
				if (toInsert.isEmpty()) break;
			}
			if (!toInsert.isEmpty()) {
				for(int i=0; i<hotbarSlots.size(); i++) {
					Slot curSlot = hotbarSlots.get(i);
					if (insertIntoEmpty(toInsert, curSlot)) inserted = true;
					if (toInsert.isEmpty()) break;
				}
			}
		}

		return inserted;
	}

	@Nullable
	@Override
	public ContainerData getPropertyDelegate() {
		return propertyDelegate;
	}

	@Override
	public GuiDescription setPropertyDelegate(ContainerData delegate) {
		this.propertyDelegate = delegate;
		return this;
	}

	/**
	 * Creates a player inventory widget from this panel's player inventory.
	 */
	public WPlayerInvPanel createPlayerInventoryPanel() {
		return new WPlayerInvPanel(this.playerInventory);
	}

	/**
	 * Creates a player inventory widget from this panel's player inventory.
	 */
	public WPlayerInvPanel createPlayerInventoryPanel(boolean hasLabel) {
		return new WPlayerInvPanel(this.playerInventory, hasLabel);
	}

	/**
	 * Creates a player inventory widget from this panel's player inventory.
	 */
	public WPlayerInvPanel createPlayerInventoryPanel(WWidget label) {
		return new WPlayerInvPanel(this.playerInventory, label);
	}

	/**
	 * Gets the block inventory at the context.
	 */
	public static Container getBlockInventory(ContainerLevelAccess ctx) { // ScreenHandlerContext -> ContainerLevelAccess, Inventory -> Container
		return getBlockInventory(ctx, () -> EmptyInventory.INSTANCE);
	}

	/**
	 * Gets the block inventory at the context.
	 */
	public static Container getBlockInventory(ContainerLevelAccess ctx, int size) {
		return getBlockInventory(ctx, () -> new SimpleContainer(size)); // SimpleInventory -> SimpleContainer
	}

	private static Container getBlockInventory(ContainerLevelAccess ctx, Supplier<Container> fallback) {
		return ctx.evaluate((level, pos) -> { // get() -> evaluate()
			BlockState state = level.getBlockState(pos);
			Block b = state.getBlock();

			if (b instanceof WorldlyContainerHolder) { // InventoryProvider -> WorldlyContainerHolder
				Container inventory = ((WorldlyContainerHolder)b).getContainer(state, level, pos); // getInventory -> getContainer
				if (inventory != null) {
					return inventory;
				}
			}

			BlockEntity be = level.getBlockEntity(pos);
			if (be!=null) {
				if (be instanceof WorldlyContainerHolder) {
					Container inventory = ((WorldlyContainerHolder)be).getContainer(state, level, pos);
					if (inventory != null) {
						return inventory;
					}
				} else if (be instanceof Container) {
					return (Container)be;
				}
			}

			return fallback.get();
		}).orElseGet(fallback);
	}

	/**
	 * Gets the property delegate at the context.
	 */
	public static ContainerData getBlockPropertyDelegate(ContainerLevelAccess ctx) {
		return ctx.evaluate((level, pos) -> {
			BlockEntity be = level.getBlockEntity(pos);
			if (be!=null && be instanceof PropertyDelegateHolder) {
				return ((PropertyDelegateHolder)be).getPropertyDelegate();
			}

			return new SimpleContainerData(0); // ArrayPropertyDelegate -> SimpleContainerData
		}).orElse(new SimpleContainerData(0));
	}

	/**
	 * Gets the property delegate at the context.
	 */
	public static ContainerData getBlockPropertyDelegate(ContainerLevelAccess ctx, int size) {
		return ctx.evaluate((level, pos) -> {
			BlockEntity be = level.getBlockEntity(pos);
			if (be!=null && be instanceof PropertyDelegateHolder) {
				return ((PropertyDelegateHolder)be).getPropertyDelegate();
			}

			return new SimpleContainerData(size);
		}).orElse(new SimpleContainerData(size));
	}

	@Override
	public boolean stillValid(Player entity) { // canUse -> stillValid
		return (blockInventory!=null) ? blockInventory.stillValid(entity) : true; // canPlayerUse -> stillValid
	}

	@Override
	public void removed(Player player) { // onClosed -> removed
		super.removed(player);
		if (blockInventory != null) blockInventory.stopOpen(player); // onClose -> stopOpen
	}

	@Override
	public boolean isFocused(WWidget widget) {
		return focus == widget;
	}

	@Override
	public WWidget getFocus() {
		return focus;
	}

	@Override
	public void requestFocus(WWidget widget) {
		if (focus==widget) return;
		if (!widget.canFocus()) return;
		if (focus!=null) focus.onFocusLost();
		focus = widget;
		focus.onFocusGained();
	}

	@Override
	public void releaseFocus(WWidget widget) {
		if (focus==widget) {
			focus = null;
			widget.onFocusLost();
		}
	}

	@Override
	public boolean isFullscreen() {
		return fullscreen;
	}

	@Override
	public void setFullscreen(boolean fullscreen) {
		this.fullscreen = fullscreen;
	}

	@Override
	public boolean isTitleVisible() {
		return titleVisible;
	}

	@Override
	public void setTitleVisible(boolean titleVisible) {
		this.titleVisible = titleVisible;
	}

	@Override
	public HorizontalAlignment getTitleAlignment() {
		return titleAlignment;
	}

	@Override
	public void setTitleAlignment(HorizontalAlignment titleAlignment) {
		this.titleAlignment = titleAlignment;
	}

	@Override
	public Vec2i getTitlePos() {
		return titlePos;
	}

	@Override
	public void setTitlePos(Vec2i titlePos) {
		this.titlePos = titlePos;
	}

	/**
	 * Gets the network side this GUI description runs on.
	 */
	public final NetworkSide getNetworkSide() {
		return world instanceof ServerLevel ? NetworkSide.SERVER : NetworkSide.CLIENT;
	}

	/**
	 * Gets the packet sender corresponding to this GUI's network side.
	 */
	public final PacketSender getPacketSender() {
		return new PacketSender((ServerPlayer) playerInventory.player);
	}

	public static class PacketSender {
		private final ServerPlayer serverPlayer;

		public PacketSender(ServerPlayer serverPlayer) {
			this.serverPlayer = serverPlayer;
		}

		public void sendToPlayer(LibGuiPacket packet) {
			ModNetwork.INSTANCE.send(packet, PacketDistributor.PLAYER.with(serverPlayer));
		}
		@OnlyIn(Dist.CLIENT)
		public void sendToServer(LibGuiPacket packet) {
			ModNetwork.INSTANCE.send(packet, PacketDistributor.SERVER.noArg());
		}
	}
}
