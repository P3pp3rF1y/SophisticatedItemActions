package net.p3pp3rf1y.sophisticateditemactions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemTransferHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record RestockAlternativeItemsPayload(List<ItemStack> filters, int minSlot, int maxSlot, boolean fillEmpty, boolean refillSingle,
		Map<Identifier, List<BlockPos>> storagePositions, Map<Identifier, List<Integer>> entityIds) implements CustomPacketPayload {
	public static final Type<RestockAlternativeItemsPayload> TYPE = new Type<>(SophisticatedCore.getIdentifier("restock_alternative_items"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RestockAlternativeItemsPayload> STREAM_CODEC = StreamCodecHelper.composite(
			ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), RestockAlternativeItemsPayload::filters, ByteBufCodecs.INT,
			RestockAlternativeItemsPayload::minSlot, ByteBufCodecs.INT, RestockAlternativeItemsPayload::maxSlot, ByteBufCodecs.BOOL,
			RestockAlternativeItemsPayload::fillEmpty, ByteBufCodecs.BOOL, RestockAlternativeItemsPayload::refillSingle,
			StreamCodecHelper.ofMap(Identifier.STREAM_CODEC, BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), HashMap::new),
			RestockAlternativeItemsPayload::storagePositions,
			StreamCodecHelper.ofMap(Identifier.STREAM_CODEC, ByteBufCodecs.INT.apply(ByteBufCodecs.list()), HashMap::new),
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
