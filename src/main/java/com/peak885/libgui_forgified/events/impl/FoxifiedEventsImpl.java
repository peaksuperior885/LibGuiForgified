package com.peak885.libgui_forgified.events.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import com.peak885.libgui_forgified.events.api.ClientTickEvents;
import com.peak885.libgui_forgified.events.api.CoreShaderRegistrationCallback;

import java.io.IOException;
import java.io.UncheckedIOException;

public class FoxifiedEventsImpl {
	public static void registerClientEvents(IEventBus modEventBus) {
		// 1. Shader Registration (Remains on modEventBus)
		modEventBus.<RegisterShadersEvent>addListener(EventPriority.HIGHEST, event -> {
			try {
				CoreShaderRegistrationCallback.RegistrationContext context = (id, vertexFormat, loadCallback) -> {
					ShaderInstance program = new ShaderInstance(event.getResourceProvider(), id, vertexFormat);
					event.registerShader(program, loadCallback);
				};
				CoreShaderRegistrationCallback.EVENT.invoker().registerShaders(context);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});

		// 2. HUD Rendering is now handled by your Mixin in GuiMixin.java
		// We remove the RenderGuiEvent listener entirely.

		// 3. Tick Events
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.<TickEvent.ClientTickEvent>addListener(EventPriority.HIGHEST, event -> {
			if (event.phase == TickEvent.Phase.START) {
				ClientTickEvents.START_CLIENT_TICK.invoker().onStartTick(Minecraft.getInstance());
			} else if (event.phase == TickEvent.Phase.END) {
				ClientTickEvents.END_CLIENT_TICK.invoker().onEndTick(Minecraft.getInstance());
			}
		});
	}
}
