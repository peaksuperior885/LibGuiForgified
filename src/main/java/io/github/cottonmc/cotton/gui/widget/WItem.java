package io.github.cottonmc.cotton.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics; // DrawContext -> GuiGraphics
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike; // ItemConvertible -> ItemLike
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries; // Registries -> BuiltInRegistries
import net.minecraft.core.Registry;
import net.minecraft.core.Holder; // RegistryEntry -> Holder
import net.minecraft.tags.TagKey;

import com.google.common.collect.ImmutableList;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A widget that displays an item or a list of items.
 *
 * @since 1.8.0
 */
public class WItem extends WWidget {
	private List<ItemStack> items;
	private int duration = 25;
	private int ticks = 0;
	private int current = 0;

	public WItem(List<ItemStack> items) {
		setItems(items);
	}

	public WItem(TagKey<? extends ItemLike> tag) { // ItemConvertible -> ItemLike
		this(getRenderStacks(tag));
	}

	public WItem(ItemStack stack) {
		this(Collections.singletonList(stack));
	}

	@Override
	public boolean canResize() {
		return true;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void tick() {
		if (ticks++ >= duration) {
			ticks = 0;
			current = (current + 1) % items.size();
		}
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void paint(GuiGraphics context, int x, int y, int mouseX, int mouseY) { // DrawContext -> GuiGraphics
		RenderSystem.enableDepthTest();
		// drawItemWithoutEntity -> renderItem
		context.renderItem(items.get(current), x + getWidth() / 2 - 8, y + getHeight() / 2 - 8);
	}

	/**
	 * Returns the animation duration of this {@code WItem}.
	 *
	 * <p>Defaults to 25 screen ticks.
	 */
	public int getDuration() {
		return duration;
	}

	public WItem setDuration(int duration) {
		this.duration = duration;
		return this;
	}

	public List<ItemStack> getItems() {
		return items;
	}

	/**
	 * Sets the item list of this {@code WItem} and resets the animation state.
	 *
	 * @param items the new item list
	 * @return this instance
	 */
	public WItem setItems(List<ItemStack> items) {
		Objects.requireNonNull(items, "stacks == null!");
		if (items.isEmpty()) throw new IllegalArgumentException("The stack list is empty!");

		this.items = items;

		// Reset the state
		current = 0;
		ticks = 0;

		return this;
	}

	/**
	 * Gets the default stacks ({@link Item#getDefaultInstance()}) of each item in a tag.
	 */
	@SuppressWarnings("unchecked")
	private static List<ItemStack> getRenderStacks(TagKey<? extends ItemLike> tag) { // ItemConvertible -> ItemLike
		// Registries.REGISTRIES -> BuiltInRegistries.REGISTRY
		Registry<ItemLike> registry = (Registry<ItemLike>) BuiltInRegistries.REGISTRY.get(tag.registry().location());
		if (registry == null) return Collections.emptyList();

		ImmutableList.Builder<ItemStack> builder = ImmutableList.builder();

		// RegistryEntry -> Holder
		for (Holder<ItemLike> item : registry.getOrCreateTag((TagKey<ItemLike>) tag)) {
			builder.add(item.value().asItem().getDefaultInstance()); // getDefaultStack -> getDefaultInstance
		}

		return builder.build();
	}
}
