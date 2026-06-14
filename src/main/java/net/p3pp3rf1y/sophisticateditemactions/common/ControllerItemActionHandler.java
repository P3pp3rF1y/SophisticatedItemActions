package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;

import java.util.Optional;

public class ControllerItemActionHandler implements IBlockEntityItemActionHandler<ControllerBlockEntityBase> {
	public static final ControllerItemActionHandler INSTANCE = new ControllerItemActionHandler();
	public static final Identifier ID = SophisticatedItemActions.getIdentifier("controller");

	@Override
	public IDepositHandler getDepositHandler(ControllerBlockEntityBase controller) {
		Vec3 center = Vec3.atCenterOf(controller.getBlockPos());
		return new IDepositHandler() {
			@Override
			public Optional<BlockPos> getPositionToOpen() {
				return Optional.empty();
			}

			@Override
			public Vec3 getPosition() {
				return center;
			}

			@Override
			public ItemMatchResult getItemMatch(ItemStackKey stackKey) {
				return ControllerItemActionHandler.this.getItemMatch(stackKey, controller);
			}

			@Override
			public int insertItem(ItemStack stack) {
				return InventoryHelper.insert(controller, stack);
			}
		};
	}

	@Override
	public IRestockHandler getRestockHandler(ControllerBlockEntityBase controller) {
		return new IRestockHandler() {
			@Override
			public Optional<BlockPos> getPositionToOpen() {
				return Optional.empty();
			}

			@Override
			public Vec3 getPosition() {
				return Vec3.atCenterOf(controller.getBlockPos());
			}

			@Override
			public int extractItem(ItemStack stack) {
				return InventoryHelper.extract(controller, stack);
			}
		};
	}

	@Override
	public Optional<StorageItemHandlerTarget> getStorageItemHandlerTarget(ControllerBlockEntityBase controller) {
		return Optional.of(new StorageItemHandlerTarget(null, Vec3.atCenterOf(controller.getBlockPos()), controller, stackKey -> getItemMatch(stackKey, controller)));
	}

	private ItemMatchResult getItemMatch(ItemStackKey stackKey, ControllerBlockEntityBase controller) {
		if (controller.hasMatchingStack(stackKey)) {
			return ItemMatchResult.MATCHING_STACK;
		} else if (controller.hasMatchingItem(stackKey.stack().getItem()) || controller.hasMatchingFilter(stackKey.stack())) {
			return ItemMatchResult.MATCHING_ITEM;
		}
		return ItemMatchResult.NO_MATCH;
	}

	@Override
	public ItemMatchResult getItemMatch(ItemStackKey stackKey, ControllerBlockEntityBase controller, Action action) {
		return ItemMatchResult.NO_MATCH;
	}

	@Override
	public Class<ControllerBlockEntityBase> getObjectClass() {
		return ControllerBlockEntityBase.class;
	}

	@Override
	public Identifier id() {
		return ID;
	}
}
