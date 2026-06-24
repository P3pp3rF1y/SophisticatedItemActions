package net.p3pp3rf1y.sophisticateditemactions.compat.sophisticatedstorage;

import net.p3pp3rf1y.sophisticateditemactions.client.ChestOpeningAnimator;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;

import java.util.Optional;

public class SophisticatedStorageClientCompat {
	private SophisticatedStorageClientCompat() {
	}

	public static void init() {
		ChestOpeningAnimator.registerChestOpeningHandler(be -> {
			if (be instanceof ChestBlockEntity chestBlockEntity && chestBlockEntity.getOpenNess(0) == 0) {
				chestBlockEntity.setShouldBeOpen(true);
				return Optional.of(() -> chestBlockEntity.setShouldBeOpen(false));
			}
			return Optional.empty();
		});
	}
}
