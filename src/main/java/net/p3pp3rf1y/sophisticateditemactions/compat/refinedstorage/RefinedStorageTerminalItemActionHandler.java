package net.p3pp3rf1y.sophisticateditemactions.compat.refinedstorage;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.network.security.ISecurityManager;
import com.refinedmods.refinedstorage.api.network.security.Permission;
import com.refinedmods.refinedstorage.api.storage.cache.IStorageCache;
import com.refinedmods.refinedstorage.blockentity.grid.GridBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;
import net.p3pp3rf1y.sophisticateditemactions.common.IBlockEntityItemActionHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.IBlockItemActionHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.IDepositHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.IRestockHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemMatchResult;

import java.util.Optional;

public class RefinedStorageTerminalItemActionHandler implements IBlockEntityItemActionHandler<GridBlockEntity> {
	public static final RefinedStorageTerminalItemActionHandler INSTANCE = new RefinedStorageTerminalItemActionHandler();
	public static final ResourceLocation ID = SophisticatedItemActions.getRL("refinedstorage_terminal");

	@Override
	public ResourceLocation id() {
		return ID;
	}

	@Override
	public Class<GridBlockEntity> getObjectClass() {
		return GridBlockEntity.class;
	}

	@Override
	public ItemMatchResult getItemMatch(ServerPlayer player, ItemStackKey stackKey, BlockPos pos, IBlockItemActionHandler.Action action) {
		return getFromBlockEntity(player, pos, grid -> canPlayerUse(grid, player, action)
				? getItemMatch(stackKey, grid, action)
				: ItemMatchResult.NO_MATCH).orElse(ItemMatchResult.NO_MATCH);
	}

	@Override
	public ItemMatchResult getItemMatch(ItemStackKey stackKey, GridBlockEntity grid, Action action) {
		return getStorageCache(grid).map(storageCache -> RefinedStorageItemActionHelper.getItemMatch(stackKey, storageCache)).orElse(ItemMatchResult.NO_MATCH);
	}

	@Override
	public IDepositHandler getDepositHandler(GridBlockEntity grid) {
		throw new UnsupportedOperationException("Use server player aware deposit handler lookup");
	}

	@Override
	public Optional<IDepositHandler> getDepositHandler(ServerPlayer player, BlockPos pos) {
		return getFromBlockEntity(player, pos, grid -> createDepositHandler(player, grid)).flatMap(handler -> handler);
	}

	@Override
	public IRestockHandler getRestockHandler(GridBlockEntity grid) {
		throw new UnsupportedOperationException("Use server player aware restock handler lookup");
	}

	@Override
	public Optional<IRestockHandler> getRestockHandler(ServerPlayer player, BlockPos pos) {
		return getFromBlockEntity(player, pos, grid -> createRestockHandler(player, grid)).flatMap(handler -> handler);
	}

	private Optional<IDepositHandler> createDepositHandler(ServerPlayer player, GridBlockEntity grid) {
		return getNetwork(grid)
				.filter(network -> hasPermission(network, Permission.INSERT, player))
				.map(network -> new IDepositHandler() {
					@Override
					public Optional<BlockPos> getPositionToOpen() {
						return Optional.of(grid.getBlockPos());
					}

					@Override
					public Vec3 getPosition() {
						return Vec3.atCenterOf(grid.getBlockPos());
					}

					@Override
					public ItemMatchResult getItemMatch(ItemStackKey stackKey) {
						return RefinedStorageTerminalItemActionHandler.this.getItemMatch(stackKey, grid, Action.DEPOSIT);
					}

					@Override
					public ItemStack insertItem(ItemStack stack) {
						return network.insertItem(stack.copy(), stack.getCount(), com.refinedmods.refinedstorage.api.util.Action.PERFORM);
					}
				});
	}

	private Optional<IRestockHandler> createRestockHandler(ServerPlayer player, GridBlockEntity grid) {
		return getNetwork(grid)
				.filter(network -> hasPermission(network, Permission.EXTRACT, player))
				.map(network -> new IRestockHandler() {
					@Override
					public Optional<BlockPos> getPositionToOpen() {
						return Optional.of(grid.getBlockPos());
					}

					@Override
					public Vec3 getPosition() {
						return Vec3.atCenterOf(grid.getBlockPos());
					}

					@Override
					public ItemStack extractItem(ItemStack stack) {
						return network.extractItem(stack.copy(), stack.getCount(), com.refinedmods.refinedstorage.api.util.Action.PERFORM);
					}
				});
	}

	private static Optional<IStorageCache<ItemStack>> getStorageCache(GridBlockEntity grid) {
		return getNetwork(grid).map(INetwork::getItemStorageCache);
	}

	private static Optional<INetwork> getNetwork(GridBlockEntity grid) {
		return Optional.ofNullable(grid.getNode().getNetwork());
	}

	private static boolean canPlayerUse(GridBlockEntity grid, ServerPlayer player, Action action) {
		return getNetwork(grid).map(network -> switch (action) {
			case DEPOSIT -> hasPermission(network, Permission.INSERT, player);
			case RESTOCK -> hasPermission(network, Permission.EXTRACT, player);
			case HIGHLIGHT -> hasPermission(network, Permission.INSERT, player) || hasPermission(network, Permission.EXTRACT, player);
		}).orElse(false);
	}

	private static boolean hasPermission(INetwork network, Permission permission, ServerPlayer player) {
		ISecurityManager securityManager = network.getSecurityManager();
		return securityManager == null || securityManager.hasPermission(permission, player);
	}
}
