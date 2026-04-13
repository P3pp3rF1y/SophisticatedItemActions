package net.p3pp3rf1y.sophisticateditemactions.compat.refinedstorage;

import com.refinedmods.refinedstorage.api.storage.IStorage;
import com.refinedmods.refinedstorage.api.storage.cache.IStorageCache;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.api.util.IComparer;
import com.refinedmods.refinedstorage.api.util.StackListEntry;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemMatchResult;

public final class RefinedStorageItemActionHelper {
	private RefinedStorageItemActionHelper() {
	}

	public static ItemMatchResult getItemMatch(ItemStackKey stackKey, IStorageCache<ItemStack> storageCache) {
		ItemMatchResult result = ItemMatchResult.NO_MATCH;

		for (StackListEntry<ItemStack> entry : storageCache.getList().getStacks()) {
			ItemStack storedStack = entry.getStack();
			if (storedStack.isEmpty()) {
				continue;
			}

			if (stackKey.matches(storedStack)) {
				return ItemMatchResult.MATCHING_STACK;
			}

			if (storedStack.getItem() == stackKey.stack().getItem()) {
				result = ItemMatchResult.MATCHING_ITEM;
			}
		}

		return result;
	}

	public static ItemStack insertItem(IStorage<ItemStack> storage, ItemStack stack) {
		return storage.insert(stack.copy(), stack.getCount(), Action.PERFORM);
	}

	public static ItemStack extractItem(IStorage<ItemStack> storage, ItemStack stack) {
		return storage.extract(stack.copy(), stack.getCount(), IComparer.COMPARE_NBT, Action.PERFORM);
	}
}
