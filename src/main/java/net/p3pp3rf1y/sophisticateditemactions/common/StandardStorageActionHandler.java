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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;

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
		return !(entity instanceof Player) && entity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).isPresent();
	}

	@Override
	public ItemMatchResult getItemMatch(ItemStackKey stackKey, Entity entity) {
		return entity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).map(cap -> getItemMatch(stackKey, cap)).orElse(ItemMatchResult.NO_MATCH);
	}

	@Override
	public Optional<IDepositHandler> getDepositHandler(Entity entity) {
		return entity.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
				.map(cap ->
						new IDepositHandler() {
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
						}
				);
	}

	@Override
	public Optional<StorageItemHandlerTarget> getStorageItemHandlerTarget(Entity entity) {
		return entity.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
				.map(cap -> new StorageItemHandlerTarget(null, entity.position(), cap));
	}

	@Override
	public Optional<IRestockHandler> getRestockHandler(Entity entity) {
		return entity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).map(cap -> new IRestockHandler() {
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

	private static ItemMatchResult getItemMatch(ItemStackKey stackKey, IItemHandler cap) {
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
		return blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).isPresent();
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
		return WorldHelper.getBlockEntity(player.level(), pos).map(blockEntity -> {
			BlockState state = blockEntity.getBlockState();
			if (state.getBlock() == Blocks.CHEST && state.getValue(ChestBlock.TYPE) == ChestType.RIGHT) {
				return ItemMatchResult.NO_MATCH;
			}
			return blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).map(cap -> getItemMatch(stackKey, cap)).orElse(ItemMatchResult.NO_MATCH);
		}).orElse(ItemMatchResult.NO_MATCH);
	}

	@Override
	public Optional<IDepositHandler> getDepositHandler(ServerPlayer player, BlockPos pos) {
		return WorldHelper.getBlockEntity(player.level(), pos).flatMap(blockEntity -> blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).map(cap -> {
					Vec3 center = Vec3.atCenterOf(blockEntity.getBlockPos());
					return new IDepositHandler() {
						@Override
						public Optional<BlockPos> getPositionToOpen() {
							if (player.level().getBlockEntity(pos) instanceof ChestBlockEntity chestBlockEntity && chestBlockEntity.getOpenNess(0) == 0) {
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
							return StandardStorageActionHandler.getItemMatch(stackKey, cap);
						}

						@Override
						public ItemStack insertItem(ItemStack stack) {
							return InventoryHelper.insertIntoInventoryMatchingFirst(stack, cap, false);
						}
					};
				}
		));
	}

	@Override
	public Optional<StorageItemHandlerTarget> getStorageItemHandlerTarget(ServerPlayer player, BlockPos pos) {
		return WorldHelper.getBlockEntity(player.level(), pos).flatMap(blockEntity -> blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).map(cap -> {
			BlockPos positionToOpen = null;
			if (player.level().getBlockEntity(pos) instanceof ChestBlockEntity chestBlockEntity && chestBlockEntity.getOpenNess(0) == 0) {
				positionToOpen = pos;
			}
			return new StorageItemHandlerTarget(positionToOpen, Vec3.atCenterOf(pos), cap);
		}));
	}

	@Override
	public Optional<IRestockHandler> getRestockHandler(ServerPlayer player, BlockPos pos) {
		return WorldHelper.getBlockEntity(player.level(), pos).flatMap(blockEntity -> blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
				.map(cap ->
						new IRestockHandler() {
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
								return InventoryHelper.extractFromInventory(stack, cap, false);
							}
						}
				)
		);
	}
}
