package net.p3pp3rf1y.sophisticateditemactions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.network.PacketBufferHelper;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemTransferHandler;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record DepositItemsMessage(int minSlot, int maxSlot, Map<ResourceLocation, List<BlockPos>> storagePositions,
		Map<ResourceLocation, List<Integer>> entityIds, boolean onlyMatching) {
	private static final int MAX_TARGET_GROUPS = 16;
	private static final int MAX_TARGETS_PER_GROUP = 512;

	public static void encode(DepositItemsMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeInt(msg.minSlot);
		packetBuffer.writeInt(msg.maxSlot);
		packetBuffer.writeMap(msg.storagePositions, FriendlyByteBuf::writeResourceLocation,
				(buf, list) -> buf.writeCollection(list, FriendlyByteBuf::writeBlockPos));
		packetBuffer.writeMap(msg.entityIds, FriendlyByteBuf::writeResourceLocation, (buf, list) -> buf.writeCollection(list, FriendlyByteBuf::writeInt));
		packetBuffer.writeBoolean(msg.onlyMatching);
	}

	public static DepositItemsMessage decode(FriendlyByteBuf packetBuffer) {
		return new DepositItemsMessage(packetBuffer.readInt(), packetBuffer.readInt(),
				PacketBufferHelper.readMap(packetBuffer, FriendlyByteBuf::readResourceLocation,
						buf -> PacketBufferHelper.readList(buf, FriendlyByteBuf::readBlockPos, MAX_TARGETS_PER_GROUP), MAX_TARGET_GROUPS),
				PacketBufferHelper.readMap(packetBuffer, FriendlyByteBuf::readResourceLocation,
						buf -> PacketBufferHelper.readList(buf, FriendlyByteBuf::readInt, MAX_TARGETS_PER_GROUP), MAX_TARGET_GROUPS),
				packetBuffer.readBoolean());
	}

	static void onMessage(DepositItemsMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg, context));
		context.setPacketHandled(true);
	}

	public static void handleMessage(DepositItemsMessage msg, NetworkEvent.Context context) {
		ServerPlayer sender = context.getSender();
		if (sender == null) {
			return;
		}
		ItemTransferHandler.handleDeposit(sender, msg.minSlot(), msg.maxSlot(), msg.storagePositions(), msg.entityIds(), msg.onlyMatching());
	}
}
