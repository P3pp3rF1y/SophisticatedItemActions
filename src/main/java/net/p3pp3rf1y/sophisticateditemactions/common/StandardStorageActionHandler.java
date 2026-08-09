package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class StandardStorageActionHandler implements IBlockItemActionHandler, IEntityItemActionHandler {
	public static final StandardStorageActionHandler INSTANCE = new StandardStorageActionHandler();
	public static final ResourceLocation ID = SophisticatedItemActions.getRL("item_handler");
	private static final Direction[] DEPOSIT_DIRECTIONS = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

	@Override
	public ResourceLocation id() {
		return ID;
	}

	@Override
	public boolean canActOn(Entity entity) {
		return !(entity instanceof Player) && entity.getCapability(Capabilities.ItemHandler.ENTITY, null) != null;
	}

	@Override
	public ItemMatchResult getItemMatch(ItemStackKey stackKey, Entity entity) {
		return getItemMatch(stackKey, entity.getCapability(Capabilities.ItemHandler.ENTITY, null));
	}

	@Override
	public Optional<IDepositHandler> getDepositHandler(Entity entity) {
		IItemHandler cap = entity.getCapability(Capabilities.ItemHandler.ENTITY, null);
		if (cap == null) {
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
				return StandardStorageActionHandler.getItemMatch(stackKey, cap);
			}

			@Override
			public ItemStack insertItem(ItemStack stack) {
				return InventoryHelper.insertIntoInventoryMatchingFirst(stack, cap, false);
			}
		});
	}

	@Override
	public Optional<IRestockHandler> getRestockHandler(Entity entity) {
		IItemHandler cap = entity.getCapability(Capabilities.ItemHandler.ENTITY, null);
		if (cap == null) {
			return Optional.empty();
		}

		return Optional.of(new IRestockHandler() {
			@Nullable
			private Set<ItemStackKey> availableItems;

			@Override
			public Optional<BlockPos> getPositionToOpen() {
				return Optional.empty();
			}

			@Override
			public Vec3 getPosition() {
				return entity.position();
			}

			@Override
			public void prepareForAlternativeRestock(List<ItemStack> filters) {
				cacheAvailableItems(cap);
			}

			@Override
			public ItemStack extractItem(ItemStack stack) {
				return extractCachedItem(stack, cap);
			}

			private void cacheAvailableItems(IItemHandler itemHandler) {
				if (availableItems != null) {
					return;
				}

				availableItems = new HashSet<>();
				InventoryHelper.iterate(itemHandler, (slot, inventoryStack) -> {
					if (!inventoryStack.isEmpty()) {
						availableItems.add(getItemStackKey(inventoryStack));
					}
				});
			}

			private ItemStack extractCachedItem(ItemStack stack, IItemHandler itemHandler) {
				ItemStackKey stackKey = getItemStackKey(stack);
				if (availableItems != null && !availableItems.contains(stackKey)) {
					return ItemStack.EMPTY;
				}

				ItemStack extracted = InventoryHelper.extractFromInventory(stack, itemHandler, false);
				if (extracted.isEmpty() && availableItems != null) {
					availableItems.remove(stackKey);
				}
				return extracted;
			}
		});
	}

	@Override
	public Optional<StorageItemHandlerTarget> getStorageItemHandlerTarget(Entity entity) {
		IItemHandler cap = entity.getCapability(Capabilities.ItemHandler.ENTITY, null);
		if (cap == null) {
			return Optional.empty();
		}

		return Optional.of(new StorageItemHandlerTarget(null, entity.position(), cap));
	}

	private static ItemMatchResult getItemMatch(ItemStackKey stackKey, @Nullable IItemHandler cap) {
		if (cap == null) {
			return ItemMatchResult.NO_MATCH;
		}
		AtomicReference<ItemMatchResult> highlightResult = new AtomicReference<>(ItemMatchResult.NO_MATCH);
		InventoryHelper.iterate(cap, (slot, stack) -> {
			if (stack.isEmpty()) {
				return;
			}
			if (stackKey.matches(stack)) {
				highlightResult.set(ItemMatchResult.MATCHING_STACK);
			} else if (stackKey.stack().getItem() == stack.getItem()) {
				highlightResult.set(ItemMatchResult.MATCHING_ITEM);
			}
		}, () -> highlightResult.get() == ItemMatchResult.MATCHING_STACK);
		return highlightResult.get();
	}

	@Override
	public boolean canActOn(Level level, BlockPos pos, BlockEntity blockEntity) {
		Level storageLevel = SubLevelCompatHelper.getLevelForPosition(level, pos);
		return storageLevel.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null;
	}

	@Override
	public BlockPos getInteractionPosToActOn(Level level, BlockPos pos, BlockEntity blockEntity, Action action) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() == Blocks.CHEST && state.getValue(ChestBlock.TYPE) == ChestType.RIGHT) {
			return pos.relative(ChestBlock.getConnectedDirection(state));
		}
		return IBlockItemActionHandler.super.getInteractionPosToActOn(level, pos, blockEntity, action);
	}

	@Override
	public ItemMatchResult getItemMatch(ServerPlayer player, ItemStackKey stackKey, BlockPos pos, Action action) {
		Level storageLevel = SubLevelCompatHelper.getLevelForPosition(player.level(), pos);
		BlockState state = storageLevel.getBlockState(pos);
		if (state.getBlock() == Blocks.CHEST && state.getValue(ChestBlock.TYPE) == ChestType.RIGHT) {
			return ItemMatchResult.NO_MATCH;
		}
		return action == Action.DEPOSIT
				? getDepositItemMatch(stackKey, storageLevel, pos)
				: getItemMatch(stackKey, storageLevel.getCapability(Capabilities.ItemHandler.BLOCK, pos, null));
	}

	private static ItemMatchResult getDepositItemMatch(ItemStackKey stackKey, Level level, BlockPos pos) {
		ItemMatchResult matchResult = ItemMatchResult.NO_MATCH;
		for (IItemHandler itemHandler : getDepositItemHandlers(level, pos)) {
			ItemMatchResult itemHandlerMatch = getItemMatch(stackKey, itemHandler);
			if (itemHandlerMatch == ItemMatchResult.MATCHING_STACK) {
				return ItemMatchResult.MATCHING_STACK;
			}
			if (itemHandlerMatch == ItemMatchResult.MATCHING_ITEM) {
				matchResult = ItemMatchResult.MATCHING_ITEM;
			}
		}
		return matchResult;
	}

	private static List<IItemHandler> getDepositItemHandlers(Level level, BlockPos pos) {
		List<IItemHandler> itemHandlers = new ArrayList<>();
		for (Direction direction : DEPOSIT_DIRECTIONS) {
			IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction);
			if (itemHandler != null) {
				itemHandlers.add(itemHandler);
			}
		}
		if (itemHandlers.isEmpty()) {
			IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (itemHandler != null) {
				itemHandlers.add(itemHandler);
			}
		}
		return itemHandlers;
	}

	private static ItemStack insertIntoDepositItemHandlers(ItemStack stack, List<IItemHandler> itemHandlers) {
		ItemStack remainingStack = stack.copy();
		ItemStack stackToInsert = remainingStack;
		List<IItemHandler> matchingItemHandlers = itemHandlers.stream().filter(itemHandler -> hasMatchingSlot(stackToInsert, itemHandler)).toList();
		List<IItemHandler> handlersToInsertInto = matchingItemHandlers.isEmpty() ? itemHandlers : matchingItemHandlers;
		for (IItemHandler itemHandler : handlersToInsertInto) {
			remainingStack = insertIntoMatchingSlots(remainingStack, itemHandler);
			if (remainingStack.isEmpty()) {
				return ItemStack.EMPTY;
			}
		}
		for (IItemHandler itemHandler : handlersToInsertInto) {
			remainingStack = insertIntoEmptySlots(remainingStack, itemHandler);
			if (remainingStack.isEmpty()) {
				return ItemStack.EMPTY;
			}
		}
		return remainingStack;
	}

	private static boolean hasMatchingSlot(ItemStack stack, IItemHandler itemHandler) {
		for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
			ItemStack slotStack = itemHandler.getStackInSlot(slot);
			if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, stack)) {
				return true;
			}
		}
		return false;
	}

	private static ItemStack insertIntoMatchingSlots(ItemStack stack, IItemHandler itemHandler) {
		ItemStack remainingStack = stack;
		for (int slot = 0; slot < itemHandler.getSlots() && !remainingStack.isEmpty(); slot++) {
			ItemStack slotStack = itemHandler.getStackInSlot(slot);
			if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, remainingStack)) {
				remainingStack = itemHandler.insertItem(slot, remainingStack, false);
			}
		}
		return remainingStack;
	}

	private static ItemStack insertIntoEmptySlots(ItemStack stack, IItemHandler itemHandler) {
		ItemStack remainingStack = stack;
		for (int slot = 0; slot < itemHandler.getSlots() && !remainingStack.isEmpty(); slot++) {
			if (itemHandler.getStackInSlot(slot).isEmpty()) {
				remainingStack = itemHandler.insertItem(slot, remainingStack, false);
			}
		}
		return remainingStack;
	}

	@Override
	public Optional<IDepositHandler> getDepositHandler(ServerPlayer player, BlockPos pos) {
		Level level = SubLevelCompatHelper.getLevelForPosition(player.level(), pos);
		List<IItemHandler> itemHandlers = getDepositItemHandlers(level, pos);
		if (itemHandlers.isEmpty()) {
			return Optional.empty();
		}
		Vec3 center = SubLevelCompatHelper.projectToWorld(level, Vec3.atCenterOf(pos));
		return Optional.of(new IDepositHandler() {
			@Override
			public Optional<BlockPos> getPositionToOpen() {
				if (level.getBlockEntity(pos) instanceof ChestBlockEntity chestBlockEntity && chestBlockEntity.getOpenNess(0) == 0) {
					return Optional.of(pos);
				}
				return Optional.empty();
			}

			@Override
			public Vec3 getPosition() {
				return center;
			}

			@Override
			public ItemMatchResult getItemMatch(ItemStackKey stackKey) {
				return getDepositItemMatch(stackKey, level, pos);
			}

			@Override
			public ItemStack insertItem(ItemStack stack) {
				return insertIntoDepositItemHandlers(stack, itemHandlers);
			}
		});
	}

	@Override
	public Optional<IRestockHandler> getRestockHandler(ServerPlayer player, BlockPos pos) {
		Level level = SubLevelCompatHelper.getLevelForPosition(player.level(), pos);
		IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
		if (itemHandler == null) {
			return Optional.empty();
		}
		Vec3 center = SubLevelCompatHelper.projectToWorld(level, Vec3.atCenterOf(pos));
		return Optional.of(new IRestockHandler() {
			@Nullable
			private Set<ItemStackKey> availableItems;

			@Override
			public Optional<BlockPos> getPositionToOpen() {
				if (level.getBlockEntity(pos) instanceof ChestBlockEntity chestBlockEntity && chestBlockEntity.getOpenNess(0) == 0) {
					return Optional.of(pos);
				}
				return Optional.empty();
			}

			@Override
			public Vec3 getPosition() {
				return center;
			}

			@Override
			public void prepareForAlternativeRestock(List<ItemStack> filters) {
				cacheAvailableItems(itemHandler);
			}

			@Override
			public ItemStack extractItem(ItemStack stack) {
				return extractCachedItem(stack, itemHandler);
			}

			private void cacheAvailableItems(IItemHandler inventory) {
				if (availableItems != null) {
					return;
				}

				availableItems = new HashSet<>();
				InventoryHelper.iterate(inventory, (slot, inventoryStack) -> {
					if (!inventoryStack.isEmpty()) {
						availableItems.add(getItemStackKey(inventoryStack));
					}
				});
			}

			private ItemStack extractCachedItem(ItemStack stack, IItemHandler inventory) {
				ItemStackKey stackKey = getItemStackKey(stack);
				if (availableItems != null && !availableItems.contains(stackKey)) {
					return ItemStack.EMPTY;
				}

				ItemStack extracted = InventoryHelper.extractFromInventory(stack, inventory, false);
				if (extracted.isEmpty() && availableItems != null) {
					availableItems.remove(stackKey);
				}
				return extracted;
			}
		});
	}

	private static ItemStackKey getItemStackKey(ItemStack stack) {
		return ItemStackKey.of(stack.copyWithCount(1));
	}

	@Override
	public Optional<StorageItemHandlerTarget> getStorageItemHandlerTarget(ServerPlayer player, BlockPos pos) {
		Level level = SubLevelCompatHelper.getLevelForPosition(player.level(), pos);
		IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
		if (itemHandler == null) {
			return Optional.empty();
		}

		BlockPos positionToOpen = null;
		if (level.getBlockEntity(pos) instanceof ChestBlockEntity chestBlockEntity && chestBlockEntity.getOpenNess(0) == 0) {
			positionToOpen = pos;
		}
		return Optional.of(new StorageItemHandlerTarget(positionToOpen, SubLevelCompatHelper.projectToWorld(level, Vec3.atCenterOf(pos)), itemHandler));
	}
}
