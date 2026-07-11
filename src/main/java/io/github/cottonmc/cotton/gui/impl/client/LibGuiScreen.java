package io.github.cottonmc.cotton.gui.impl.client;

import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.List;

public interface LibGuiScreen {
	List<GuiEventListener> libgui$getChildren();
}
