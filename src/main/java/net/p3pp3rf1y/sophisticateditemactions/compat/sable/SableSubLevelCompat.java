package net.p3pp3rf1y.sophisticateditemactions.compat.sable;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticateditemactions.common.ISubLevelCompat;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SableSubLevelCompat implements ISubLevelCompat {
	@Override
	public double distanceSquared(Level level, Vec3 first, Vec3 second) {
		return SableCompanion.INSTANCE.distanceSquaredWithSubLevels(level, first, second);
	}

	@Override
	public Vec3 projectToWorld(Level level, Vec3 position) {
		return SableCompanion.INSTANCE.projectOutOfSubLevel(level, position);
	}

	@Override
	public List<BlockEntity> getBlockEntitiesInRange(Level level, BlockPos origin, int range) {
		Set<BlockEntity> blockEntities = new LinkedHashSet<>(WorldHelper.getBlockEntitiesInRange(level, origin, range));
		BoundingBox3d bounds = new BoundingBox3d(origin).expand(range);
		for (SubLevelAccess subLevelAccess : SableCompanion.INSTANCE.getAllIntersecting(level, bounds)) {
			if (subLevelAccess instanceof ServerSubLevel serverSubLevel) {
				collectBlockEntities(level, origin, range, blockEntities, serverSubLevel);
			} else if (subLevelAccess instanceof SubLevel subLevel) {
				collectBlockEntities(level, origin, range, blockEntities, subLevel);
			}
		}
		return new ArrayList<>(blockEntities);
	}

	@Override
	public boolean isInSubLevel(Level level, BlockPos pos) {
		return SableCompanion.INSTANCE.getContaining(level, pos) != null;
	}

	@Override
	public Level getLevelForPosition(Level level, BlockPos pos) {
		SubLevelAccess subLevelAccess = SableCompanion.INSTANCE.getContaining(level, pos);
		if (subLevelAccess instanceof Level subLevel) {
			return subLevel;
		}
		return level;
	}

	@Override
	public boolean mayInteract(Player player, Level level, BlockPos pos) {
		Level interactionLevel = getLevelForPosition(level, pos);
		return !(interactionLevel instanceof ServerLevel serverLevel) || player.mayInteract(serverLevel, pos);
	}

	private static void collectBlockEntities(Level level, BlockPos origin, int range, Set<BlockEntity> blockEntities, ServerSubLevel subLevel) {
		collectBlockEntities(level, origin, range, blockEntities, (SubLevel) subLevel);
	}

	private static void collectBlockEntities(Level level, BlockPos origin, int range, Set<BlockEntity> blockEntities, SubLevel subLevel) {
		for (var chunkHolder : subLevel.getPlot().getLoadedChunks()) {
			for (BlockEntity blockEntity : chunkHolder.getChunk().getBlockEntities().values()) {
				if (blockEntity == null || blockEntity.isRemoved()) {
					continue;
				}
				if (SableCompanion.INSTANCE.distanceSquaredWithSubLevels(level, origin.getCenter(), Vec3.atCenterOf(blockEntity.getBlockPos())) <= (double) range * range) {
					blockEntities.add(blockEntity);
				}
			}
		}
	}
}
