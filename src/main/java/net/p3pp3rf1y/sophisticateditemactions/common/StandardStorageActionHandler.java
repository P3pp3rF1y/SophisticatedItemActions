package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class StandardStorageActionHandler implements IBlockItemActionHandler, IEntityItemActionHandler {
	public static final StandardStorageActionHandler INSTANCE = new StandardStorageActionHandler();
	public static final ResourceLocation ID = SophisticatedItemActions.getRL("item_handler");

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
				return InventoryHelper.extractFromInventory(stack, cap, false);
			}
		});
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
		return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null;
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
		return getItemMatch(stackKey, player.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, null));
	}

	@Override
	public Optional<IDepositHandler> getDepositHandler(ServerPlayer player, BlockPos pos) {
		Level level = player.level();
		IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
		if (itemHandler == null) {
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
				return StandardStorageActionHandler.getItemMatch(stackKey, itemHandler);
			}

			@Override
			public ItemStack insertItem(ItemStack stack) {
				return InventoryHelper.insertIntoInventoryMatchingFirst(stack, itemHandler, false);
			}
		});
	}

	@Override
	public Optional<IRestockHandler> getRestockHandler(ServerPlayer player, BlockPos pos) {
		IItemHandler itemHandler = player.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
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
			public ItemStack extractItem(ItemStack stack) {
				return InventoryHelper.extractFromInventory(stack, itemHandler, false);
			}
		});
	}
}
