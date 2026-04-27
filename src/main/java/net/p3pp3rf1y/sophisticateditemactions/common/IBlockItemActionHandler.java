package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.BlockHighlightGroups;

import java.util.List;
import java.util.Optional;

public interface IBlockItemActionHandler {
	ResourceLocation id();

	boolean canActOn(Level level, BlockPos pos, BlockEntity blockEntity);

	default BlockPos getInteractionPosToActOn(BlockPos pos, BlockEntity blockEntity) {
		return pos;
	}

	default BlockPos getInteractionPosToActOn(Level level, BlockPos pos, BlockEntity blockEntity, Action action) {
		if (action == Action.HIGHLIGHT) {
			return BlockHighlightGroups.getCanonicalHighlightPos(level, pos);
		}
		return getInteractionPosToActOn(pos, blockEntity);
	}

	default List<BlockPos> getHighlightPositions(ServerPlayer player, BlockPos pos) {
		return BlockHighlightGroups.getHighlightPositions(SubLevelCompatHelper.getLevelForPosition(player.level(), pos), pos);
	}

	ItemMatchResult getItemMatch(ServerPlayer player, ItemStackKey stackKey, BlockPos pos, Action action);

	Optional<IDepositHandler> getDepositHandler(ServerPlayer player, BlockPos pos);

	Optional<IRestockHandler> getRestockHandler(ServerPlayer player, BlockPos pos);

	enum Action {
		DEPOSIT, RESTOCK, HIGHLIGHT
	}
}
