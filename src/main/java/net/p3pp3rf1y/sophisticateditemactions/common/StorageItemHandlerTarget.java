package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;

import javax.annotation.Nullable;
import java.util.function.Function;

public record StorageItemHandlerTarget(@Nullable BlockPos positionToOpen, Vec3 position, IItemHandler itemHandler, Function<ItemStackKey, ItemMatchResult> itemMatcher) {
	public StorageItemHandlerTarget(@Nullable BlockPos positionToOpen, Vec3 position, IItemHandler itemHandler) {
		this(positionToOpen, position, itemHandler, stackKey -> getItemMatch(stackKey, itemHandler));
	}

	private static ItemMatchResult getItemMatch(ItemStackKey stackKey, IItemHandler itemHandler) {
		ItemMatchResult matchResult = ItemMatchResult.NO_MATCH;
		for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
			ItemStack slotStack = itemHandler.getStackInSlot(slot);
			if (slotStack.isEmpty()) {
				continue;
			}

			if (stackKey.matches(slotStack)) {
				return ItemMatchResult.MATCHING_STACK;
			}
			if (slotStack.getItem() == stackKey.stack().getItem()) {
				matchResult = ItemMatchResult.MATCHING_ITEM;
			}
		}
		return matchResult;
	}
}
