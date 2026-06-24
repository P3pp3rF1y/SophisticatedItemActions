package net.p3pp3rf1y.sophisticateditemactions.compat.refinedstorage;

import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.common.storage.portablegrid.AbstractPortableGridBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
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
import net.p3pp3rf1y.sophisticateditemactions.mixin.AccessorAbstractPortableGridBlockEntity;

import java.util.Optional;

public class RefinedStoragePortableGridItemActionHandler implements IBlockEntityItemActionHandler<AbstractPortableGridBlockEntity> {
	public static final RefinedStoragePortableGridItemActionHandler INSTANCE = new RefinedStoragePortableGridItemActionHandler();
	public static final Identifier ID = SophisticatedItemActions.getIdentifier("refinedstorage_portable_grid");

	@Override
	public Identifier id() {
		return ID;
	}

	@Override
	public Class<AbstractPortableGridBlockEntity> getObjectClass() {
		return AbstractPortableGridBlockEntity.class;
	}

	@Override
	public ItemMatchResult getItemMatch(ItemStackKey stackKey, AbstractPortableGridBlockEntity portableGrid, IBlockItemActionHandler.Action action) {
		return getStorage(portableGrid).map(storage -> RefinedStorageItemActionHelper.getItemMatch(stackKey, storage)).orElse(ItemMatchResult.NO_MATCH);
	}

	@Override
	public IDepositHandler getDepositHandler(AbstractPortableGridBlockEntity portableGrid) {
		throw new UnsupportedOperationException("Use server player aware deposit handler lookup");
	}

	@Override
	public Optional<IDepositHandler> getDepositHandler(ServerPlayer player, BlockPos pos) {
		return getFromBlockEntity(player, pos, portableGrid -> getStorage(portableGrid).map(storage -> new IDepositHandler() {
			@Override
			public Optional<BlockPos> getPositionToOpen() {
				return Optional.of(portableGrid.getBlockPos());
			}

			@Override
			public Vec3 getPosition() {
				return Vec3.atCenterOf(portableGrid.getBlockPos());
			}

			@Override
			public ItemMatchResult getItemMatch(ItemStackKey stackKey) {
				return RefinedStorageItemActionHelper.getItemMatch(stackKey, storage);
			}

			@Override
			public int insertItem(ItemStack stack) {
				return RefinedStorageItemActionHelper.insertItem(storage, player, stack);
			}
		})).flatMap(handler -> handler);
	}

	@Override
	public IRestockHandler getRestockHandler(AbstractPortableGridBlockEntity portableGrid) {
		throw new UnsupportedOperationException("Use server player aware restock handler lookup");
	}

	@Override
	public Optional<IRestockHandler> getRestockHandler(ServerPlayer player, BlockPos pos) {
		return getFromBlockEntity(player, pos, portableGrid -> getStorage(portableGrid).map(storage -> new IRestockHandler() {
			@Override
			public Optional<BlockPos> getPositionToOpen() {
				return Optional.of(portableGrid.getBlockPos());
			}

			@Override
			public Vec3 getPosition() {
				return Vec3.atCenterOf(portableGrid.getBlockPos());
			}

			@Override
			public int extractItem(ItemStack stack) {
				return RefinedStorageItemActionHelper.extractItem(storage, player, stack);
			}
		})).flatMap(handler -> handler);
	}

	private static Optional<Storage> getStorage(AbstractPortableGridBlockEntity portableGrid) {
		com.refinedmods.refinedstorage.common.api.grid.Grid grid = ((AccessorAbstractPortableGridBlockEntity) portableGrid)
				.sophisticatedItemActions$invokeGetGrid();
		return grid.isGridActive() ? Optional.of(grid.getItemStorage()) : Optional.empty();
	}
}
