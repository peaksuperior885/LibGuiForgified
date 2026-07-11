package io.github.cottonmc.cotton.gui.impl.client;

import net.minecraft.client.gui.screens.Screen; // Package change
import net.minecraft.world.entity.LivingEntity; // Package change
import net.minecraft.world.entity.player.Player; // PlayerEntity -> Player
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext; // ItemUsageContext -> UseOnContext
import net.minecraft.world.InteractionResult; // ActionResult -> InteractionResult
import net.minecraft.world.InteractionHand; // Hand -> InteractionHand
import net.minecraft.Util;
import net.minecraft.ReportedException; // CrashException -> ReportedException
import net.minecraft.CrashReport;
import net.minecraft.world.level.Level; // World -> Level

import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Crashes the game if a LibGui screen is opened in {@code Item.use/useOnBlock/useOnEntity}.
 */
public final class ItemUseChecker {
	// Setting this property to "true" disables the check.
	private static final String ALLOW_ITEM_USE_PROPERTY = "libgui.allowItemUse";

	// Stack walker instance used to check the caller.
	private static final StackWalker STACK_WALKER =
		StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

	// Simple container replacement for net.minecraft.util.Pair to clean up dependencies
	private record MethodIdentity(String name, MethodType type) {}
	private record CallerIdentity(Class<?> declaringClass, String methodName) {}

	// List of banned item use methods.
	private static final List<MethodIdentity> ITEM_USE_METHODS = Util.make(new ArrayList<>(), result -> {
		Class<InteractionHand> hand = InteractionHand.class;
		Class<InteractionResult> actionResult = InteractionResult.class;
		Class<LivingEntity> livingEntity = LivingEntity.class;
		Class<Player> player = Player.class;
		Class<ItemStack> itemStack = ItemStack.class;
		Class<UseOnContext> useOnContext = UseOnContext.class;
		Class<Level> level = Level.class;

		// use -> maps to 'use' in Mojang mappings
		result.add(resolveItemMethod("use", actionResult, level, player, hand));
		// useOnBlock -> maps to 'useOn' in Mojang mappings
		result.add(resolveItemMethod("useOn", actionResult, useOnContext));
		// useOnEntity -> maps to 'interactLivingEntity' in Mojang mappings
		result.add(resolveItemMethod("interactLivingEntity", actionResult, itemStack, player, livingEntity, hand));
	});

	private static MethodIdentity resolveItemMethod(String deobfName, Class<?> returnType, Class<?>... parameterTypes) {
		// Directly find the runtime method name using standard reflection on the mapped class
		try {
			Item.class.getMethod(deobfName, parameterTypes);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Could not find standard mapped Item method: " + deobfName, e);
		}

		return new MethodIdentity(deobfName, MethodType.methodType(returnType, parameterTypes));
	}

	/**
	 * Checks whether the specified screen is a LibGui screen opened
	 * from an item usage method.
	 *
	 * @throws ReportedException if opening the screen is not allowed
	 */
	public static void checkSetScreen(Screen screen) {
		if (!(screen instanceof CottonScreenImpl cs) || Boolean.getBoolean(ALLOW_ITEM_USE_PROPERTY)) return;

		// Check if this is called via Item.use. If so, crash the game.
		@Nullable CallerIdentity useMethodCaller = STACK_WALKER.walk(s -> s
				.skip(3) // checkSetScreen, setScreen injection, setScreen
				.flatMap(frame -> {
					if (!Item.class.isAssignableFrom(frame.getDeclaringClass())) return Stream.empty();

					return ITEM_USE_METHODS.stream()
						.filter(method -> method.name().equals(frame.getMethodName()) &&
							method.type().equals(frame.getMethodType()))
						.map(method -> new CallerIdentity(frame.getDeclaringClass(), method.name()));
				})
				.findFirst())
			.orElse(null);

		if (useMethodCaller != null) {
			String message = """
                   [LibGui] Screens cannot be opened in item use methods. Some alternatives include:
                      - Using a packet together with LightweightGuiDescription
                      - Using an ItemSyncedGuiDescription
                   Setting the screen in item use methods leads to threading issues and
                   other potential crashes on both the client and the server.
                   If you want to disable this check, set the system property %s to "true"."""
				.formatted(ALLOW_ITEM_USE_PROPERTY);
			var cause = new UnsupportedOperationException(message);
			cause.fillInStackTrace();
			CrashReport report = CrashReport.forThrowable(cause, "Opening screen");
			report.addCategory("Screen opening details") // addElement -> addCategory
				.setDetail("Screen class", screen.getClass().getName()) // add -> setDetail
				.setDetail("GUI description", () -> cs.getDescription().getClass().getName())
				.setDetail("Item class", () -> useMethodCaller.declaringClass().getName())
				.setDetail("Involved method", useMethodCaller.methodName());
			throw new ReportedException(report); // CrashException -> ReportedException
		}
	}
}
