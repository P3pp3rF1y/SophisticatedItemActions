package net.p3pp3rf1y.sophisticateditemactions.compat.sable;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticateditemactions.common.SubLevelCompatHelper;

public class SableCompat implements ICompat {
	@Override
	public void init(IEventBus modBus) {
		SubLevelCompatHelper.setCompat(new SableSubLevelCompat());
		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			SableClientCompat.init();
		}
	}

	@Override
	public void setup() {
		// noop
	}
}
