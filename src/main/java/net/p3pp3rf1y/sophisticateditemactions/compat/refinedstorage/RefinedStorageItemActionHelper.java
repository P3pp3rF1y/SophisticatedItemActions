package net.p3pp3rf1y.sophisticateditemactions.compat.refinedstorage;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemMatchResult;

public final class RefinedStorageItemActionHelper {
	private RefinedStorageItemActionHelper() {
	}

	public static ItemMatchResult getItemMatch(ItemStackKey stackKey, Storage storage) {
		ItemMatchResult result = ItemMatchResult.NO_MATCH;

		for (ResourceAmount resourceAmount : storage.getAll()) {
			if (!(resourceAmount.resource() instanceof ItemResource itemResource) || resourceAmount.amount() <= 0) {
				continue;
			}

			ItemStack storedStack = itemResource.toItemStack();
			if (stackKey.matches(storedStack)) {
				return ItemMatchResult.MATCHING_STACK;
			}

			if (storedStack.getItem() == stackKey.stack().getItem()) {
				result = ItemMatchResult.MATCHING_ITEM;
			}
		}

		return result;
	}

	public static int insertItem(Storage storage, ServerPlayer player, ItemStack stack) {
		ItemResource resource = ItemResource.ofItemStack(stack);
		long inserted = storage.insert(resource, stack.getCount(), Action.EXECUTE, new PlayerActor(player));
		return (int) Math.max(0, inserted);
	}

	public static int extractItem(Storage storage, ServerPlayer player, ItemStack stack) {
		ItemResource resource = ItemResource.ofItemStack(stack);
		long extracted = storage.extract(resource, stack.getCount(), Action.EXECUTE, new PlayerActor(player));
		return (int) Math.max(0, extracted);
	}
}
