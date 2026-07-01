package net.p3pp3rf1y.sophisticateditemactions.compat.ftblibrary;

import dev.ftb.mods.ftblibrary.ui.IFocusableWidget;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.ScreenWrapper;
import dev.ftb.mods.ftblibrary.ui.Widget;
import net.minecraft.client.gui.screens.Screen;
import net.p3pp3rf1y.sophisticateditemactions.client.ClientEventHandler;

public class FtbLibraryClientCompat {
	private FtbLibraryClientCompat() {
	}

	public static void init() {
		ClientEventHandler.registerFocusedScreenProvider(FtbLibraryClientCompat::hasFocusedWidget);
	}

	private static boolean hasFocusedWidget(Screen screen) {
		return screen instanceof ScreenWrapper screenWrapper && hasFocusedWidget(screenWrapper.getGui());
	}

	private static boolean hasFocusedWidget(Panel panel) {
		for (Widget widget : panel.getWidgets()) {
			if (widget instanceof IFocusableWidget focusableWidget && focusableWidget.isFocused()) {
				return true;
			}
			if (widget instanceof Panel childPanel && hasFocusedWidget(childPanel)) {
				return true;
			}
		}

		return false;
	}
}
