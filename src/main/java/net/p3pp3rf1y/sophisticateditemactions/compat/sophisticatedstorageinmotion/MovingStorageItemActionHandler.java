package net.p3pp3rf1y.sophisticateditemactions.compat.sophisticatedstorageinmotion;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.inventory.ISlotTracker;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;
import net.p3pp3rf1y.sophisticateditemactions.common.IDepositHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.IEntityItemActionHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.IRestockHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemMatchResult;
import net.p3pp3rf1y.sophisticatedstorageinmotion.SophisticatedStorageInMotion;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.IMovingStorageEntity;

import java.util.Optional;

public class MovingStorageItemActionHandler implements IEntityItemActionHandler {
	public static final MovingStorageItemActionHandler INSTANCE = new MovingStorageItemActionHandler();
	public static final ResourceLocation ID = SophisticatedItemActions.getRL(SophisticatedStorageInMotion.MOD_ID);

	@Override
	public ResourceLocation id() {
		return ID;
	}

	@Override
	public boolean canActOn(Entity entity) {
		return entity instanceof IMovingStorageEntity movingStorage && !movingStorage.getStorageItem().isEmpty();
	}

	@Override
	public ItemMatchResult getItemMatch(ItemStackKey stackKey, Entity entity) {
		return getItemMatch(stackKey, entity, false);
	}

	private static ItemMatchResult getItemMatch(ItemStackKey stackKey, Entity entity, boolean includeMemorizedAndFiltered) {
		if (!(entity instanceof IMovingStorageEntity movingStorage)) {
			return ItemMatchResult.NO_MATCH;
		}

		ISlotTracker slotTracker = movingStorage.getStorageHolder().getStorageWrapper().getInventoryHandler().getSlotTracker();
		if (slotTracker.getPartialStacks().contains(stackKey) || slotTracker.getFullStacks().contains(stackKey) || (includeMemorizedAndFiltered && slotTracker.hasExactStackMemorized(stackKey))) {
			return ItemMatchResult.MATCHING_STACK;
		} else if (slotTracker.getItems().contains(stackKey.stack().getItem()) || (includeMemorizedAndFiltered && slotTracker.hasItemMemorizedOrFiltered(stackKey.stack().getItem()))) {
			return ItemMatchResult.MATCHING_ITEM;
		}
		return ItemMatchResult.NO_MATCH;
	}

	@Override
	public Optional<IDepositHandler> getDepositHandler(Entity entity) {
		if (!(entity instanceof IMovingStorageEntity movingStorage)) {
			return Optional.empty();
		}
		return Optional.of(new IDepositHandler() {
			@Override
			public Optional<BlockPos> getPositionToOpen() {
				return Optional.empty();
			}

			@Override
			public Vec3 getPosition() {
				return entity.position();
			}

			@Override
			public ItemMatchResult getItemMatch(ItemStackKey stackKey) {
				return MovingStorageItemActionHandler.getItemMatch(stackKey, entity, true);
			}

			@Override
			public ItemStack insertItem(ItemStack stack) {
				return movingStorage.getStorageHolder().getStorageWrapper().getInventoryForInputOutput().insertItem(stack, false);
			}
		});
	}

	@Override
	public Optional<IRestockHandler> getRestockHandler(Entity entity) {
		if (!(entity instanceof IMovingStorageEntity movingStorage)) {
			return Optional.empty();
		}
		return Optional.of(new IRestockHandler() {
			@Override
			public Optional<BlockPos> getPositionToOpen() {
				return Optional.empty();
			}

			@Override
			public Vec3 getPosition() {
				return entity.position();
			}

			@Override
			public ItemStack extractItem(ItemStack stack) {
				return movingStorage.getStorageHolder().getStorageWrapper().getInventoryForInputOutput().extractItem(stack, false);
			}
		});
	}
}
