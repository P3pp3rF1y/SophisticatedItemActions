package net.p3pp3rf1y.sophisticateditemactions.common;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public interface IItemActionPayloadHandler<T> {
	Identifier id();

	StreamCodec<ByteBuf, T> codec();

	record HighlightResult(int stackCounts, int itemCounts) {
	}
}
