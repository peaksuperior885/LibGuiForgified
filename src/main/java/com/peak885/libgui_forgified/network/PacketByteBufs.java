package com.peak885.libgui_forgified.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

public class PacketByteBufs {
	/**
	 * Returns a new heap memory-backed instance of a friendly byte buf.
	 *
	 * @return a new buf
	 */
	public static FriendlyByteBuf create() {
		return new FriendlyByteBuf(Unpooled.buffer());
	}
}
