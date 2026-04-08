package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.network.SyncBlockHighlightsPayload;
import net.p3pp3rf1y.sophisticatedcore.util.BlockHighlightGroups;
import net.p3pp3rf1y.sophisticatedcore.util.RandHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticateditemactions.client.gui.ItemActionsTranslationHelper;
import net.p3pp3rf1y.sophisticateditemactions.network.RequestItemHighlightsPayload;
import net.p3pp3rf1y.sophisticateditemactions.network.SyncEntityHighlightsPayload;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HighlightHandler {
	public static final int MATCHING_STACK_HIGHLIGHT_COLOR = 0x4CAF50;
	public static final int MATCHING_ITEM_HIGHLIGHT_COLOR = 0x42A5F5;
	private static final int HIGHLIGHT_RANGE = 32;

	public static RequestItemHighlightsPayload createHighlightRequestPayload(Player player, ItemStack stack) {
		Map<Identifier, List<BlockPos>> positions = new HashMap<>();
		Map<Identifier, Map<BlockPos, BlockPos>> canonicalPositions = new HashMap<>();

		WorldHelper.getBlockEntitiesInRange(player.level(), player.blockPosition(), HIGHLIGHT_RANGE)
				.forEach(be ->
						ItemActionHandlerRegistry.getBlockHandlerFor(player.level(), be.getBlockPos(), be, IBlockItemActionHandler.Action.HIGHLIGHT)
								.ifPresent(handler -> {
									BlockPos canonicalPos = handler.getInteractionPosToActOn(player.level(), be.getBlockPos(), be, IBlockItemActionHandler.Action.HIGHLIGHT);
									canonicalPositions.computeIfAbsent(handler.id(), k -> new HashMap<>()).putIfAbsent(canonicalPos, canonicalPos);
								})
				);
		canonicalPositions.forEach((handlerId, posMap) -> positions.put(handlerId, new ArrayList<>(posMap.values())));

		Map<Identifier, List<Integer>> entities = new HashMap<>();
		player.level().getEntities(player, player.getBoundingBox().inflate(HIGHLIGHT_RANGE),
						e -> !(e instanceof Player) && e.distanceTo(player) <= HIGHLIGHT_RANGE)
				.forEach(e ->
						ItemActionHandlerRegistry.getEntityHandlerIdFor(e)
								.ifPresent(id -> entities.computeIfAbsent(id, k -> new ArrayList<>()).add(e.getId()))
				);
		if (!positions.isEmpty() || !entities.isEmpty()) {
			return new RequestItemHighlightsPayload(stack, positions, entities);
		} else {
			player.sendOverlayMessage(ItemActionsTranslationHelper.INSTANCE.translStatusMessage("no_storage_in_range").setStyle(Style.EMPTY.withColor(0xFF5555)));
			player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1, 0.45f + RandHelper.getRandomMinusOneToOne(player.level().getRandom()) * 0.1F);
			return null;
		}
	}

	public static void handleHighlight(Player player, ItemStackKey stackKey, Map<Identifier, List<BlockPos>> storagePositions, Map<Identifier, List<Integer>> entities) {
		AtomicInteger stackMatchNumber = new AtomicInteger(0);
		AtomicInteger itemMatchNumber = new AtomicInteger(0);

		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		Map<BlockPos, List<BlockPos>> stackPositions = new LinkedHashMap<>();
		Map<BlockPos, List<BlockPos>> itemPositions = new LinkedHashMap<>();

		storagePositions.forEach((handlerId, positions) ->
				ItemActionHandlerRegistry.getBlockHandler(handlerId).ifPresent(handler ->
						positions.forEach(pos -> {
							switch (handler.getItemMatch(serverPlayer, stackKey, pos, IBlockItemActionHandler.Action.HIGHLIGHT)) {
								case MATCHING_STACK -> stackPositions.putIfAbsent(getHighlightGroupKey(handler.getHighlightPositions(serverPlayer, pos), pos), handler.getHighlightPositions(serverPlayer, pos));
								case MATCHING_ITEM -> itemPositions.putIfAbsent(getHighlightGroupKey(handler.getHighlightPositions(serverPlayer, pos), pos), handler.getHighlightPositions(serverPlayer, pos));
							}
						})
				)
		);

		stackMatchNumber.addAndGet(stackPositions.size());
		itemMatchNumber.addAndGet(itemPositions.size());
		PacketDistributor.sendToPlayer(serverPlayer, new SyncBlockHighlightsPayload(
				Map.of(
						MATCHING_STACK_HIGHLIGHT_COLOR, new ArrayList<>(stackPositions.values()),
						MATCHING_ITEM_HIGHLIGHT_COLOR, new ArrayList<>(itemPositions.values())
				)
		));

		List<Integer> stackEntities = new ArrayList<>();
		List<Integer> itemEntities = new ArrayList<>();

		entities.forEach((handlerId, entityIds) ->
				ItemActionHandlerRegistry.getEntityHandler(handlerId).ifPresent(handler ->
						entityIds.forEach(entityId -> {
							Entity entity = player.level().getEntity(entityId);
							if (entity == null) {
								return;
							}

							switch (handler.getItemMatch(stackKey, entity)) {
								case MATCHING_STACK -> stackEntities.add(entityId);
								case MATCHING_ITEM -> itemEntities.add(entityId);
							}
						})
				)
		);

		stackMatchNumber.addAndGet(stackEntities.size());
		itemMatchNumber.addAndGet(itemEntities.size());

		PacketDistributor.sendToPlayer(serverPlayer, new SyncEntityHighlightsPayload(
				Map.of(
						MATCHING_STACK_HIGHLIGHT_COLOR, stackEntities,
						MATCHING_ITEM_HIGHLIGHT_COLOR, itemEntities
				)
		));

		Level level = player.level();

		Component message = null;
		if (stackMatchNumber.get() == 0 && itemMatchNumber.get() == 0) {
			message = ItemActionsTranslationHelper.INSTANCE.translStatusMessage("no_matching_items_found");
			level.playSound(null, player, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1, 0.7f + RandHelper.getRandomMinusOneToOne(level.getRandom()) * 0.1F);
		} else {
			if (stackMatchNumber.get() > 0) {
				message = ItemActionsTranslationHelper.INSTANCE.translStatusMessage("matching_stacks_found", Component.literal(String.valueOf(stackMatchNumber.get())).withColor(0x4CAF50));
			}
			if (itemMatchNumber.get() > 0) {
				MutableComponent itemMessage = ItemActionsTranslationHelper.INSTANCE.translStatusMessage("matching_items_found", Component.literal(String.valueOf(itemMatchNumber.get())).withColor(0x42A5F5));
				if (message != null) {
					message = message.plainCopy().append(" ").append(itemMessage);
				} else {
					message = itemMessage;
				}
			}
			level.playSound(null, player, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1, 0.95f + RandHelper.getRandomMinusOneToOne(level.getRandom()) * 0.1F);
		}

		player.sendOverlayMessage(message);
	}

	private static BlockPos getHighlightGroupKey(List<BlockPos> positions, BlockPos fallbackPos) {
		return positions.stream().min(Comparator.comparingLong(BlockPos::asLong)).orElse(fallbackPos);
	}
}
