package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;

import java.util.List;
import java.util.Optional;

public interface IEntityItemActionHandler {
	Identifier id();

	boolean canActOn(Entity entity);

	ItemMatchResult getItemMatch(ItemStackKey stackKey, Entity entity);

	Optional<IDepositHandler> getDepositHandler(Entity entity);

	Optional<IRestockHandler> getRestockHandler(Entity entity);

	default Optional<StorageItemHandlerTarget> getStorageItemHandlerTarget(Entity entity) {
		return Optional.empty();
	}

	default List<StorageItemHandlerTarget> getStorageItemHandlerTargets(Entity entity) {
		return getStorageItemHandlerTarget(entity).map(List::of).orElseGet(List::of);
	}

	default Optional<List<BlockPos>> getCustomHighlightPositions(ItemStackKey stackKey, Entity entity) {
		return Optional.empty();
	}

	default Optional<List<List<BlockPos>>> getCustomHighlightPositionGroups(ItemStackKey stackKey, Entity entity) {
		return getCustomHighlightPositions(stackKey, entity).map(List::of);
	}

	default Optional<List<HighlightGroup>> getCustomHighlightGroupsWithMatch(ItemStackKey stackKey, Entity entity) {
		return getCustomHighlightPositionGroups(stackKey, entity)
				.map(groups -> groups.stream().map(group -> new HighlightGroup(group, getItemMatch(stackKey, entity))).toList());
	}

	default Optional<List<BlockPos>> getCustomRenderedHighlightPositions(ItemStackKey stackKey, Entity entity) {
		return Optional.empty();
	}

	default Optional<List<List<BlockPos>>> getCustomRenderedHighlightGroups(ItemStackKey stackKey, Entity entity) {
		return getCustomRenderedHighlightPositions(stackKey, entity).map(List::of);
	}

	default Optional<List<HighlightGroup>> getCustomRenderedHighlightGroupsWithMatch(ItemStackKey stackKey, Entity entity) {
		return getCustomRenderedHighlightGroups(stackKey, entity)
				.map(groups -> groups.stream().map(group -> new HighlightGroup(group, getItemMatch(stackKey, entity))).toList());
	}

	record HighlightGroup(List<BlockPos> positions, ItemMatchResult matchResult) {
	}
}
