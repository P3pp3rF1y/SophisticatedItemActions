package net.p3pp3rf1y.sophisticateditemactions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.network.PacketBufferHelper;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemTransferHandler;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record RestockRecipeItemsMessage(List<List<ItemStack>> ingredientOptions, Map<ResourceLocation, List<BlockPos>> storagePositions,
		Map<ResourceLocation, List<Integer>> entityIds) {
	private static final int MAX_INGREDIENTS = 64;
	private static final int MAX_ALTERNATIVES_PER_INGREDIENT = 256;
	private static final int MAX_TARGET_GROUPS = 16;
	private static final int MAX_TARGETS_PER_GROUP = 512;
	public static void encode(RestockRecipeItemsMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeCollection(msg.ingredientOptions, (buf, options) -> buf.writeCollection(options, FriendlyByteBuf::writeItem));
		packetBuffer.writeMap(msg.storagePositions, FriendlyByteBuf::writeResourceLocation,
				(buf, list) -> buf.writeCollection(list, FriendlyByteBuf::writeBlockPos));
		packetBuffer.writeMap(msg.entityIds, FriendlyByteBuf::writeResourceLocation, (buf, list) -> buf.writeCollection(list, FriendlyByteBuf::writeInt));
	}

	public static RestockRecipeItemsMessage decode(FriendlyByteBuf packetBuffer) {
		return new RestockRecipeItemsMessage(
				PacketBufferHelper.readList(packetBuffer, buf -> PacketBufferHelper.readList(buf, FriendlyByteBuf::readItem, MAX_ALTERNATIVES_PER_INGREDIENT),
						MAX_INGREDIENTS),
				PacketBufferHelper.readMap(packetBuffer, FriendlyByteBuf::readResourceLocation,
						buf -> PacketBufferHelper.readList(buf, FriendlyByteBuf::readBlockPos, MAX_TARGETS_PER_GROUP), MAX_TARGET_GROUPS),
				PacketBufferHelper.readMap(packetBuffer, FriendlyByteBuf::readResourceLocation,
						buf -> PacketBufferHelper.readList(buf, FriendlyByteBuf::readInt, MAX_TARGETS_PER_GROUP), MAX_TARGET_GROUPS));
	}

	static void onMessage(RestockRecipeItemsMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg, context));
		context.setPacketHandled(true);
	}

	public static void handleMessage(RestockRecipeItemsMessage msg, NetworkEvent.Context context) {
		ServerPlayer sender = context.getSender();
		if (sender == null) {
			return;
		}
		ItemTransferHandler.handleRecipeRestock(sender, msg.storagePositions(), msg.entityIds(), msg.ingredientOptions());
	}
}
