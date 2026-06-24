package net.p3pp3rf1y.sophisticateditemactions.client;

import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class NudgeScreenSuppressorRegistry {
	private static final List<Predicate<Screen>> SUPPRESSORS = new ArrayList<>();

	private NudgeScreenSuppressorRegistry() {
	}

	public static void register(Predicate<Screen> suppressor) {
		SUPPRESSORS.add(suppressor);
	}

	public static boolean isSuppressed(Screen screen) {
		for (Predicate<Screen> suppressor : SUPPRESSORS) {
			if (suppressor.test(screen)) {
				return true;
			}
		}

		return false;
	}
}
