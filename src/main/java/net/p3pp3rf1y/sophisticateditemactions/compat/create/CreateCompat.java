package net.p3pp3rf1y.sophisticateditemactions.compat.create;

import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemActionHandlerRegistry;

public class CreateCompat implements ICompat {
	@Override
	public void setup() {
		ItemActionHandlerRegistry.register(ItemVaultItemActionHandler.INSTANCE);
	}
}
