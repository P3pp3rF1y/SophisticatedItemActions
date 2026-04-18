package net.p3pp3rf1y.sophisticateditemactions.compat.ae2;

import net.neoforged.bus.api.IEventBus;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemActionHandlerRegistry;

public class AppliedEnergistics2Compat implements ICompat {
	@Override
	public void init(IEventBus modBus) {
		ItemActionHandlerRegistry.register(AppliedEnergistics2TerminalItemActionHandler.INSTANCE);
		ItemActionHandlerRegistry.register(AppliedEnergistics2MEChestItemActionHandler.INSTANCE);
	}

	@Override
	public void setup() {
		// noop
	}
}
