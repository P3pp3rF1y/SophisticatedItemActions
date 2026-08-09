package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.RandHelper;
import net.p3pp3rf1y.sophisticateditemactions.Config;
import net.p3pp3rf1y.sophisticateditemactions.client.gui.ItemActionsTranslationHelper;
import net.p3pp3rf1y.sophisticateditemactions.network.DepositItemsPayload;
import net.p3pp3rf1y.sophisticateditemactions.network.RestockAlternativeItemsPayload;
import net.p3pp3rf1y.sophisticateditemactions.network.RestockItemsPayload;
import net.p3pp3rf1y.sophisticateditemactions.network.RestockRecipeItemsPayload;
import net.p3pp3rf1y.sophisticateditemactions.network.SyncItemTransfersPayload;

import java.util.*;

public class ItemTransferHandler {
	public static final int INTERACTION_RANGE = 10;

	public static void depositMultipleItems(Player player, boolean mainInventory, boolean hotbar, boolean onlyMatching) {
		if (!mainInventory && !hotbar) {
			throw new IllegalArgumentException("At least one of mainInventory or hotbar must be true");
		}

		int minSlot = hotbar ? 0 : 9;
		int maxSlot = mainInventory ? 36 : 9;
		depositItem(player, minSlot, maxSlot, onlyMatching);
	}

	public static void depositItem(Player player, int itemSlot, boolean onlyMatching) {
		depositItem(player, itemSlot, itemSlot + 1, onlyMatching);
	}

	private static void depositItem(Player player, int minSlot, int maxSlot, boolean onlyMatching) {
		Map<Identifier, List<BlockPos>> storages = getInteractionStoragePositionsAround(player);
		Map<Identifier, List<Integer>> entities = getStorageEntitiesAround(player);

		if (!storages.isEmpty() || !entities.isEmpty()) {
			ClientPacketDistributor.sendToServer(new DepositItemsPayload(minSlot, maxSlot, storages, entities, onlyMatching));
		} else {
			playError(player, ItemActionsTranslationHelper.INSTANCE.translStatusMessage("no_storage_in_range").setStyle(Style.EMPTY.withColor(0xFF5555)));
		}
	}

	private static Map<Identifier, List<BlockPos>> getInteractionStoragePositionsAround(Player player) {
		Map<Identifier, Set<BlockPos>> tempStorages = new HashMap<>();
		Level level = player.level();
		SubLevelCompatHelper.getBlockEntitiesInRange(level, player.blockPosition(), INTERACTION_RANGE).forEach(be -> {
			Level storageLevel = be.getLevel() == null ? level : be.getLevel();
			ItemActionHandlerRegistry.getBlockHandlerFor(storageLevel, be.getBlockPos(), be, IBlockItemActionHandler.Action.DEPOSIT).ifPresent(handler -> {
				if (SubLevelCompatHelper.mayInteract(player, level, be.getBlockPos())) {
					tempStorages.computeIfAbsent(handler.id(), k -> new HashSet<>())
							.add(handler.getInteractionPosToActOn(storageLevel, be.getBlockPos(), be, IBlockItemActionHandler.Action.DEPOSIT));
				}
			});
		});

		Map<Identifier, List<BlockPos>> storages = new HashMap<>();
		tempStorages.forEach((key, value) -> storages.put(key, new ArrayList<>(value)));
		return storages;
	}

	private static Map<Identifier, List<Integer>> getStorageEntitiesAround(Player player) {
		Map<Identifier, List<Integer>> entities = new HashMap<>();
		double interactionRangeSqr = INTERACTION_RANGE * INTERACTION_RANGE;
		player.level()
				.getEntities(player, player.getBoundingBox().inflate(INTERACTION_RANGE),
						e -> SubLevelCompatHelper.distanceSquared(player.level(), player.position(), e.position()) <= interactionRangeSqr)
				.forEach(e -> ItemActionHandlerRegistry.getEntityHandlerIdFor(e)
						.ifPresent(id -> entities.computeIfAbsent(id, k -> new ArrayList<>()).add(e.getId())));
		return entities;
	}

	public static void handleDeposit(Player player, int minSlot, int maxSlot, Map<Identifier, List<BlockPos>> storagePositions,
			Map<Identifier, List<Integer>> entities, boolean onlyMatching) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		List<IDepositHandler> handlers = collectAndSortDepositHandlers(player, storagePositions, entities, serverPlayer);

		Map<Vec3, ItemTransferData> deposited = new HashMap<>();
		Set<Integer> depositedFromSlots = new HashSet<>();
		List<ItemStack> depositedStacks = new ArrayList<>();
		for (int slot = minSlot; slot < maxSlot; slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.isEmpty() || shouldSkipRegularDepositFromSlot(player, slot)) {
				continue;
			}

			List<IDepositHandler> followUpHandlers = new ArrayList<>();
			List<IDepositHandler> followupEmptyHandlers = new ArrayList<>();

