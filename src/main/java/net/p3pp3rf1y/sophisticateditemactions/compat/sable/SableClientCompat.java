package net.p3pp3rf1y.sophisticateditemactions.compat.sable;

import net.p3pp3rf1y.sophisticateditemactions.common.SubLevelCompatHelper;

public class SableClientCompat {
	private SableClientCompat() {
	}

	public static void init() {
		SubLevelCompatHelper.setClientCompat(new SableClientSubLevelCompat());
	}
}
