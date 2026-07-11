package com.peak885.libgui_forgified.network;

import io.github.cottonmc.cotton.gui.impl.ScreenNetworkingImpl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

public record LibGuiPacket(int syncId, ResourceLocation message, FriendlyByteBuf rest) implements CustomPacketPayload {

	public static final Type<LibGuiPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("libgui", "packet"));

	public static final StreamCodec<FriendlyByteBuf, LibGuiPacket> STREAM_CODEC = StreamCodec.of(
		(StreamEncoder<FriendlyByteBuf, LibGuiPacket>) (buf, packet) -> packet.write(buf),
		(StreamDecoder<FriendlyByteBuf, LibGuiPacket>) LibGuiPacket::new
	);

	// Decoding constructor
	public LibGuiPacket(FriendlyByteBuf buffer) {
		this(buffer.readVarInt(), buffer.readResourceLocation(), buffer);
	}

	// Encoding method
	public void write(FriendlyByteBuf buffer) {
		buffer.writeVarInt(this.syncId);
		buffer.writeResourceLocation(this.message);
		buffer.writeBytes(this.rest);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/**
	 * Handles the payload context processing.
	 * Can be referenced directly in your network registrar lambda (e.g., context -> packet.handle(context))
	 */
	public static void handle(LibGuiPacket packet, ServerPlayer sender) {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null && sender != null) {
			server.execute(() ->
				ScreenNetworkingImpl.handle(server, sender, packet.rest)
			);
		}
	}
}
