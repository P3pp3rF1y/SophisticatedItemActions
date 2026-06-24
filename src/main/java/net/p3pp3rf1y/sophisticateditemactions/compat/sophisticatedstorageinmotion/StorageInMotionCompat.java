package net.p3pp3rf1y.sophisticateditemactions.compat.sophisticatedstorageinmotion;

import net.neoforged.bus.api.IEventBus;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemActionHandlerRegistry;

public class StorageInMotionCompat implements ICompat {
	@Override
	public void init(IEventBus modBus) {
		ItemActionHandlerRegistry.register(MovingStorageItemActionHandler.INSTANCE);
	}

	@Override
	public void setup() {
		// noop
	}
}
