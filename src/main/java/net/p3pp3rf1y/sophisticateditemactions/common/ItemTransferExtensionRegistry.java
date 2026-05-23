package net.p3pp3rf1y.sophisticateditemactions.common;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class ItemTransferExtensionRegistry {
	@Nullable
	private static IItemTransferExtension extension = null;

	private ItemTransferExtensionRegistry() {}

	public static void register(IItemTransferExtension extension) {
		ItemTransferExtensionRegistry.extension = extension;
	}

	public static Optional<IItemTransferExtension> getExtension() {
		return Optional.ofNullable(extension);
	}
}
