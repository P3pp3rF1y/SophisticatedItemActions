package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

import java.util.List;

public interface ISubLevelCompat {
	double distanceSquared(Level level, Vec3 first, Vec3 second);

	Vec3 projectToWorld(Level level, Vec3 position);

	default List<BlockEntity> getBlockEntitiesInRange(Level level, BlockPos origin, int range) {
		return List.of();
	}

	default boolean isInSubLevel(Level level, BlockPos pos) {
		return false;
	}

	default Level getLevelForPosition(Level level, BlockPos pos) {
		return level;
	}

	default boolean mayInteract(Player player, Level level, BlockPos pos) {
		return player.mayInteract(getLevelForPosition(level, pos), pos);
	}

	@Nullable
	default RenderedHighlight getRenderedHighlight(Level level, List<BlockPos> positions, float partialTick) {
		return null;
	}

	default ISubLevelCompat getClientCompat() {
		return this;
	}
}
