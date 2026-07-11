package io.github.cottonmc.cotton.gui;

import net.minecraft.world.inventory.ContainerData; // PropertyDelegate -> ContainerData

/**
 * This interface can be implemented on block entity classes
 * for providing a container data delegate.
 *
 * @see SyncedGuiDescription#getBlockPropertyDelegate(net.minecraft.world.inventory.ContainerLevelAccess)
 */
public interface PropertyDelegateHolder {
	/**
	 * Gets this block entity's container data delegate.
	 *
	 * <p>On the client, the returned delegate <b>must</b> have a working implementation of
	 * {@link ContainerData#set(int, int)}.
	 *
	 * @return the container data delegate
	 */
	ContainerData getPropertyDelegate(); // PropertyDelegate -> ContainerData
}
