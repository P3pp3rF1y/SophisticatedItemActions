package net.p3pp3rf1y.sophisticateditemactions.compat.refinedstorage;

import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemActionHandlerRegistry;

public class RefinedStorageCompat implements ICompat {
	@Override
	public void setup() {
		ItemActionHandlerRegistry.register(RefinedStorageTerminalItemActionHandler.INSTANCE);
		ItemActionHandlerRegistry.register(RefinedStoragePortableGridItemActionHandler.INSTANCE);
	}
}
