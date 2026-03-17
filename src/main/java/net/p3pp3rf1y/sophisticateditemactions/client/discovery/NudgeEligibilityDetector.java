package net.p3pp3rf1y.sophisticateditemactions.client.discovery;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public class NudgeEligibilityDetector {
	public boolean isDisplayContextAllowed(Minecraft minecraft) {
		if (minecraft.player == null || minecraft.level == null) {
			return false;
		}

		Screen screen = minecraft.screen;
		return screen instanceof AbstractContainerScreen<?>;
	}

	public boolean isStorageContainerScreen(Screen screen) {
		return screen instanceof AbstractContainerScreen<?>
				&& !(screen instanceof InventoryScreen)
				&& !(screen instanceof CreativeModeInventoryScreen);
	}
}
