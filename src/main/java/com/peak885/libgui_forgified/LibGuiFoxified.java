package com.peak885.libgui_forgified;

import io.github.cottonmc.cotton.gui.impl.LibGuiCommon;
import io.github.cottonmc.cotton.gui.impl.client.LibGuiClient;
import io.github.cottonmc.cotton.gui.impl.modmenu.ModMenuSupport;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import com.peak885.libgui_forgified.events.impl.FoxifiedEventsImpl;
import com.peak885.libgui_forgified.network.ModNetwork;

@Mod(LibGuiCommon.MOD_ID)
public class LibGuiFoxified {

	public LibGuiFoxified() {
		var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

		modEventBus.addListener(this::commonSetup);

		if (FMLLoader.getDist().isClient()) {
			FoxifiedEventsImpl.registerClientEvents(modEventBus);
			LibGuiClient.onInitializeClient();

			ModLoadingContext.get().registerExtensionPoint(
				ConfigScreenHandler.ConfigScreenFactory.class,
				() -> new ModMenuSupport().getModConfigScreenFactory()
			);
		}
	}

	private void commonSetup(final FMLCommonSetupEvent event) {
		event.enqueueWork(ModNetwork::register);
	}
}
