package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ItemActionHandlerRegistry {
	private ItemActionHandlerRegistry() {
	}

	private static final Map<ResourceLocation, IBlockItemActionHandler> blockHandlersRegistry = new LinkedHashMap<>();
	private static final Map<ResourceLocation, IEntityItemActionHandler> entityHandlersRegistry = new LinkedHashMap<>();

	static {
		register(ControllerItemActionHandler.INSTANCE);
		register(ControllableStorageItemActionHandler.INSTANCE);
	}

	public static void register(IBlockItemActionHandler handler) {
		blockHandlersRegistry.put(handler.id(), handler);
	}

	public static Optional<IBlockItemActionHandler> getBlockHandler(ResourceLocation id) {
		if (StandardStorageActionHandler.INSTANCE.id().equals(id)) {
			return Optional.of(StandardStorageActionHandler.INSTANCE);
		}

		return Optional.ofNullable(blockHandlersRegistry.get(id));
	}

	public static Optional<ResourceLocation> getBlockHandlerIdFor(Level level, BlockPos pos, BlockEntity blockEntity, IBlockItemActionHandler.Action action) {
		return getBlockHandlerFor(level, pos, blockEntity, action).map(IBlockItemActionHandler::id);
	}

	public static Optional<IBlockItemActionHandler> getBlockHandlerFor(Level level, BlockPos pos, BlockEntity blockEntity, IBlockItemActionHandler.Action action) {
		for (IBlockItemActionHandler h : blockHandlersRegistry.values()) {
			if (h.canActOn(level, pos, blockEntity)) {
				return Optional.of(h);
			}
		}

		IBlockItemActionHandler fallback = StandardStorageActionHandler.INSTANCE;
		if (fallback.canActOn(level, pos, blockEntity)) {
			return Optional.of(fallback);
		}

		return Optional.empty();
	}

	public static void register(IEntityItemActionHandler handler) {
		entityHandlersRegistry.put(handler.id(), handler);
	}

	public static Optional<IEntityItemActionHandler> getEntityHandler(ResourceLocation id) {
		if (StandardStorageActionHandler.INSTANCE.id().equals(id)) {
			return Optional.of(StandardStorageActionHandler.INSTANCE);
		}

		return Optional.ofNullable(entityHandlersRegistry.get(id));
	}

	public static Optional<ResourceLocation> getEntityHandlerIdFor(Entity entity) {
		for (IEntityItemActionHandler h : entityHandlersRegistry.values()) {
			if (h.canActOn(entity)) {
				return Optional.of(h.id());
			}
		}

		if (StandardStorageActionHandler.INSTANCE.canActOn(entity)) {
			return Optional.of(StandardStorageActionHandler.INSTANCE.id());
		}

		return Optional.empty();
	}
}
