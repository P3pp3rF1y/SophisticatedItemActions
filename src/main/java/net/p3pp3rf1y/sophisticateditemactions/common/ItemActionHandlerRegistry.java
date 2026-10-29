package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import javax.annotation.Nullable;

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

	public static boolean canUseBlockHandler(Player player, BlockPos pos, IBlockItemActionHandler handler, IBlockItemActionHandler.Action action, int range) {
		Level level = SubLevelCompatHelper.getLevelForPosition(player.level(), pos);
		if (SubLevelCompatHelper.distanceSquared(player.level(), player.position(), Vec3.atCenterOf(pos)) > (double) range * range || !level.hasChunkAt(pos)
				|| !SubLevelCompatHelper.mayInteract(player, player.level(), pos)) {
			return false;
		}
		BlockEntity blockEntity = level.getBlockEntity(pos);
		return blockEntity != null && getBlockHandlerFor(level, pos, blockEntity, action).filter(selected -> selected.id().equals(handler.id())).isPresent();
	}

	public static boolean canUseEntityHandler(Player player, @Nullable Entity entity, IEntityItemActionHandler handler, int range) {
		return entity != null && entity.isAlive()
				&& SubLevelCompatHelper.distanceSquared(player.level(), player.position(), entity.position()) <= (double) range * range
				&& WorldHelper.playerMayInteract(player, entity) && getEntityHandlerIdFor(entity).filter(handler.id()::equals).isPresent();
	}

	public static Optional<ResourceLocation> getBlockHandlerIdFor(Level level, BlockPos pos, BlockEntity blockEntity, IBlockItemActionHandler.Action action) {
		return getBlockHandlerFor(level, pos, blockEntity, action).map(IBlockItemActionHandler::id);
	}

	public static Optional<IBlockItemActionHandler> getBlockHandlerFor(Level level, BlockPos pos, BlockEntity blockEntity,
			IBlockItemActionHandler.Action action) {
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
