package net.p3pp3rf1y.sophisticateditemactions.client.discovery;

import net.minecraft.nbt.CompoundTag;

public class PersistedNudgeActionState {
	private static final String SUCCESSFUL_USE_COUNT_TAG = "successfulUseCount";
	private static final String LAST_SUCCESSFUL_USE_PLAY_TIME_TAG = "lastSuccessfulUsePlayTime";
	private static final String LAST_SHOWN_PLAY_TIME_TAG = "lastShownPlayTime";

	private int successfulUseCount;
	private long lastSuccessfulUsePlayTime = Long.MIN_VALUE;
	private long lastShownPlayTime = Long.MIN_VALUE;

	public int getSuccessfulUseCount() {
		return successfulUseCount;
	}

	public long getLastSuccessfulUsePlayTime() {
		return lastSuccessfulUsePlayTime;
	}

	public long getLastShownPlayTime() {
		return lastShownPlayTime;
	}

	public void recordShown(long playTime) {
		lastShownPlayTime = playTime;
	}

	public void recordSuccessfulUse(long playTime) {
		successfulUseCount++;
		lastSuccessfulUsePlayTime = playTime;
	}

	public CompoundTag toTag() {
		CompoundTag tag = new CompoundTag();
		tag.putInt(SUCCESSFUL_USE_COUNT_TAG, successfulUseCount);
		if (lastSuccessfulUsePlayTime != Long.MIN_VALUE) {
			tag.putLong(LAST_SUCCESSFUL_USE_PLAY_TIME_TAG, lastSuccessfulUsePlayTime);
		}
		if (lastShownPlayTime != Long.MIN_VALUE) {
			tag.putLong(LAST_SHOWN_PLAY_TIME_TAG, lastShownPlayTime);
		}
		return tag;
	}

	public static PersistedNudgeActionState fromTag(CompoundTag tag) {
		PersistedNudgeActionState state = new PersistedNudgeActionState();
		state.successfulUseCount = tag.getInt(SUCCESSFUL_USE_COUNT_TAG);
		if (tag.contains(LAST_SUCCESSFUL_USE_PLAY_TIME_TAG)) {
			state.lastSuccessfulUsePlayTime = tag.getLong(LAST_SUCCESSFUL_USE_PLAY_TIME_TAG);
		}
		if (tag.contains(LAST_SHOWN_PLAY_TIME_TAG)) {
			state.lastShownPlayTime = tag.getLong(LAST_SHOWN_PLAY_TIME_TAG);
		}
		return state;
	}
}
