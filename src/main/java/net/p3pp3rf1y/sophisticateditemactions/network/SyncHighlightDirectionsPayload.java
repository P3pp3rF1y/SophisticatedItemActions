package net.p3pp3rf1y.sophisticateditemactions.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;
import net.p3pp3rf1y.sophisticateditemactions.client.ClientEventHandler;
import net.p3pp3rf1y.sophisticateditemactions.client.gui.HighlightDirectionOverlay;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SyncHighlightDirectionsPayload(Map<Integer, List<List<BlockPos>>> blockTargets, Map<Integer, List<Integer>> entityTargets, boolean hasMatches,
		int durationTicks) implements CustomPacketPayload {
	public static final Type<SyncHighlightDirectionsPayload> TYPE = new Type<>(SophisticatedItemActions.getRL("sync_highlight_directions"));
	public static final StreamCodec<ByteBuf, SyncHighlightDirectionsPayload> STREAM_CODEC = StreamCodec.composite(
			StreamCodecHelper.ofMap(ByteBufCodecs.INT, BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list()), HashMap::new),
			SyncHighlightDirectionsPayload::blockTargets,
			StreamCodecHelper.ofMap(ByteBufCodecs.INT, ByteBufCodecs.INT.apply(ByteBufCodecs.list()), HashMap::new),
			SyncHighlightDirectionsPayload::entityTargets, ByteBufCodecs.BOOL, SyncHighlightDirectionsPayload::hasMatches, ByteBufCodecs.INT,
			SyncHighlightDirectionsPayload::durationTicks, SyncHighlightDirectionsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SyncHighlightDirectionsPayload payload, IPayloadContext context) {
		ClientEventHandler.handleHighlightResult(payload.hasMatches());
		HighlightDirectionOverlay.addHighlightTargets(payload.blockTargets(), payload.entityTargets(), payload.durationTicks());
	}
}
