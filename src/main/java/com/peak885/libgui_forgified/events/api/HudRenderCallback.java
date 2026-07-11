package com.peak885.libgui_forgified.events.api;

import net.minecraft.client.gui.GuiGraphics;
import com.peak885.libgui_forgified.base.api.Event;
import com.peak885.libgui_forgified.base.api.EventFactory;

public interface HudRenderCallback {
	Event<HudRenderCallback> EVENT = EventFactory.createArrayBacked(HudRenderCallback.class, (listeners) -> (graphics, delta) -> {
		for (HudRenderCallback event : listeners) {
			event.onHudRender(graphics, delta);
		}
	});

	/**
	 * Called after rendering the HUD elements.
	 *
	 * @param graphics the {@link GuiGraphics} instance
	 * @param tickDelta the partial tick for interpolation
	 */
	void onHudRender(GuiGraphics graphics, float tickDelta);
}
