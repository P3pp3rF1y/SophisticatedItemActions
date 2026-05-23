package net.p3pp3rf1y.sophisticateditemactions.compat.sophisticatedbackpacks;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
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

	private BackpackItemTransferHandler() {}

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
	public int depositFromInventorySources(Player player, int minSlot, int maxSlot, boolean onlyMatching, List<StorageItemHandlerTarget> storageTargets, Map<Vec3, ItemTransferData> inserted) {
		int transferredStackCount = 0;
		for (ItemStack backpack : getBackpacksInScope(player, minSlot, maxSlot)) {
			IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(backpack);
			List<DepositUpgradeWrapper> depositUpgrades = backpackWrapper.getUpgradeHandler().getWrappersThatImplement(DepositUpgradeWrapper.class);
			for (StorageItemHandlerTarget storageTarget : storageTargets) {
				ResourceHandler<ItemResource> filteredStorageHandler = new DepositFilteredResourceHandler(storageTarget, onlyMatching);
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
		return inventoryTransferred > 0 ?
				ItemActionsTranslationHelper.INSTANCE.translStatusMessage("deposited_items_from_inventory_and_backpacks", inventoryCount, backpackCount) :
				ItemActionsTranslationHelper.INSTANCE.translStatusMessage("deposited_items_from_backpacks", backpackCount);
	}

	@Override
	public int restockToInventorySources(Player player, int minSlot, int maxSlot, ItemStack filter, boolean fillEmpty, List<StorageItemHandlerTarget> storageTargets, Map<Vec3, ItemTransferData> restocked) {
		int transferredStackCount = 0;
		for (ItemStack backpack : getBackpacksInScope(player, minSlot, maxSlot)) {
			IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(backpack);
			List<RestockUpgradeWrapper> restockUpgrades = backpackWrapper.getUpgradeHandler().getWrappersThatImplement(RestockUpgradeWrapper.class);
			ResourceHandler<ItemResource> backpackInventory = backpackWrapper.getInventoryForUpgradeProcessing();
			for (StorageItemHandlerTarget storageTarget : storageTargets) {
				ResourceHandler<ItemResource> filteredStorageHandler = new RestockFilteredResourceHandler(storageTarget.itemHandler(), backpackInventory, filter, fillEmpty);
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
		return inventoryTransferred > 0 ?
				ItemActionsTranslationHelper.INSTANCE.translStatusMessage("restocked_items_to_inventory_and_backpacks", inventoryCount, backpackCount) :
				ItemActionsTranslationHelper.INSTANCE.translStatusMessage("restocked_items_to_backpacks", backpackCount);
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
				if (!inventoryHandlerName.equals(PlayerInventoryProvider.MAIN_INVENTORY) && !inventoryHandlerName.equals(PlayerInventoryProvider.OFFHAND_INVENTORY) && !inventoryHandlerName.equals(PlayerInventoryProvider.ARMOR_INVENTORY)) {
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

	private static void logTransferredStacks(Map<Vec3, ItemTransferData> transferData, StorageItemHandlerTarget storageTarget, List<ItemStack> transferredStacks) {
		if (transferredStacks.isEmpty()) {
			return;
		}

		transferData.computeIfAbsent(storageTarget.position(), k -> new ItemTransferData(storageTarget.positionToOpen(), storageTarget.position(), new ArrayList<>()))
				.itemsTransferred().addAll(transferredStacks);
	}

	private static class DepositFilteredResourceHandler implements ResourceHandler<ItemResource> {
		private final StorageItemHandlerTarget storageTarget;
		private final ResourceHandler<ItemResource> storageHandler;
		private final boolean onlyMatching;

		private DepositFilteredResourceHandler(StorageItemHandlerTarget storageTarget, boolean onlyMatching) {
			this.storageTarget = storageTarget;
			storageHandler = storageTarget.itemHandler();
			this.onlyMatching = onlyMatching;
		}

		@Override
		public int size() {
			return storageHandler.size();
		}

		@Override
		public ItemResource getResource(int index) {
			return storageHandler.getResource(index);
		}

		@Override
		public long getAmountAsLong(int index) {
			return storageHandler.getAmountAsLong(index);
		}

		@Override
		public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
			return allowsInsert(resource) ? storageHandler.insert(index, resource, amount, transaction) : 0;
		}

		@Override
		public int insert(ItemResource resource, int amount, TransactionContext transaction) {
			if (!allowsInsert(resource)) {
				return 0;
			}

			int inserted = 0;
			for (int index = 0; index < storageHandler.size(); index++) {
				ItemResource slotResource = storageHandler.getResource(index);
				if (!slotResource.isEmpty() && slotResource.equals(resource)) {
					inserted += storageHandler.insert(index, resource, amount - inserted, transaction);
				}
				if (inserted == amount) {
					return inserted;
				}
			}

			for (int index = 0; index < storageHandler.size(); index++) {
				if (storageHandler.getResource(index).isEmpty()) {
					inserted += storageHandler.insert(index, resource, amount - inserted, transaction);
				}
				if (inserted == amount) {
					return inserted;
				}
			}

			return inserted;
		}

		@Override
		public int extract(ItemResource resource, int amount, TransactionContext transaction) {
			return storageHandler.extract(resource, amount, transaction);
		}

		@Override
		public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
			return storageHandler.extract(index, resource, amount, transaction);
		}

		@Override
		public long getCapacityAsLong(int index, ItemResource resource) {
			return storageHandler.getCapacityAsLong(index, resource);
		}

		@Override
		public boolean isValid(int index, ItemResource resource) {
			return allowsInsert(resource) && storageHandler.isValid(index, resource);
		}

		private boolean allowsInsert(ItemResource resource) {
			ItemMatchResult matchResult = storageTarget.itemMatcher().apply(ItemStackKey.of(resource.toStack()));
			return matchResult == ItemMatchResult.MATCHING_STACK || matchResult == ItemMatchResult.MATCHING_ITEM || (!onlyMatching && matchResult == ItemMatchResult.NO_MATCH);
		}
	}

	private static class RestockFilteredResourceHandler implements ResourceHandler<ItemResource> {
		private final ResourceHandler<ItemResource> storageHandler;
		private final ResourceHandler<ItemResource> backpackInventory;
		private final ItemStack filter;
		private final boolean fillEmpty;

		private RestockFilteredResourceHandler(ResourceHandler<ItemResource> storageHandler, ResourceHandler<ItemResource> backpackInventory, ItemStack filter, boolean fillEmpty) {
			this.storageHandler = storageHandler;
			this.backpackInventory = backpackInventory;
			this.filter = filter;
			this.fillEmpty = fillEmpty;
		}

		@Override
		public int size() {
			return storageHandler.size();
		}

		@Override
		public ItemResource getResource(int index) {
			ItemResource resource = storageHandler.getResource(index);
			return allowsExtract(resource) ? resource : ItemResource.of(ItemStack.EMPTY);
		}

		@Override
		public long getAmountAsLong(int index) {
			return allowsExtract(storageHandler.getResource(index)) ? storageHandler.getAmountAsLong(index) : 0;
		}

		@Override
		public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
			return storageHandler.insert(index, resource, amount, transaction);
		}

		@Override
		public int insert(ItemResource resource, int amount, TransactionContext transaction) {
			return storageHandler.insert(resource, amount, transaction);
		}

		@Override
		public int extract(ItemResource resource, int amount, TransactionContext transaction) {
			return allowsExtract(resource) ? storageHandler.extract(resource, amount, transaction) : 0;
		}

		@Override
		public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
			return allowsExtract(storageHandler.getResource(index)) ? storageHandler.extract(index, resource, amount, transaction) : 0;
		}

		@Override
		public long getCapacityAsLong(int index, ItemResource resource) {
			return storageHandler.getCapacityAsLong(index, resource);
		}

		@Override
		public boolean isValid(int index, ItemResource resource) {
			return storageHandler.isValid(index, resource);
		}

		private boolean allowsExtract(ItemResource resource) {
			if (resource.isEmpty()) {
				return false;
			}

			ItemStack stack = resource.toStack();
			if (fillEmpty && !filter.isEmpty() && !(filter.getItem() instanceof BackpackItem) && ItemStack.isSameItemSameComponents(stack, filter)) {
				return true;
			}

			ItemStackKey stackKey = ItemStackKey.of(stack);
			return InventoryHelper.iterate(backpackInventory, (slot, backpackResource, amount) -> !backpackResource.isEmpty() && stackKey.matches(backpackResource.toStack(amount)), () -> false, Boolean.TRUE::equals);
		}
	}
}
