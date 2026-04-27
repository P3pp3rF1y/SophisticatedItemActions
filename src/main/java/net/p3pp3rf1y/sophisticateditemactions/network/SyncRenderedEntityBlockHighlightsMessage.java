package net.p3pp3rf1y.sophisticateditemactions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticateditemactions.client.ClientCompatRenderHelper;
import net.p3pp3rf1y.sophisticateditemactions.common.EntityBlockHighlightData;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record SyncRenderedEntityBlockHighlightsMessage(Map<Integer, List<EntityBlockHighlightData>> highlightPositions,
													   int durationTicks) {
	public static void encode(SyncRenderedEntityBlockHighlightsMessage message, FriendlyByteBuf buffer) {
		buffer.writeMap(message.highlightPositions, FriendlyByteBuf::writeInt, (buf, list) -> buf.writeCollection(list, (listBuf, data) -> data.encode(listBuf)));
		buffer.writeInt(message.durationTicks);
	}

	public static SyncRenderedEntityBlockHighlightsMessage decode(FriendlyByteBuf buffer) {
		return new SyncRenderedEntityBlockHighlightsMessage(
				buffer.readMap(FriendlyByteBuf::readInt, buf -> buf.readList(EntityBlockHighlightData::decode)),
				buffer.readInt());
	}

	static void onMessage(SyncRenderedEntityBlockHighlightsMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(message));
		context.setPacketHandled(true);
	}

	public static void handleMessage(SyncRenderedEntityBlockHighlightsMessage message) {
		ClientCompatRenderHelper.addEntityBlockHighlights(message.highlightPositions(), message.durationTicks());
	}
}
