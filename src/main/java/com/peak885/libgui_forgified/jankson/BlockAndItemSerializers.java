package com.peak885.libgui_forgified.jankson;

import java.util.Collection;
import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.api.Marshaller;

public class BlockAndItemSerializers {

	public static ItemStack getItemStack(JsonObject json, Marshaller m) {
		String itemIdString = json.get(String.class, "item");

		Item item = BuiltInRegistries.ITEM
			.getOptional(ResourceLocation.parse(itemIdString))
			.orElse(Items.AIR);

		ItemStack stack = new ItemStack(item);

		if (json.containsKey("count")) {
			Integer count = json.get(Integer.class, "count");
			if (count != null) {
				stack.setCount(count);
			}
		}

		return stack;
	}

	public static ItemStack getItemStackPrimitive(String s, Marshaller m) {
		Item item = BuiltInRegistries.ITEM
			.getOptional(ResourceLocation.parse(s))
			.orElse(Items.AIR);

		return new ItemStack(item);
	}

	public static JsonElement saveItemStack(ItemStack stack, Marshaller m) {
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		JsonPrimitive primitive = new JsonPrimitive(id.toString());

		if (stack.getCount() == 1) return primitive;

		JsonObject result = new JsonObject();
		result.put("item", new JsonPrimitive(id.toString()));
		result.put("count", new JsonPrimitive(stack.getCount()));

		return result;
	}

	@Deprecated
	public static Block getBlockPrimitive(String blockIdString, Marshaller m) {
		return BuiltInRegistries.BLOCK
			.getOptional(ResourceLocation.parse(blockIdString))
			.orElse(null);
	}

	@Deprecated
	public static JsonElement saveBlock(Block block, Marshaller m) {
		return new JsonPrimitive(BuiltInRegistries.BLOCK.getKey(block).toString());
	}

	public static BlockState getBlockStatePrimitive(String blockIdString, Marshaller m) {
		Optional<Block> blockOpt = BuiltInRegistries.BLOCK
			.getOptional(ResourceLocation.parse(blockIdString));

		return blockOpt.map(Block::defaultBlockState).orElse(null);
	}

	public static BlockState getBlockState(JsonObject json, Marshaller m) {
		String blockIdString = json.get(String.class, "block");

		Block block = BuiltInRegistries.BLOCK
			.getOptional(ResourceLocation.parse(blockIdString))
			.orElse(null);

		if (block == null) return null;

		BlockState state = block.defaultBlockState();

		JsonObject stateObject = json.getObject("BlockStateTag");
		if (stateObject == null) stateObject = json;

		Collection<Property<?>> properties = state.getProperties();

		for (String key : stateObject.keySet()) {

			if (stateObject == json &&
				(key.equals("BlockStateTag") || key.equals("block"))) {
				continue;
			}

			for (Property<?> property : properties) {
				if (property.getName().equals(key)) {

					String val = stateObject.get(String.class, key);
					state = withProperty(state, property, val);

					break;
				}
			}
		}

		return state;
	}

	public static JsonElement saveBlockState(BlockState state, Marshaller m) {
		BlockState defaultState = state.getBlock().defaultBlockState();

		if (state.equals(defaultState)) {
			return new JsonPrimitive(
				BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()
			);
		}

		JsonObject result = new JsonObject();
		result.put("block", new JsonPrimitive(
			BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()
		));

		JsonObject stateObject = result;

		for (Property<?> property : state.getProperties()) {
			String key = property.getName();

			if (key.equals("block") || key.equals("BlockStateTag")) {
				stateObject = new JsonObject();
				result.put("BlockStateTag", stateObject);
				break;
			}
		}

		for (Property<?> property : state.getProperties()) {
			if (state.getValue(property).equals(defaultState.getValue(property))) continue;

			String key = property.getName();
			String val = getProperty(state, property);

			stateObject.put(key, new JsonPrimitive(val));
		}

		return result;
	}

	public static <T extends Comparable<T>> BlockState withProperty(
		BlockState state,
		Property<T> property,
		String stringValue
	) {
		Optional<T> val = property.getValue(stringValue);
		if (val.isPresent()) {
			return state.setValue(property, val.get());
		}
		return state;
	}

	public static <T extends Comparable<T>> String getProperty(
		BlockState state,
		Property<T> property
	) {
		return property.getName(state.getValue(property));
	}
}
