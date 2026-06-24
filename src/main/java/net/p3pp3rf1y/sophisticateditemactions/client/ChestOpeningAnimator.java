package net.p3pp3rf1y.sophisticateditemactions.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;

import java.util.HashMap;
import java.util.Map;

public class ChestOpeningAnimator {
	private static final Map<BlockPos, ChestClosingInfo> chestClosingInfos = new HashMap<>();

	public static void animateOpeningIfOpenable(Level level, BlockPos pos) {
		BlockEntity be = level.getBlockEntity(pos);

		if (be instanceof ChestBlockEntity chestBlockEntity && chestBlockEntity.getOpenNess(0) == 0) {
			chestBlockEntity.setShouldBeOpen(true);
			chestClosingInfos.put(pos,
					new ChestClosingInfo(level.getGameTime() + 3 + level.getRandom().nextInt(3), () -> chestBlockEntity.setShouldBeOpen(false)));
		} else if (be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chestBlockEntity && chestBlockEntity.getOpenNess(0) == 0) {
			chestBlockEntity.triggerEvent(1, 1);
			chestClosingInfos.put(pos, new ChestClosingInfo(level.getGameTime() + 3 + level.getRandom().nextInt(3), () -> chestBlockEntity.triggerEvent(1, 0)));
		}
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
}
