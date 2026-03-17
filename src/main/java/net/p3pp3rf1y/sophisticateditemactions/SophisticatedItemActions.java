package net.p3pp3rf1y.sophisticateditemactions;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.p3pp3rf1y.sophisticateditemactions.client.ClientEventHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.CommonEventHandler;
import net.p3pp3rf1y.sophisticateditemactions.init.ModCompat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(SophisticatedItemActions.MOD_ID)
public class SophisticatedItemActions {
	public static final String MOD_ID = "sophisticateditemactions";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	public SophisticatedItemActions(IEventBus modBus, Dist dist, ModContainer container) {
		container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
		CommonEventHandler.registerHandlers(modBus);
		ModCompat.register();
		if (dist == Dist.CLIENT) {
			ClientEventHandler.registerHandlers(modBus);
		}
	}

	public static ResourceLocation getRL(String regName) {
		return ResourceLocation.parse(getRegistryName(regName));
	}

	public static String getRegistryName(String regName) {
		return MOD_ID + ":" + regName;
	}
}
