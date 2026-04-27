package net.p3pp3rf1y.sophisticateditemactions.client.discovery;

import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;

public class PersistedNudgeHistory {
	private static final String VERSION_TAG = "version";
	private static final int CURRENT_VERSION = 1;

	private final EnumMap<NudgeHintType, PersistedNudgeActionState> actionStates = new EnumMap<>(NudgeHintType.class);

	public PersistedNudgeHistory() {
		for (NudgeHintType hintType : NudgeHintType.values()) {
			actionStates.put(hintType, new PersistedNudgeActionState());
		}
	}

	public PersistedNudgeActionState getActionState(NudgeHintType hintType) {
		return actionStates.get(hintType);
	}

	public CompoundTag toTag() {
		CompoundTag tag = new CompoundTag();
		tag.putInt(VERSION_TAG, CURRENT_VERSION);
		for (NudgeHintType hintType : NudgeHintType.values()) {
			tag.put(hintType.name(), actionStates.get(hintType).toTag());
		}
		return tag;
	}

	public static PersistedNudgeHistory fromTag(CompoundTag tag) {
		PersistedNudgeHistory history = new PersistedNudgeHistory();
		for (NudgeHintType hintType : NudgeHintType.values()) {
			if (tag.contains(hintType.name(), CompoundTag.TAG_COMPOUND)) {
				history.actionStates.put(hintType, PersistedNudgeActionState.fromTag(tag.getCompound(hintType.name())));
			}
		}
		return history;
	}
}
