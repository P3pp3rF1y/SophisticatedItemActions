package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class StandardStorageActionHandler implements IBlockItemActionHandler, IEntityItemActionHandler {
	public static final StandardStorageActionHandler INSTANCE = new StandardStorageActionHandler();
	public static final Identifier ID = SophisticatedItemActions.getIdentifier("item_handler");
	private static final Direction[] DEPOSIT_DIRECTIONS = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

	@Override
	public Identifier id() {
		return ID;
	}

	@Override
	public boolean canActOn(Entity entity) {
		return !(entity instanceof Player) && entity.getCapability(Capabilities.Item.ENTITY, null) != null;
	}

	@Override
	public ItemMatchResult getItemMatch(ItemStackKey stackKey, Entity entity) {
		return getItemMatch(stackKey, entity.getCapability(Capabilities.Item.ENTITY, null));
	}

	@Override
	public Optional<IDepositHandler> getDepositHandler(Entity entity) {
		ResourceHandler<ItemResource> cap = entity.getCapability(Capabilities.Item.ENTITY, null);
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
			public int insertItem(ItemStack stack) {
				return InventoryHelper.insertMatchingFirst(cap, stack);
			}
		});
	}

	@Override
	public Optional<IRestockHandler> getRestockHandler(Entity entity) {
		ResourceHandler<ItemResource> cap = entity.getCapability(Capabilities.Item.ENTITY, null);
		if (cap == null) {
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
			public int extractItem(ItemStack stack) {
				return InventoryHelper.extract(cap, stack);
			}
		});
	}

	@Override
	public Optional<StorageItemHandlerTarget> getStorageItemHandlerTarget(Entity entity) {
		ResourceHandler<ItemResource> cap = entity.getCapability(Capabilities.Item.ENTITY, null);
		if (cap == null) {
			return Optional.empty();
		}

		return Optional.of(new StorageItemHandlerTarget(null, entity.position(), cap));
	}

	private static ItemMatchResult getItemMatch(ItemStackKey stackKey, @Nullable ResourceHandler<ItemResource> cap) {
		if (cap == null) {
			return ItemMatchResult.NO_MATCH;
		}
		AtomicReference<ItemMatchResult> highlightResult = new AtomicReference<>(ItemMatchResult.NO_MATCH);
		InventoryHelper.iterate(cap, (slot, resource, amount) -> {
			if (resource.isEmpty()) {
				return;
			}
			if (resource.matches(stackKey.stack())) {
				highlightResult.set(ItemMatchResult.MATCHING_STACK);
			} else if (stackKey.stack().getItem() == resource.getItem()) {
				highlightResult.set(ItemMatchResult.MATCHING_ITEM);
			}
		}, () -> highlightResult.get() == ItemMatchResult.MATCHING_STACK);
		return highlightResult.get();
	}

	@Override
	public boolean canActOn(Level level, BlockPos pos, BlockEntity blockEntity) {
		return level.getCapability(Capabilities.Item.BLOCK, pos, null) != null;
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
		BlockState state = player.level().getBlockState(pos);
		if (state.getBlock() == Blocks.CHEST && state.getValue(ChestBlock.TYPE) == ChestType.RIGHT) {
			return ItemMatchResult.NO_MATCH;
		}
		return action == Action.DEPOSIT ? getDepositItemMatch(stackKey, player.level(), pos) : getItemMatch(stackKey, player.level().getCapability(Capabilities.Item.BLOCK, pos, null));
	}

	private static ItemMatchResult getDepositItemMatch(ItemStackKey stackKey, Level level, BlockPos pos) {
		ItemMatchResult matchResult = ItemMatchResult.NO_MATCH;
		for (ResourceHandler<ItemResource> itemHandler : getDepositItemHandlers(level, pos)) {
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

	private static List<ResourceHandler<ItemResource>> getDepositItemHandlers(Level level, BlockPos pos) {
		List<ResourceHandler<ItemResource>> itemHandlers = new ArrayList<>();
		for (Direction direction : DEPOSIT_DIRECTIONS) {
			ResourceHandler<ItemResource> itemHandler = level.getCapability(Capabilities.Item.BLOCK, pos, direction);
			if (itemHandler != null) {
				itemHandlers.add(itemHandler);
			}
		}
		if (itemHandlers.isEmpty()) {
			ResourceHandler<ItemResource> itemHandler = level.getCapability(Capabilities.Item.BLOCK, pos, null);
			if (itemHandler != null) {
				itemHandlers.add(itemHandler);
			}
		}
		return itemHandlers;
	}

	private static int insertIntoDepositItemHandlers(ItemStack stack, List<ResourceHandler<ItemResource>> itemHandlers) {
		ItemResource resource = ItemResource.of(stack);
		List<ResourceHandler<ItemResource>> matchingItemHandlers = itemHandlers.stream().filter(itemHandler -> hasMatchingSlot(resource, itemHandler)).toList();
		List<ResourceHandler<ItemResource>> handlersToInsertInto = matchingItemHandlers.isEmpty() ? itemHandlers : matchingItemHandlers;
		int amount = stack.getCount();
		int inserted = 0;
		try (Transaction tx = Transaction.openRoot()) {
			for (ResourceHandler<ItemResource> itemHandler : handlersToInsertInto) {
				inserted += insertIntoMatchingSlots(resource, amount - inserted, itemHandler, tx);
				if (inserted == amount) {
					tx.commit();
					return inserted;
				}
			}
			for (ResourceHandler<ItemResource> itemHandler : handlersToInsertInto) {
				inserted += insertIntoEmptySlots(resource, amount - inserted, itemHandler, tx);
				if (inserted == amount) {
					tx.commit();
					return inserted;
				}
			}
			if (inserted > 0) {
				tx.commit();
			}
		}
		return inserted;
	}

	private static boolean hasMatchingSlot(ItemResource resource, ResourceHandler<ItemResource> itemHandler) {
		for (int slot = 0; slot < itemHandler.size(); slot++) {
			ItemResource slotResource = itemHandler.getResource(slot);
			if (!slotResource.isEmpty() && slotResource.equals(resource)) {
				return true;
			}
		}
		return false;
	}

	private static int insertIntoMatchingSlots(ItemResource resource, int amount, ResourceHandler<ItemResource> itemHandler, Transaction tx) {
		int inserted = 0;
		for (int slot = 0; slot < itemHandler.size() && inserted < amount; slot++) {
			ItemResource slotResource = itemHandler.getResource(slot);
			if (!slotResource.isEmpty() && slotResource.equals(resource)) {
				inserted += itemHandler.insert(slot, resource, amount - inserted, tx);
			}
		}
		return inserted;
	}

	private static int insertIntoEmptySlots(ItemResource resource, int amount, ResourceHandler<ItemResource> itemHandler, Transaction tx) {
		int inserted = 0;
		for (int slot = 0; slot < itemHandler.size() && inserted < amount; slot++) {
			if (itemHandler.getResource(slot).isEmpty()) {
				inserted += itemHandler.insert(slot, resource, amount - inserted, tx);
			}
		}
		return inserted;
	}

	@Override
	public Optional<IDepositHandler> getDepositHandler(ServerPlayer player, BlockPos pos) {
		Level level = player.level();
		List<ResourceHandler<ItemResource>> itemHandlers = getDepositItemHandlers(level, pos);
		if (itemHandlers.isEmpty()) {
			return Optional.empty();
		}
		Vec3 center = Vec3.atCenterOf(pos);
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
				return StandardStorageActionHandler.getDepositItemMatch(stackKey, level, pos);
			}

			@Override
			public int insertItem(ItemStack stack) {
				return insertIntoDepositItemHandlers(stack, itemHandlers);
			}
		});
	}

	@Override
	public Optional<IRestockHandler> getRestockHandler(ServerPlayer player, BlockPos pos) {
		ResourceHandler<ItemResource> itemHandler = player.level().getCapability(Capabilities.Item.BLOCK, pos, null);
		if (itemHandler == null) {
			return Optional.empty();
		}
		return Optional.of(new IRestockHandler() {
			@Override
			public Optional<BlockPos> getPositionToOpen() {
				if (player.level().getBlockEntity(pos) instanceof ChestBlockEntity chestBlockEntity && chestBlockEntity.getOpenNess(0) == 0) {
					return Optional.of(pos);
				}
				return Optional.empty();
			}

			@Override
			public Vec3 getPosition() {
				return Vec3.atCenterOf(pos);
			}

			@Override
			public int extractItem(ItemStack stack) {
				return InventoryHelper.extract(itemHandler, stack);
			}
		});
	}

	@Override
	public Optional<StorageItemHandlerTarget> getStorageItemHandlerTarget(ServerPlayer player, BlockPos pos) {
		Level level = player.level();
		ResourceHandler<ItemResource> itemHandler = level.getCapability(Capabilities.Item.BLOCK, pos, null);
		if (itemHandler == null) {
			return Optional.empty();
		}

		BlockPos positionToOpen = null;
		if (level.getBlockEntity(pos) instanceof ChestBlockEntity chestBlockEntity && chestBlockEntity.getOpenNess(0) == 0) {
			positionToOpen = pos;
		}
		return Optional.of(new StorageItemHandlerTarget(positionToOpen, Vec3.atCenterOf(pos), itemHandler));
	}
}