			ItemStackKey stackKey = ItemStackKey.of(stack);
			for (IDepositHandler depositHandler : handlers) {
				ItemMatchResult match = depositHandler.getItemMatch(stackKey);
				if (match == ItemMatchResult.MATCHING_STACK) {
					int inserted = depositToHandlerAndLog(depositHandler, stack, player, slot, deposited, depositedFromSlots, depositedStacks);
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
					int inserted = depositToHandlerAndLog(depositHandler, stack, player, slot, deposited, depositedFromSlots, depositedStacks);
					if (inserted > 0) {
						stack = stack.copyWithCount(stack.getCount() - inserted);
					}
					if (stack.isEmpty()) {
						break;
					}
				}
			}
		}

		int extensionDepositedStacks = ItemTransferExtensionRegistry.getExtension().map(extension -> extension.depositFromInventorySources(player, minSlot,
				maxSlot, onlyMatching, collectStorageItemHandlerTargets(player, storagePositions, entities, serverPlayer), deposited)).orElse(0);

		Vec3 playerPos = SubLevelCompatHelper.projectToWorld(player.level(), player.getEyePosition().add(0, -0.1, 0));
		List<ItemTransferData> itemTransferData = deposited.values().stream().toList();
		PacketDistributor.sendToPlayer(serverPlayer, new SyncItemTransfersPayload(itemTransferData, playerPos, true));
		PacketDistributor.sendToPlayersTrackingEntity(serverPlayer, new SyncItemTransfersPayload(itemTransferData, playerPos, true));

		showDepositMessage(player, minSlot, maxSlot, depositedFromSlots, depositedStacks, extensionDepositedStacks,
				hasDepositExtensionSourceInScope(player, minSlot, maxSlot));
	}

	private static void showDepositMessage(Player player, int minSlot, int maxSlot, Set<Integer> depositedFromSlots, List<ItemStack> depositedStacks,
			int extensionDepositedStacks, boolean hasDepositExtensionSourceInScope) {
		Component message;
		Level level = player.level();
		if (depositedFromSlots.isEmpty() && extensionDepositedStacks == 0) {
			if (maxSlot - minSlot == 1 && !hasDepositExtensionSourceInScope) {
				message = ItemActionsTranslationHelper.INSTANCE.translStatusMessage("cannot_deposit_item",
						Component.literal(player.getInventory().getItem(minSlot).getHoverName().getString()).withStyle(ChatFormatting.RED));
			} else {
				message = ItemActionsTranslationHelper.INSTANCE.translStatusMessage("cannot_deposit_items");
			}
			player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1, 0.45f + RandHelper.getRandomMinusOneToOne(level.random) * 0.1F);
			player.displayClientMessage(message, true);
			return;
		}

		MutableComponent mutableMessage = Component.empty();
		if (extensionDepositedStacks > 0) {
			ItemTransferExtensionRegistry.getExtension().map(extension -> extension.getDepositMessage(depositedFromSlots.size(), extensionDepositedStacks))
					.ifPresent(mutableMessage::append);
		} else if (!depositedFromSlots.isEmpty()) {
			mutableMessage.append(maxSlot - minSlot == 1
					? ItemActionsTranslationHelper.INSTANCE.translStatusMessage("deposited_item",
							Component.literal(depositedStacks.getFirst().getHoverName().getString()).withStyle(ChatFormatting.DARK_GREEN))
					: ItemActionsTranslationHelper.INSTANCE.translStatusMessage("deposited_items",
							Component.literal(String.valueOf(depositedFromSlots.size())).withStyle(ChatFormatting.DARK_GREEN)));
		}
		player.displayClientMessage(mutableMessage, true);
	}

	private static List<IDepositHandler> collectAndSortDepositHandlers(Player player, Map<Identifier, List<BlockPos>> storagePositions,
			Map<Identifier, List<Integer>> entities, ServerPlayer serverPlayer) {
		List<IDepositHandler> handlers = new ArrayList<>();

		storagePositions.forEach((handlerId, positions) -> ItemActionHandlerRegistry.getBlockHandler(handlerId).ifPresent(handler -> positions.forEach(pos -> {
			if (SubLevelCompatHelper.mayInteract(player, player.level(), pos)) {
				handler.getDepositHandler(serverPlayer, pos).ifPresent(handlers::add);
			}

		})));

		entities.forEach((handlerId, entityIds) -> ItemActionHandlerRegistry.getEntityHandler(handlerId).ifPresent(handler -> entityIds.forEach(entityId -> {
			Entity entity = player.level().getEntity(entityId);
			if (entity == null) {
				return;
			}

			handler.getDepositHandler(entity).ifPresent(handlers::add);
		})));

		handlers.sort(Comparator.comparingDouble(h -> SubLevelCompatHelper.distanceSquared(player.level(), player.position(), h.getPosition())));
		return handlers;
	}

	private static List<StorageItemHandlerTarget> collectStorageItemHandlerTargets(Player player, Map<Identifier, List<BlockPos>> storagePositions,
			Map<Identifier, List<Integer>> entities, ServerPlayer serverPlayer) {
		List<StorageItemHandlerTarget> targets = new ArrayList<>();

		storagePositions.forEach((handlerId, positions) -> ItemActionHandlerRegistry.getBlockHandler(handlerId).ifPresent(handler -> positions.forEach(pos -> {
			if (SubLevelCompatHelper.mayInteract(player, player.level(), pos)) {
				targets.addAll(handler.getStorageItemHandlerTargets(serverPlayer, pos));
			}
		})));

		entities.forEach((handlerId, entityIds) -> ItemActionHandlerRegistry.getEntityHandler(handlerId).ifPresent(handler -> entityIds.forEach(entityId -> {
			Entity entity = player.level().getEntity(entityId);
			if (entity == null) {
				return;
			}

			targets.addAll(handler.getStorageItemHandlerTargets(entity));
		})));

		targets.sort(Comparator.comparingDouble(target -> SubLevelCompatHelper.distanceSquared(player.level(), player.position(), target.position())));
		return targets;
	}

	private static boolean shouldSkipRegularDepositFromSlot(Player player, int slot) {
		return ItemTransferExtensionRegistry.getExtension().map(extension -> extension.shouldSkipRegularDepositFromSlot(player, slot)).orElse(false);
	}

	private static boolean hasDepositExtensionSourceInScope(Player player, int minSlot, int maxSlot) {
		for (int slot = minSlot; slot < maxSlot; slot++) {
			if (shouldSkipRegularDepositFromSlot(player, slot)) {
				return true;
			}
		}
		return false;
	}

	private static int depositToHandlerAndLog(IDepositHandler depositHandler, ItemStack stack, Player player, int slot, Map<Vec3, ItemTransferData> deposited,
			Set<Integer> depositedFromSlots, List<ItemStack> depositedStacks) {
		int inserted = depositHandler.insertItem(stack);
		if (inserted > 0) {
			ItemStack transferredStack = stack.copyWithCount(inserted);
			deposited
					.computeIfAbsent(depositHandler.getPosition(),
							k -> new ItemTransferData(depositHandler.getPositionToOpen().orElse(null), depositHandler.getPosition(), new ArrayList<>()))
					.itemsTransferred().add(transferredStack);
			depositedStacks.add(transferredStack);
			ItemStack remainingStack = stack.getCount() == inserted ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
			player.getInventory().setItem(slot, remainingStack);
			depositedFromSlots.add(slot);
			return inserted;
		}
		return 0;
	}

	public static void restockMultipleItems(Player player, ItemStack filter, boolean mainInventory, boolean hotbar, boolean fillEmpty, boolean refillSingle) {
		if (!mainInventory && !hotbar) {
			throw new IllegalArgumentException("At least one of mainInventory or hotbar must be true");
		}

		int minSlot = hotbar ? 0 : 9;
		int maxSlot = mainInventory ? 36 : 9;
		restockItem(player, filter, minSlot, maxSlot, fillEmpty, refillSingle);
	}

	public static void restockRecipeItems(Player player, List<List<ItemStack>> ingredientOptions) {
		if (ingredientOptions.isEmpty()) {
			return;
		}

		Map<Identifier, List<BlockPos>> storages = getInteractionStoragePositionsAround(player);
		Map<Identifier, List<Integer>> entities = getStorageEntitiesAround(player);
		if (!storages.isEmpty() || !entities.isEmpty()) {
			ClientPacketDistributor.sendToServer(new RestockRecipeItemsPayload(ingredientOptions, storages, entities));
		} else {
			playError(player, ItemActionsTranslationHelper.INSTANCE.translStatusMessage("no_storage_in_range").setStyle(Style.EMPTY.withColor(0xFF5555)));
		}
	}

	public static void restockAlternativeItems(Player player, List<ItemStack> filters, boolean mainInventory, boolean hotbar, boolean fillEmpty,
			boolean refillSingle) {
		if (!mainInventory && !hotbar) {
			throw new IllegalArgumentException("At least one of mainInventory or hotbar must be true");
		}

		int minSlot = hotbar ? 0 : 9;
		int maxSlot = mainInventory ? 36 : 9;
		restockAlternativeItems(player, filters, minSlot, maxSlot, fillEmpty, refillSingle);
	}

	public static void restockAlternativeItem(Player player, List<ItemStack> filters, int itemSlot, boolean fillEmpty, boolean refillSingle) {
		if (filters.isEmpty()) {
			return;
		}

		ItemStack item = player.getInventory().getItem(itemSlot);
		if (item.isEmpty() && !fillEmpty) {
			return;
		}
		if (!item.isEmpty() && !matchesAnyFilter(item, filters)) {
			return;
		}
		if (!fillEmpty && item.getCount() == item.getMaxStackSize()) {
			playError(player, ItemActionsTranslationHelper.INSTANCE.translStatusMessage("cannot_restock_full_stack",
					item.getHoverName().copy().setStyle(Style.EMPTY.withColor(0xFF5555))));
			return;
		}

		restockAlternativeItems(player, filters, itemSlot, itemSlot + 1, fillEmpty, refillSingle);
	}

	public static void restockItem(Player player, ItemStack filter, int itemSlot, boolean fillEmpty, boolean refillSingle) {
		ItemStack item = player.getInventory().getItem(itemSlot);
		if (item.isEmpty() && (!fillEmpty || filter.isEmpty()) && !hasInventorySourceInScope(player, itemSlot, itemSlot + 1)) {
			return;
		}
		if (!fillEmpty && item.getCount() == item.getMaxStackSize() && !hasInventorySourceInScope(player, itemSlot, itemSlot + 1)) {
			playError(player, ItemActionsTranslationHelper.INSTANCE.translStatusMessage("cannot_restock_full_stack",
					item.getHoverName().copy().setStyle(Style.EMPTY.withColor(0xFF5555))));
			return;
		}

		restockItem(player, filter, itemSlot, itemSlot + 1, fillEmpty, refillSingle);
	}

	private static void restockItem(Player player, ItemStack filter, int minSlot, int maxSlot, boolean fillEmpty, boolean refillSingle) {
		if (maxSlot - minSlot > 1 && checkStacksDoNotAllowRestock(player, minSlot, maxSlot, fillEmpty)
				&& !hasInventorySourceInScope(player, minSlot, maxSlot)) {
			playError(player,
					ItemActionsTranslationHelper.INSTANCE.translStatusMessage("cannot_restock_full_stacks").setStyle(Style.EMPTY.withColor(0xFF5555)));
			return;
		}
		Map<Identifier, List<BlockPos>> storages = getInteractionStoragePositionsAround(player);
		Map<Identifier, List<Integer>> entities = getStorageEntitiesAround(player);

		if (!storages.isEmpty() || !entities.isEmpty()) {
			ClientPacketDistributor.sendToServer(new RestockItemsPayload(filter, minSlot, maxSlot, fillEmpty, refillSingle, storages, entities));
		} else {
			playError(player, ItemActionsTranslationHelper.INSTANCE.translStatusMessage("no_storage_in_range").setStyle(Style.EMPTY.withColor(0xFF5555)));
		}
	}

	private static void restockAlternativeItems(Player player, List<ItemStack> filters, int minSlot, int maxSlot, boolean fillEmpty, boolean refillSingle) {
		if (filters.isEmpty()) {
			return;
		}
		if (maxSlot - minSlot > 1 && checkStacksDoNotAllowAlternativeRestock(player, filters, minSlot, maxSlot, fillEmpty)) {
			playError(player,
					ItemActionsTranslationHelper.INSTANCE.translStatusMessage("cannot_restock_full_stacks").setStyle(Style.EMPTY.withColor(0xFF5555)));
			return;
		}

		Map<Identifier, List<BlockPos>> storages = getInteractionStoragePositionsAround(player);
		Map<Identifier, List<Integer>> entities = getStorageEntitiesAround(player);
		if (!storages.isEmpty() || !entities.isEmpty()) {
			ClientPacketDistributor.sendToServer(new RestockAlternativeItemsPayload(filters, minSlot, maxSlot, fillEmpty, refillSingle, storages, entities));
		} else {
			playError(player, ItemActionsTranslationHelper.INSTANCE.translStatusMessage("no_storage_in_range").setStyle(Style.EMPTY.withColor(0xFF5555)));
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

	private static boolean checkStacksDoNotAllowAlternativeRestock(Player player, List<ItemStack> filters, int minSlot, int maxSlot, boolean fillEmpty) {
		for (int slot = minSlot; slot < maxSlot; slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if ((fillEmpty && stack.isEmpty()) || (!stack.isEmpty() && matchesAnyFilter(stack, filters) && stack.getCount() < stack.getMaxStackSize())) {
				return false;
			}
		}
		return true;
	}

	private static boolean hasInventorySourceInScope(Player player, int minSlot, int maxSlot) {
		return ItemTransferExtensionRegistry.getExtension().map(extension -> extension.hasInventorySourceInScope(player, minSlot, maxSlot)).orElse(false);
	}

	private static void playError(Player player, MutableComponent message) {
		player.displayClientMessage(message, true);
		player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1, 0.45f + RandHelper.getRandomMinusOneToOne(player.level().random) * 0.1F);
	}

	private static void syncRestockTransfers(ServerPlayer player, Map<Vec3, ItemTransferData> restocked) {
		Vec3 playerPos = SubLevelCompatHelper.projectToWorld(player.level(), player.getEyePosition().add(0, -0.3, 0));
		List<ItemTransferData> itemTransferData = restocked.values().stream().toList();
		PacketDistributor.sendToPlayer(player, new SyncItemTransfersPayload(itemTransferData, playerPos, false));
		PacketDistributor.sendToPlayersTrackingEntity(player, new SyncItemTransfersPayload(itemTransferData, playerPos, false));
	}

	private static void showAlternativeRestockMessage(Player player, int minSlot, int maxSlot, boolean fillEmpty, ItemStack filter,
			Set<Integer> restockedPlayerSlots, List<ItemStack> restockedStacks) {
		if (restockedPlayerSlots.isEmpty()) {
			if (maxSlot - minSlot == 1) {
				ItemStack item = fillEmpty ? filter : player.getInventory().getItem(minSlot);
				player.displayClientMessage(ItemActionsTranslationHelper.INSTANCE.translStatusMessage("cannot_restock_item",
						Component.literal(item.getHoverName().getString()).withStyle(ChatFormatting.RED)), true);
			} else {
				player.displayClientMessage(ItemActionsTranslationHelper.INSTANCE.translStatusMessage("cannot_restock_items"), true);
			}
			player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1, 0.45f + RandHelper.getRandomMinusOneToOne(player.level().random) * 0.1F);
			return;
		}

		if (maxSlot - minSlot == 1) {
			player.displayClientMessage(ItemActionsTranslationHelper.INSTANCE.translStatusMessage("restocked_item",
					Component.literal(restockedStacks.getFirst().getHoverName().getString()).withStyle(ChatFormatting.DARK_GREEN)), true);
		} else {
			player.displayClientMessage(ItemActionsTranslationHelper.INSTANCE.translStatusMessage("restocked_items",
					Component.literal(String.valueOf(restockedPlayerSlots.size())).withStyle(ChatFormatting.DARK_GREEN)), true);
		}
	}

	public static void handleAlternativeRestock(Player player, Map<Identifier, List<BlockPos>> storagePositions, Map<Identifier, List<Integer>> entities,
			List<ItemStack> filters, int minSlot, int maxSlot, boolean fillEmpty, boolean refillSingle) {
		if (!(player instanceof ServerPlayer serverPlayer) || filters.isEmpty()) {
			return;
		}

		List<IRestockHandler> restockHandlers = collectAndSortRestockHandlers(player, storagePositions, entities, serverPlayer);
		Map<Vec3, ItemTransferData> restocked = new HashMap<>();
		Set<Integer> restockedPlayerSlots = new HashSet<>();
		List<ItemStack> restockedStacks = new ArrayList<>();
		for (int playerInventorySlot = minSlot; playerInventorySlot < maxSlot; playerInventorySlot++) {
			ItemStack playerInventoryStack = player.getInventory().getItem(playerInventorySlot);
			if (fillEmpty && (playerInventoryStack.isEmpty() || matchesAnyFilter(playerInventoryStack, filters))) {
				int countToRestock = refillSingle
						? 1
						: playerInventoryStack.isEmpty()
								? filters.getFirst().getMaxStackSize()
								: playerInventoryStack.getMaxStackSize() - playerInventoryStack.getCount();
				restockAlternativeSlot(restockHandlers, filters, player, playerInventorySlot, countToRestock, restocked, restockedPlayerSlots, restockedStacks);
			} else if (!playerInventoryStack.isEmpty() && matchesAnyFilter(playerInventoryStack, filters)) {
				int countToRestock = refillSingle ? 1 : playerInventoryStack.getMaxStackSize() - playerInventoryStack.getCount();
				restockAlternativeSlot(restockHandlers, List.of(playerInventoryStack), player, playerInventorySlot, countToRestock, restocked,
						restockedPlayerSlots, restockedStacks);
			}
		}

		syncRestockTransfers(serverPlayer, restocked);
		showAlternativeRestockMessage(player, minSlot, maxSlot, fillEmpty, filters.getFirst(), restockedPlayerSlots, restockedStacks);
	}

	public static void handleRecipeRestock(Player player, Map<Identifier, List<BlockPos>> storagePositions, Map<Identifier, List<Integer>> entities,
			List<List<ItemStack>> ingredientOptions) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		if (!Config.SERVER.recipeRestockEnabled.get()) {
			player.displayClientMessage(ItemActionsTranslationHelper.INSTANCE.translStatusMessage("recipe_restock_disabled"), true);
			return;
		}

		List<ItemStack> inventory = new ArrayList<>();
		for (int slot = 0; slot < 36; slot++) {
			inventory.add(player.getInventory().getItem(slot));
		}
		List<List<ItemStack>> missingIngredientOptions = getMissingRecipeIngredientOptions(ingredientOptions, inventory);
		if (missingIngredientOptions.isEmpty()) {
			player.displayClientMessage(ItemActionsTranslationHelper.INSTANCE.translStatusMessage("recipe_restocked"), true);
			return;
		}

		List<IRestockHandler> restockHandlers = collectAndSortRestockHandlers(player, storagePositions, entities, serverPlayer);
		Map<Vec3, ItemTransferData> restocked = new HashMap<>();
		Set<Integer> restockedPlayerSlots = new HashSet<>();
		List<ItemStack> restockedStacks = new ArrayList<>();
		for (List<ItemStack> ingredientOptionsToRestock : missingIngredientOptions) {
			restockRecipeIngredient(restockHandlers, ingredientOptionsToRestock, player, restocked, restockedPlayerSlots, restockedStacks);
		}

		syncRestockTransfers(serverPlayer, restocked);
		if (restockedPlayerSlots.isEmpty()) {
			player.displayClientMessage(ItemActionsTranslationHelper.INSTANCE.translStatusMessage("cannot_restock_recipe"), true);
			player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1, 0.45f + RandHelper.getRandomMinusOneToOne(player.level().random) * 0.1F);
		} else {
			int restockedCount = restockedStacks.stream().mapToInt(ItemStack::getCount).sum();
			player.displayClientMessage(ItemActionsTranslationHelper.INSTANCE.translStatusMessage("restocked_recipe_items",
					Component.literal(String.valueOf(restockedCount)).withStyle(ChatFormatting.DARK_GREEN)), true);
		}
	}

	static List<List<ItemStack>> getMissingRecipeIngredientOptions(List<List<ItemStack>> ingredientOptions, List<ItemStack> inventory) {
		List<List<ItemStack>> targets = new ArrayList<>();
		for (List<ItemStack> options : ingredientOptions) {
			List<ItemStack> normalizedOptions = normalizeIngredientOptions(options);
			if (normalizedOptions.isEmpty()) {
				continue;
			}

			List<ItemStack> matchingTarget = targets.stream().filter(target -> hasSameIngredientOptions(target, normalizedOptions)).findFirst().orElse(null);
			if (matchingTarget == null) {
				targets.add(normalizedOptions);
			} else {
				int count = (int) Math.min((long) matchingTarget.getFirst().getCount() + normalizedOptions.getFirst().getCount(),
						36L * matchingTarget.getFirst().getMaxStackSize());
				matchingTarget.forEach(stack -> stack.setCount(count));
			}
		}
		targets.sort(Comparator.comparingInt(List::size));
		List<ItemStack> availableInventory = inventory.stream().map(ItemStack::copy).toList();
		List<List<ItemStack>> missingIngredientOptions = new ArrayList<>();
		for (List<ItemStack> options : targets) {
			int remaining = options.getFirst().getCount();
			for (ItemStack inventoryStack : availableInventory) {
				if (remaining > 0 && matchesAnyFilter(inventoryStack, options)) {
					int consumed = Math.min(remaining, inventoryStack.getCount());
					inventoryStack.shrink(consumed);
					remaining -= consumed;
				}
			}
			if (remaining > 0) {
				int missingCount = remaining;
				missingIngredientOptions.add(options.stream().map(stack -> stack.copyWithCount(missingCount)).toList());
			}
		}
		return missingIngredientOptions;
	}

	private static boolean hasSameIngredientOptions(List<ItemStack> first, List<ItemStack> second) {
		if (first.size() != second.size()) {
			return false;
		}
		for (int index = 0; index < first.size(); index++) {
			if (!ItemStack.isSameItemSameComponents(first.get(index), second.get(index))) {
				return false;
			}
		}
		return true;
	}

	private static List<ItemStack> normalizeIngredientOptions(List<ItemStack> options) {
		ItemStack firstOption = options.stream().filter(stack -> !stack.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
		if (firstOption.isEmpty()) {
			return List.of();
		}
		int count = Math.min(firstOption.getCount(), 36 * firstOption.getMaxStackSize());
		List<ItemStack> normalizedOptions = new ArrayList<>();
		for (ItemStack option : options) {
			if (!option.isEmpty() && !matchesAnyFilter(option, normalizedOptions)) {
				normalizedOptions.add(option.copyWithCount(count));
			}
		}
		return normalizedOptions;
	}

	private static void restockRecipeIngredient(List<IRestockHandler> restockHandlers, List<ItemStack> ingredientOptions, Player player,
			Map<Vec3, ItemTransferData> restocked, Set<Integer> restockedPlayerSlots, List<ItemStack> restockedStacks) {
		int remaining = ingredientOptions.getFirst().getCount();
		for (int slot = 0; slot < 36 && remaining > 0; slot++) {
			ItemStack inventoryStack = player.getInventory().getItem(slot);
			if (!inventoryStack.isEmpty() && matchesAnyFilter(inventoryStack, ingredientOptions)
					&& inventoryStack.getCount() < inventoryStack.getMaxStackSize()) {
				remaining -= restockRecipeSlot(restockHandlers, List.of(inventoryStack), player, slot, remaining, restocked, restockedPlayerSlots,
						restockedStacks);
			}
		}
		for (int slot = 0; slot < 36 && remaining > 0; slot++) {
			if (player.getInventory().getItem(slot).isEmpty()) {
				remaining -= restockRecipeSlot(restockHandlers, ingredientOptions, player, slot, remaining, restocked, restockedPlayerSlots, restockedStacks);
			}
		}
	}

	private static int restockRecipeSlot(List<IRestockHandler> restockHandlers, List<ItemStack> filters, Player player, int slot, int remaining,
			Map<Vec3, ItemTransferData> restocked, Set<Integer> restockedPlayerSlots, List<ItemStack> restockedStacks) {
		ItemStack inventoryStack = player.getInventory().getItem(slot);
		int originalCount = inventoryStack.getCount();
		restockAlternativeSlot(restockHandlers, filters, player, slot, remaining, restocked, restockedPlayerSlots, restockedStacks);
		return Math.max(0, player.getInventory().getItem(slot).getCount() - originalCount);
	}

	private static void restockAlternativeSlot(List<IRestockHandler> restockHandlers, List<ItemStack> filters, Player player, int slot, int countToRestock,
			Map<Vec3, ItemTransferData> restocked, Set<Integer> restockedPlayerSlots, List<ItemStack> restockedStacks) {
		ItemStack inventoryStack = player.getInventory().getItem(slot);
		if (!inventoryStack.isEmpty()) {
			restockSlot(restockHandlers, inventoryStack, inventoryStack, restocked, restockedPlayerSlots, restockedStacks, player, slot,
					Math.min(countToRestock, inventoryStack.getMaxStackSize() - inventoryStack.getCount()));
			return;
		}

		if (filters.size() > 1) {
			restockHandlers.forEach(handler -> handler.prepareForAlternativeRestock(filters));
		}
		for (ItemStack filter : filters) {
			restockSlot(restockHandlers, filter, inventoryStack, restocked, restockedPlayerSlots, restockedStacks, player, slot,
					Math.min(countToRestock, filter.getMaxStackSize()));
			if (!player.getInventory().getItem(slot).isEmpty()) {
				return;
			}
		}
	}

	private static boolean matchesAnyFilter(ItemStack stack, List<ItemStack> filters) {
		return !stack.isEmpty() && filters.stream().anyMatch(filter -> ItemStack.isSameItemSameComponents(stack, filter));
	}

	public static void handleRestock(Player player, Map<Identifier, List<BlockPos>> storagePositions, Map<Identifier, List<Integer>> entities, int minSlot,
			int maxSlot, ItemStack filter, boolean fillEmpty, boolean refillSingle) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		List<IRestockHandler> restockHandlers = collectAndSortRestockHandlers(player, storagePositions, entities, serverPlayer);

		Map<Vec3, ItemTransferData> restocked = new HashMap<>();
		Set<Integer> restockedPlayerSlots = new HashSet<>();
		List<ItemStack> restockedStacks = new ArrayList<>();
		for (int playerInventorySlot = minSlot; playerInventorySlot < maxSlot; playerInventorySlot++) {
			ItemStack playerInventoryStack = player.getInventory().getItem(playerInventorySlot);
			if (fillEmpty && !filter.isEmpty()) {
				if (playerInventoryStack.isEmpty() || ItemStack.isSameItemSameComponents(playerInventoryStack, filter)) {
					int countToRestock = refillSingle ? 1 : filter.getMaxStackSize() - playerInventoryStack.getCount();
					restockSlot(restockHandlers, filter, playerInventoryStack, restocked, restockedPlayerSlots, restockedStacks, player, playerInventorySlot,
							countToRestock);
				}
			} else {
				if (!playerInventoryStack.isEmpty()) {
					int countToRestock = refillSingle ? 1 : playerInventoryStack.getMaxStackSize() - playerInventoryStack.getCount();
					restockSlot(restockHandlers, playerInventoryStack, playerInventoryStack, restocked, restockedPlayerSlots, restockedStacks, player,
							playerInventorySlot, countToRestock);
				}
			}
		}

		int extensionRestockedStacks = ItemTransferExtensionRegistry.getExtension().map(extension -> extension.restockToInventorySources(player, minSlot,
				maxSlot, filter, fillEmpty, collectStorageItemHandlerTargets(player, storagePositions, entities, serverPlayer), restocked)).orElse(0);

		Vec3 playerPos = SubLevelCompatHelper.projectToWorld(player.level(), player.getEyePosition().add(0, -0.3, 0));
		List<ItemTransferData> itemTransferData = restocked.values().stream().toList();
		PacketDistributor.sendToPlayer(serverPlayer, new SyncItemTransfersPayload(itemTransferData, playerPos, false));
		PacketDistributor.sendToPlayersTrackingEntity(serverPlayer, new SyncItemTransfersPayload(itemTransferData, playerPos, false));

		showRestockMessage(player, minSlot, maxSlot, filter, fillEmpty, restockedPlayerSlots, restockedStacks, extensionRestockedStacks);
	}

	private static void showRestockMessage(Player player, int minSlot, int maxSlot, ItemStack filter, boolean fillEmpty, Set<Integer> restockedPlayerSlots,
			List<ItemStack> restockedStacks, int extensionRestockedStacks) {
		Level level = player.level();
		if (restockedPlayerSlots.isEmpty() && extensionRestockedStacks == 0) {
			Component message;
			if (maxSlot - minSlot == 1) {
				ItemStack item = fillEmpty ? filter : player.getInventory().getItem(minSlot);
				message = ItemActionsTranslationHelper.INSTANCE.translStatusMessage("cannot_restock_item",
						Component.literal(item.getHoverName().getString()).withStyle(ChatFormatting.RED));
			} else {
				message = ItemActionsTranslationHelper.INSTANCE.translStatusMessage("cannot_restock_items");
			}
			player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1, 0.45f + RandHelper.getRandomMinusOneToOne(level.random) * 0.1F);
			player.displayClientMessage(message, true);
			return;
		}

		MutableComponent message = Component.empty();
		if (extensionRestockedStacks > 0) {
			ItemTransferExtensionRegistry.getExtension().map(extension -> extension.getRestockMessage(restockedPlayerSlots.size(), extensionRestockedStacks))
					.ifPresent(message::append);
		} else if (!restockedPlayerSlots.isEmpty()) {
			message.append(maxSlot - minSlot == 1
					? ItemActionsTranslationHelper.INSTANCE.translStatusMessage("restocked_item",
							Component.literal(restockedStacks.getFirst().getHoverName().getString()).withStyle(ChatFormatting.DARK_GREEN))
					: ItemActionsTranslationHelper.INSTANCE.translStatusMessage("restocked_items",
							Component.literal(String.valueOf(restockedPlayerSlots.size())).withStyle(ChatFormatting.DARK_GREEN)));
		}
		player.displayClientMessage(message, true);
	}

	private static void restockSlot(List<IRestockHandler> restockHandlers, ItemStack filter, ItemStack playerInventoryStack,
			Map<Vec3, ItemTransferData> restocked, Set<Integer> restockedPlayerSlots, List<ItemStack> restockedStacks, Player player, int playerInventorySlot,
			int countToRestock) {
		if (playerInventoryStack.getCount() >= filter.getMaxStackSize()) {
			return;
		}

		ItemStack stackToExtract = filter.copyWithCount(countToRestock);

		int totalExtracted = 0;
		int originalCount = stackToExtract.getCount();
		for (IRestockHandler handler : restockHandlers) {
			List<IRestockHandler.RestockTransfer> transfers = handler.extractTransfers(stackToExtract);
			if (!transfers.isEmpty()) {
				int extracted = 0;
				for (IRestockHandler.RestockTransfer transfer : transfers) {
					ItemStack transferredStack = transfer.stack().copy();
					restocked.computeIfAbsent(transfer.position(), k -> new ItemTransferData(transfer.positionToOpen(), transfer.position(), new ArrayList<>()))
							.itemsTransferred().add(transferredStack);
					restockedStacks.add(transferredStack);
					if (playerInventoryStack.isEmpty()) {
						playerInventoryStack = transfer.stack().copy();
					} else {
						playerInventoryStack.grow(transfer.stack().getCount());
					}
					player.getInventory().setItem(playerInventorySlot, playerInventoryStack);
					restockedPlayerSlots.add(playerInventorySlot);
					extracted += transfer.stack().getCount();
				}
				totalExtracted += extracted;
				stackToExtract = stackToExtract.copyWithCount(stackToExtract.getCount() - extracted);
			}
			if (totalExtracted >= originalCount) {
				break;
			}
		}
	}

	private static List<IRestockHandler> collectAndSortRestockHandlers(Player player, Map<Identifier, List<BlockPos>> storagePositions,
			Map<Identifier, List<Integer>> entities, ServerPlayer serverPlayer) {
		List<IRestockHandler> handlers = new ArrayList<>();

		storagePositions.forEach((handlerId, positions) -> ItemActionHandlerRegistry.getBlockHandler(handlerId).ifPresent(handler -> positions.forEach(pos -> {
			if (SubLevelCompatHelper.mayInteract(player, player.level(), pos)) {
				handler.getRestockHandler(serverPlayer, pos).ifPresent(handlers::add);
			}
		})));

		entities.forEach((handlerId, entityIds) -> ItemActionHandlerRegistry.getEntityHandler(handlerId).ifPresent(handler -> entityIds.forEach(entityId -> {
			Entity entity = player.level().getEntity(entityId);
			if (entity == null) {
				return;
			}

			handler.getRestockHandler(entity).ifPresent(handlers::add);
		})));

		handlers.sort(Comparator.comparingDouble(h -> SubLevelCompatHelper.distanceSquared(player.level(), player.position(), h.getPosition())));
		return handlers;
	}
}
