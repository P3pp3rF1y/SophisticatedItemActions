package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.BlockHighlightGroups;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface IBlockEntityItemActionHandler<T> extends IBlockItemActionHandler {
	@Override
	default boolean canActOn(Level level, BlockPos pos, BlockEntity blockEntity) {
		return getObjectClass().isInstance(blockEntity);
	}

	@Override
	default BlockPos getInteractionPosToActOn(Level level, BlockPos pos, BlockEntity blockEntity, Action action) {
		if (!getObjectClass().isInstance(blockEntity)) {
			return pos;
		}
		T object = getObjectClass().cast(blockEntity);
		return action == Action.HIGHLIGHT ? getCanonicalHighlightPos(pos, object) : getPosToActOn(pos, object, action);
	}

	@Override
	default BlockPos getInteractionPosToActOn(BlockPos pos, BlockEntity blockEntity) {
		return getObjectClass().isInstance(blockEntity) ? getDepositPosToActOn(pos, getObjectClass().cast(blockEntity)) : pos;
	}

	default BlockPos getPosToActOn(BlockPos pos, T blockEntity, Action action) {
		return getDepositPosToActOn(pos, blockEntity);
	}

	default BlockPos getDepositPosToActOn(BlockPos pos, T blockEntity) {
		return pos;
	}

	default BlockPos getCanonicalHighlightPos(BlockPos pos, T blockEntity) {
		return BlockHighlightGroups.getCanonicalHighlightPos(((BlockEntity) blockEntity).getLevel(), pos);
	}

	@Override
	default List<BlockPos> getHighlightPositions(ServerPlayer player, BlockPos pos) {
		return getFromBlockEntity(player, pos, this::getHighlightPositions).orElse(List.of(pos));
	}

	default List<BlockPos> getHighlightPositions(T object) {
		return BlockHighlightGroups.getHighlightPositions(((BlockEntity) object).getLevel(), ((BlockEntity) object).getBlockPos());
	}

	@Override
	default ItemMatchResult getItemMatch(ServerPlayer player, ItemStackKey stackKey, BlockPos pos, Action action) {
		return getFromBlockEntity(player, pos, be -> getItemMatch(stackKey, be, action)).orElse(ItemMatchResult.NO_MATCH);
	}

	default <R> Optional<R> getFromBlockEntity(ServerPlayer player, BlockPos pos, Function<T, R> getter) {
		return WorldHelper.getBlockEntity(SubLevelCompatHelper.getLevelForPosition(player.level(), pos), pos, getObjectClass()).map(getter);
	}

	@Override
	default Optional<IDepositHandler> getDepositHandler(ServerPlayer player, BlockPos pos) {
		return getFromBlockEntity(player, pos, this::getDepositHandler);
	}

	IDepositHandler getDepositHandler(T object);

	@Override
	default Optional<IRestockHandler> getRestockHandler(ServerPlayer player, BlockPos pos) {
		return getFromBlockEntity(player, pos, this::getRestockHandler);
	}

	IRestockHandler getRestockHandler(T object);

	@Override
	default Optional<StorageItemHandlerTarget> getStorageItemHandlerTarget(ServerPlayer player, BlockPos pos) {
		return getFromBlockEntity(player, pos, object -> getStorageItemHandlerTarget(object).orElse(null));
	}

	@Override
	default List<StorageItemHandlerTarget> getStorageItemHandlerTargets(ServerPlayer player, BlockPos pos) {
		return getStorageItemHandlerTarget(player, pos).map(List::of).orElseGet(List::of);
	}

	default Optional<StorageItemHandlerTarget> getStorageItemHandlerTarget(T object) {
		return Optional.empty();
	}

	ItemMatchResult getItemMatch(ItemStackKey stackKey, T object, Action action);

	Class<T> getObjectClass();
}
