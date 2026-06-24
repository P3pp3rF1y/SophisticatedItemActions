package net.p3pp3rf1y.sophisticateditemactions.compat.sophisticatedstorage;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemActionHandlerRegistry;
import net.p3pp3rf1y.sophisticateditemactions.compat.CompatModIds;

public class SophisticatedStorageCompat implements ICompat {
	@Override
	public void setup() {
		ItemActionHandlerRegistry.register(ControllableStorageItemActionHandler.INSTANCE);
		StorageIOItemActionHandler.register();
		if (ModList.get().isLoaded(CompatModIds.CREATE)) {
			SophisticatedStorageCreateCompat.init();
		}
		if (FMLEnvironment.dist == Dist.CLIENT) {
			SophisticatedStorageClientCompat.init();
		}
	}
}
