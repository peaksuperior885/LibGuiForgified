package io.github.cottonmc.cotton.gui;

import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player; // PlayerEntity -> Player
import net.minecraft.world.entity.player.Inventory; // PlayerInventory -> Inventory
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.MenuType; // ScreenHandlerType -> MenuType

import java.util.Objects;

/**
 * A {@link SyncedGuiDescription} for an {@linkplain ItemStack item stack}
 * in an {@linkplain net.minecraft.world.Container container}.
 *
 * <p>The owning item is represented with a {@link SlotAccess}, which can be
 * an item in an entity's inventory or a block's container, or any other reference
 * to an item stack.
 *
 *
 * @since 7.0.0
 */
public class ItemSyncedGuiDescription extends SyncedGuiDescription {
	/**
	 * A reference to the owning item stack of this GUI.
	 */
	protected final SlotAccess owner; // StackReference -> SlotAccess

	/**
	 * The initial item stack of this GUI. This stack must <strong>not</strong> be mutated!
	 */
	protected final ItemStack ownerStack;

	/**
	 * Constructs an {@code ItemSyncedGuiDescription}.
	 *
	 * @param type            the screen handler type
	 * @param syncId          the sync ID
	 * @param playerInventory the inventory of the player viewing this GUI description
	 * @param owner           a reference to the owning item stack of this GUI description
	 */
	public ItemSyncedGuiDescription(MenuType<?> type, int syncId, Inventory playerInventory, SlotAccess owner) { // ScreenHandlerType -> MenuType, PlayerInventory -> Inventory, StackReference -> SlotAccess
		super(type, syncId, playerInventory);
		this.owner = Objects.requireNonNull(owner, "Owner cannot be null");
		this.ownerStack = owner.get().copy();
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation for {@code ItemSyncedGuiDescription} returns {@code true} if and only if
	 * the {@linkplain #owner current owning item stack} is {@linkplain ItemStack#matches matching}
	 * to the {@linkplain #ownerStack original owner}.
	 *
	 * <p>If the item components are intended to change, subclasses should override this method to only check
	 * the item and the count. Those subclasses should also take care to respond properly
	 * to any component changes in the owning item stack.
	 */
	@Override
	public boolean stillValid(Player entity) {
		return ItemStack.matches(ownerStack, owner.get());
	}
}
