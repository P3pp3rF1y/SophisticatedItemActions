package net.p3pp3rf1y.sophisticateditemactions.common;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

public record EntityBlockHighlightData(int entityId, List<BlockPos> positions) {
	public static final StreamCodec<ByteBuf, EntityBlockHighlightData> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT,
			EntityBlockHighlightData::entityId,
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()),
			EntityBlockHighlightData::positions,
			EntityBlockHighlightData::new
	);
}
