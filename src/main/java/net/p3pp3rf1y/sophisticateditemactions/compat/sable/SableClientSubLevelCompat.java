package net.p3pp3rf1y.sophisticateditemactions.compat.sable;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.p3pp3rf1y.sophisticatedcore.util.VoxelOutliner;
import net.p3pp3rf1y.sophisticateditemactions.common.RenderedHighlight;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SableClientSubLevelCompat extends SableSubLevelCompat {
	@Override
	@Nullable
	public RenderedHighlight getRenderedHighlight(Level level, List<BlockPos> positions, float partialTick) {
		if (positions.isEmpty()) {
			return null;
		}

		SubLevelAccess subLevelAccess = SableCompanion.INSTANCE.getContaining(level, positions.getFirst());
		if (!(subLevelAccess instanceof ClientSubLevel clientSubLevel)) {
			return null;
		}

		BlockPos origin = positions.getFirst();
		VoxelShape shape = Shapes.empty();
		Vec3 centerSum = Vec3.ZERO;
		int count = 0;
		for (BlockPos pos : positions) {
			Level storageLevel = getLevelForPosition(level, pos);
			if (!storageLevel.isLoaded(pos) || storageLevel.isEmptyBlock(pos)) {
				continue;
			}

			shape = Shapes.join(shape, storageLevel.getBlockState(pos).getShape(storageLevel, pos).move(pos.getX() - origin.getX(), pos.getY() - origin.getY(),
					pos.getZ() - origin.getZ()), BooleanOp.OR);
			centerSum = centerSum.add(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
			count++;
		}

		if (shape.isEmpty() || count == 0) {
			return null;
		}

		List<VoxelOutliner.Edge> edges = new ArrayList<>();
		var renderPose = clientSubLevel.renderPose(partialTick);
		Vec3 localOrigin = Vec3.atLowerCornerOf(origin);
		Vec3 rotationPoint = new Vec3(renderPose.rotationPoint().x(), renderPose.rotationPoint().y(), renderPose.rotationPoint().z()).subtract(localOrigin);
		for (VoxelOutliner.Edge edge : VoxelOutliner.linesFromVoxelShapeSimplified(shape, BlockPos.ZERO)) {
			edges.add(new VoxelOutliner.Edge(edge.a(), edge.b()));
		}

		return new RenderedHighlight(edges, centerSum.scale(1D / count).subtract(localOrigin),
				new RenderedHighlight.RenderTransform(new Vec3(renderPose.position().x(), renderPose.position().y(), renderPose.position().z()), rotationPoint,
						new Quaternionf(renderPose.orientation()), new Vec3(renderPose.scale().x(), renderPose.scale().y(), renderPose.scale().z())));
	}
}
