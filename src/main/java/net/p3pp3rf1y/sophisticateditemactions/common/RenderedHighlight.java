package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.util.VoxelOutliner;
import org.joml.Quaternionf;

import javax.annotation.Nullable;

import java.util.List;

public record RenderedHighlight(List<VoxelOutliner.Edge> edges, Vec3 pivot, @Nullable RenderTransform transform) {
	public RenderedHighlight(List<VoxelOutliner.Edge> edges, Vec3 pivot) {
		this(edges, pivot, null);
	}

	public record RenderTransform(Vec3 position, Vec3 rotationPoint, Quaternionf orientation, Vec3 scale) {
		public RenderTransform {
			orientation = new Quaternionf(orientation);
		}
	}
}
