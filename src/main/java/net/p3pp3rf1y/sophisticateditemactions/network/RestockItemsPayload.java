package net.p3pp3rf1y.sophisticateditemactions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemTransferHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record RestockItemsPayload(ItemStack filter, int minSlot, int maxSlot, boolean fillEmpty, boolean refillSingle,
		Map<ResourceLocation, List<BlockPos>> storagePositions, Map<ResourceLocation, List<Integer>> entityIds) implements CustomPacketPayload {
	public static final Type<RestockItemsPayload> TYPE = new Type<>(SophisticatedCore.getRL("restock_items"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RestockItemsPayload> STREAM_CODEC = StreamCodecHelper.composite(ItemStack.OPTIONAL_STREAM_CODEC,
			RestockItemsPayload::filter, ByteBufCodecs.INT, RestockItemsPayload::minSlot, ByteBufCodecs.INT, RestockItemsPayload::maxSlot, ByteBufCodecs.BOOL,
			RestockItemsPayload::fillEmpty, ByteBufCodecs.BOOL, RestockItemsPayload::refillSingle,
			StreamCodecHelper.ofMap(ResourceLocation.STREAM_CODEC, BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), HashMap::new),
			RestockItemsPayload::storagePositions,
			StreamCodecHelper.ofMap(ResourceLocation.STREAM_CODEC, ByteBufCodecs.INT.apply(ByteBufCodecs.list()), HashMap::new), RestockItemsPayload::entityIds,
			RestockItemsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(RestockItemsPayload payload, IPayloadContext context) {
		ItemTransferHandler.handleRestock(context.player(), payload.storagePositions(), payload.entityIds, payload.minSlot(), payload.maxSlot(),
				payload.filter(), payload.fillEmpty(), payload.refillSingle());
	}

}
