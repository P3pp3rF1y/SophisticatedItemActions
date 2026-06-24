package net.p3pp3rf1y.sophisticateditemactions.compat.sophisticatedstorage;

import net.p3pp3rf1y.sophisticateditemactions.compat.create.ContraptionStorageItemActionHandler;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;

public class SophisticatedStorageCreateCompat {
	private SophisticatedStorageCreateCompat() {
	}

	public static void init() {
		ContraptionStorageItemActionHandler.registerSupportedChestBlock(block -> block instanceof ChestBlock);
	}
}
