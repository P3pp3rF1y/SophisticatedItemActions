package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;

import java.util.List;
import java.util.Optional;

public interface IEntityItemActionHandler {
	ResourceLocation id();

	boolean canActOn(Entity entity);

	ItemMatchResult getItemMatch(ItemStackKey stackKey, Entity entity);

	default Optional<List<BlockPos>> getCustomHighlightPositions(ItemStackKey stackKey, Entity entity) {
		return Optional.empty();
	}

	default Optional<List<BlockPos>> getCustomRenderedHighlightPositions(ItemStackKey stackKey, Entity entity) {
		return Optional.empty();
	}

	Optional<IDepositHandler> getDepositHandler(Entity entity);

	Optional<IRestockHandler> getRestockHandler(Entity entity);
}
