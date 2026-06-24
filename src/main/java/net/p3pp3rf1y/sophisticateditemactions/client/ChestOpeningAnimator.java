package net.p3pp3rf1y.sophisticateditemactions.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ChestOpeningAnimator {
	private static final Map<BlockPos, ChestClosingInfo> chestClosingInfos = new HashMap<>();
	private static final List<ChestOpeningHandler> chestOpeningHandlers = new ArrayList<>(List.of(ChestOpeningAnimator::tryOpenVanillaChest));

	public static void registerChestOpeningHandler(ChestOpeningHandler chestOpeningHandler) {
		chestOpeningHandlers.add(chestOpeningHandler);
	}

	public static void animateOpeningIfOpenable(Level level, BlockPos pos) {
		BlockEntity be = level.getBlockEntity(pos);
		for (ChestOpeningHandler chestOpeningHandler : chestOpeningHandlers) {
			Optional<Runnable> startClose = chestOpeningHandler.tryOpen(be);
			if (startClose.isPresent()) {
				chestClosingInfos.put(pos, new ChestClosingInfo(level.getGameTime() + 3 + level.getRandom().nextInt(3), startClose.get()));
				return;
			}
		}
	}

	private static Optional<Runnable> tryOpenVanillaChest(BlockEntity be) {
		if (be instanceof ChestBlockEntity chestBlockEntity && chestBlockEntity.getOpenNess(0) == 0) {
			chestBlockEntity.triggerEvent(1, 1);
			return Optional.of(() -> chestBlockEntity.triggerEvent(1, 0));
		}
		return Optional.empty();
	}

	public static void tick(Level level) {
		chestClosingInfos.entrySet().removeIf(entry -> {
			if (level.getGameTime() >= entry.getValue().closeStartTime()) {
				entry.getValue().startClose().run();
				return true;
			}
			return false;
		});
	}

	private record ChestClosingInfo(long closeStartTime, Runnable startClose) {
	}

	public interface ChestOpeningHandler {
		Optional<Runnable> tryOpen(BlockEntity be);
	}
}
