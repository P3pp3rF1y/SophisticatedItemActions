package net.p3pp3rf1y.sophisticateditemactions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemTransferHandler;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record RestockRecipeItemsMessage(List<List<ItemStack>> ingredientOptions, Map<ResourceLocation, List<BlockPos>> storagePositions,
		Map<ResourceLocation, List<Integer>> entityIds) {
	public static void encode(RestockRecipeItemsMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeCollection(msg.ingredientOptions, (buf, options) -> buf.writeCollection(options, FriendlyByteBuf::writeItem));
		packetBuffer.writeMap(msg.storagePositions, FriendlyByteBuf::writeResourceLocation,
				(buf, list) -> buf.writeCollection(list, FriendlyByteBuf::writeBlockPos));
		packetBuffer.writeMap(msg.entityIds, FriendlyByteBuf::writeResourceLocation, (buf, list) -> buf.writeCollection(list, FriendlyByteBuf::writeInt));
	}

	public static RestockRecipeItemsMessage decode(FriendlyByteBuf packetBuffer) {
		return new RestockRecipeItemsMessage(packetBuffer.readList(buf -> buf.readList(FriendlyByteBuf::readItem)),
				packetBuffer.readMap(FriendlyByteBuf::readResourceLocation, buf -> buf.readList(FriendlyByteBuf::readBlockPos)),
				packetBuffer.readMap(FriendlyByteBuf::readResourceLocation, buf -> buf.readList(FriendlyByteBuf::readInt)));
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
