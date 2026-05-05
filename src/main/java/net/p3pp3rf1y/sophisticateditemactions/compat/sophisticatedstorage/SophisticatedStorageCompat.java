package net.p3pp3rf1y.sophisticateditemactions.compat.sophisticatedstorage;

import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

public class SophisticatedStorageCompat implements ICompat {
	@Override
	public void setup() {
		StorageIOItemActionHandler.register();
	}
}
