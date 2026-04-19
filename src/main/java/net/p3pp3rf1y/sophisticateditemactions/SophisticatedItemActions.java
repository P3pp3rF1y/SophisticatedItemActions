package net.p3pp3rf1y.sophisticateditemactions;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
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
		if (dist == Dist.CLIENT && !ModList.get().isLoaded("configured")) {
			container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
		}
		CommonEventHandler.registerHandlers(modBus);
		ModCompat.register();
		if (dist == Dist.CLIENT) {
			ClientEventHandler.registerHandlers(modBus);
		}
	}

	public static Identifier getIdentifier(String regName) {
		return Identifier.parse(getRegistryName(regName));
	}

	public static String getRegistryName(String regName) {
		return MOD_ID + ":" + regName;
	}
}
