package io.github.cottonmc.cotton.gui.impl.mixin.client;

import io.github.cottonmc.cotton.gui.impl.client.LibGuiScreen;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(ContainerEventHandler.class)
public interface ContainerEventHandlerMixin extends LibGuiScreen {

	@SuppressWarnings("unchecked")
	@Override
	default List<GuiEventListener> libgui$getChildren() {
		return (List<GuiEventListener>) ((ContainerEventHandler) this).children();
	}
}
