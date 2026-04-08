package net.p3pp3rf1y.sophisticateditemactions.init;

import net.minecraftforge.fml.ModList;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticateditemactions.compat.create.CreateCompat;
import net.p3pp3rf1y.sophisticateditemactions.compat.recipeviewers.emi.EmiCompat;
import net.p3pp3rf1y.sophisticateditemactions.compat.recipeviewers.jei.JeiCompat;
import net.p3pp3rf1y.sophisticateditemactions.compat.recipeviewers.rei.ReiCompat;
import net.p3pp3rf1y.sophisticateditemactions.compat.sophisticatedstorageinmotion.StorageInMotionCompat;
import net.p3pp3rf1y.sophisticatedstorageinmotion.SophisticatedStorageInMotion;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static net.p3pp3rf1y.sophisticateditemactions.compat.CompatModIds.STORAGE_IN_MOTION;

public class ModCompat {
	private ModCompat() {}

	private static final Map<String, Supplier<Callable<ICompat>>> compatFactories = new HashMap<>();
	private static final Map<String, ICompat> loadedCompats = new HashMap<>();

	public static void register() {
		compatFactories.put(net.p3pp3rf1y.sophisticateditemactions.compat.CompatModIds.CREATE, () -> CreateCompat::new);
		compatFactories.put(CompatModIds.JEI, () -> JeiCompat::new);
		compatFactories.put(CompatModIds.EMI, () -> EmiCompat::new);
		compatFactories.put(CompatModIds.REI, () -> ReiCompat::new);
		compatFactories.put(STORAGE_IN_MOTION, () -> StorageInMotionCompat::new);
	}

	public static void compatsSetup() {
		loadedCompats.values().forEach(ICompat::setup);
	}

	public static void initCompats() {
		for (Map.Entry<String, Supplier<Callable<ICompat>>> entry : compatFactories.entrySet()) {
			if (ModList.get().isLoaded(entry.getKey())) {
				try {
					loadedCompats.put(entry.getKey(), entry.getValue().get().call());
				} catch (Exception e) {
					SophisticatedStorageInMotion.LOGGER.error("Error instantiating compatibility ", e);
				}
			}
		}

		loadedCompats.values().forEach(ICompat::init);
	}
}
