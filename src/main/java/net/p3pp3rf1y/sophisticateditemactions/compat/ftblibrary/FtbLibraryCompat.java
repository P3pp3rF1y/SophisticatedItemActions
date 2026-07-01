package net.p3pp3rf1y.sophisticateditemactions.compat.ftblibrary;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

public class FtbLibraryCompat implements ICompat {
	@Override
	public void init(IEventBus modBus) {
		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			FtbLibraryClientCompat.init();
		}
	}

	@Override
	public void setup() {
		// noop
	}
}
