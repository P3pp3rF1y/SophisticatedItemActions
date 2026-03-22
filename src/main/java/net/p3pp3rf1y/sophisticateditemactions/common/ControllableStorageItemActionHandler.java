package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.inventory.ISlotTracker;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;

import java.util.Optional;

public class ControllableStorageItemActionHandler implements IBlockEntityItemActionHandler<IControllableStorage> {
	public static final ControllableStorageItemActionHandler INSTANCE = new ControllableStorageItemActionHandler();
	public static final ResourceLocation ID = SophisticatedCore.getRL("controllable_storage");

	@Override
	public ItemMatchResult getItemMatch(ItemStackKey stackKey, IControllableStorage storage, Action action) {
		ISlotTracker slotTracker = storage.getStorageWrapper().getInventoryHandler().getSlotTracker();
		return getItemMatchResult(stackKey, slotTracker, false);
	}

	private static ItemMatchResult getItemMatchResult(ItemStackKey stackKey, ISlotTracker slotTracker, boolean includeMemorizedAndFiltered) {
		if (slotTracker.getPartialStacks().contains(stackKey) || slotTracker.getFullStacks().contains(stackKey) || (includeMemorizedAndFiltered && slotTracker.hasExactStackMemorized(stackKey))) {
			return ItemMatchResult.MATCHING_STACK;
		} else if (slotTracker.getItems().contains(stackKey.stack().getItem()) || (includeMemorizedAndFiltered && slotTracker.hasItemMemorizedOrFiltered(stackKey.stack().getItem()))) {
			return ItemMatchResult.MATCHING_ITEM;
		}
		return ItemMatchResult.NO_MATCH;
	}

	@Override
	public Class<IControllableStorage> getObjectClass() {
		return IControllableStorage.class;
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	@Override
	public IRestockHandler getRestockHandler(IControllableStorage storage) {
		return new IRestockHandler() {
			@Override
			public Optional<BlockPos> getPositionToOpen() {
				if (storage instanceof ChestBlockEntity chestBlockEntity && chestBlockEntity.getOpenNess(0) == 0) {
					return Optional.of(storage.getStorageBlockPos());
				}
				return Optional.empty();
			}

			@Override
			public Vec3 getPosition() {
				return Vec3.atCenterOf(storage.getStorageBlockPos());
			}

			@Override
			public ItemStack extractItem(ItemStack stack) {
				return storage.getStorageWrapper().getInventoryForInputOutput().extractItem(stack, false);
			}
		};
	}

	@Override
	public BlockPos getDepositPosToActOn(BlockPos pos, IControllableStorage controllableStorage) {
		return controllableStorage.getControllerPos().orElse(pos);
	}

	@Override
	public IDepositHandler getDepositHandler(IControllableStorage storage) {
		Vec3 center = Vec3.atCenterOf(storage.getStorageBlockPos());
		return new IDepositHandler() {
			@Override
			public Vec3 getPosition() {
				return center;
			}

			@Override
			public Optional<BlockPos> getPositionToOpen() {
				if (storage instanceof ChestBlockEntity chestBlockEntity && chestBlockEntity.getOpenNess(0) == 0) {
					return Optional.of(storage.getStorageBlockPos());
				}
				return Optional.empty();
			}

			@Override
			public ItemMatchResult getItemMatch(ItemStackKey stackKey) {
				ISlotTracker slotTracker = storage.getStorageWrapper().getInventoryHandler().getSlotTracker();
				return getItemMatchResult(stackKey, slotTracker, true);
			}

			@Override
			public ItemStack insertItem(ItemStack stack) {
				return storage.getStorageWrapper().getInventoryForInputOutput().insertItem(stack, false);
			}
		};
	}
}
