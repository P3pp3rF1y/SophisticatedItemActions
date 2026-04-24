package net.p3pp3rf1y.sophisticateditemactions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticateditemactions.client.gui.HighlightDirectionOverlay;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record SyncHighlightDirectionsMessage(Map<Integer, List<List<BlockPos>>> blockTargets,
											 Map<Integer, List<Integer>> entityTargets,
											 int durationTicks) {
	public static void encode(SyncHighlightDirectionsMessage message, FriendlyByteBuf buffer) {
		buffer.writeMap(message.blockTargets, FriendlyByteBuf::writeInt, (buf, groups) -> buf.writeCollection(groups, (groupBuf, group) -> groupBuf.writeCollection(group, FriendlyByteBuf::writeBlockPos)));
		buffer.writeMap(message.entityTargets, FriendlyByteBuf::writeInt, (buf, list) -> buf.writeCollection(list, FriendlyByteBuf::writeInt));
		buffer.writeInt(message.durationTicks);
	}

	public static SyncHighlightDirectionsMessage decode(FriendlyByteBuf buffer) {
		return new SyncHighlightDirectionsMessage(
				buffer.readMap(FriendlyByteBuf::readInt, buf -> buf.readList(groupBuf -> groupBuf.readList(FriendlyByteBuf::readBlockPos))),
				buffer.readMap(FriendlyByteBuf::readInt, buf -> buf.readList(FriendlyByteBuf::readInt)),
				buffer.readInt());
	}

	static void onMessage(SyncHighlightDirectionsMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(message));
		context.setPacketHandled(true);
	}

	public static void handleMessage(SyncHighlightDirectionsMessage message) {
		HighlightDirectionOverlay.addHighlightTargets(message.blockTargets(), message.entityTargets(), message.durationTicks());
	}
}
