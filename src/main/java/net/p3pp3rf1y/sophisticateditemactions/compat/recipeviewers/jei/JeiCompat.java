package net.p3pp3rf1y.sophisticateditemactions.compat.recipeviewers.jei;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

public class JeiCompat implements ICompat {
	@Override
	public void init(IEventBus modBus) {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			JeiClientCompat.init();
		}
	}

	@Override
	public void setup() {
		// noop
	}
}
