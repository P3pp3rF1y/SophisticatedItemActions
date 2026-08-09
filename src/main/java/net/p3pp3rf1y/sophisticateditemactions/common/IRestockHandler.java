package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public interface IRestockHandler {
	Optional<BlockPos> getPositionToOpen();
	Vec3 getPosition();
	int extractItem(ItemStack stack);

	/**
	 * Allows enumerable inventories to cache their available item types before alternatives are probed.
	 */
	default void prepareForAlternativeRestock(List<ItemStack> filters) {
	}

	default List<RestockTransfer> extractTransfers(ItemStack stack) {
		int extracted = extractItem(stack);
		return extracted <= 0 ? List.of() : List.of(new RestockTransfer(getPositionToOpen().orElse(null), getPosition(), stack.copyWithCount(extracted)));
	}

	record RestockTransfer(@Nullable BlockPos positionToOpen, Vec3 position, ItemStack stack) {
	}
}
