package net.p3pp3rf1y.sophisticateditemactions.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticateditemactions.common.EntityBlockHighlightData;

import java.util.List;
import java.util.Map;

public class ClientCompatRenderHelper {
	private static EntityBlockHighlightSink entityBlockHighlightSink = (highlightPositions, durationTicks) -> {
	};
	private static RenderCallback renderCallback = (poseStack, partialTick, cameraPos) -> {
	};

	private ClientCompatRenderHelper() {
	}

	public static void registerEntityBlockHighlightSink(EntityBlockHighlightSink sink) {
		entityBlockHighlightSink = sink;
	}

	public static void registerRenderCallback(RenderCallback callback) {
		renderCallback = callback;
	}

	public static void addEntityBlockHighlights(Map<Integer, List<EntityBlockHighlightData>> highlightPositions, int durationTicks) {
		entityBlockHighlightSink.addHighlightedPositions(highlightPositions, durationTicks);
	}

	public static void render(PoseStack poseStack, float partialTick, Vec3 cameraPos) {
		renderCallback.render(poseStack, partialTick, cameraPos);
	}

	@FunctionalInterface
	public interface EntityBlockHighlightSink {
		void addHighlightedPositions(Map<Integer, List<EntityBlockHighlightData>> highlightPositions, int durationTicks);
	}

	@FunctionalInterface
	public interface RenderCallback {
		void render(PoseStack poseStack, float partialTick, Vec3 cameraPos);
	}
}
