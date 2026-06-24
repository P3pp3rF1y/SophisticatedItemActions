package net.p3pp3rf1y.sophisticateditemactions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemTransferHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record DepositItemsPayload(int minSlot, int maxSlot, Map<ResourceLocation, List<BlockPos>> storagePositions,
		Map<ResourceLocation, List<Integer>> entityIds, boolean onlyMatching) implements CustomPacketPayload {
	public static final Type<DepositItemsPayload> TYPE = new Type<>(SophisticatedCore.getRL("deposit_items"));
	public static final StreamCodec<RegistryFriendlyByteBuf, DepositItemsPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT,
			DepositItemsPayload::minSlot, ByteBufCodecs.INT, DepositItemsPayload::maxSlot,
			StreamCodecHelper.ofMap(ResourceLocation.STREAM_CODEC, BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), HashMap::new),
			DepositItemsPayload::storagePositions,
			StreamCodecHelper.ofMap(ResourceLocation.STREAM_CODEC, ByteBufCodecs.INT.apply(ByteBufCodecs.list()), HashMap::new), DepositItemsPayload::entityIds,
			ByteBufCodecs.BOOL, DepositItemsPayload::onlyMatching, DepositItemsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(DepositItemsPayload payload, IPayloadContext context) {
		ItemTransferHandler.handleDeposit(context.player(), payload.minSlot(), payload.maxSlot(), payload.storagePositions(), payload.entityIds(),
				payload.onlyMatching());
	}
}
