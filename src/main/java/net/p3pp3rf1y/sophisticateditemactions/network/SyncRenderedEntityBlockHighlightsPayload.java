package net.p3pp3rf1y.sophisticateditemactions.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;
import net.p3pp3rf1y.sophisticateditemactions.client.render.RenderedEntityBlockHighlightRenderer;
import net.p3pp3rf1y.sophisticateditemactions.common.EntityBlockHighlightData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SyncRenderedEntityBlockHighlightsPayload(Map<Integer, List<EntityBlockHighlightData>> highlightPositions,
																   int durationTicks) implements CustomPacketPayload {
	public static final Type<SyncRenderedEntityBlockHighlightsPayload> TYPE = new Type<>(SophisticatedItemActions.getRL("sync_rendered_entity_block_highlights"));
	public static final StreamCodec<ByteBuf, SyncRenderedEntityBlockHighlightsPayload> STREAM_CODEC = StreamCodec.composite(
			StreamCodecHelper.ofMap(ByteBufCodecs.INT, EntityBlockHighlightData.STREAM_CODEC.apply(ByteBufCodecs.list()), HashMap::new),
			SyncRenderedEntityBlockHighlightsPayload::highlightPositions,
			ByteBufCodecs.INT,
			SyncRenderedEntityBlockHighlightsPayload::durationTicks,
			SyncRenderedEntityBlockHighlightsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SyncRenderedEntityBlockHighlightsPayload payload, IPayloadContext context) {
		RenderedEntityBlockHighlightRenderer.addHighlightedPositions(payload.highlightPositions(), payload.durationTicks());
	}
}
