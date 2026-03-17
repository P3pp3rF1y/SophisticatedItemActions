package net.p3pp3rf1y.sophisticateditemactions.client.discovery;

import java.util.EnumSet;

public class NudgeActionUsageTracker {
	private static final EnumSet<NudgeHintType> USED_HINTS = EnumSet.noneOf(NudgeHintType.class);

	private NudgeActionUsageTracker() {
	}

	public static void markUsed(NudgeHintType hintType) {
		USED_HINTS.add(hintType);
	}

	public static boolean consumeUsed(NudgeHintType hintType) {
		return USED_HINTS.remove(hintType);
	}

	public static void reset() {
		USED_HINTS.clear();
	}
}
