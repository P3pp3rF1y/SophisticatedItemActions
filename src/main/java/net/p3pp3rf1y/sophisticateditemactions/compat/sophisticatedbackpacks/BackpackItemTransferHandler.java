package net.p3pp3rf1y.sophisticateditemactions.compat.sophisticatedbackpacks;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeItem;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticateditemactions.client.gui.ItemActionsTranslationHelper;
import net.p3pp3rf1y.sophisticateditemactions.common.IItemTransferExtension;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemMatchResult;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemTransferData;
import net.p3pp3rf1y.sophisticateditemactions.common.StorageItemHandlerTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BackpackItemTransferHandler implements IItemTransferExtension {
	public static final BackpackItemTransferHandler INSTANCE = new BackpackItemTransferHandler();

	private BackpackItemTransferHandler() {
	}

	@Override
	public boolean hasInventorySourceInScope(Player player, int minSlot, int maxSlot) {
		return !getBackpacksInScope(player, minSlot, maxSlot).isEmpty();
	}

	@Override
	public boolean shouldSkipRegularDepositFromSlot(Player player, int slot) {
		ItemStack stack = player.getInventory().getItem(slot);
		return stack.getItem() instanceof BackpackItem && hasDepositUpgrade(stack);
	}

	@Override
	public int depositFromInventorySources(Player player, int minSlot, int maxSlot, boolean onlyMatching, List<StorageItemHandlerTarget> storageTargets,
			Map<Vec3, ItemTransferData> inserted) {
		int transferredStackCount = 0;
		for (ItemStack backpack : getBackpacksInScope(player, minSlot, maxSlot)) {
			IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(backpack);
			List<DepositUpgradeWrapper> depositUpgrades = backpackWrapper.getUpgradeHandler().getWrappersThatImplement(DepositUpgradeWrapper.class);
			for (StorageItemHandlerTarget storageTarget : storageTargets) {
				IItemHandler filteredStorageHandler = new DepositFilteredItemHandler(storageTarget, onlyMatching);
				for (DepositUpgradeWrapper depositUpgrade : depositUpgrades) {
					List<ItemStack> transferredStacks = depositUpgrade.depositToHandler(filteredStorageHandler);
					transferredStackCount += transferredStacks.size();
					logTransferredStacks(inserted, storageTarget, transferredStacks);
				}
			}
		}
		return transferredStackCount;
	}

	@Override
	public Component getDepositMessage(int inventoryTransferred, int backpackTransferred) {
		Component inventoryCount = Component.literal(String.valueOf(inventoryTransferred)).withStyle(ChatFormatting.DARK_GREEN);
		Component backpackCount = Component.literal(String.valueOf(backpackTransferred)).withStyle(ChatFormatting.DARK_GREEN);
		return inventoryTransferred > 0
				? ItemActionsTranslationHelper.INSTANCE.translStatusMessage("deposited_items_from_inventory_and_backpacks", inventoryCount, backpackCount)
				: ItemActionsTranslationHelper.INSTANCE.translStatusMessage("deposited_items_from_backpacks", backpackCount);
	}

	@Override
	public int restockToInventorySources(Player player, int minSlot, int maxSlot, ItemStack filter, boolean fillEmpty,
			List<StorageItemHandlerTarget> storageTargets, Map<Vec3, ItemTransferData> restocked) {
		int transferredStackCount = 0;
		for (ItemStack backpack : getBackpacksInScope(player, minSlot, maxSlot)) {
			IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(backpack);
			List<RestockUpgradeWrapper> restockUpgrades = backpackWrapper.getUpgradeHandler().getWrappersThatImplement(RestockUpgradeWrapper.class);
			IItemHandler backpackInventory = backpackWrapper.getInventoryForUpgradeProcessing();
			for (StorageItemHandlerTarget storageTarget : storageTargets) {
				IItemHandler filteredStorageHandler = new RestockFilteredItemHandler(storageTarget.itemHandler(), backpackInventory, filter, fillEmpty);
				for (RestockUpgradeWrapper restockUpgrade : restockUpgrades) {
					List<ItemStack> transferredStacks = restockUpgrade.restockFromHandler(filteredStorageHandler);
					transferredStackCount += transferredStacks.size();
					logTransferredStacks(restocked, storageTarget, transferredStacks);
				}
			}
		}
		return transferredStackCount;
	}

	@Override
	public Component getRestockMessage(int inventoryTransferred, int backpackTransferred) {
		Component inventoryCount = Component.literal(String.valueOf(inventoryTransferred)).withStyle(ChatFormatting.DARK_GREEN);
		Component backpackCount = Component.literal(String.valueOf(backpackTransferred)).withStyle(ChatFormatting.DARK_GREEN);
		return inventoryTransferred > 0
				? ItemActionsTranslationHelper.INSTANCE.translStatusMessage("restocked_items_to_inventory_and_backpacks", inventoryCount, backpackCount)
				: ItemActionsTranslationHelper.INSTANCE.translStatusMessage("restocked_items_to_backpacks", backpackCount);
	}

	@Override
	public boolean hasRecipeInventorySource(Player player) {
		return getRecipeBackpacks(player).stream().anyMatch(backpack -> backpack.getUpgradeHandler().getWrappersThatImplement(RefillUpgradeWrapper.class)
				.stream().anyMatch(BackpackItemTransferHandler::isAdvancedRefill));
	}

	@Override
	public int restockRecipeItems(Player player, List<List<ItemStack>> ingredientOptions) {
		int restockedItemCount = 0;
		List<IBackpackWrapper> backpacks = getRecipeBackpacks(player);
		for (List<ItemStack> options : ingredientOptions) {
			restockedItemCount += restockRecipeIngredient(player, backpacks, options);
		}
		return restockedItemCount;
	}

	private static int restockRecipeIngredient(Player player, List<IBackpackWrapper> backpacks, List<ItemStack> options) {
		int remaining = options.getFirst().getCount();
		for (ItemStack filter : options) {
			remaining -= restockIngredientFromBackpacks(player, backpacks, filter, remaining);
			if (remaining == 0) {
				break;
			}
		}
		return options.getFirst().getCount() - remaining;
	}

	private static int restockIngredientFromBackpacks(Player player, List<IBackpackWrapper> backpacks, ItemStack filter, int count) {
		int restockedItemCount = 0;
		for (IBackpackWrapper backpack : backpacks) {
			restockedItemCount += restockIngredientFromBackpack(player, backpack, filter, count - restockedItemCount);
			if (restockedItemCount == count) {
				break;
			}
		}
		return restockedItemCount;
	}

	private static int restockIngredientFromBackpack(Player player, IBackpackWrapper backpack, ItemStack filter, int count) {
		int restockedItemCount = 0;
		for (RefillUpgradeWrapper refillUpgrade : backpack.getUpgradeHandler().getWrappersThatImplement(RefillUpgradeWrapper.class)) {
			if (isAdvancedRefill(refillUpgrade)) {
				restockedItemCount += restockIngredientFromAdvancedRefill(player, backpack, filter, count - restockedItemCount);
			}
			if (restockedItemCount == count) {
				break;
			}
		}
		return restockedItemCount;
	}

	private static int restockIngredientFromAdvancedRefill(Player player, IBackpackWrapper backpack, ItemStack filter, int count) {
		int restockedItemCount = 0;
		while (restockedItemCount < count) {
			int availableSpace = getAvailableInventorySpace(player, filter);
			if (availableSpace == 0) {
				return restockedItemCount;
			}

			ItemStack extracted = InventoryHelper.extractFromInventory(
					filter.copyWithCount(Math.min(Math.min(count - restockedItemCount, availableSpace), filter.getMaxStackSize())),
					backpack.getInventoryForUpgradeProcessing(), false);
			if (extracted.isEmpty()) {
				return restockedItemCount;
			}

			restockedItemCount += insertIntoPlayerInventory(player, extracted);
		}
		return restockedItemCount;
	}

	private static boolean isAdvancedRefill(RefillUpgradeWrapper refillUpgrade) {
		return refillUpgrade.getUpgradeStack().getItem() instanceof RefillUpgradeItem refillUpgradeItem && refillUpgradeItem.supportsBlockPick();
	}

	private static List<IBackpackWrapper> getRecipeBackpacks(Player player) {
		List<IBackpackWrapper> backpacks = new ArrayList<>();
		PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryHandlerName, identifier, slot) -> {
			backpacks.add(BackpackWrapper.fromStack(backpack));
			return false;
		});
		return backpacks;
	}

	private static int getAvailableInventorySpace(Player player, ItemStack stack) {
		int availableSpace = 0;
		for (int slot = 0; slot < 36; slot++) {
			ItemStack inventoryStack = player.getInventory().getItem(slot);
			if (inventoryStack.isEmpty()) {
				availableSpace += stack.getMaxStackSize();
			} else if (ItemStack.isSameItemSameComponents(inventoryStack, stack)) {
				availableSpace += inventoryStack.getMaxStackSize() - inventoryStack.getCount();
			}
		}
		return availableSpace;
	}

	private static int insertIntoPlayerInventory(Player player, ItemStack stack) {
		int originalCount = stack.getCount();
		for (int slot = 0; slot < 36 && !stack.isEmpty(); slot++) {
			ItemStack inventoryStack = player.getInventory().getItem(slot);
			if (ItemStack.isSameItemSameComponents(inventoryStack, stack)) {
				int countToAdd = Math.min(stack.getCount(), inventoryStack.getMaxStackSize() - inventoryStack.getCount());
				inventoryStack.grow(countToAdd);
				stack.shrink(countToAdd);
			}
		}
		for (int slot = 0; slot < 36 && !stack.isEmpty(); slot++) {
			if (player.getInventory().getItem(slot).isEmpty()) {
				int countToAdd = Math.min(stack.getCount(), stack.getMaxStackSize());
				player.getInventory().setItem(slot, stack.copyWithCount(countToAdd));
				stack.shrink(countToAdd);
			}
		}
		return originalCount - stack.getCount();
	}

	private static List<ItemStack> getBackpacksInScope(Player player, int minSlot, int maxSlot) {
		List<ItemStack> backpacks = new ArrayList<>();
		for (int slot = minSlot; slot < maxSlot; slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.getItem() instanceof BackpackItem) {
				backpacks.add(stack);
			}
		}

		if (minSlot == 0) {
			ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
			if (chestStack.getItem() instanceof BackpackItem) {
				backpacks.add(chestStack);
			}

			PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryHandlerName, identifier, slot) -> {
				if (!inventoryHandlerName.equals(PlayerInventoryProvider.MAIN_INVENTORY)
						&& !inventoryHandlerName.equals(PlayerInventoryProvider.OFFHAND_INVENTORY)
						&& !inventoryHandlerName.equals(PlayerInventoryProvider.ARMOR_INVENTORY)) {
					backpacks.add(backpack);
				}
				return false;
			});
		}

		return backpacks;
	}

	private static boolean hasDepositUpgrade(ItemStack backpack) {
		IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(backpack);
		return !backpackWrapper.getUpgradeHandler().getWrappersThatImplement(DepositUpgradeWrapper.class).isEmpty();
	}

	private static void logTransferredStacks(Map<Vec3, ItemTransferData> transferData, StorageItemHandlerTarget storageTarget,
			List<ItemStack> transferredStacks) {
		if (transferredStacks.isEmpty()) {
			return;
		}

		transferData
				.computeIfAbsent(storageTarget.position(),
						k -> new ItemTransferData(storageTarget.positionToOpen(), storageTarget.position(), new ArrayList<>()))
				.itemsTransferred().addAll(transferredStacks);
	}

	private static class DepositFilteredItemHandler implements IItemHandlerSimpleInserter {
		private final StorageItemHandlerTarget storageTarget;
		private final IItemHandler storageHandler;
		private final boolean onlyMatching;

		private DepositFilteredItemHandler(StorageItemHandlerTarget storageTarget, boolean onlyMatching) {
			this.storageTarget = storageTarget;
			storageHandler = storageTarget.itemHandler();
			this.onlyMatching = onlyMatching;
		}

		@Override
		public ItemStack insertItem(ItemStack stack, boolean simulate) {
			if (!allowsInsert(stack)) {
				return stack;
			}
			return storageHandler instanceof IItemHandlerSimpleInserter simpleInserter
					? simpleInserter.insertItem(stack, simulate)
					: InventoryHelper.insertIntoInventoryMatchingFirst(stack, storageHandler, simulate);
		}

		@Override
		public int getSlots() {
			return storageHandler.getSlots();
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return storageHandler.getStackInSlot(slot);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return allowsInsert(stack) ? storageHandler.insertItem(slot, stack, simulate) : stack;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return storageHandler.extractItem(slot, amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return storageHandler.getSlotLimit(slot);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return allowsInsert(stack) && storageHandler.isItemValid(slot, stack);
		}

		@Override
		public void setStackInSlot(int slot, ItemStack stack) {
			if (storageHandler instanceof IItemHandlerSimpleInserter simpleInserter) {
				simpleInserter.setStackInSlot(slot, stack);
				return;
			}
			throw new UnsupportedOperationException("setStackInSlot is not supported");
		}

		private boolean allowsInsert(ItemStack stack) {
			ItemMatchResult matchResult = storageTarget.itemMatcher().apply(ItemStackKey.of(stack));
			return matchResult == ItemMatchResult.MATCHING_STACK || matchResult == ItemMatchResult.MATCHING_ITEM
					|| (!onlyMatching && matchResult == ItemMatchResult.NO_MATCH);
		}
	}

	private static class RestockFilteredItemHandler implements IItemHandler {
		private final IItemHandler storageHandler;
		private final IItemHandler backpackInventory;
		private final ItemStack filter;
		private final boolean fillEmpty;

		private RestockFilteredItemHandler(IItemHandler storageHandler, IItemHandler backpackInventory, ItemStack filter, boolean fillEmpty) {
			this.storageHandler = storageHandler;
			this.backpackInventory = backpackInventory;
			this.filter = filter;
			this.fillEmpty = fillEmpty;
		}

		@Override
		public int getSlots() {
			return storageHandler.getSlots();
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return storageHandler.getStackInSlot(slot);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return storageHandler.insertItem(slot, stack, simulate);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			ItemStack stack = storageHandler.getStackInSlot(slot);
			return allowsExtract(stack) ? storageHandler.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return storageHandler.getSlotLimit(slot);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return storageHandler.isItemValid(slot, stack);
		}

		private boolean allowsExtract(ItemStack stack) {
			if (stack.isEmpty()) {
				return false;
			}

			if (fillEmpty && !filter.isEmpty() && !(filter.getItem() instanceof BackpackItem) && ItemStack.isSameItemSameComponents(stack, filter)) {
				return true;
			}

			ItemStackKey stackKey = ItemStackKey.of(stack);
			for (int slot = 0; slot < backpackInventory.getSlots(); slot++) {
				ItemStack backpackStack = backpackInventory.getStackInSlot(slot);
				if (!backpackStack.isEmpty() && stackKey.matches(backpackStack)) {
					return true;
				}
			}
			return false;
		}
	}
}
