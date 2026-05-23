package net.p3pp3rf1y.sophisticateditemactions.compat.create;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.compat.create.ContraptionHelper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;
import net.p3pp3rf1y.sophisticateditemactions.common.IDepositHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.IEntityItemActionHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.IRestockHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemMatchResult;
import net.p3pp3rf1y.sophisticateditemactions.common.StorageItemHandlerTarget;
import net.p3pp3rf1y.sophisticateditemactions.common.SubLevelCompatHelper;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class ContraptionStorageItemActionHandler implements IEntityItemActionHandler {
	public static final ContraptionStorageItemActionHandler INSTANCE = new ContraptionStorageItemActionHandler();
	public static final Identifier ID = SophisticatedItemActions.getIdentifier("create_contraption");

	@Override
	public Identifier id() {
		return ID;
	}

	@Override
	public boolean canActOn(Entity entity) {
		return entity instanceof AbstractContraptionEntity contraptionEntity && !getMountedStoragesSnapshot(contraptionEntity).isEmpty();
	}

	@Override
	public ItemMatchResult getItemMatch(ItemStackKey stackKey, Entity entity) {
		return entity instanceof AbstractContraptionEntity contraptionEntity ? getItemMatch(stackKey, getMountedStorageHandlers(getMountedStoragesSnapshot(contraptionEntity))) : ItemMatchResult.NO_MATCH;
	}

	@Override
	public Optional<IDepositHandler> getDepositHandler(Entity entity) {
		if (!(entity instanceof AbstractContraptionEntity contraptionEntity)) {
			return Optional.empty();
		}

		Map<BlockPos, MountedStorageInfo> storages = getMountedStoragesSnapshot(contraptionEntity);
		if (storages.isEmpty()) {
			return Optional.empty();
		}

		AtomicReference<Vec3> lastTransferPosition = new AtomicReference<>(getEntityPosition(contraptionEntity));

		return Optional.of(new IDepositHandler() {
			@Override
			public Optional<BlockPos> getPositionToOpen() {
				return Optional.empty();
			}

			@Override
			public Vec3 getPosition() {
				return lastTransferPosition.get();
			}

			@Override
			public ItemMatchResult getItemMatch(ItemStackKey stackKey) {
				return ContraptionStorageItemActionHandler.getItemMatch(stackKey, getMountedStorageHandlers(storages));
			}

			@Override
			public int insertItem(ItemStack stack) {
				return insertIntoMountedStorages(contraptionEntity, storages, stack, lastTransferPosition);
			}
		});
	}

	@Override
	public Optional<IRestockHandler> getRestockHandler(Entity entity) {
		if (!(entity instanceof AbstractContraptionEntity contraptionEntity)) {
			return Optional.empty();
		}

		Map<BlockPos, MountedStorageInfo> storages = getMountedStoragesSnapshot(contraptionEntity);
		if (storages.isEmpty()) {
			return Optional.empty();
		}

		AtomicReference<Vec3> lastTransferPosition = new AtomicReference<>(getEntityPosition(contraptionEntity));

		return Optional.of(new IRestockHandler() {
			@Override
			public Optional<BlockPos> getPositionToOpen() {
				return Optional.empty();
			}

			@Override
			public Vec3 getPosition() {
				return lastTransferPosition.get();
			}

			@Override
			public int extractItem(ItemStack stack) {
				return extractFromMountedStorages(contraptionEntity, storages, stack, lastTransferPosition);
			}

			@Override
			public List<RestockTransfer> extractTransfers(ItemStack stack) {
				return extractTransfersFromMountedStorages(contraptionEntity, storages, stack, lastTransferPosition);
			}
		});
	}

	@Override
	public List<StorageItemHandlerTarget> getStorageItemHandlerTargets(Entity entity) {
		if (!(entity instanceof AbstractContraptionEntity contraptionEntity)) {
			return List.of();
		}

		Map<BlockPos, MountedStorageInfo> storages = getMountedStoragesSnapshot(contraptionEntity);
		if (storages.isEmpty()) {
			return List.of();
		}

		return storages.entrySet().stream()
				.map(storageEntry -> new StorageItemHandlerTarget(null, getStoragePosition(contraptionEntity, storageEntry.getKey()), storageEntry.getValue().itemHandler()))
				.toList();
	}

	@Override
	public Optional<List<BlockPos>> getCustomHighlightPositions(ItemStackKey stackKey, Entity entity) {
		return getMatchingStorageWorldPositions(stackKey, entity);
	}

	@Override
	public Optional<List<List<BlockPos>>> getCustomHighlightPositionGroups(ItemStackKey stackKey, Entity entity) {
		return getMatchingStorageWorldPositionGroups(stackKey, entity);
	}

	@Override
	public Optional<List<HighlightGroup>> getCustomHighlightGroupsWithMatch(ItemStackKey stackKey, Entity entity) {
		return getMatchingStorageWorldGroupsWithMatch(stackKey, entity);
	}

	@Override
	public Optional<List<BlockPos>> getCustomRenderedHighlightPositions(ItemStackKey stackKey, Entity entity) {
		return getMatchingStorageLocalPositions(stackKey, entity);
	}

	@Override
	public Optional<List<List<BlockPos>>> getCustomRenderedHighlightGroups(ItemStackKey stackKey, Entity entity) {
		return getMatchingStorageLocalPositionGroups(stackKey, entity);
	}

	@Override
	public Optional<List<HighlightGroup>> getCustomRenderedHighlightGroupsWithMatch(ItemStackKey stackKey, Entity entity) {
		return getMatchingStorageLocalGroupsWithMatch(stackKey, entity);
	}

	private static Optional<List<BlockPos>> getMatchingStorageWorldPositions(ItemStackKey stackKey, Entity entity) {
		return getMatchingStorageWorldPositionGroups(stackKey, entity).map(groups -> groups.stream().flatMap(List::stream).toList());
	}

	private static Optional<List<List<BlockPos>>> getMatchingStorageWorldPositionGroups(ItemStackKey stackKey, Entity entity) {
		return getMatchingStorageWorldGroupsWithMatch(stackKey, entity).map(groups -> groups.stream().map(HighlightGroup::positions).toList());
	}

	private static Optional<List<HighlightGroup>> getMatchingStorageWorldGroupsWithMatch(ItemStackKey stackKey, Entity entity) {
		if (!(entity instanceof AbstractContraptionEntity contraptionEntity) || contraptionEntity.getContraption() == null) {
			return Optional.empty();
		}

		Map<Long, HighlightGroup> matchingPositions = new LinkedHashMap<>();
		getMountedStoragesSnapshot(contraptionEntity).forEach((pos, storage) -> {
			ItemMatchResult matchResult = getItemMatch(stackKey, storage.itemHandler());
			if (matchResult != ItemMatchResult.NO_MATCH) {
				matchingPositions.putIfAbsent(storage.canonicalPos().asLong(), new HighlightGroup(storage.highlightPositions().stream()
						.map(highlightPos -> BlockPos.containing(SubLevelCompatHelper.projectToWorld(contraptionEntity.level(), contraptionEntity.toGlobalVector(Vec3.atCenterOf(highlightPos), 0))))
						.toList(), matchResult));
			}
		});

		return matchingPositions.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(matchingPositions.values()));
	}

	private static Optional<List<BlockPos>> getMatchingStorageLocalPositions(ItemStackKey stackKey, Entity entity) {
		return getMatchingStorageLocalPositionGroups(stackKey, entity).map(groups -> groups.stream().flatMap(List::stream).toList());
	}

	private static Optional<List<List<BlockPos>>> getMatchingStorageLocalPositionGroups(ItemStackKey stackKey, Entity entity) {
		return getMatchingStorageLocalGroupsWithMatch(stackKey, entity).map(groups -> groups.stream().map(HighlightGroup::positions).toList());
	}

	private static Optional<List<HighlightGroup>> getMatchingStorageLocalGroupsWithMatch(ItemStackKey stackKey, Entity entity) {
		if (!(entity instanceof AbstractContraptionEntity contraptionEntity) || contraptionEntity.getContraption() == null) {
			return Optional.empty();
		}

		Map<Long, HighlightGroup> matchingPositions = new LinkedHashMap<>();
		getMountedStoragesSnapshot(contraptionEntity).forEach((pos, storage) -> {
			ItemMatchResult matchResult = getItemMatch(stackKey, storage.itemHandler());
			if (matchResult != ItemMatchResult.NO_MATCH) {
				matchingPositions.putIfAbsent(storage.canonicalPos().asLong(), new HighlightGroup(storage.highlightPositions(), matchResult));
			}
		});

		return matchingPositions.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(matchingPositions.values()));
	}

	private static Vec3 getEntityPosition(AbstractContraptionEntity contraptionEntity) {
		for (BlockPos pos : getMountedStoragesSnapshot(contraptionEntity).keySet()) {
			return SubLevelCompatHelper.projectToWorld(contraptionEntity.level(), contraptionEntity.toGlobalVector(Vec3.atCenterOf(pos), 0));
		}
		return SubLevelCompatHelper.projectToWorld(contraptionEntity.level(), contraptionEntity.position());
	}

	private static int insertIntoMountedStorages(AbstractContraptionEntity contraptionEntity, Map<BlockPos, MountedStorageInfo> storages, ItemStack stack,
			AtomicReference<Vec3> lastTransferPosition) {
		int inserted = 0;
		inserted += insertIntoMatchingStorages(contraptionEntity, storages, stack, lastTransferPosition, ItemMatchResult.MATCHING_STACK, inserted);
		if (inserted < stack.getCount()) {
			inserted += insertIntoMatchingStorages(contraptionEntity, storages, stack, lastTransferPosition, ItemMatchResult.MATCHING_ITEM, inserted);
		}
		if (inserted < stack.getCount()) {
			inserted += insertIntoMatchingStorages(contraptionEntity, storages, stack, lastTransferPosition, ItemMatchResult.NO_MATCH, inserted);
		}
		return inserted;
	}

	private static int insertIntoMatchingStorages(AbstractContraptionEntity contraptionEntity, Map<BlockPos, MountedStorageInfo> storages, ItemStack stack,
			AtomicReference<Vec3> lastTransferPosition, ItemMatchResult matchType, int alreadyInserted) {
		int inserted = 0;
		ItemStack remaining = stack.copyWithCount(stack.getCount() - alreadyInserted);
		ItemStackKey stackKey = ItemStackKey.of(stack);
		for (var storageEntry : storages.entrySet()) {
			if (getItemMatch(stackKey, storageEntry.getValue().itemHandler()) != matchType) {
				continue;
			}

			int storageInserted = InventoryHelper.insertMatchingFirst(storageEntry.getValue().itemHandler(), remaining);
			if (storageInserted > 0) {
				lastTransferPosition.set(getStoragePosition(contraptionEntity, storageEntry.getKey()));
				inserted += storageInserted;
				remaining = remaining.copyWithCount(remaining.getCount() - storageInserted);
				if (remaining.isEmpty()) {
					break;
				}
			}
		}
		return inserted;
	}

	private static int extractFromMountedStorages(AbstractContraptionEntity contraptionEntity, Map<BlockPos, MountedStorageInfo> storages, ItemStack stack,
			AtomicReference<Vec3> lastTransferPosition) {
		int extracted = 0;
		ItemStack stackToExtract = stack.copy();
		for (var storageEntry : storages.entrySet()) {
			int storageExtracted = InventoryHelper.extract(storageEntry.getValue().itemHandler(), stackToExtract);
			if (storageExtracted <= 0) {
				continue;
			}

			extracted += storageExtracted;
			lastTransferPosition.set(getStoragePosition(contraptionEntity, storageEntry.getKey()));
			stackToExtract = stackToExtract.copyWithCount(stackToExtract.getCount() - storageExtracted);
			if (stackToExtract.isEmpty()) {
				break;
			}
		}
		return extracted;
	}

	private static List<IRestockHandler.RestockTransfer> extractTransfersFromMountedStorages(AbstractContraptionEntity contraptionEntity, Map<BlockPos, MountedStorageInfo> storages, ItemStack stack,
			AtomicReference<Vec3> lastTransferPosition) {
		List<IRestockHandler.RestockTransfer> transfers = new ArrayList<>();
		ItemStack stackToExtract = stack.copy();
		for (var storageEntry : storages.entrySet()) {
			int extracted = InventoryHelper.extract(storageEntry.getValue().itemHandler(), stackToExtract);
			if (extracted <= 0) {
				continue;
			}

			Vec3 position = getStoragePosition(contraptionEntity, storageEntry.getKey());
			lastTransferPosition.set(position);
			transfers.add(new IRestockHandler.RestockTransfer(null, position, stackToExtract.copyWithCount(extracted)));
			stackToExtract = stackToExtract.copyWithCount(stackToExtract.getCount() - extracted);
			if (stackToExtract.isEmpty()) {
				break;
			}
		}
		return transfers;
	}

	private static Vec3 getStoragePosition(AbstractContraptionEntity contraptionEntity, BlockPos pos) {
		return SubLevelCompatHelper.projectToWorld(contraptionEntity.level(), contraptionEntity.toGlobalVector(Vec3.atCenterOf(pos), 0));
	}

	private static ItemMatchResult getItemMatch(ItemStackKey stackKey, Map<BlockPos, ResourceHandler<ItemResource>> storages) {
		AtomicReference<ItemMatchResult> matchResult = new AtomicReference<>(ItemMatchResult.NO_MATCH);
		storages.forEach((pos, storage) -> {
			ItemMatchResult storageMatch = getItemMatch(stackKey, storage);
			if (storageMatch == ItemMatchResult.MATCHING_STACK) {
				matchResult.set(ItemMatchResult.MATCHING_STACK);
			} else if (storageMatch == ItemMatchResult.MATCHING_ITEM && matchResult.get() == ItemMatchResult.NO_MATCH) {
				matchResult.set(ItemMatchResult.MATCHING_ITEM);
			}
		});
		return matchResult.get();
	}

	private static ItemMatchResult getItemMatch(ItemStackKey stackKey, @Nullable ResourceHandler<ItemResource> itemHandler) {
		if (itemHandler == null) {
			return ItemMatchResult.NO_MATCH;
		}

		AtomicReference<ItemMatchResult> matchResult = new AtomicReference<>(ItemMatchResult.NO_MATCH);
		InventoryHelper.iterate(itemHandler, (slot, resource, amount) -> {
			if (resource.isEmpty()) {
				return;
			}
			if (resource.matches(stackKey.stack())) {
				matchResult.set(ItemMatchResult.MATCHING_STACK);
			} else if (resource.getItem() == stackKey.stack().getItem()) {
				matchResult.set(ItemMatchResult.MATCHING_ITEM);
			}
		}, () -> matchResult.get() == ItemMatchResult.MATCHING_STACK);
		return matchResult.get();
	}

	private static Map<BlockPos, MountedStorageInfo> getMountedStoragesSnapshot(AbstractContraptionEntity contraptionEntity) {
		if (contraptionEntity.getContraption() == null) {
			return Map.of();
		}

		Map<BlockPos, MountedStorageInfo> storages = new LinkedHashMap<>();
		ContraptionHelper.getMountedItemStorages(contraptionEntity).forEach((localPos, mountedStorage) -> storages.put(localPos, new MountedStorageInfo(getItemHandler(mountedStorage), getCanonicalStoragePos(contraptionEntity, localPos), getHighlightPositions(contraptionEntity, localPos))));
		return storages;
	}

	@SuppressWarnings("unchecked")
	private static ResourceHandler<ItemResource> getItemHandler(MountedItemStorage mountedStorage) {
		return (ResourceHandler<ItemResource>) mountedStorage;
	}

	private static Map<BlockPos, ResourceHandler<ItemResource>> getMountedStorageHandlers(Map<BlockPos, MountedStorageInfo> storages) {
		Map<BlockPos, ResourceHandler<ItemResource>> handlers = new LinkedHashMap<>();
		storages.forEach((pos, storage) -> handlers.put(pos, storage.itemHandler()));
		return handlers;
	}

	private static BlockPos getCanonicalStoragePos(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
		return getHighlightPositions(contraptionEntity, localPos).stream().min(java.util.Comparator.comparingLong(BlockPos::asLong)).orElse(localPos);
	}

	private static List<BlockPos> getHighlightPositions(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
		return getDoubleChestPositions(contraptionEntity, localPos);
	}

	private static List<BlockPos> getDoubleChestPositions(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
		var blockInfo = contraptionEntity.getContraption().getBlocks().get(localPos);
		if (blockInfo == null) {
			return Collections.singletonList(localPos);
		}

		BlockState state = blockInfo.state();
		if (!isDoubleChestState(state)) {
			return Collections.singletonList(localPos);
		}

		BlockPos otherPart = localPos.relative(getConnectedDirection(state));
		if (!contraptionEntity.getContraption().getBlocks().containsKey(otherPart)) {
			return Collections.singletonList(localPos);
		}

		List<BlockPos> positions = new ArrayList<>(2);
		positions.add(localPos);
		positions.add(otherPart);
		positions.sort(BlockPos::compareTo);
		return List.copyOf(positions);
	}

	private static boolean isDoubleChestState(BlockState state) {
		return isSupportedChestBlock(state) && state.hasProperty(BlockStateProperties.CHEST_TYPE) && state.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE;
	}

	private static boolean isSupportedChestBlock(BlockState state) {
		return state.getBlock() instanceof ChestBlock || state.getBlock() instanceof net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
	}

	private static Direction getConnectedDirection(BlockState state) {
		Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
		return state.getValue(BlockStateProperties.CHEST_TYPE) == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
	}

	private record MountedStorageInfo(ResourceHandler<ItemResource> itemHandler, BlockPos canonicalPos, List<BlockPos> highlightPositions) {
	}
}
