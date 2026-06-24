package net.p3pp3rf1y.sophisticateditemactions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticateditemactions.common.HighlightHandler;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record RequestItemHighlightsMessage(ItemStack stack, Map<ResourceLocation, List<BlockPos>> inventoryPositions,
		Map<ResourceLocation, List<Integer>> entities) {

	public static void encode(RequestItemHighlightsMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeItemStack(msg.stack(), false);
		packetBuffer.writeMap(msg.inventoryPositions(), FriendlyByteBuf::writeResourceLocation,
				(buf, list) -> buf.writeCollection(list, FriendlyByteBuf::writeBlockPos));
		packetBuffer.writeMap(msg.entities(), FriendlyByteBuf::writeResourceLocation, (buf, list) -> buf.writeCollection(list, FriendlyByteBuf::writeInt));
	}

	public static RequestItemHighlightsMessage decode(FriendlyByteBuf packetBuffer) {
		return new RequestItemHighlightsMessage(packetBuffer.readItem(),
				packetBuffer.readMap(FriendlyByteBuf::readResourceLocation, buf -> buf.readList(FriendlyByteBuf::readBlockPos)),
				packetBuffer.readMap(FriendlyByteBuf::readResourceLocation, buf -> buf.readList(FriendlyByteBuf::readInt)));
	}

	static void onMessage(RequestItemHighlightsMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(context.getSender(), msg));
		context.setPacketHandled(true);
	}

	public static void handleMessage(@Nullable ServerPlayer player, RequestItemHighlightsMessage msg) {
		if (player == null) {
			return;
		}

		ItemStackKey stackKey = ItemStackKey.of(msg.stack());

		Map<ResourceLocation, List<BlockPos>> inventoryPositions = msg.inventoryPositions();
		Map<ResourceLocation, List<Integer>> entities = msg.entities();

		HighlightHandler.handleHighlight(player, stackKey, inventoryPositions, entities);
	}
}
