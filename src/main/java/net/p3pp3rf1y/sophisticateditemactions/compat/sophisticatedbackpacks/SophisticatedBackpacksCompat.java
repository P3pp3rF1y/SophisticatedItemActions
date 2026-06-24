package net.p3pp3rf1y.sophisticateditemactions.compat.sophisticatedbackpacks;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemTransferExtensionRegistry;

public class SophisticatedBackpacksCompat implements ICompat {
	@Override
	public void init(IEventBus modBus) {
		ItemTransferExtensionRegistry.register(BackpackItemTransferHandler.INSTANCE);

		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			SophisticatedBackpacksClientCompat.init();
		}
	}

	@Override
	public void setup() {
		// noop
	}
}
