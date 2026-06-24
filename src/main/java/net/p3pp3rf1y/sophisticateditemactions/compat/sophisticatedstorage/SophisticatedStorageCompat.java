package net.p3pp3rf1y.sophisticateditemactions.compat.sophisticatedstorage;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemActionHandlerRegistry;

public class SophisticatedStorageCompat implements ICompat {
	@Override
	public void init(IEventBus modBus) {
		ItemActionHandlerRegistry.register(ControllableStorageItemActionHandler.INSTANCE);
		ItemActionHandlerRegistry.register(StorageIOItemActionHandler.INSTANCE);
		if (ModList.get().isLoaded(CompatModIds.CREATE)) {
			SophisticatedStorageCreateCompat.init();
		}
		if (FMLEnvironment.dist == Dist.CLIENT) {
			SophisticatedStorageClientCompat.init();
		}
	}

	@Override
	public void setup() {
		// noop
	}
}
