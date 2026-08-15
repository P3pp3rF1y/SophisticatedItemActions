package net.p3pp3rf1y.sophisticateditemactions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticateditemactions.common.HighlightHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record RequestItemHighlightsPayload(ItemStack stack, Map<Identifier, List<BlockPos>> inventoryPositions,
		Map<Identifier, List<Integer>> entities) implements CustomPacketPayload {
	public static final Type<RequestItemHighlightsPayload> TYPE = new Type<>(SophisticatedCore.getIdentifier("request_item_highlights"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RequestItemHighlightsPayload> STREAM_CODEC = StreamCodec.composite(ItemStack.STREAM_CODEC,
			RequestItemHighlightsPayload::stack,
			ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(512)), 16),
			RequestItemHighlightsPayload::inventoryPositions,
			ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.INT.apply(ByteBufCodecs.list(512)), 16),
			RequestItemHighlightsPayload::entities, RequestItemHighlightsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(RequestItemHighlightsPayload payload, IPayloadContext context) {
		ItemStackKey stackKey = ItemStackKey.of(payload.stack());
		Player player = context.player();

		Map<Identifier, List<BlockPos>> inventoryPositions = payload.inventoryPositions();
		Map<Identifier, List<Integer>> entities = payload.entities();

		HighlightHandler.handleHighlight(player, stackKey, inventoryPositions, entities);
	}
}
