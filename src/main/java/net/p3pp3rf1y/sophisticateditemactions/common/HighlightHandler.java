package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.network.SyncBlockHighlightsPayload;
import net.p3pp3rf1y.sophisticatedcore.util.RandHelper;
import net.p3pp3rf1y.sophisticateditemactions.client.gui.ItemActionsTranslationHelper;
import net.p3pp3rf1y.sophisticateditemactions.network.RequestItemHighlightsPayload;
import net.p3pp3rf1y.sophisticateditemactions.network.SyncEntityHighlightsPayload;
import net.p3pp3rf1y.sophisticateditemactions.network.SyncHighlightDirectionsPayload;
import net.p3pp3rf1y.sophisticateditemactions.network.SyncRenderedEntityBlockHighlightsPayload;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class HighlightHandler {
	public static final int MATCHING_STACK_HIGHLIGHT_COLOR = 0x4CAF50;
	public static final int MATCHING_ITEM_HIGHLIGHT_COLOR = 0x42A5F5;
	private static final int HIGHLIGHT_RANGE = 32;
	private static final int MIN_HIGHLIGHT_DURATION = 40;
	private static final int MAX_HIGHLIGHT_DURATION = 160;
	public static void highlightItem(Player player, ItemStack stack) {
		Map<ResourceLocation, List<BlockPos>> positions = new HashMap<>();
		Map<ResourceLocation, Map<BlockPos, BlockPos>> canonicalPositions = new HashMap<>();

		SubLevelCompatHelper.getBlockEntitiesInRange(player.level(), player.blockPosition(), HIGHLIGHT_RANGE).forEach(be -> ItemActionHandlerRegistry
				.getBlockHandlerFor(be.getLevel() == null ? player.level() : be.getLevel(), be.getBlockPos(), be, IBlockItemActionHandler.Action.HIGHLIGHT)
				.ifPresent(handler -> {
					Level storageLevel = be.getLevel() == null ? player.level() : be.getLevel();
					BlockPos canonicalPos = handler.getInteractionPosToActOn(storageLevel, be.getBlockPos(), be, IBlockItemActionHandler.Action.HIGHLIGHT);
					canonicalPositions.computeIfAbsent(handler.id(), k -> new HashMap<>()).putIfAbsent(canonicalPos, canonicalPos);
				}));
		canonicalPositions.forEach((handlerId, posMap) -> positions.put(handlerId, new ArrayList<>(posMap.values())));

		Map<ResourceLocation, List<Integer>> entities = new HashMap<>();
		double highlightRangeSqr = HIGHLIGHT_RANGE * HIGHLIGHT_RANGE;
		player.level().getEntities(player, player.getBoundingBox().inflate(HIGHLIGHT_RANGE),
				e -> !(e instanceof Player) && SubLevelCompatHelper.distanceSquared(player.level(), player.position(), e.position()) <= highlightRangeSqr)
				.forEach(e -> ItemActionHandlerRegistry.getEntityHandlerIdFor(e)
						.ifPresent(id -> entities.computeIfAbsent(id, k -> new ArrayList<>()).add(e.getId())));
		if (!positions.isEmpty() || !entities.isEmpty()) {
			PacketDistributor.sendToServer(new RequestItemHighlightsPayload(stack, positions, entities));
		} else {
			player.displayClientMessage(
					ItemActionsTranslationHelper.INSTANCE.translStatusMessage("no_storage_in_range").setStyle(Style.EMPTY.withColor(0xFF5555)), true);
			player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1, 0.45f + RandHelper.getRandomMinusOneToOne(player.level().random) * 0.1F);
		}
	}

	public static void handleHighlight(Player player, ItemStackKey stackKey, Map<ResourceLocation, List<BlockPos>> storagePositions,
			Map<ResourceLocation, List<Integer>> entities) {
		AtomicInteger stackMatchNumber = new AtomicInteger(0);
		AtomicInteger itemMatchNumber = new AtomicInteger(0);

		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		Map<BlockPos, List<BlockPos>> stackPositions = new LinkedHashMap<>();
		Map<BlockPos, List<BlockPos>> itemPositions = new LinkedHashMap<>();
		Map<Integer, List<EntityBlockHighlightData>> renderedEntityStackHighlights = new HashMap<>();
		Map<Integer, List<EntityBlockHighlightData>> renderedEntityItemHighlights = new HashMap<>();

		storagePositions.forEach((handlerId, positions) -> ItemActionHandlerRegistry.getBlockHandler(handlerId).ifPresent(handler -> positions.forEach(pos -> {
			switch (handler.getItemMatch(serverPlayer, stackKey, pos, IBlockItemActionHandler.Action.HIGHLIGHT)) {
				case MATCHING_STACK -> stackPositions.putIfAbsent(getHighlightGroupKey(handler.getHighlightPositions(serverPlayer, pos), pos),
						handler.getHighlightPositions(serverPlayer, pos));
				case MATCHING_ITEM -> itemPositions.putIfAbsent(getHighlightGroupKey(handler.getHighlightPositions(serverPlayer, pos), pos),
						handler.getHighlightPositions(serverPlayer, pos));
			}
		})));

		stackMatchNumber.addAndGet(stackPositions.size());
		itemMatchNumber.addAndGet(itemPositions.size());
		List<Integer> stackEntities = new ArrayList<>();
		List<Integer> itemEntities = new ArrayList<>();

		entities.forEach((handlerId, entityIds) -> ItemActionHandlerRegistry.getEntityHandler(handlerId).ifPresent(handler -> entityIds.forEach(entityId -> {
			Entity entity = player.level().getEntity(entityId);
			if (entity == null) {
				return;
			}

			Optional<List<IEntityItemActionHandler.HighlightGroup>> customRenderedHighlightGroups = handler.getCustomRenderedHighlightGroupsWithMatch(stackKey,
					entity);
			if (customRenderedHighlightGroups.isPresent()) {
				customRenderedHighlightGroups.get().forEach(group -> {
					switch (group.matchResult()) {
						case MATCHING_STACK -> renderedEntityStackHighlights.computeIfAbsent(MATCHING_STACK_HIGHLIGHT_COLOR, k -> new ArrayList<>())
								.add(new EntityBlockHighlightData(entityId, List.of(group.positions())));
						case MATCHING_ITEM -> renderedEntityItemHighlights.computeIfAbsent(MATCHING_ITEM_HIGHLIGHT_COLOR, k -> new ArrayList<>())
								.add(new EntityBlockHighlightData(entityId, List.of(group.positions())));
					}
				});
			}

			Optional<List<IEntityItemActionHandler.HighlightGroup>> customHighlightPositionGroups = handler.getCustomHighlightGroupsWithMatch(stackKey, entity);
			if (customHighlightPositionGroups.isPresent()) {
				customHighlightPositionGroups.get().forEach(group -> {
					switch (group.matchResult()) {
						case MATCHING_STACK ->
							stackPositions.putIfAbsent(getHighlightGroupKey(group.positions(), BlockPos.containing(entity.position())), group.positions());
						case MATCHING_ITEM ->
							itemPositions.putIfAbsent(getHighlightGroupKey(group.positions(), BlockPos.containing(entity.position())), group.positions());
					}
				});
				return;
			}

			switch (handler.getItemMatch(stackKey, entity)) {
				case MATCHING_STACK -> stackEntities.add(entityId);
				case MATCHING_ITEM -> itemEntities.add(entityId);
			}
		})));

		stackMatchNumber.set(stackPositions.size() + stackEntities.size());
		itemMatchNumber.set(itemPositions.size() + itemEntities.size());

		Map<Integer, List<List<BlockPos>>> blockHighlights = Map.of(MATCHING_STACK_HIGHLIGHT_COLOR, new ArrayList<>(stackPositions.values()),
				MATCHING_ITEM_HIGHLIGHT_COLOR, new ArrayList<>(itemPositions.values()));
		BlockHighlightSplit blockHighlightSplit = splitBlockHighlights(serverPlayer, blockHighlights);
		Map<Integer, List<List<BlockPos>>> projectedBlockHighlights = projectBlockHighlights(serverPlayer, blockHighlights);

		Map<Integer, List<Integer>> entityHighlights = Map.of(MATCHING_STACK_HIGHLIGHT_COLOR, stackEntities, MATCHING_ITEM_HIGHLIGHT_COLOR, itemEntities);
		int highlightDuration = getHighlightDuration(serverPlayer, blockHighlights, entityHighlights);
		PacketDistributor.sendToPlayer(serverPlayer, new SyncBlockHighlightsPayload(blockHighlightSplit.worldHighlights(), highlightDuration));
		PacketDistributor.sendToPlayer(serverPlayer,
				new net.p3pp3rf1y.sophisticateditemactions.network.SyncRenderedBlockHighlightsPayload(blockHighlights, highlightDuration));
		PacketDistributor.sendToPlayer(serverPlayer, new SyncRenderedEntityBlockHighlightsPayload(
				mergeRenderedEntityHighlights(renderedEntityStackHighlights, renderedEntityItemHighlights), highlightDuration));
		PacketDistributor.sendToPlayer(serverPlayer, new SyncEntityHighlightsPayload(entityHighlights, highlightDuration));
		PacketDistributor.sendToPlayer(serverPlayer, new SyncHighlightDirectionsPayload(projectedBlockHighlights, entityHighlights, highlightDuration));

		Level level = player.level();

		Component message = null;
		if (stackMatchNumber.get() == 0 && itemMatchNumber.get() == 0) {
			message = ItemActionsTranslationHelper.INSTANCE.translStatusMessage("no_matching_items_found");
			player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1, 0.7f + RandHelper.getRandomMinusOneToOne(level.random) * 0.1F);
		} else {
			if (stackMatchNumber.get() > 0) {
				message = ItemActionsTranslationHelper.INSTANCE.translStatusMessage("matching_stacks_found",
						Component.literal(String.valueOf(stackMatchNumber.get())).withColor(0x4CAF50));
			}
			if (itemMatchNumber.get() > 0) {
				MutableComponent itemMessage = ItemActionsTranslationHelper.INSTANCE.translStatusMessage("matching_items_found",
						Component.literal(String.valueOf(itemMatchNumber.get())).withColor(0x42A5F5));
				if (message != null) {
					message = message.plainCopy().append(" ").append(itemMessage);
				} else {
					message = itemMessage;
				}
			}
			player.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1,
					0.95f + RandHelper.getRandomMinusOneToOne(level.random) * 0.1F);
		}

		player.displayClientMessage(message, true);
	}

	private static BlockPos getHighlightGroupKey(List<BlockPos> positions, BlockPos fallbackPos) {
		return positions.stream().min(Comparator.comparingLong(BlockPos::asLong)).orElse(fallbackPos);
	}

	private static BlockHighlightSplit splitBlockHighlights(ServerPlayer player, Map<Integer, List<List<BlockPos>>> blockHighlights) {
		Map<Integer, List<List<BlockPos>>> worldHighlights = new HashMap<>();
		Map<Integer, List<List<BlockPos>>> subLevelHighlights = new HashMap<>();
		blockHighlights.forEach((color, groups) -> groups.forEach(group -> {
			boolean hasSubLevelPos = group.stream().anyMatch(pos -> SubLevelCompatHelper.getLevelForPosition(player.level(), pos) != player.level());
			(hasSubLevelPos ? subLevelHighlights : worldHighlights).computeIfAbsent(color, k -> new ArrayList<>()).add(group);
		}));
		return new BlockHighlightSplit(worldHighlights, subLevelHighlights);
	}

	private static Map<Integer, List<List<BlockPos>>> projectBlockHighlights(ServerPlayer player, Map<Integer, List<List<BlockPos>>> blockHighlights) {
		Map<Integer, List<List<BlockPos>>> projectedHighlights = new HashMap<>();
		blockHighlights.forEach((color, groups) -> projectedHighlights.put(color, groups.stream().map(group -> group.stream().map(pos -> {
			Level storageLevel = SubLevelCompatHelper.getLevelForPosition(player.level(), pos);
			Vec3 worldPos = SubLevelCompatHelper.projectToWorld(storageLevel, Vec3.atCenterOf(pos));
			return BlockPos.containing(worldPos);
		}).toList()).toList()));
		return projectedHighlights;
	}

	private static Map<Integer, List<EntityBlockHighlightData>> mergeRenderedEntityHighlights(Map<Integer, List<EntityBlockHighlightData>> stackHighlights,
			Map<Integer, List<EntityBlockHighlightData>> itemHighlights) {
		Map<Integer, List<EntityBlockHighlightData>> renderedHighlights = new HashMap<>();
		stackHighlights.forEach((color, highlights) -> renderedHighlights.computeIfAbsent(color, k -> new ArrayList<>()).addAll(highlights));
		itemHighlights.forEach((color, highlights) -> renderedHighlights.computeIfAbsent(color, k -> new ArrayList<>()).addAll(highlights));
		return renderedHighlights;
	}

	private static int getHighlightDuration(ServerPlayer player, Map<Integer, List<List<BlockPos>>> blockHighlights,
			Map<Integer, List<Integer>> entityHighlights) {
		double farthestDistanceSqr = 0;
		for (List<List<BlockPos>> groups : blockHighlights.values()) {
			for (List<BlockPos> group : groups) {
				if (group.isEmpty()) {
					continue;
				}

				double x = 0;
				double y = 0;
				double z = 0;
				for (BlockPos pos : group) {
					Level storageLevel = SubLevelCompatHelper.getLevelForPosition(player.level(), pos);
					Vec3 worldPos = SubLevelCompatHelper.projectToWorld(storageLevel, Vec3.atCenterOf(pos));
					x += worldPos.x;
					y += worldPos.y;
					z += worldPos.z;
				}

				int count = group.size();
				farthestDistanceSqr = Math.max(farthestDistanceSqr, player.distanceToSqr(x / count, y / count, z / count));
			}
		}

		for (List<Integer> entityIds : entityHighlights.values()) {
			for (int entityId : entityIds) {
				Entity entity = player.level().getEntity(entityId);
				if (entity != null) {
					farthestDistanceSqr = Math.max(farthestDistanceSqr,
							SubLevelCompatHelper.distanceSquared(player.level(), player.position(), entity.position()));
				}
			}
		}

		double distanceRatio = Math.min(Math.sqrt(farthestDistanceSqr) / HIGHLIGHT_RANGE, 1);
		return MIN_HIGHLIGHT_DURATION + (int) Math.round(distanceRatio * (MAX_HIGHLIGHT_DURATION - MIN_HIGHLIGHT_DURATION));
	}

}
