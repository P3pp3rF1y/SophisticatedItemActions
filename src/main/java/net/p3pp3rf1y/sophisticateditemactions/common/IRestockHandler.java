package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public interface IRestockHandler {
	Optional<BlockPos> getPositionToOpen();
	Vec3 getPosition();
	ItemStack extractItem(ItemStack stack);

	default List<RestockTransfer> extractTransfers(ItemStack stack) {
		ItemStack extracted = extractItem(stack);
		return extracted.isEmpty() ? List.of() : List.of(new RestockTransfer(getPositionToOpen().orElse(null), getPosition(), extracted));
	}

	record RestockTransfer(@Nullable BlockPos positionToOpen, Vec3 position, ItemStack stack) {
	}
}
