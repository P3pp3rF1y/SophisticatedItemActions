package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import javax.annotation.Nullable;

import java.util.List;

public class SubLevelCompatHelper {
	private static ISubLevelCompat compat = new ISubLevelCompat() {
		@Override
		public double distanceSquared(Level level, Vec3 first, Vec3 second) {
			return first.distanceToSqr(second);
		}

		@Override
		public Vec3 projectToWorld(Level level, Vec3 position) {
			return position;
		}

		@Override
		public List<BlockEntity> getBlockEntitiesInRange(Level level, BlockPos origin, int range) {
			return WorldHelper.getBlockEntitiesInRange(level, origin, range);
		}
	};
	private static ISubLevelCompat clientCompat = compat;

	private SubLevelCompatHelper() {
	}

	public static void setCompat(ISubLevelCompat compat) {
		SubLevelCompatHelper.compat = compat;
		clientCompat = compat.getClientCompat();
	}

	public static void setClientCompat(ISubLevelCompat compat) {
		clientCompat = compat;
	}

	public static double distanceSquared(Level level, Vec3 first, Vec3 second) {
		return compat.distanceSquared(level, first, second);
	}

	public static Vec3 projectToWorld(Level level, Vec3 position) {
		return compat.projectToWorld(level, position);
	}

	public static List<BlockEntity> getBlockEntitiesInRange(Level level, BlockPos origin, int range) {
		return compat.getBlockEntitiesInRange(level, origin, range);
	}

	public static boolean isInSubLevel(Level level, BlockPos pos) {
		return compat.isInSubLevel(level, pos);
	}

	public static Level getLevelForPosition(Level level, BlockPos pos) {
		return compat.getLevelForPosition(level, pos);
	}

	public static boolean mayInteract(Player player, Level level, BlockPos pos) {
		return compat.mayInteract(player, level, pos);
	}

	@Nullable
	public static RenderedHighlight getRenderedHighlight(Level level, List<BlockPos> positions, float partialTick) {
		return clientCompat.getRenderedHighlight(level, positions, partialTick);
	}
}
