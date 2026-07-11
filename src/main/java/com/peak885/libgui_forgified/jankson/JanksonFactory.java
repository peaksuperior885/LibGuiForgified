package com.peak885.libgui_forgified.jankson;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonNull;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class JanksonFactory {

	@SuppressWarnings("removal")
	public static Jankson.Builder builder() {
		Jankson.Builder builder = Jankson.builder();

		// ResourceLocation setup
		builder.registerDeserializer(String.class, ResourceLocation.class,
			(s, m) -> ResourceLocation.parse(s));

		builder.registerSerializer(ResourceLocation.class,
			(i, m) -> new JsonPrimitive(i.toString()));

		// BlockState setup
		builder.registerDeserializer(String.class, BlockState.class,
			BlockAndItemSerializers::getBlockStatePrimitive);

		builder.registerDeserializer(JsonObject.class, BlockState.class, (json, marshaller) -> {
			// If your original method just took a JsonElement,
			// you can safely cast it to JsonObject here if the input is guaranteed to be one
			return BlockAndItemSerializers.getBlockState(json, marshaller);
		});

		builder.registerSerializer(BlockState.class,
			BlockAndItemSerializers::saveBlockState);

		// Map registrations using live instances from BuiltInRegistries
		register(builder, net.minecraft.world.item.Item.class, BuiltInRegistries.ITEM);
		register(builder, net.minecraft.world.level.block.Block.class, BuiltInRegistries.BLOCK);
		register(builder, net.minecraft.sounds.SoundEvent.class, BuiltInRegistries.SOUND_EVENT);
		register(builder, net.minecraft.world.effect.MobEffect.class, BuiltInRegistries.MOB_EFFECT);
		register(builder, net.minecraft.world.entity.EntityType.class, BuiltInRegistries.ENTITY_TYPE);

		return builder;
	}

	// Handles the widening cast cleanly for game registries (like EntityType<?>)
	private static <T> void register(
		Jankson.Builder builder,
		Class<T> clazz,
		Registry<? extends T> registry
	) {
		builder.registerDeserializer(String.class, clazz,
			(s, m) -> registry.getOptional(ResourceLocation.parse(s)).orElse(null));

		builder.registerSerializer(clazz,
			(o, m) -> {
				// Cast is necessary to bridge registry key fetchers matching wildcarded extensions
				@SuppressWarnings("unchecked")
				ResourceLocation id = ((Registry<T>) registry).getKey(o);
				return id == null ? JsonNull.INSTANCE : new JsonPrimitive(id.toString());
			});
	}

	public static Jankson createJankson() {
		return builder().build();
	}
}
