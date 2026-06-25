package net.p3pp3rf1y.sophisticateditemactions.compat.refinedstorage;

import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.node.NetworkNode;
import com.refinedmods.refinedstorage.api.network.security.Permission;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.common.api.security.PlatformSecurityNetworkComponent;
import com.refinedmods.refinedstorage.common.grid.AbstractGridBlockEntity;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;
import net.p3pp3rf1y.sophisticateditemactions.common.*;
import net.p3pp3rf1y.sophisticateditemactions.mixin.AccessorAbstractGridBlockEntity;

import java.util.Optional;

public class RefinedStorageTerminalItemActionHandler implements IBlockEntityItemActionHandler<AbstractGridBlockEntity> {
	public static final RefinedStorageTerminalItemActionHandler INSTANCE = new RefinedStorageTerminalItemActionHandler();
	public static final ResourceLocation ID = SophisticatedItemActions.getRL("refinedstorage_terminal");

	@Override
	public ResourceLocation id() {
		return ID;
	}

	@Override
	public Class<AbstractGridBlockEntity> getObjectClass() {
		return AbstractGridBlockEntity.class;
	}

	@Override
	public ItemMatchResult getItemMatch(ServerPlayer player, ItemStackKey stackKey, BlockPos pos, IBlockItemActionHandler.Action action) {
		return getFromBlockEntity(player, pos,
				grid -> canPlayerUse(grid, player, getPermission(action)) ? getItemMatch(stackKey, grid, action) : ItemMatchResult.NO_MATCH)
				.orElse(ItemMatchResult.NO_MATCH);
	}

	@Override
	public ItemMatchResult getItemMatch(ItemStackKey stackKey, AbstractGridBlockEntity grid, IBlockItemActionHandler.Action action) {
		return getStorage(grid).map(storage -> RefinedStorageItemActionHelper.getItemMatch(stackKey, storage)).orElse(ItemMatchResult.NO_MATCH);
	}

	@Override
	public IDepositHandler getDepositHandler(AbstractGridBlockEntity grid) {
		throw new UnsupportedOperationException("Use server player aware deposit handler lookup");
	}

	@Override
	public Optional<IDepositHandler> getDepositHandler(ServerPlayer player, BlockPos pos) {
		return getFromBlockEntity(player, pos, grid -> createDepositHandler(player, grid)).flatMap(handler -> handler);
	}

	@Override
	public IRestockHandler getRestockHandler(AbstractGridBlockEntity grid) {
		throw new UnsupportedOperationException("Use server player aware restock handler lookup");
	}

	@Override
	public Optional<IRestockHandler> getRestockHandler(ServerPlayer player, BlockPos pos) {
		return getFromBlockEntity(player, pos, grid -> createRestockHandler(player, grid)).flatMap(handler -> handler);
	}

	private Optional<IDepositHandler> createDepositHandler(ServerPlayer player, AbstractGridBlockEntity grid) {
		if (!canPlayerUse(grid, player, BuiltinPermission.INSERT)) {
			return Optional.empty();
		}

		return getStorage(grid).map(storage -> new IDepositHandler() {
			@Override
			public Optional<BlockPos> getPositionToOpen() {
				return Optional.of(grid.getBlockPos());
			}

			@Override
			public Vec3 getPosition() {
				return SubLevelCompatHelper.projectToWorld(grid.getLevel(), Vec3.atCenterOf(grid.getBlockPos()));
			}

			@Override
			public ItemMatchResult getItemMatch(ItemStackKey stackKey) {
				return RefinedStorageItemActionHelper.getItemMatch(stackKey, storage);
			}

			@Override
			public ItemStack insertItem(ItemStack stack) {
				return RefinedStorageItemActionHelper.insertItem(storage, player, stack);
			}
		});
	}

	private Optional<IRestockHandler> createRestockHandler(ServerPlayer player, AbstractGridBlockEntity grid) {
		if (!canPlayerUse(grid, player, BuiltinPermission.EXTRACT)) {
			return Optional.empty();
		}

		return getStorage(grid).map(storage -> new IRestockHandler() {
			@Override
			public Optional<BlockPos> getPositionToOpen() {
				return Optional.of(grid.getBlockPos());
			}

			@Override
			public Vec3 getPosition() {
				return SubLevelCompatHelper.projectToWorld(grid.getLevel(), Vec3.atCenterOf(grid.getBlockPos()));
			}

			@Override
			public ItemStack extractItem(ItemStack stack) {
				return RefinedStorageItemActionHelper.extractItem(storage, player, stack);
			}
		});
	}

	private static Optional<StorageNetworkComponent> getStorage(AbstractGridBlockEntity grid) {
		if (!grid.isGridActive()) {
			return Optional.empty();
		}

		return getNetwork(grid).map(network -> network.getComponent(StorageNetworkComponent.class));
	}

	private static boolean canPlayerUse(AbstractGridBlockEntity grid, ServerPlayer player, Permission permission) {
		return getNetwork(grid).map(network -> network.getComponent(PlatformSecurityNetworkComponent.class).isAllowed(permission, player)).orElse(false);
	}

	private static Optional<Network> getNetwork(AbstractGridBlockEntity grid) {
		NetworkNode networkNode = ((AccessorAbstractGridBlockEntity) grid).sophisticatedItemActions$getMainNetworkNode();
		return Optional.ofNullable(networkNode.getNetwork());
	}

	private static Permission getPermission(IBlockItemActionHandler.Action action) {
		return switch (action) {
			case DEPOSIT -> BuiltinPermission.INSERT;
			case RESTOCK -> BuiltinPermission.EXTRACT;
			case HIGHLIGHT -> BuiltinPermission.OPEN;
		};
	}

}
