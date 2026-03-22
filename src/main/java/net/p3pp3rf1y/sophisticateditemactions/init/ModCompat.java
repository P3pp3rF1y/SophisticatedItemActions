package net.p3pp3rf1y.sophisticateditemactions.init;

import net.p3pp3rf1y.sophisticatedcore.compat.CompatInfo;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatRegistry;
import net.p3pp3rf1y.sophisticateditemactions.compat.recipeviewers.emi.EmiCompat;
import net.p3pp3rf1y.sophisticateditemactions.compat.recipeviewers.jei.JeiCompat;
import net.p3pp3rf1y.sophisticateditemactions.compat.recipeviewers.rei.ReiCompat;
import net.p3pp3rf1y.sophisticateditemactions.compat.sophisticatedstorageinmotion.StorageInMotionCompat;

import static net.p3pp3rf1y.sophisticateditemactions.compat.CompatModIds.STORAGE_IN_MOTION;

public class ModCompat {
	private ModCompat() {}

	public static void register() {
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.JEI), () -> modBus -> new JeiCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.EMI), () -> modBus -> new EmiCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.REI), () -> modBus -> new ReiCompat());
		CompatRegistry.registerCompat(new CompatInfo(STORAGE_IN_MOTION), () -> modBus -> new StorageInMotionCompat());
	}
}
