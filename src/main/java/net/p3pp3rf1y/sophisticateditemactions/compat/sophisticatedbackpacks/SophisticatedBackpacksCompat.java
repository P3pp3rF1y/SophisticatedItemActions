package net.p3pp3rf1y.sophisticateditemactions.compat.sophisticatedbackpacks;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemTransferExtensionRegistry;

public class SophisticatedBackpacksCompat implements ICompat {
	@Override
	public void setup() {
		ItemTransferExtensionRegistry.register(BackpackItemTransferHandler.INSTANCE);

		if (FMLEnvironment.dist == Dist.CLIENT) {
			SophisticatedBackpacksClientCompat.init();
		}
	}
}
