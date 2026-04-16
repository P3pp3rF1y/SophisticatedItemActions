package net.p3pp3rf1y.sophisticateditemactions.client.discovery;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticateditemactions.client.NudgeScreenSuppressorRegistry;

public class NudgeEligibilityDetector {
	private static final int MIN_STORAGE_SLOTS = 27;
	private static final int MIN_PLAYER_SLOTS = 36;
	private Screen cachedScreen = null;
	private boolean cachedEligibility = false;

	public boolean isDisplayContextAllowed(Minecraft minecraft) {
		if (minecraft.player == null || minecraft.level == null) {
			return false;
		}

		return isStorageContainerScreen(minecraft.screen);
	}

	public boolean isStorageContainerScreen(Screen screen) {
		if (screen == null) {
			clearCache();
			return false;
		}

		if (screen == cachedScreen) {
			return cachedEligibility;
		}

		if (!(screen instanceof AbstractContainerScreen<?> containerScreen)
				|| screen instanceof InventoryScreen
				|| screen instanceof CreativeModeInventoryScreen
				|| screen instanceof SettingsScreen
				|| NudgeScreenSuppressorRegistry.isSuppressed(screen)) {
			cachedScreen = screen;
			cachedEligibility = false;
			return false;
		}

		int storageSlots = 0;
		int playerSlots = 0;
		for (Slot slot : containerScreen.getMenu().slots) {
			if (slot.container instanceof Inventory) {
				playerSlots++;
			} else {
				storageSlots++;
			}
		}

		cachedScreen = screen;
		cachedEligibility = storageSlots >= MIN_STORAGE_SLOTS && playerSlots >= MIN_PLAYER_SLOTS;
		return cachedEligibility;
	}

	public void clearCache() {
		cachedScreen = null;
		cachedEligibility = false;
	}
}
