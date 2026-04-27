package net.p3pp3rf1y.sophisticateditemactions.compat.create;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemActionHandlerRegistry;

public class CreateCompat implements ICompat {
	@Override
	public void setup() {
		ItemActionHandlerRegistry.register(ItemVaultItemActionHandler.INSTANCE);
		ItemActionHandlerRegistry.register(ContraptionStorageItemActionHandler.INSTANCE);
		if (FMLEnvironment.dist == Dist.CLIENT) {
			CreateClientCompat.init();
		}
	}
}
