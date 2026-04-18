package net.p3pp3rf1y.sophisticateditemactions.compat.ae2;

import appeng.blockentity.storage.MEChestBlockEntity;
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

public class AppliedEnergistics2MEChestItemActionHandler implements IBlockEntityItemActionHandler<MEChestBlockEntity> {
	public static final AppliedEnergistics2MEChestItemActionHandler INSTANCE = new AppliedEnergistics2MEChestItemActionHandler();
	public static final ResourceLocation ID = SophisticatedItemActions.getRL("ae2_me_chest");

	@Override
	public ResourceLocation id() {
		return ID;
	}

	@Override
	public Class<MEChestBlockEntity> getObjectClass() {
		return MEChestBlockEntity.class;
	}

	@Override
	public ItemMatchResult getItemMatch(ServerPlayer player, ItemStackKey stackKey, BlockPos pos, IBlockItemActionHandler.Action action) {
		return getFromBlockEntity(player, pos, meChest -> AppliedEnergistics2ItemActionHelper.getItemMatch(player, stackKey, meChest, action))
				.orElse(ItemMatchResult.NO_MATCH);
	}

	@Override
	public ItemMatchResult getItemMatch(ItemStackKey stackKey, MEChestBlockEntity meChest, IBlockItemActionHandler.Action action) {
		return ItemMatchResult.NO_MATCH;
	}

	@Override
	public IDepositHandler getDepositHandler(MEChestBlockEntity meChest) {
		throw new UnsupportedOperationException("Use server player aware deposit handler lookup");
	}

	@Override
	public Optional<IDepositHandler> getDepositHandler(ServerPlayer player, BlockPos pos) {
		return getFromBlockEntity(player, pos, meChest -> Optional.of(new IDepositHandler() {
			@Override
			public Optional<BlockPos> getPositionToOpen() {
				return Optional.of(meChest.getBlockPos());
			}

			@Override
			public Vec3 getPosition() {
				return Vec3.atCenterOf(meChest.getBlockPos());
			}

			@Override
			public ItemMatchResult getItemMatch(ItemStackKey stackKey) {
				return AppliedEnergistics2ItemActionHelper.getItemMatch(player, stackKey, meChest, IBlockItemActionHandler.Action.DEPOSIT);
			}

			@Override
			public int insertItem(ItemStack stack) {
				return AppliedEnergistics2ItemActionHelper.insertItem(player, stack, meChest);
			}
		})).flatMap(handler -> handler);
	}

	@Override
	public IRestockHandler getRestockHandler(MEChestBlockEntity meChest) {
		throw new UnsupportedOperationException("Use server player aware restock handler lookup");
	}

	@Override
	public Optional<IRestockHandler> getRestockHandler(ServerPlayer player, BlockPos pos) {
		return getFromBlockEntity(player, pos, meChest -> Optional.of(new IRestockHandler() {
			@Override
			public Optional<BlockPos> getPositionToOpen() {
				return Optional.of(meChest.getBlockPos());
			}

			@Override
			public Vec3 getPosition() {
				return Vec3.atCenterOf(meChest.getBlockPos());
			}

			@Override
			public int extractItem(ItemStack stack) {
				return AppliedEnergistics2ItemActionHelper.extractItem(player, stack, meChest);
			}
		})).flatMap(handler -> handler);
	}
}
