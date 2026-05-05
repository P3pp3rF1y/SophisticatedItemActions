package net.p3pp3rf1y.sophisticateditemactions.init;

import net.p3pp3rf1y.sophisticatedcore.compat.CompatInfo;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatRegistry;
import net.p3pp3rf1y.sophisticateditemactions.compat.ae2.AppliedEnergistics2Compat;
import net.p3pp3rf1y.sophisticateditemactions.compat.create.CreateCompat;
import net.p3pp3rf1y.sophisticateditemactions.compat.refinedstorage.RefinedStorageCompat;
import net.p3pp3rf1y.sophisticateditemactions.compat.recipeviewers.emi.EmiCompat;
import net.p3pp3rf1y.sophisticateditemactions.compat.recipeviewers.jei.JeiCompat;
import net.p3pp3rf1y.sophisticateditemactions.compat.recipeviewers.rei.ReiCompat;
import net.p3pp3rf1y.sophisticateditemactions.compat.sable.SableCompat;
import net.p3pp3rf1y.sophisticateditemactions.compat.sophisticatedbackpacks.SophisticatedBackpacksCompat;
import net.p3pp3rf1y.sophisticateditemactions.compat.sophisticatedstorage.SophisticatedStorageCompat;
import net.p3pp3rf1y.sophisticateditemactions.compat.sophisticatedstorageinmotion.StorageInMotionCompat;

import static net.p3pp3rf1y.sophisticateditemactions.compat.CompatModIds.REFINED_STORAGE;
import static net.p3pp3rf1y.sophisticateditemactions.compat.CompatModIds.SABLE;
import static net.p3pp3rf1y.sophisticateditemactions.compat.CompatModIds.SOPHISTICATED_BACKPACKS;
import static net.p3pp3rf1y.sophisticateditemactions.compat.CompatModIds.SOPHISTICATED_STORAGE;
import static net.p3pp3rf1y.sophisticateditemactions.compat.CompatModIds.STORAGE_IN_MOTION;

public class ModCompat {
	private ModCompat() {}

	public static void register() {
		CompatRegistry.registerCompat(new CompatInfo(net.p3pp3rf1y.sophisticateditemactions.compat.CompatModIds.AE2), () -> modBus -> new AppliedEnergistics2Compat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.CREATE), () -> modBus -> new CreateCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.JEI), () -> modBus -> new JeiCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.EMI), () -> modBus -> new EmiCompat());
		CompatRegistry.registerCompat(new CompatInfo(CompatModIds.REI), () -> modBus -> new ReiCompat());
		CompatRegistry.registerCompat(new CompatInfo(REFINED_STORAGE), () -> modBus -> new RefinedStorageCompat());
		CompatRegistry.registerCompat(new CompatInfo(SABLE), () -> modBus -> new SableCompat());
		CompatRegistry.registerCompat(new CompatInfo(SOPHISTICATED_BACKPACKS), () -> modBus -> new SophisticatedBackpacksCompat());
		CompatRegistry.registerCompat(new CompatInfo(SOPHISTICATED_STORAGE), () -> modBus -> new SophisticatedStorageCompat());
		CompatRegistry.registerCompat(new CompatInfo(STORAGE_IN_MOTION), () -> modBus -> new StorageInMotionCompat());
	}
}
