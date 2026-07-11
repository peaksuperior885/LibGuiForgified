package io.github.cottonmc.cotton.gui.impl;

import net.minecraft.world.entity.player.Player; // PlayerEntity -> Player
import net.minecraft.network.FriendlyByteBuf; // PacketByteBuf -> FriendlyByteBuf
import net.minecraft.world.inventory.AbstractContainerMenu; // ScreenHandler -> AbstractContainerMenu
import net.minecraft.resources.ResourceLocation; // Identifier -> ResourceLocation

import io.github.cottonmc.cotton.gui.SyncedGuiDescription;
import io.github.cottonmc.cotton.gui.networking.NetworkSide;
import io.github.cottonmc.cotton.gui.networking.ScreenNetworking;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.peak885.libgui_forgified.network.LibGuiPacket;
import com.peak885.libgui_forgified.network.PacketByteBufs; // Assuming this creates FriendlyByteBuf in your port

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class ScreenNetworkingImpl implements ScreenNetworking {
	// Packet structure:
	//   syncId: int
	//   message: ResourceLocation
	//   rest: buf

	public static final ResourceLocation SCREEN_MESSAGE_S2C = ResourceLocation.fromNamespaceAndPath(LibGuiCommon.MOD_ID, "screen_message_s2c"); // new Identifier -> ResourceLocation.fromNamespaceAndPath
	public static final ResourceLocation SCREEN_MESSAGE_C2S = ResourceLocation.fromNamespaceAndPath(LibGuiCommon.MOD_ID, "screen_message_c2s");

	private static final Logger LOGGER = LogManager.getLogger();
	private static final Map<SyncedGuiDescription, ScreenNetworkingImpl> instanceCache = new WeakHashMap<>();

	private final Map<ResourceLocation, MessageReceiver> messages = new HashMap<>();
	private SyncedGuiDescription description;
	private final NetworkSide side;

	private ScreenNetworkingImpl(SyncedGuiDescription description, NetworkSide side) {
		this.description = description;
		this.side = side;
	}

	public void receive(ResourceLocation message, MessageReceiver receiver) {
		Objects.requireNonNull(message, "message");
		Objects.requireNonNull(receiver, "receiver");

		if (!messages.containsKey(message)) {
			messages.put(message, receiver);
		} else {
			throw new IllegalStateException("Message " + message + " on side " + side + " already registered");
		}
	}

	@Override
	public void send(ResourceLocation message, Consumer<FriendlyByteBuf> writer) { // PacketByteBuf -> FriendlyByteBuf
		Objects.requireNonNull(message, "message");
		Objects.requireNonNull(writer, "writer");

		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(description.containerId); // syncId -> containerId
		buf.writeResourceLocation(message); // writeIdentifier -> writeResourceLocation
		writer.accept(buf);

		LibGuiPacket packet = new LibGuiPacket(description.containerId, message, buf);
		if (side == NetworkSide.SERVER){
			description.getPacketSender().sendToPlayer(packet);
		} else {
			description.getPacketSender().sendToServer(packet);
		}
	}

	public static void handle(Executor executor, Player player, FriendlyByteBuf buf) { // PlayerEntity -> Player, PacketByteBuf -> FriendlyByteBuf
		AbstractContainerMenu screenHandler = player.containerMenu; // currentScreenHandler -> containerMenu

		// Packet data
		int syncId = buf.readVarInt();
		ResourceLocation messageId = buf.readResourceLocation(); // readIdentifier -> readResourceLocation

		if (!(screenHandler instanceof SyncedGuiDescription)) {
			LOGGER.error("Received message packet for screen handler {} which is not a SyncedGuiDescription", screenHandler);
			return;
		} else if (syncId != screenHandler.containerId) { // syncId -> containerId
			LOGGER.error("Received message for sync ID {}, current sync ID: {}", syncId, screenHandler.containerId);
			return;
		}

		ScreenNetworkingImpl networking = instanceCache.get(screenHandler);

		if (networking != null) {
			MessageReceiver receiver = networking.messages.get(messageId);

			if (receiver != null) {
				buf.retain();
				executor.execute(() -> {
					try {
						receiver.onMessage(buf);
					} catch (Exception e) {
						LOGGER.error("Error handling screen message {} for {} on side {}", messageId, screenHandler, networking.side, e);
					} finally {
						buf.release();
					}
				});
			} else {
				LOGGER.warn("Message {} not registered for {} on side {}", messageId, screenHandler, networking.side);
			}
		} else {
			LOGGER.warn("GUI description {} does not use networking", screenHandler);
		}
	}

	public static ScreenNetworking of(SyncedGuiDescription description, NetworkSide networkSide) {
		Objects.requireNonNull(description, "description");
		Objects.requireNonNull(networkSide, "networkSide");

		if (description.getNetworkSide() == networkSide) {
			return instanceCache.computeIfAbsent(description, it -> new ScreenNetworkingImpl(description, networkSide));
		} else {
			return DummyNetworking.INSTANCE;
		}
	}

	private static final class DummyNetworking extends ScreenNetworkingImpl {
		static final DummyNetworking INSTANCE = new DummyNetworking();

		private DummyNetworking() {
			super(null, null);
		}

		@Override
		public void receive(ResourceLocation message, MessageReceiver receiver) {
			// NO-OP
		}

		@Override
		public void send(ResourceLocation message, Consumer<FriendlyByteBuf> writer) {
			// NO-OP
		}
	}
}
