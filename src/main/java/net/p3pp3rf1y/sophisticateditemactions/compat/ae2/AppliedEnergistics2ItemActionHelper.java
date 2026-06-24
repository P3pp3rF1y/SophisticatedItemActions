package net.p3pp3rf1y.sophisticateditemactions.compat.ae2;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.blockentity.storage.ChestBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticateditemactions.common.IBlockItemActionHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemMatchResult;

public final class AppliedEnergistics2ItemActionHelper {
	private AppliedEnergistics2ItemActionHelper() {
	}

	public static <T extends ITerminalHost & IActionHost> ItemMatchResult getItemMatch(ServerPlayer player, ItemStackKey stackKey, T host,
			IBlockItemActionHandler.Action action) {
		MEStorage storage = host.getInventory();
		if (storage == null || !isConnected(host)) {
			return ItemMatchResult.NO_MATCH;
		}

		IActionSource actionSource = IActionSource.ofPlayer(player, host);
		AEItemKey stack = AEItemKey.of(stackKey.stack());
		if (stack == null) {
			return ItemMatchResult.NO_MATCH;
		}

		if (storage.extract(stack, 1, Actionable.SIMULATE, actionSource) > 0) {
			return ItemMatchResult.MATCHING_STACK;
		}

		if (canExtractFirstFuzzyMatch(storage, actionSource, host, stack)) {
			return ItemMatchResult.MATCHING_ITEM;
		}

		return ItemMatchResult.NO_MATCH;
	}

	public static ItemMatchResult getItemMatch(ServerPlayer player, ItemStackKey stackKey, ChestBlockEntity chest, IBlockItemActionHandler.Action action) {
		MEStorage storage = chest.getInventory();
		if (storage == null || !isConnected(chest)) {
			return ItemMatchResult.NO_MATCH;
		}

		IActionSource actionSource = IActionSource.ofPlayer(player);
		AEItemKey stack = AEItemKey.of(stackKey.stack());
		if (stack == null) {
			return ItemMatchResult.NO_MATCH;
		}

		if (storage.extract(stack, 1, Actionable.SIMULATE, actionSource) > 0) {
			return ItemMatchResult.MATCHING_STACK;
		}

		if (canExtractFirstFuzzyMatch(storage, actionSource, chest, stack)) {
			return ItemMatchResult.MATCHING_ITEM;
		}

		return ItemMatchResult.NO_MATCH;
	}

	public static <T extends ITerminalHost & IActionHost> ItemStack insertItem(ServerPlayer player, ItemStack stack, T host) {
		MEStorage storage = host.getInventory();
		AEItemKey key = AEItemKey.of(stack);
		IEnergySource energySource = getEnergySource(host);
		if (storage == null || key == null || energySource == null || !isConnected(host)) {
			return stack;
		}

		long inserted = StorageHelper.poweredInsert(energySource, storage, key, stack.getCount(), IActionSource.ofPlayer(player, host));
		if (inserted <= 0) {
			return stack;
		}
		if (inserted >= stack.getCount()) {
			return ItemStack.EMPTY;
		}
		return stack.copyWithCount((int) (stack.getCount() - inserted));
	}

	public static <T extends ITerminalHost & IActionHost> ItemStack extractItem(ServerPlayer player, ItemStack stack, T host) {
		MEStorage storage = host.getInventory();
		AEItemKey key = AEItemKey.of(stack);
		IEnergySource energySource = getEnergySource(host);
		if (storage == null || key == null || energySource == null || !isConnected(host)) {
			return ItemStack.EMPTY;
		}

		long extracted = StorageHelper.poweredExtraction(energySource, storage, key, stack.getCount(), IActionSource.ofPlayer(player, host));
		return extracted <= 0 ? ItemStack.EMPTY : key.toStack((int) extracted);
	}

	public static ItemStack insertItem(ServerPlayer player, ItemStack stack, ChestBlockEntity chest) {
		MEStorage storage = chest.getInventory();
		AEItemKey key = AEItemKey.of(stack);
		IEnergySource energySource = getEnergySource(chest);
		if (storage == null || key == null || energySource == null || !isConnected(chest)) {
			return stack;
		}

		long inserted = StorageHelper.poweredInsert(energySource, storage, key, stack.getCount(), IActionSource.ofPlayer(player));
		if (inserted <= 0) {
			return stack;
		}
		if (inserted >= stack.getCount()) {
			return ItemStack.EMPTY;
		}
		return stack.copyWithCount((int) (stack.getCount() - inserted));
	}

	public static ItemStack extractItem(ServerPlayer player, ItemStack stack, ChestBlockEntity chest) {
		MEStorage storage = chest.getInventory();
		AEItemKey key = AEItemKey.of(stack);
		IEnergySource energySource = getEnergySource(chest);
		if (storage == null || key == null || energySource == null || !isConnected(chest)) {
			return ItemStack.EMPTY;
		}

		long extracted = StorageHelper.poweredExtraction(energySource, storage, key, stack.getCount(), IActionSource.ofPlayer(player));
		return extracted <= 0 ? ItemStack.EMPTY : key.toStack((int) extracted);
	}

	private static <T extends IActionHost> IEnergySource getEnergySource(T host) {
		return host.getActionableNode() != null && host.getActionableNode().getGrid() != null ? host.getActionableNode().getGrid().getEnergyService() : null;
	}

	private static <T extends IActionHost> boolean canExtractFirstFuzzyMatch(MEStorage storage, IActionSource actionSource, T host, AEItemKey stack) {
		if (host.getActionableNode() == null || host.getActionableNode().getGrid() == null) {
			return false;
		}

		IStorageService storageService = host.getActionableNode().getGrid().getStorageService();
		if (storageService == null) {
			return false;
		}

		for (var entry : storageService.getCachedInventory().findFuzzy(stack, FuzzyMode.IGNORE_ALL)) {
			if (entry.getKey() instanceof AEItemKey candidate && storage.extract(candidate, 1, Actionable.SIMULATE, actionSource) > 0) {
				return true;
			}
			break;
		}

		return false;
	}

	private static boolean canExtractFirstFuzzyMatch(MEStorage storage, IActionSource actionSource, ChestBlockEntity chest, AEItemKey stack) {
		if (chest.getMainNode() == null || chest.getMainNode().getGrid() == null) {
			return false;
		}

		IStorageService storageService = chest.getMainNode().getGrid().getStorageService();
		if (storageService == null) {
			return false;
		}

		for (var entry : storageService.getCachedInventory().findFuzzy(stack, FuzzyMode.IGNORE_ALL)) {
			if (entry.getKey() instanceof AEItemKey candidate && storage.extract(candidate, 1, Actionable.SIMULATE, actionSource) > 0) {
				return true;
			}
			break;
		}

		return false;
	}

	private static <T extends IActionHost> boolean isConnected(T host) {
		return host.getActionableNode() != null && host.getActionableNode().getGrid() != null;
	}

	private static boolean isConnected(ChestBlockEntity chest) {
		return chest.getMainNode() != null && chest.getMainNode().getGrid() != null;
	}
}
