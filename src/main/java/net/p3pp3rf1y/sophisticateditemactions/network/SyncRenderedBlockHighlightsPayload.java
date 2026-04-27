package net.p3pp3rf1y.sophisticateditemactions.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;
import net.p3pp3rf1y.sophisticateditemactions.client.render.RenderedBlockHighlightRenderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SyncRenderedBlockHighlightsPayload(Map<Integer, List<List<BlockPos>>> highlightPositions, int durationTicks) implements CustomPacketPayload {
	public static final Type<SyncRenderedBlockHighlightsPayload> TYPE = new Type<>(SophisticatedItemActions.getIdentifier("sync_rendered_block_highlights"));
	public static final StreamCodec<ByteBuf, SyncRenderedBlockHighlightsPayload> STREAM_CODEC = StreamCodec.composite(
			StreamCodecHelper.ofMap(ByteBufCodecs.INT, BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list()), HashMap::new),
			SyncRenderedBlockHighlightsPayload::highlightPositions,
			ByteBufCodecs.INT,
			SyncRenderedBlockHighlightsPayload::durationTicks,
			SyncRenderedBlockHighlightsPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SyncRenderedBlockHighlightsPayload payload, IPayloadContext context) {
		RenderedBlockHighlightRenderer.addHighlightedPositions(payload.highlightPositions(), payload.durationTicks());
	}
}
