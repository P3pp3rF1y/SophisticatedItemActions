package net.p3pp3rf1y.sophisticateditemactions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemTransferHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record RestockRegisteredRecipePayload(Identifier recipeId, boolean fullStacks, Map<Identifier, List<BlockPos>> storagePositions,
		Map<Identifier, List<Integer>> entityIds) implements CustomPacketPayload {
	private static final int MAX_TARGET_GROUPS = 16;
	private static final int MAX_TARGETS_PER_GROUP = 512;
	public static final Type<RestockRegisteredRecipePayload> TYPE = new Type<>(SophisticatedCore.getIdentifier("restock_registered_recipe"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RestockRegisteredRecipePayload> STREAM_CODEC = StreamCodec.composite(Identifier.STREAM_CODEC,
			RestockRegisteredRecipePayload::recipeId, ByteBufCodecs.BOOL, RestockRegisteredRecipePayload::fullStacks,
			ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_TARGETS_PER_GROUP)),
					MAX_TARGET_GROUPS),
			RestockRegisteredRecipePayload::storagePositions, ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC,
					ByteBufCodecs.INT.apply(ByteBufCodecs.list(MAX_TARGETS_PER_GROUP)), MAX_TARGET_GROUPS),
			RestockRegisteredRecipePayload::entityIds, RestockRegisteredRecipePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(RestockRegisteredRecipePayload payload, IPayloadContext context) {
		ItemTransferHandler.handleRecipeRestock(context.player(), payload.storagePositions(), payload.entityIds(), payload.recipeId(), payload.fullStacks());
	}
}
