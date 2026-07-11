package com.peak885.libgui_forgified.base.api;

import com.peak885.libgui_forgified.base.impl.EventFactoryImpl;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;

/**
 * Ported EventFactory for Forge 1.21.1.
 */
public final class EventFactory {
	private EventFactory() { }

	public static <T> Event<T> createArrayBacked(Class<? super T> type, Function<T[], T> invokerFactory) {
		return EventFactoryImpl.createArrayBacked(type, invokerFactory);
	}

	public static <T> Event<T> createArrayBacked(Class<T> type, T emptyInvoker, Function<T[], T> invokerFactory) {
		return createArrayBacked(type, listeners -> {
			if (listeners.length == 0) {
				return emptyInvoker;
			} else if (listeners.length == 1) {
				return listeners[0];
			} else {
				return invokerFactory.apply(listeners);
			}
		});
	}

	/**
	 * Creates an array-backed event with phase ordering support.
	 * Identifier -> ResourceLocation migration applied.
	 */
	public static <T> Event<T> createWithPhases(Class<? super T> type, Function<T[], T> invokerFactory, ResourceLocation... defaultPhases) {
		EventFactoryImpl.ensureContainsDefault(defaultPhases);
		EventFactoryImpl.ensureNoDuplicates(defaultPhases);

		Event<T> event = createArrayBacked(type, invokerFactory);

		for (int i = 1; i < defaultPhases.length; ++i) {
			event.addPhaseOrdering(defaultPhases[i-1], defaultPhases[i]);
		}

		return event;
	}

	@Deprecated
	public static String getHandlerName(Object handler) {
		return handler.getClass().getName();
	}

	@Deprecated
	public static boolean isProfilingEnabled() {
		return false;
	}

	@Deprecated(forRemoval = true)
	public static void invalidate() {
		EventFactoryImpl.invalidate();
	}
}
