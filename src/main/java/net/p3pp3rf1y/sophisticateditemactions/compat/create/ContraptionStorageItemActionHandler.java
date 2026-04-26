package net.p3pp3rf1y.sophisticateditemactions.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.compat.create.ContraptionHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.create.MountedStorageBase;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;
import net.p3pp3rf1y.sophisticateditemactions.common.IDepositHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.IEntityItemActionHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.IRestockHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemMatchResult;
import net.p3pp3rf1y.sophisticateditemactions.common.SubLevelCompatHelper;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ContraptionStorageItemActionHandler implements IEntityItemActionHandler {
	public static final ContraptionStorageItemActionHandler INSTANCE = new ContraptionStorageItemActionHandler();
	public static final ResourceLocation ID = SophisticatedItemActions.getRL("create_contraption");

	@Override
	public ResourceLocation id() {
		return ID;
	}

	@Override
	public boolean canActOn(Entity entity) {
		return entity instanceof AbstractContraptionEntity contraptionEntity && !getMountedStoragesSnapshot(contraptionEntity).isEmpty();
	}

	@Override
	public ItemMatchResult getItemMatch(ItemStackKey stackKey, Entity entity) {
		return entity instanceof AbstractContraptionEntity contraptionEntity ? getItemMatch(stackKey, getMountedStoragesSnapshot(contraptionEntity)) : ItemMatchResult.NO_MATCH;
	}

	@Override
	public Optional<IDepositHandler> getDepositHandler(Entity entity) {
		if (!(entity instanceof AbstractContraptionEntity contraptionEntity)) {
			return Optional.empty();
		}

		Map<BlockPos, IItemHandler> storages = getMountedStoragesSnapshot(contraptionEntity);
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
				return ContraptionStorageItemActionHandler.getItemMatch(stackKey, storages);
			}

			@Override
			public ItemStack insertItem(ItemStack stack) {
				return insertIntoMountedStorages(contraptionEntity, storages, stack, lastTransferPosition);
			}
		});
	}

	@Override
	public Optional<IRestockHandler> getRestockHandler(Entity entity) {
		if (!(entity instanceof AbstractContraptionEntity contraptionEntity)) {
			return Optional.empty();
		}

		Map<BlockPos, IItemHandler> storages = getMountedStoragesSnapshot(contraptionEntity);
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
			public ItemStack extractItem(ItemStack stack) {
				return extractFromMountedStorages(contraptionEntity, storages, stack, lastTransferPosition);
			}
		});
	}

	@Override
	public Optional<List<BlockPos>> getCustomHighlightPositions(ItemStackKey stackKey, Entity entity) {
		return getMatchingStorageWorldPositions(stackKey, entity);
	}

	@Override
	public Optional<List<BlockPos>> getCustomRenderedHighlightPositions(ItemStackKey stackKey, Entity entity) {
		return getMatchingStorageLocalPositions(stackKey, entity);
	}

	private static Optional<List<BlockPos>> getMatchingStorageWorldPositions(ItemStackKey stackKey, Entity entity) {
		if (!(entity instanceof AbstractContraptionEntity contraptionEntity) || contraptionEntity.getContraption() == null) {
			return Optional.empty();
		}

		List<BlockPos> matchingPositions = new ArrayList<>();
		getMountedStoragesSnapshot(contraptionEntity).forEach((pos, storage) -> {
			if (getItemMatch(stackKey, storage) != ItemMatchResult.NO_MATCH) {
				matchingPositions.add(BlockPos.containing(SubLevelCompatHelper.projectToWorld(contraptionEntity.level(), contraptionEntity.toGlobalVector(Vec3.atCenterOf(pos), 0))));
			}
		});

		return matchingPositions.isEmpty() ? Optional.empty() : Optional.of(matchingPositions);
	}

	private static Optional<List<BlockPos>> getMatchingStorageLocalPositions(ItemStackKey stackKey, Entity entity) {
		if (!(entity instanceof AbstractContraptionEntity contraptionEntity) || contraptionEntity.getContraption() == null) {
			return Optional.empty();
		}

		List<BlockPos> matchingPositions = new ArrayList<>();
		getMountedStoragesSnapshot(contraptionEntity).forEach((pos, storage) -> {
			if (getItemMatch(stackKey, storage) != ItemMatchResult.NO_MATCH) {
				matchingPositions.add(pos);
			}
		});

		return matchingPositions.isEmpty() ? Optional.empty() : Optional.of(matchingPositions);
	}

	private static Vec3 getEntityPosition(AbstractContraptionEntity contraptionEntity) {
		for (BlockPos pos : getMountedStoragesSnapshot(contraptionEntity).keySet()) {
			return SubLevelCompatHelper.projectToWorld(contraptionEntity.level(), contraptionEntity.toGlobalVector(Vec3.atCenterOf(pos), 0));
		}
		return SubLevelCompatHelper.projectToWorld(contraptionEntity.level(), contraptionEntity.position());
	}

	private static ItemStack insertIntoMountedStorages(AbstractContraptionEntity contraptionEntity, Map<BlockPos, IItemHandler> storages, ItemStack stack, AtomicReference<Vec3> lastTransferPosition) {
		ItemStack remaining = stack;
		remaining = insertIntoMatchingStorages(contraptionEntity, storages, remaining, lastTransferPosition, ItemMatchResult.MATCHING_STACK);
		if (!remaining.isEmpty()) {
			remaining = insertIntoMatchingStorages(contraptionEntity, storages, remaining, lastTransferPosition, ItemMatchResult.MATCHING_ITEM);
		}
		if (!remaining.isEmpty()) {
			remaining = insertIntoMatchingStorages(contraptionEntity, storages, remaining, lastTransferPosition, ItemMatchResult.NO_MATCH);
		}
		return remaining;
	}

	private static ItemStack insertIntoMatchingStorages(AbstractContraptionEntity contraptionEntity, Map<BlockPos, IItemHandler> storages, ItemStack stack, AtomicReference<Vec3> lastTransferPosition, ItemMatchResult matchType) {
		ItemStack remaining = stack;
		ItemStackKey stackKey = ItemStackKey.of(stack);
		for (var storageEntry : storages.entrySet()) {
			if (getItemMatch(stackKey, storageEntry.getValue()) != matchType) {
				continue;
			}

			ItemStack newRemaining = InventoryHelper.insertIntoInventoryMatchingFirst(remaining, storageEntry.getValue(), false);
			if (newRemaining.getCount() < remaining.getCount()) {
				lastTransferPosition.set(getStoragePosition(contraptionEntity, storageEntry.getKey()));
				remaining = newRemaining;
				if (remaining.isEmpty()) {
					break;
				}
			}
		}
		return remaining;
	}

	private static ItemStack extractFromMountedStorages(AbstractContraptionEntity contraptionEntity, Map<BlockPos, IItemHandler> storages, ItemStack stack, AtomicReference<Vec3> lastTransferPosition) {
		ItemStack extracted = ItemStack.EMPTY;
		ItemStack stackToExtract = stack.copy();
		for (var storageEntry : storages.entrySet()) {
			ItemStack storageExtracted = InventoryHelper.extractFromInventory(stackToExtract, storageEntry.getValue(), false);
			if (storageExtracted.isEmpty()) {
				continue;
			}

			if (extracted.isEmpty()) {
				extracted = storageExtracted.copy();
				lastTransferPosition.set(getStoragePosition(contraptionEntity, storageEntry.getKey()));
			} else {
				extracted.grow(storageExtracted.getCount());
			}

			stackToExtract.shrink(storageExtracted.getCount());
			if (stackToExtract.isEmpty()) {
				break;
			}
		}
		return extracted;
	}

	private static Vec3 getStoragePosition(AbstractContraptionEntity contraptionEntity, BlockPos pos) {
		return SubLevelCompatHelper.projectToWorld(contraptionEntity.level(), contraptionEntity.toGlobalVector(Vec3.atCenterOf(pos), 0));
	}

	private static ItemMatchResult getItemMatch(ItemStackKey stackKey, Map<BlockPos, IItemHandler> storages) {
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

	private static Map<BlockPos, IItemHandler> getMountedStoragesSnapshot(AbstractContraptionEntity contraptionEntity) {
		if (contraptionEntity.getContraption() == null) {
			return Map.of();
		}

		Map<BlockPos, IItemHandler> storages = new LinkedHashMap<>();
		contraptionEntity.getContraption().getBlocks().forEach((localPos, blockInfo) -> {
			MountedStorageBase mountedStorage = ContraptionHelper.getMountedStorage(contraptionEntity, localPos);
			if (mountedStorage instanceof IItemHandler itemHandler) {
				storages.put(localPos, itemHandler);
			}
		});
		return storages;
	}
}
