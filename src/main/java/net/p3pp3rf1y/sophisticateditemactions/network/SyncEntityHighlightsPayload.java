
package net.p3pp3rf1y.sophisticateditemactions.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;
import net.p3pp3rf1y.sophisticateditemactions.client.render.EntityHighlightRenderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SyncEntityHighlightsPayload(Map<Integer, List<Integer>> entityHighlights, int durationTicks) implements CustomPacketPayload {
	public static final Type<SyncEntityHighlightsPayload> TYPE = new Type<>(SophisticatedItemActions.getRL("sync_entity_highlights"));
	public static final StreamCodec<ByteBuf, SyncEntityHighlightsPayload> STREAM_CODEC = StreamCodec.composite(
			StreamCodecHelper.ofMap(ByteBufCodecs.INT, ByteBufCodecs.INT.apply(ByteBufCodecs.list()), HashMap::new),
			SyncEntityHighlightsPayload::entityHighlights, ByteBufCodecs.INT, SyncEntityHighlightsPayload::durationTicks, SyncEntityHighlightsPayload::new);
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SyncEntityHighlightsPayload payload, IPayloadContext context) {
		EntityHighlightRenderer.addHighlightedEntities(payload.entityHighlights(), payload.durationTicks());
	}
}
