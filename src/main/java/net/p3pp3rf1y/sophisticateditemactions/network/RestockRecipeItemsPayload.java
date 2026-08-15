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
import net.p3pp3rf1y.sophisticateditemactions.common.ItemTransferHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record RestockRecipeItemsPayload(List<List<ItemStack>> ingredientOptions, Map<ResourceLocation, List<BlockPos>> storagePositions,
		Map<ResourceLocation, List<Integer>> entityIds) implements CustomPacketPayload {
	public static final Type<RestockRecipeItemsPayload> TYPE = new Type<>(SophisticatedCore.getRL("restock_recipe_items"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RestockRecipeItemsPayload> STREAM_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list(256)).apply(ByteBufCodecs.list(64)), RestockRecipeItemsPayload::ingredientOptions,
			ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(512)), 16),
			RestockRecipeItemsPayload::storagePositions,
			ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.INT.apply(ByteBufCodecs.list(512)), 16),
			RestockRecipeItemsPayload::entityIds, RestockRecipeItemsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(RestockRecipeItemsPayload payload, IPayloadContext context) {
		ItemTransferHandler.handleRecipeRestock(context.player(), payload.storagePositions(), payload.entityIds(), payload.ingredientOptions());
	}
}
