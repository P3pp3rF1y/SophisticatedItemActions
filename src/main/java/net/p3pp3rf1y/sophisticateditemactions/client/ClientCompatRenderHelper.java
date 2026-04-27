package net.p3pp3rf1y.sophisticateditemactions.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticateditemactions.common.EntityBlockHighlightData;

import java.util.List;
import java.util.Map;

public class ClientCompatRenderHelper {
	private static EntityBlockHighlightRenderer entityBlockHighlightRenderer = (highlightPositions, durationTicks) -> {
	};
	private static CustomGeometryRenderer customGeometryRenderer = (submitNodeCollector, poseStack, partialTick, cameraPos) -> {
	};

	private ClientCompatRenderHelper() {}

	public static void registerEntityBlockHighlightRenderer(EntityBlockHighlightRenderer renderer) {
		entityBlockHighlightRenderer = renderer;
	}

	public static void registerCustomGeometryRenderer(CustomGeometryRenderer renderer) {
		customGeometryRenderer = renderer;
	}

	public static void addEntityBlockHighlights(Map<Integer, List<EntityBlockHighlightData>> highlightPositions, int durationTicks) {
		entityBlockHighlightRenderer.addHighlightedPositions(highlightPositions, durationTicks);
	}

	public static void renderCustomGeometry(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, float partialTick, Vec3 cameraPos) {
		customGeometryRenderer.render(submitNodeCollector, poseStack, partialTick, cameraPos);
	}

	@FunctionalInterface
	public interface EntityBlockHighlightRenderer {
		void addHighlightedPositions(Map<Integer, List<EntityBlockHighlightData>> highlightPositions, int durationTicks);
	}

	@FunctionalInterface
	public interface CustomGeometryRenderer {
		void render(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, float partialTick, Vec3 cameraPos);
	}
}
