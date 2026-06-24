package net.p3pp3rf1y.sophisticateditemactions.compat.refinedstorage;
import com.refinedmods.refinedstorage.blockentity.grid.portable.IPortableGrid;
import com.refinedmods.refinedstorage.blockentity.grid.portable.PortableGridBlockEntity;
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

public class RefinedStoragePortableGridItemActionHandler implements IBlockEntityItemActionHandler<PortableGridBlockEntity> {
	public static final RefinedStoragePortableGridItemActionHandler INSTANCE = new RefinedStoragePortableGridItemActionHandler();
	public static final ResourceLocation ID = SophisticatedItemActions.getRL("refinedstorage_portable_grid");

	@Override
	public ResourceLocation id() {
		return ID;
	}

	@Override
	public Class<PortableGridBlockEntity> getObjectClass() {
		return PortableGridBlockEntity.class;
	}

	@Override
	public ItemMatchResult getItemMatch(ItemStackKey stackKey, PortableGridBlockEntity portableGrid, IBlockItemActionHandler.Action action) {
		return getPortableGrid(portableGrid).filter(IPortableGrid::isGridActive)
				.map(portable -> RefinedStorageItemActionHelper.getItemMatch(stackKey, portable.getItemCache())).orElse(ItemMatchResult.NO_MATCH);
	}

	@Override
	public IDepositHandler getDepositHandler(PortableGridBlockEntity portableGrid) {
		throw new UnsupportedOperationException("Use server player aware deposit handler lookup");
	}

	@Override
	public Optional<IDepositHandler> getDepositHandler(ServerPlayer player, BlockPos pos) {
		return getFromBlockEntity(player, pos,
				portableGrid -> getPortableGrid(portableGrid).filter(IPortableGrid::isGridActive).map(portable -> new IDepositHandler() {
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
						return RefinedStorageItemActionHelper.getItemMatch(stackKey, portable.getItemCache());
					}

					@Override
					public ItemStack insertItem(ItemStack stack) {
						return RefinedStorageItemActionHelper.insertItem(portable.getItemStorage(), stack);
					}
				})).flatMap(handler -> handler);
	}

	@Override
	public IRestockHandler getRestockHandler(PortableGridBlockEntity portableGrid) {
		throw new UnsupportedOperationException("Use server player aware restock handler lookup");
	}

	@Override
	public Optional<IRestockHandler> getRestockHandler(ServerPlayer player, BlockPos pos) {
		return getFromBlockEntity(player, pos,
				portableGrid -> getPortableGrid(portableGrid).filter(IPortableGrid::isGridActive).map(portable -> new IRestockHandler() {
					@Override
					public Optional<BlockPos> getPositionToOpen() {
						return Optional.of(portableGrid.getBlockPos());
					}

					@Override
					public Vec3 getPosition() {
						return Vec3.atCenterOf(portableGrid.getBlockPos());
					}

					@Override
					public ItemStack extractItem(ItemStack stack) {
						return RefinedStorageItemActionHelper.extractItem(portable.getItemStorage(), stack);
					}
				})).flatMap(handler -> handler);
	}

	@SuppressWarnings("unchecked")
	private static Optional<IPortableGrid> getPortableGrid(PortableGridBlockEntity portableGrid) {
		return Optional.of(portableGrid);
	}
}
