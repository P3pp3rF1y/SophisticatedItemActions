
package net.p3pp3rf1y.sophisticateditemactions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticateditemactions.client.render.EntityHighlightRenderer;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record SyncEntityHighlightsMessage(Map<Integer, List<Integer>> entityHighlights, int durationTicks) {
	public static void encode(SyncEntityHighlightsMessage message, FriendlyByteBuf buffer) {
		buffer.writeMap(message.entityHighlights, FriendlyByteBuf::writeInt, (buf, list) -> buf.writeCollection(list, FriendlyByteBuf::writeInt));
		buffer.writeInt(message.durationTicks);
	}

	public static SyncEntityHighlightsMessage decode(FriendlyByteBuf buffer) {
		return new SyncEntityHighlightsMessage(buffer.readMap(FriendlyByteBuf::readInt, buf -> buf.readList(FriendlyByteBuf::readInt)), buffer.readInt());
	}

	public SyncEntityHighlightsMessage(Map<Integer, List<Integer>> entityHighlights) {
		this(entityHighlights, EntityHighlightRenderer.HIGHLIGHT_DURATION);
	}

	static void onMessage(SyncEntityHighlightsMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg));
		context.setPacketHandled(true);
	}

	public static void handleMessage(SyncEntityHighlightsMessage msg) {
		EntityHighlightRenderer.addHighlightedEntities(msg.entityHighlights(), msg.durationTicks());
	}
}
