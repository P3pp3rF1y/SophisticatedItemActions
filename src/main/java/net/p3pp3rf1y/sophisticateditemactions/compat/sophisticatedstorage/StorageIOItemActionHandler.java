package net.p3pp3rf1y.sophisticateditemactions.compat.sophisticatedstorage;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;
import net.p3pp3rf1y.sophisticateditemactions.common.IBlockItemActionHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.IDepositHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.IRestockHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemMatchResult;
import net.p3pp3rf1y.sophisticateditemactions.common.StandardStorageActionHandler;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageIOBlockEntity;

import java.util.Optional;

public class StorageIOItemActionHandler implements IBlockItemActionHandler {
	public static final StorageIOItemActionHandler INSTANCE = new StorageIOItemActionHandler();
	private static final Identifier ID = SophisticatedItemActions.getIdentifier("sophisticated_storage_io");

	@Override
	public Identifier id() {
		return ID;
	}

	@Override
	public boolean canActOn(Level level, BlockPos pos, BlockEntity blockEntity) {
		return blockEntity instanceof StorageIOBlockEntity;
	}

	@Override
	public ItemMatchResult getItemMatch(ServerPlayer player, ItemStackKey stackKey, BlockPos pos, Action action) {
		if (action == Action.HIGHLIGHT) {
			return ItemMatchResult.NO_MATCH;
		}
		return StandardStorageActionHandler.INSTANCE.getItemMatch(player, stackKey, pos, action);
	}

	@Override
	public Optional<IDepositHandler> getDepositHandler(ServerPlayer player, BlockPos pos) {
		return StandardStorageActionHandler.INSTANCE.getDepositHandler(player, pos);
	}

	@Override
	public Optional<IRestockHandler> getRestockHandler(ServerPlayer player, BlockPos pos) {
		return StandardStorageActionHandler.INSTANCE.getRestockHandler(player, pos);
	}
}
