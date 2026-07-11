package io.github.cottonmc.cotton.gui.impl.modmenu;

import net.minecraft.network.chat.Component;

import io.github.cottonmc.cotton.gui.client.CottonClientScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import com.peak885.libgui_forgified.loader.gui.ModConfigScreenInitializer;

public class ModMenuSupport implements ModConfigScreenInitializer {
	@Override
	public ConfigScreenHandler.ConfigScreenFactory getModConfigScreenFactory() {
		// Text.translatable -> Component.translatable
		return new ConfigScreenHandler.ConfigScreenFactory(screen -> new CottonClientScreen(Component.translatable("options.libgui.libgui_settings"), new ConfigGui(screen)) {
			@Override
			public void onClose() { // close -> onClose
				// this.client -> this.minecraft
				if (this.minecraft != null) {
					this.minecraft.setScreen(screen);
				}
			}
		});
	}
}
