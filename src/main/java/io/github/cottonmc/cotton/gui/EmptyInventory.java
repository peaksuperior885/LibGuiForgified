package io.github.cottonmc.cotton.gui;

import net.minecraft.world.Container; // Inventory -> Container
import net.minecraft.world.entity.player.Player; // PlayerEntity -> Player
import net.minecraft.world.item.ItemStack;

/**
 * An empty inventory that cannot hold any items.
 */
public class EmptyInventory implements Container { // Inventory -> Container
	public static final EmptyInventory INSTANCE = new EmptyInventory();

	private EmptyInventory() {}

	@Override
	public void clearContent() { // clear -> clearContent
	}

	@Override
	public int getContainerSize() { // size -> getContainerSize
		return 0;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public ItemStack getItem(int slot) { // getStack -> getItem
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int count) { // removeStack -> removeItem
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) { // removeStack -> removeItemNoUpdate
		return ItemStack.EMPTY;
	}

	@Override
	public void setItem(int slot, ItemStack stack) { // setStack -> setItem
	}

	@Override
	public void setChanged() { // markDirty -> setChanged
	}

	@Override
	public boolean stillValid(Player player) { // canPlayerUse -> stillValid
		return true;
	}
}
