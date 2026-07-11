package com.peak885.libgui_forgified.events.api;

import java.io.IOException;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance; // Updated from ShaderProgram
import net.minecraft.resources.ResourceLocation;       // Updated from Identifier
import org.jetbrains.annotations.ApiStatus;
import com.peak885.libgui_forgified.base.api.Event;
import com.peak885.libgui_forgified.base.api.EventFactory;

/**
 * Called when core shaders (ShaderInstances) are loaded to register custom modded shaders.
 */
@FunctionalInterface
public interface CoreShaderRegistrationCallback {
	Event<CoreShaderRegistrationCallback> EVENT = EventFactory.createArrayBacked(CoreShaderRegistrationCallback.class, callbacks -> context -> {
		for (CoreShaderRegistrationCallback callback : callbacks) {
			callback.registerShaders(context);
		}
	});

	void registerShaders(RegistrationContext context) throws IOException;

	@ApiStatus.NonExtendable
	interface RegistrationContext {
		/**
		 * Creates and registers a core shader program.
		 *
		 * @param id           the program ID
		 * @param vertexFormat the vertex format used by the shader
		 * @param loadCallback a callback that is called when the shader program has been successfully loaded
		 */
		void register(ResourceLocation id, VertexFormat vertexFormat, Consumer<ShaderInstance> loadCallback) throws IOException;
	}
}
