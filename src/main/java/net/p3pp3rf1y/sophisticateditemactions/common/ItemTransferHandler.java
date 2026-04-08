package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.ChatFormatting;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.RandHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticateditemactions.client.gui.ItemActionsTranslationHelper;
import net.p3pp3rf1y.sophisticateditemactions.network.DepositItemsPayload;
import net.p3pp3rf1y.sophisticateditemactions.network.RestockItemsPayload;
import net.p3pp3rf1y.sophisticateditemactions.network.SyncItemTransfersPayload;

import java.util.*;

public class ItemTransferHandler {
	public static final int INTERACTION_RANGE = 10;

	public static DepositItemsPayload createDepositMultipleItemsPayload(Player player, boolean mainInventory, boolean hotbar, boolean onlyMatching) {
		if (!mainInventory && !hotbar) {
			throw new IllegalArgumentException("At least one of mainInventory or hotbar must be true");
		}

		int minSlot = hotbar ? 0 : 9;
		int maxSlot = mainInventory ? 36 : 9;
		return createDepositPayload(player, minSlot, maxSlot, onlyMatching);
	}

	public static DepositItemsPayload createDepositItemPayload(Player player, int itemSlot, boolean onlyMatching) {
		return createDepositPayload(player, itemSlot, itemSlot + 1, onlyMatching);
	}

	private static DepositItemsPayload createDepositPayload(Player player, int minSlot, int maxSlot, boolean onlyMatching) {
		Map<Identifier, List<BlockPos>> storages = getInteractionStoragePositionsAround(player);
		Map<Identifier, List<Integer>> entities = getStorageEntitiesAround(player);

		if (!storages.isEmpty() || !entities.isEmpty()) {
			return new DepositItemsPayload(minSlot, maxSlot, storages, entities, onlyMatching);
		} else {
			playError(player, ItemActionsTranslationHelper.INSTANCE.translStatusMessage("no_storage_in_range").setStyle(Style.EMPTY.withColor(0xFF5555)));
			return null;
		}
	}

	private static Map<Identifier, List<BlockPos>> getInteractionStoragePositionsAround(Player player) {
		Map<Identifier, Set<BlockPos>> tempStorages = new HashMap<>();
		Level level = player.level();
		WorldHelper.getBlockEntitiesInRange(level, player.blockPosition(), INTERACTION_RANGE).forEach(be -> {
			ItemActionHandlerRegistry.getBlockHandlerFor(level, be.getBlockPos(), be, IBlockItemActionHandler.Action.DEPOSIT)
					.ifPresent(handler -> {
						tempStorages.computeIfAbsent(handler.id(), k -> new HashSet<>()).add(handler.getInteractionPosToActOn(level, be.getBlockPos(), be, IBlockItemActionHandler.Action.DEPOSIT));
					});
		});

		Map<Identifier, List<BlockPos>> storages = new HashMap<>();
		tempStorages.forEach((key, value) -> storages.put(key, new ArrayList<>(value)));
		return storages;
	}

	private static Map<Identifier, List<Integer>> getStorageEntitiesAround(Player player) {
		Map<Identifier, List<Integer>> entities = new HashMap<>();
		player.level().getEntities(player, player.getBoundingBox().inflate(INTERACTION_RANGE),
						e -> e.distanceTo(player) <= INTERACTION_RANGE)
				.forEach(e ->
						ItemActionHandlerRegistry.getEntityHandlerIdFor(e)
								.ifPresent(id -> entities.computeIfAbsent(id, k -> new ArrayList<>()).add(e.getId()))
				);
		return entities;
	}

	public static void handleDeposit(Player player, int minSlot, int maxSlot, Map<Identifier, List<BlockPos>> storagePositions, Map<Identifier, List<Integer>> entities, boolean onlyMatching) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		List<IDepositHandler> handlers = collectAndSortDepositHandlers(player, storagePositions, entities, serverPlayer);

		Map<Vec3, ItemTransferData> deposited = new HashMap<>();
		Set<Integer> depositedFromSlots = new HashSet<>();
		for (int slot = minSlot; slot < maxSlot; slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}

			List<IDepositHandler> followUpHandlers = new ArrayList<>();
			List<IDepositHandler> followupEmptyHandlers = new ArrayList<>();

