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
import net.p3pp3rf1y.sophisticateditemactions.common.ItemTransferHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record RestockRecipeItemsPayload(List<List<ItemStack>> ingredientOptions, Map<Identifier, List<BlockPos>> storagePositions,
		Map<Identifier, List<Integer>> entityIds) implements CustomPacketPayload {
	private static final int MAX_INGREDIENTS = 64;
	private static final int MAX_ALTERNATIVES_PER_INGREDIENT = 2048;
	private static final int MAX_TARGET_GROUPS = 16;
	private static final int MAX_TARGETS_PER_GROUP = 512;
	public static final Type<RestockRecipeItemsPayload> TYPE = new Type<>(SophisticatedCore.getIdentifier("restock_recipe_items"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RestockRecipeItemsPayload> STREAM_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ALTERNATIVES_PER_INGREDIENT)).apply(ByteBufCodecs.list(MAX_INGREDIENTS)),
			RestockRecipeItemsPayload::ingredientOptions,
			ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_TARGETS_PER_GROUP)), MAX_TARGET_GROUPS),
			RestockRecipeItemsPayload::storagePositions,
			ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.INT.apply(ByteBufCodecs.list(MAX_TARGETS_PER_GROUP)), MAX_TARGET_GROUPS),
			RestockRecipeItemsPayload::entityIds, RestockRecipeItemsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(RestockRecipeItemsPayload payload, IPayloadContext context) {
		ItemTransferHandler.handleRecipeRestock(context.player(), payload.storagePositions(), payload.entityIds(), payload.ingredientOptions());
	}
}
