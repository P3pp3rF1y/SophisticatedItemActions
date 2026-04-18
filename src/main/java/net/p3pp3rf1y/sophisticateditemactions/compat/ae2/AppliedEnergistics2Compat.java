package net.p3pp3rf1y.sophisticateditemactions.compat.ae2;

import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemActionHandlerRegistry;

public class AppliedEnergistics2Compat implements ICompat {
	@Override
	public void setup() {
		ItemActionHandlerRegistry.register(AppliedEnergistics2TerminalItemActionHandler.INSTANCE);
		ItemActionHandlerRegistry.register(AppliedEnergistics2MEChestItemActionHandler.INSTANCE);
	}
}