			ItemStackKey stackKey = ItemStackKey.of(stack);
			for (IDepositHandler depositHandler : handlers) {
				ItemMatchResult match = depositHandler.getItemMatch(stackKey);
				if (match == ItemMatchResult.MATCHING_STACK) {
					int inserted = depositToHandlerAndLog(depositHandler, stack, player, slot, deposited, depositedFromSlots);
					if (inserted > 0) {
						stack = stack.copyWithCount(stack.getCount() - inserted);
					}
					if (stack.isEmpty()) {
						break;
					}
				} else {
					if (match == ItemMatchResult.MATCHING_ITEM) {
						followUpHandlers.add(depositHandler);
					} else if (!onlyMatching && match == ItemMatchResult.NO_MATCH) {
						followupEmptyHandlers.add(depositHandler);
					}
				}
			}

			followUpHandlers.addAll(followupEmptyHandlers);

			if (!stack.isEmpty()) {
				for (IDepositHandler depositHandler : followUpHandlers) {
					int inserted = depositToHandlerAndLog(depositHandler, stack, player, slot, deposited, depositedFromSlots);
					if (inserted > 0) {
						stack = stack.copyWithCount(stack.getCount() - inserted);
					}
					if (stack.isEmpty()) {
						break;
					}
				}
			}
		}

		Vec3 playerPos = player.getEyePosition().add(0, -0.1, 0);
		List<ItemTransferData> itemTransferData = deposited.values().stream().toList();
		PacketDistributor.sendToPlayer(serverPlayer, new SyncItemTransfersPayload(itemTransferData, playerPos, true));
		PacketDistributor.sendToPlayersTrackingEntity(serverPlayer, new SyncItemTransfersPayload(itemTransferData, playerPos, true));

		showDepositMessage(player, minSlot, maxSlot, deposited, depositedFromSlots);
	}

	private static void showDepositMessage(Player player, int minSlot, int maxSlot, Map<Vec3, ItemTransferData> inserted, Set<Integer> depositedFromSlots) {
		Component message;
		Level level = player.level();
		if (maxSlot - minSlot == 1) {
			if (inserted.isEmpty()) {
				message = ItemActionsTranslationHelper.INSTANCE.translStatusMessage("cannot_deposit_item",
						Component.literal(player.getInventory().getItem(minSlot).getHoverName().getString()).withStyle(ChatFormatting.RED));
				level.playSound(null, player, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1, 0.7f + RandHelper.getRandomMinusOneToOne(level.getRandom()) * 0.1F);
			} else {
				message = ItemActionsTranslationHelper.INSTANCE.translStatusMessage("deposited_item",
						Component.literal(inserted.values().iterator().next().itemsTransferred().getFirst().getHoverName().getString()).withStyle(ChatFormatting.DARK_GREEN));
			}
		} else {
			if (inserted.isEmpty()) {
				message = ItemActionsTranslationHelper.INSTANCE.translStatusMessage("cannot_deposit_items");
				level.playSound(null, player, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1, 0.7f + RandHelper.getRandomMinusOneToOne(level.getRandom()) * 0.1F);
			} else {
				message = ItemActionsTranslationHelper.INSTANCE.translStatusMessage("deposited_items", Component.literal(String.valueOf(depositedFromSlots.size())).withStyle(ChatFormatting.DARK_GREEN));
			}
		}
		player.sendOverlayMessage(message);
	}

	private static List<IDepositHandler> collectAndSortDepositHandlers(Player player, Map<Identifier, List<BlockPos>> storagePositions, Map<Identifier, List<Integer>> entities, ServerPlayer serverPlayer) {
		List<IDepositHandler> handlers = new ArrayList<>();

		storagePositions.forEach((handlerId, positions) ->
				ItemActionHandlerRegistry.getBlockHandler(handlerId).ifPresent(handler ->
						positions.forEach(pos -> {
							if (WorldHelper.playerMayInteract(player, pos)) {
								handler.getDepositHandler(serverPlayer, pos).ifPresent(handlers::add);
							}

						})
				)
		);

		entities.forEach((handlerId, entityIds) ->
				ItemActionHandlerRegistry.getEntityHandler(handlerId).ifPresent(handler ->
						entityIds.forEach(entityId -> {
							Entity entity = player.level().getEntity(entityId);
							if (entity == null) {
								return;
							}

							handler.getDepositHandler(entity).ifPresent(handlers::add);
						})
				)
		);

		handlers.sort(Comparator.comparingDouble(h -> player.distanceToSqr(h.getPosition())));
		return handlers;
	}

	private static int depositToHandlerAndLog(IDepositHandler depositHandler, ItemStack stack, Player player, int slot, Map<Vec3, ItemTransferData> deposited, Set<Integer> depositedFromSlots) {
		int inserted = depositHandler.insertItem(stack);
		if (inserted > 0) {
			deposited.computeIfAbsent(depositHandler.getPosition(), k -> new ItemTransferData(depositHandler.getPositionToOpen().orElse(null), depositHandler.getPosition(), new ArrayList<>())).itemsTransferred().add(stack.copyWithCount(inserted));
			ItemStack remainingStack = stack.getCount() == inserted ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
			player.getInventory().setItem(slot, remainingStack);
			depositedFromSlots.add(slot);
			return inserted;
		}
		return 0;
	}

	public static RestockItemsPayload createRestockMultipleItemsPayload(Player player, ItemStack filter, boolean mainInventory, boolean hotbar, boolean fillEmpty, boolean refillSingle) {
		if (!mainInventory && !hotbar) {
			throw new IllegalArgumentException("At least one of mainInventory or hotbar must be true");
		}

		int minSlot = hotbar ? 0 : 9;
		int maxSlot = mainInventory ? 36 : 9;
		return createRestockPayload(player, filter, minSlot, maxSlot, fillEmpty, refillSingle);
	}

	public static RestockItemsPayload createRestockItemPayload(Player player, ItemStack filter, int itemSlot, boolean fillEmpty, boolean refillSingle) {
		ItemStack item = player.getInventory().getItem(itemSlot);
		if (!fillEmpty && item.getCount() == item.getMaxStackSize()) {
			playError(player, ItemActionsTranslationHelper.INSTANCE.translStatusMessage("cannot_restock_full_stack", item.getHoverName().copy().setStyle(Style.EMPTY.withColor(0xFF5555))));
			return null;
		}

		return createRestockPayload(player, filter, itemSlot, itemSlot + 1, fillEmpty, refillSingle);
	}

	private static RestockItemsPayload createRestockPayload(Player player, ItemStack filter, int minSlot, int maxSlot, boolean fillEmpty, boolean refillSingle) {
		if (maxSlot - minSlot > 1 && checkStacksDoNotAllowRestock(player, minSlot, maxSlot, fillEmpty)) {
			playError(player, ItemActionsTranslationHelper.INSTANCE.translStatusMessage("cannot_restock_full_stacks").setStyle(Style.EMPTY.withColor(0xFF5555)));
			return null;
		}
		Map<Identifier, List<BlockPos>> storages = getInteractionStoragePositionsAround(player);
		Map<Identifier, List<Integer>> entities = getStorageEntitiesAround(player);

		if (!storages.isEmpty() || !entities.isEmpty()) {
			return new RestockItemsPayload(filter, minSlot, maxSlot, fillEmpty, refillSingle, storages, entities);
		} else {
			playError(player, ItemActionsTranslationHelper.INSTANCE.translStatusMessage("no_storage_in_range").setStyle(Style.EMPTY.withColor(0xFF5555)));
			return null;
		}
	}

	private static boolean checkStacksDoNotAllowRestock(Player player, int minSlot, int maxSlot, boolean fillEmpty) {
		for (int slot = minSlot; slot < maxSlot; slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if ((fillEmpty && stack.isEmpty()) || (!stack.isEmpty() && stack.getCount() < stack.getMaxStackSize())) {
				return false;
			}
		}
		return true;
	}

	private static void playError(Player player, MutableComponent message) {
		player.sendOverlayMessage(message);
		player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1, 0.45f + RandHelper.getRandomMinusOneToOne(player.level().getRandom()) * 0.1F);
	}

	public static void handleRestock(Player player, Map<Identifier, List<BlockPos>> storagePositions, Map<Identifier, List<Integer>> entities, int minSlot, int maxSlot, ItemStack filter, boolean fillEmpty, boolean refillSingle) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		List<IRestockHandler> restockHandlers = collectAndSortRestockHandlers(player, storagePositions, entities, serverPlayer);

		Map<Vec3, ItemTransferData> restocked = new HashMap<>();
		Set<Integer> restockedPlayerSlots = new HashSet<>();
		for (int playerInventorySlot = minSlot; playerInventorySlot < maxSlot; playerInventorySlot++) {
			ItemStack playerInventoryStack = player.getInventory().getItem(playerInventorySlot);
			if (fillEmpty && !filter.isEmpty()) {
				if (playerInventoryStack.isEmpty() || ItemStack.isSameItemSameComponents(playerInventoryStack, filter)) {
					int countToRestock = refillSingle ? 1 : filter.getMaxStackSize() - playerInventoryStack.getCount();
					restockSlot(restockHandlers, filter, playerInventoryStack, restocked, restockedPlayerSlots, player, playerInventorySlot, countToRestock);
				}
			} else {
				if (!playerInventoryStack.isEmpty()) {
					int countToRestock = refillSingle ? 1 : playerInventoryStack.getMaxStackSize() - playerInventoryStack.getCount();
					restockSlot(restockHandlers, playerInventoryStack, playerInventoryStack, restocked, restockedPlayerSlots, player, playerInventorySlot, countToRestock);
				}
			}
		}

		Vec3 playerPos = player.getEyePosition().add(0, -0.3, 0);
		List<ItemTransferData> itemTransferData = restocked.values().stream().toList();
		PacketDistributor.sendToPlayer(serverPlayer, new SyncItemTransfersPayload(itemTransferData, playerPos, false));
		PacketDistributor.sendToPlayersTrackingEntity(serverPlayer, new SyncItemTransfersPayload(itemTransferData, playerPos, false));

		Level level = player.level();
		Component message;
		if (maxSlot - minSlot == 1) {
			if (restocked.isEmpty()) {
				ItemStack item = fillEmpty ? filter : player.getInventory().getItem(minSlot);
				message = ItemActionsTranslationHelper.INSTANCE.translStatusMessage("cannot_restock_item",
						Component.literal(item.getHoverName().getString()).withStyle(ChatFormatting.RED));
				level.playSound(null, player, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1, 0.7f + RandHelper.getRandomMinusOneToOne(level.getRandom()) * 0.1F);
			} else {
				message = ItemActionsTranslationHelper.INSTANCE.translStatusMessage("restocked_item",
						Component.literal(restocked.values().iterator().next().itemsTransferred().getFirst().getHoverName().getString()).withStyle(ChatFormatting.DARK_GREEN));
			}
		} else {
			if (restocked.isEmpty()) {
				message = ItemActionsTranslationHelper.INSTANCE.translStatusMessage("cannot_restock_items");
				level.playSound(null, player, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1, 0.7f + RandHelper.getRandomMinusOneToOne(level.getRandom()) * 0.1F);
			} else {
				message = ItemActionsTranslationHelper.INSTANCE.translStatusMessage("restocked_items", Component.literal(String.valueOf(restockedPlayerSlots.size())).withStyle(ChatFormatting.DARK_GREEN));
			}
		}
		player.sendOverlayMessage(message);
	}

	private static void restockSlot(List<IRestockHandler> restockHandlers, ItemStack filter, ItemStack playerInventoryStack, Map<Vec3, ItemTransferData> restocked, Set<Integer> restockedPlayerSlots, Player player, int playerInventorySlot, int countToRestock) {
		if (playerInventoryStack.getCount() >= filter.getMaxStackSize()) {
			return;
		}

		ItemStack stackToExtract = filter.copyWithCount(countToRestock);

		int totalExtracted = 0;
		int originalCount = stackToExtract.getCount();
		for (IRestockHandler handler : restockHandlers) {
			int extracted = handler.extractItem(stackToExtract);
			if (extracted > 0) {
				ItemStack extractedStack = stackToExtract.copyWithCount(extracted);
				restocked.computeIfAbsent(handler.getPosition(), k -> new ItemTransferData(handler.getPositionToOpen().orElse(null), handler.getPosition(), new ArrayList<>())).itemsTransferred().add(extractedStack.copy());
				if (playerInventoryStack.isEmpty()) {
					playerInventoryStack = extractedStack;
				} else {
					playerInventoryStack.grow(extracted);
				}
				player.getInventory().setItem(playerInventorySlot, playerInventoryStack);
				restockedPlayerSlots.add(playerInventorySlot);
				totalExtracted += extracted;
				stackToExtract = stackToExtract.copyWithCount(stackToExtract.getCount() - extracted);
			}
			if (totalExtracted >= originalCount) {
				break;
			}
		}
	}

	private static List<IRestockHandler> collectAndSortRestockHandlers(Player player, Map<Identifier, List<BlockPos>> storagePositions, Map<Identifier, List<Integer>> entities, ServerPlayer serverPlayer) {
		List<IRestockHandler> handlers = new ArrayList<>();

		storagePositions.forEach((handlerId, positions) ->
				ItemActionHandlerRegistry.getBlockHandler(handlerId).ifPresent(handler ->
						positions.forEach(pos -> {
							if (WorldHelper.playerMayInteract(player, pos)) {
								handler.getRestockHandler(serverPlayer, pos).ifPresent(handlers::add);
							}
						})
				)
		);

		entities.forEach((handlerId, entityIds) ->
				ItemActionHandlerRegistry.getEntityHandler(handlerId).ifPresent(handler ->
						entityIds.forEach(entityId -> {
							Entity entity = player.level().getEntity(entityId);
							if (entity == null) {
								return;
							}

							handler.getRestockHandler(entity).ifPresent(handlers::add);
						})
				)
		);

		handlers.sort(Comparator.comparingDouble(h -> player.distanceToSqr(h.getPosition())));
		return handlers;
	}
}
