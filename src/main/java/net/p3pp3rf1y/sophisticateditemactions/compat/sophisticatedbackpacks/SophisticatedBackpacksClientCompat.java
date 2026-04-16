package net.p3pp3rf1y.sophisticateditemactions.compat.sophisticatedbackpacks;

import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticateditemactions.client.NudgeScreenSuppressorRegistry;

public class SophisticatedBackpacksClientCompat {
	private SophisticatedBackpacksClientCompat() {}

	public static void init() {
		NudgeScreenSuppressorRegistry.register(screen -> screen instanceof BackpackScreen backpackScreen
				&& backpackScreen.getMenu().getBackpackContext().getType() != BackpackContext.ContextType.BLOCK_BACKPACK);
	}
}
