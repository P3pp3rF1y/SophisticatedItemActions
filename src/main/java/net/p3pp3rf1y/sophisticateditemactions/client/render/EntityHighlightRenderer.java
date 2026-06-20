package net.p3pp3rf1y.sophisticateditemactions.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.client.render.BlockHighlightRenderHelper;
import net.p3pp3rf1y.sophisticatedcore.client.render.BlockHighlightRenderer;
import net.p3pp3rf1y.sophisticatedcore.util.Easing;
import net.p3pp3rf1y.sophisticatedcore.util.VoxelOutliner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityHighlightRenderer {
	public static final int HIGHLIGHT_DURATION = 40;
	private static long highlightExpireTime = 0;

	private static Map<Integer, List<Integer>> highlightedStackEntityIds = new HashMap<>();

	public static void addHighlightedEntities(Map<Integer, List<Integer>> highlightEntities, int durationTicks) {
		highlightEntities.forEach((color, entityIds) ->
				highlightedStackEntityIds.computeIfAbsent(color, k -> new ArrayList<>()).addAll(entityIds)
		);
		highlightExpireTime = Minecraft.getInstance().level.getGameTime() + durationTicks;
	}

	public static void render(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, float partialTick, Vec3 cameraPos) {
		Minecraft mc = Minecraft.getInstance();
		if (highlightExpireTime < mc.level.getGameTime()) {
			if (!highlightedStackEntityIds.isEmpty()) {
				highlightedStackEntityIds.clear();
			}
			return;
		}
		highlightedStackEntityIds.forEach((color, highlightedEntities) -> {
			highlightedEntities.forEach(he -> submitHighlightedEntity(submitNodeCollector, poseStack, partialTick, cameraPos, he, mc, color));
		});
	}

	private static void submitHighlightedEntity(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, float partialTick, Vec3 cameraPos, int entityId, Minecraft mc, int color) {
		Entity entity = mc.level.getEntity(entityId);
		if (entity == null) {
			return;
		}

		AABB boundingBox = entity.getBoundingBox();
		double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
		double y = Mth.lerp(partialTick, entity.yOld, entity.getY());
		double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());
		poseStack.pushPose();
		double halfH = boundingBox.getYsize() * 0.5;
		poseStack.translate(x - cameraPos.x(), y - cameraPos.y(), z - cameraPos.z());
		poseStack.translate(0, halfH, 0);
		float scale = 1 + Easing.EASE_IN_OUT_CUBIC.ease((float) BlockHighlightRenderer.tri01(mc.level.getGameTime(), 15, partialTick)) * 0.05f;
		poseStack.scale(scale, scale, scale);
		poseStack.translate(0, -halfH, 0);
		BlockHighlightRenderHelper.submitThickEdges(submitNodeCollector, poseStack, color, VoxelOutliner.edgesFromAABB(boundingBox), entity.getX(), entity.getY(), entity.getZ());
		poseStack.popPose();
	}
}
