package net.p3pp3rf1y.sophisticateditemactions.common;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public interface IItemActionPayloadHandler<T> {
	ResourceLocation id();

	StreamCodec<ByteBuf, T> codec();

	record HighlightResult(int stackCounts, int itemCounts) {
	}
}
