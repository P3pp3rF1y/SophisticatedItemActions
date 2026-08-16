
package net.p3pp3rf1y.sophisticateditemactions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.network.ISplittableMessage;
import net.p3pp3rf1y.sophisticateditemactions.client.render.ItemTransferClientHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemTransferData;

import java.util.List;
import java.util.function.Supplier;

public record SyncItemTransfersMessage(List<ItemTransferData> itemTransferData, Vec3 playerPos, boolean fromPlayer,
		boolean recipeRestock) implements ISplittableMessage {
	public static void encode(SyncItemTransfersMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeCollection(msg.itemTransferData, (friendlyByteBuf, itemTransferData) -> itemTransferData.encode(friendlyByteBuf));
		packetBuffer.writeDouble(msg.playerPos.x());
		packetBuffer.writeDouble(msg.playerPos.y());
		packetBuffer.writeDouble(msg.playerPos.z());
		packetBuffer.writeBoolean(msg.fromPlayer);
		packetBuffer.writeBoolean(msg.recipeRestock);
	}

	public static SyncItemTransfersMessage decode(FriendlyByteBuf packetBuffer) {
		return new SyncItemTransfersMessage(packetBuffer.readList(ItemTransferData::decode),
				new Vec3(packetBuffer.readDouble(), packetBuffer.readDouble(), packetBuffer.readDouble()), packetBuffer.readBoolean(),
				packetBuffer.readBoolean());
	}

	static void onMessage(SyncItemTransfersMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg));
		context.setPacketHandled(true);
	}

	public static void handleMessage(SyncItemTransfersMessage message) {
		ItemTransferClientHandler.handleItemTransfers(message.itemTransferData(), message.playerPos(), message.fromPlayer(), message.recipeRestock());
	}
}
