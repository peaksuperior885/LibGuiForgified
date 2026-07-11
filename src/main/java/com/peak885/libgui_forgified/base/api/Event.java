package com.peak885.libgui_forgified.base.api;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/**
 * Ported Event class for Forge 1.21.1.
 * Note: If this is purely for a GUI library's internal communication,
 * consider migrating to a native Forge EventBus implementation.
 */
@ApiStatus.NonExtendable
public abstract class Event<T> {

	protected volatile T invoker;

	public final T invoker() {
		return invoker;
	}

	public abstract void register(T listener);

	// Identifier -> ResourceLocation in Mojmap
	public static final ResourceLocation DEFAULT_PHASE = ResourceLocation.fromNamespaceAndPath("fake_fabric", "default");

	public void register(ResourceLocation phase, T listener) {
		register(listener);
	}

	public void addPhaseOrdering(ResourceLocation firstPhase, ResourceLocation secondPhase) {
		// Logic for phase ordering needs to be implemented if you are building the factory
	}
}
