package com.peak885.libgui_forgified.network;

import io.github.cottonmc.cotton.gui.impl.LibGuiCommon;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.SimpleChannel;

public class ModNetwork {
	private static final int PROTOCOL_VERSION = 1;
	public static SimpleChannel INSTANCE;
	private static int packetId = 0;

	private static int id() {
		return packetId++;
	}

	public static void register() {
		ResourceLocation id = ResourceLocation.parse(
			LibGuiCommon.MOD_ID + ":screen_message"
		);

		System.out.println(id);

		SimpleChannel channel = ChannelBuilder
			.named(id)
			.networkProtocolVersion(PROTOCOL_VERSION)
			.clientAcceptedVersions((status, version) -> true)
			.serverAcceptedVersions((status, version) -> true)
			.simpleChannel();

		INSTANCE = channel;

		// Register message using Forge's 1.21.1 standard signature
		channel.messageBuilder(LibGuiPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
			.encoder(LibGuiPacket::write)
			.decoder(LibGuiPacket::new)
			.consumerNetworkThread((packet, context) -> {
				// context.getSender() returns the ServerPlayer on the network thread
				if (context.getSender() != null) {
					LibGuiPacket.handle(packet, context.getSender());
				}
				context.setPacketHandled(true);
			})
			.add();
	}
}
