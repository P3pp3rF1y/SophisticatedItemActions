package net.p3pp3rf1y.sophisticateditemactions.client.discovery;

import java.util.EnumSet;

public class NudgeSessionState {
	private final EnumSet<NudgeHintType> shownHints = EnumSet.noneOf(NudgeHintType.class);
	private final EnumSet<NudgeHintType> suppressedHints = EnumSet.noneOf(NudgeHintType.class);
	private long lastNudgeTick = Long.MIN_VALUE;
	private int nudgesShown;

	public boolean wasShown(NudgeHintType hintType) {
		return shownHints.contains(hintType);
	}

	public boolean isSuppressed(NudgeHintType hintType) {
		return suppressedHints.contains(hintType);
	}

	public void markShown(NudgeHintType hintType, long gameTime) {
		shownHints.add(hintType);
		nudgesShown++;
		lastNudgeTick = gameTime;
	}

	public void suppress(NudgeHintType hintType) {
		suppressedHints.add(hintType);
	}

	public long getLastNudgeTick() {
		return lastNudgeTick;
	}

	public int getNudgesShown() {
		return nudgesShown;
	}

	public void reset() {
		shownHints.clear();
		suppressedHints.clear();
		lastNudgeTick = Long.MIN_VALUE;
		nudgesShown = 0;
	}
}
