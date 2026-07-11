package com.peak885.libgui_forgified.base.impl;

import java.lang.reflect.Array;
import java.util.Arrays;
import net.minecraft.resources.ResourceLocation;

import com.peak885.libgui_forgified.base.impl.toposort.SortableNode;

/**
 * Data of an {@link ArrayBackedEvent} phase.
 */
class EventPhaseData<T> extends SortableNode<EventPhaseData<T>> {
	final ResourceLocation id;
	T[] listeners;

	@SuppressWarnings("unchecked")
	EventPhaseData(ResourceLocation id, Class<?> listenerClass) {
		this.id = id;
		this.listeners = (T[]) Array.newInstance(listenerClass, 0);
	}

	void addListener(T listener) {
		int oldLength = listeners.length;
		listeners = Arrays.copyOf(listeners, oldLength + 1);
		listeners[oldLength] = listener;
	}

	@Override
	protected String getDescription() {
		return id.toString();
	}
}
