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

public record RestockAlternativeItemsPayload(List<ItemStack> filters, int minSlot, int maxSlot, boolean fillEmpty, boolean refillSingle,
		Map<ResourceLocation, List<BlockPos>> storagePositions, Map<ResourceLocation, List<Integer>> entityIds) implements CustomPacketPayload {
	private static final int MAX_FILTERS = 64;
	private static final int MAX_TARGET_GROUPS = 16;
	private static final int MAX_TARGETS_PER_GROUP = 512;
	public static final Type<RestockAlternativeItemsPayload> TYPE = new Type<>(SophisticatedCore.getRL("restock_alternative_items"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RestockAlternativeItemsPayload> STREAM_CODEC = StreamCodecHelper.composite(
			ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_FILTERS)), RestockAlternativeItemsPayload::filters, ByteBufCodecs.INT,
			RestockAlternativeItemsPayload::minSlot, ByteBufCodecs.INT, RestockAlternativeItemsPayload::maxSlot, ByteBufCodecs.BOOL,
			RestockAlternativeItemsPayload::fillEmpty, ByteBufCodecs.BOOL, RestockAlternativeItemsPayload::refillSingle,
			ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_TARGETS_PER_GROUP)),
					MAX_TARGET_GROUPS),
			RestockAlternativeItemsPayload::storagePositions, ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC,
					ByteBufCodecs.INT.apply(ByteBufCodecs.list(MAX_TARGETS_PER_GROUP)), MAX_TARGET_GROUPS),
			RestockAlternativeItemsPayload::entityIds, RestockAlternativeItemsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(RestockAlternativeItemsPayload payload, IPayloadContext context) {
		ItemTransferHandler.handleAlternativeRestock(context.player(), payload.storagePositions(), payload.entityIds(), payload.filters(), payload.minSlot(),
				payload.maxSlot(), payload.fillEmpty(), payload.refillSingle());
	}
}
