package io.github.cottonmc.cotton.gui.impl.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat; // VertexFormats -> DefaultVertexFormat
import net.minecraft.client.renderer.ShaderInstance; // ShaderProgram -> ShaderInstance
import net.minecraft.resources.ResourceLocation; // Identifier -> ResourceLocation
import net.minecraftforge.client.event.RegisterShadersEvent; // Native Forge Event
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import io.github.cottonmc.cotton.gui.impl.LibGuiCommon;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = LibGuiCommon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class LibGuiShaders {
	private static @Nullable ShaderInstance tiledRectangle;

	/**
	 * Listens to Forge's shader registration event on the Mod Bus.
	 */
	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) {
		try {
			event.registerShader(
				new ShaderInstance(
					event.getResourceProvider(),
					ResourceLocation.fromNamespaceAndPath(LibGuiCommon.MOD_ID, "tiled_rectangle"),
					DefaultVertexFormat.POSITION
				),
				program -> tiledRectangle = program
			);
		} catch (IOException e) {
			throw new RuntimeException("Failed to load LibGui core shaders", e);
		}
	}

	private static ShaderInstance assertPresent(@Nullable ShaderInstance program, String name) {
		if (program == null) {
			throw new NullPointerException("Shader libgui:" + name + " not initialised!");
		}

		return program;
	}

	public static ShaderInstance getTiledRectangle() {
		return assertPresent(tiledRectangle, "tiled_rectangle");
	}
}
