package net.p3pp3rf1y.sophisticateditemactions.compat.create;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemActionHandlerRegistry;

public class CreateCompat implements ICompat {
	@Override
	public void init(IEventBus modBus) {
		ItemActionHandlerRegistry.register(ItemVaultItemActionHandler.INSTANCE);
		ItemActionHandlerRegistry.register(ContraptionStorageItemActionHandler.INSTANCE);
		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			CreateClientCompat.init();
		}
	}

	@Override
	public void setup() {
		// noop
	}
}
