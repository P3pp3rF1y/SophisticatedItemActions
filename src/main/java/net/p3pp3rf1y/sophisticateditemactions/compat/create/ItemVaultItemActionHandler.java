package net.p3pp3rf1y.sophisticateditemactions.compat.create;

import com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.BlockHighlightGroups;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;
import net.p3pp3rf1y.sophisticateditemactions.common.IBlockEntityItemActionHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.IDepositHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.IRestockHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemMatchResult;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class ItemVaultItemActionHandler implements IBlockEntityItemActionHandler<ItemVaultBlockEntity> {
	public static final ItemVaultItemActionHandler INSTANCE = new ItemVaultItemActionHandler();
	public static final ResourceLocation ID = SophisticatedItemActions.getRL("create_item_vault");

	@Override
	public ResourceLocation id() {
		return ID;
	}

	@Override
	public BlockPos getPosToActOn(BlockPos pos, ItemVaultBlockEntity vault, Action action) {
		ItemVaultBlockEntity controller = getController(vault);
		return controller != null ? controller.getBlockPos() : pos;
	}

	@Override
	public List<BlockPos> getHighlightPositions(ItemVaultBlockEntity vault) {
		ItemVaultBlockEntity controller = getController(vault);
		return controller == null ? BlockHighlightGroups.getHighlightPositions(vault.getLevel(), vault.getBlockPos()) : getVaultPositions(controller);
	}

	@Override
	public ItemMatchResult getItemMatch(ItemStackKey stackKey, ItemVaultBlockEntity vault, Action action) {
		return getItemMatch(stackKey, getVaultItemHandler(vault));
	}

	@Override
	public Class<ItemVaultBlockEntity> getObjectClass() {
		return ItemVaultBlockEntity.class;
	}

	@Override
	public IDepositHandler getDepositHandler(ItemVaultBlockEntity vault) {
		ItemVaultBlockEntity controller = getController(vault);
		IItemHandler itemHandler = getVaultItemHandler(vault);
		Vec3 center = getVaultCenter(controller != null ? controller : vault);
		BlockPos controllerPos = controller != null ? controller.getBlockPos() : vault.getBlockPos();
		return new IDepositHandler() {
			@Override
			public Optional<BlockPos> getPositionToOpen() {
				return Optional.of(controllerPos);
			}

			@Override
			public Vec3 getPosition() {
				return center;
			}

			@Override
			public ItemMatchResult getItemMatch(ItemStackKey stackKey) {
				return ItemVaultItemActionHandler.getItemMatch(stackKey, itemHandler);
			}

			@Override
			public ItemStack insertItem(ItemStack stack) {
				return InventoryHelper.insertIntoInventoryMatchingFirst(stack, itemHandler, false);
			}
		};
	}

	@Override
	public IRestockHandler getRestockHandler(ItemVaultBlockEntity vault) {
		ItemVaultBlockEntity controller = getController(vault);
		IItemHandler itemHandler = getVaultItemHandler(vault);
		Vec3 center = getVaultCenter(controller != null ? controller : vault);
		BlockPos controllerPos = controller != null ? controller.getBlockPos() : vault.getBlockPos();
		return new IRestockHandler() {
			@Override
			public Optional<BlockPos> getPositionToOpen() {
				return Optional.of(controllerPos);
			}

			@Override
			public Vec3 getPosition() {
				return center;
			}

			@Override
			public ItemStack extractItem(ItemStack stack) {
				return InventoryHelper.extractFromInventory(stack, itemHandler, false);
			}
		};
	}

	private static ItemMatchResult getItemMatch(ItemStackKey stackKey, @Nullable IItemHandler itemHandler) {
		if (itemHandler == null) {
			return ItemMatchResult.NO_MATCH;
		}

		AtomicReference<ItemMatchResult> matchResult = new AtomicReference<>(ItemMatchResult.NO_MATCH);
		InventoryHelper.iterate(itemHandler, (slot, stack) -> {
			if (stack.isEmpty()) {
				return;
			}
			if (stackKey.matches(stack)) {
				matchResult.set(ItemMatchResult.MATCHING_STACK);
			} else if (stack.getItem() == stackKey.stack().getItem()) {
				matchResult.set(ItemMatchResult.MATCHING_ITEM);
			}
		}, () -> matchResult.get() == ItemMatchResult.MATCHING_STACK);
		return matchResult.get();
	}

	@Nullable
	private static ItemVaultBlockEntity getController(ItemVaultBlockEntity vault) {
		return vault.getControllerBE();
	}

	@Nullable
	private static IItemHandler getVaultItemHandler(ItemVaultBlockEntity vault) {
		ItemVaultBlockEntity controller = getController(vault);
		if (controller == null || controller.getLevel() == null) {
			return null;
		}
		return controller.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, controller.getBlockPos(), null);
	}

	private static List<BlockPos> getVaultPositions(ItemVaultBlockEntity controller) {
		List<BlockPos> positions = new ArrayList<>();
		Direction.Axis axis = controller.getMainConnectionAxis();
		BlockPos origin = controller.getBlockPos();
		int width = controller.getWidth();
		int height = controller.getHeight();

		for (int yOffset = 0; yOffset < height; yOffset++) {
			for (int xOffset = 0; xOffset < width; xOffset++) {
				for (int zOffset = 0; zOffset < width; zOffset++) {
					positions.add(axis == Direction.Axis.Z ? origin.offset(xOffset, zOffset, yOffset) : origin.offset(yOffset, xOffset, zOffset));
				}
			}
		}

		return positions;
	}

	private static Vec3 getVaultCenter(ItemVaultBlockEntity controller) {
		List<BlockPos> positions = getVaultPositions(controller);
		BlockPos min = positions.getFirst();
		BlockPos max = positions.getFirst();
		for (BlockPos pos : positions) {
			min = new BlockPos(Math.min(min.getX(), pos.getX()), Math.min(min.getY(), pos.getY()), Math.min(min.getZ(), pos.getZ()));
			max = new BlockPos(Math.max(max.getX(), pos.getX()), Math.max(max.getY(), pos.getY()), Math.max(max.getZ(), pos.getZ()));
		}
		return new Vec3((min.getX() + max.getX() + 1) / 2D, (min.getY() + max.getY() + 1) / 2D, (min.getZ() + max.getZ() + 1) / 2D);
	}
}
