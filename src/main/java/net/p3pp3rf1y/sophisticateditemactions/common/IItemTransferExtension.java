package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

public interface IItemTransferExtension {
	boolean hasInventorySourceInScope(Player player, int minSlot, int maxSlot);

	boolean shouldSkipRegularDepositFromSlot(Player player, int slot);

	int depositFromInventorySources(Player player, int minSlot, int maxSlot, boolean onlyMatching, List<StorageItemHandlerTarget> storageTargets,
			Map<Vec3, ItemTransferData> inserted);

	Component getDepositMessage(int inventoryTransferred, int extensionTransferred);

	int restockToInventorySources(Player player, int minSlot, int maxSlot, ItemStack filter, boolean fillEmpty, List<StorageItemHandlerTarget> storageTargets,
			Map<Vec3, ItemTransferData> restocked);

	Component getRestockMessage(int inventoryTransferred, int extensionTransferred);

	boolean hasRecipeInventorySource(Player player);

	int restockRecipeItems(Player player, List<List<ItemStack>> ingredientOptions);
}
