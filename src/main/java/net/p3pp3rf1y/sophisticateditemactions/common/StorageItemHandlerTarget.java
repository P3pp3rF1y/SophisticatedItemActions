package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public record StorageItemHandlerTarget(@Nullable BlockPos positionToOpen, Vec3 position, ResourceHandler<ItemResource> itemHandler,
		Function<ItemStackKey, ItemMatchResult> itemMatcher) {
	public StorageItemHandlerTarget(@Nullable BlockPos positionToOpen, Vec3 position, ResourceHandler<ItemResource> itemHandler) {
		this(positionToOpen, position, itemHandler, stackKey -> getItemMatch(stackKey, itemHandler));
	}

	private static ItemMatchResult getItemMatch(ItemStackKey stackKey, ResourceHandler<ItemResource> itemHandler) {
		AtomicReference<ItemMatchResult> matchResult = new AtomicReference<>(ItemMatchResult.NO_MATCH);
		InventoryHelper.iterate(itemHandler, (slot, resource, amount) -> {
			if (resource.isEmpty()) {
				return;
			}
			if (resource.matches(stackKey.stack())) {
				matchResult.set(ItemMatchResult.MATCHING_STACK);
			} else if (resource.getItem() == stackKey.stack().getItem()) {
				matchResult.set(ItemMatchResult.MATCHING_ITEM);
			}
		}, () -> matchResult.get() == ItemMatchResult.MATCHING_STACK);
		return matchResult.get();
	}
}
