package net.p3pp3rf1y.sophisticateditemactions.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.client.render.BlockHighlightRenderHelper;
import net.p3pp3rf1y.sophisticatedcore.client.render.BlockHighlightRenderer;
import net.p3pp3rf1y.sophisticatedcore.util.Easing;
import net.p3pp3rf1y.sophisticateditemactions.common.RenderedHighlight;
import net.p3pp3rf1y.sophisticateditemactions.common.SubLevelCompatHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RenderedBlockHighlightRenderer {
	private static final Map<Integer, List<List<BlockPos>>> highlightedPositions = new HashMap<>();
	private static long highlightExpireTime = 0;

	private RenderedBlockHighlightRenderer() {
	}

	public static void addHighlightedPositions(Map<Integer, List<List<BlockPos>>> highlightPositions, int durationTicks) {
		highlightPositions.forEach((color, positions) -> highlightedPositions.computeIfAbsent(color, k -> new ArrayList<>()).addAll(positions));
		highlightExpireTime = Minecraft.getInstance().level.getGameTime() + durationTicks;
	}

	public static void render(PoseStack poseStack, float partialTick, Vec3 cameraPos) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}

		if (highlightExpireTime < mc.level.getGameTime()) {
			if (!highlightedPositions.isEmpty()) {
				highlightedPositions.clear();
			}
			return;
		}

		Map<Integer, List<RenderedHighlight>> renderedHighlights = new HashMap<>();
		highlightedPositions.forEach((color, positionGroups) -> renderedHighlights.put(color, positionGroups.stream()
				.map(positions -> SubLevelCompatHelper.getRenderedHighlight(mc.level, positions, partialTick)).filter(Objects::nonNull).toList()));

		SubmitNodeCollector submitNodeCollector = mc.gameRenderer.getSubmitNodeStorage();
		renderedHighlights.forEach((color, highlights) -> highlights
				.forEach(highlight -> renderHighlightedBlock(submitNodeCollector, poseStack, partialTick, cameraPos, highlight, mc, color)));
	}

	private static void renderHighlightedBlock(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, float partialTick, Vec3 cameraPos,
			RenderedHighlight highlight, Minecraft mc, int color) {
		poseStack.pushPose();
		RenderedHighlight.RenderTransform transform = highlight.transform();
		if (transform == null) {
			poseStack.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());
		} else {
			poseStack.translate(transform.position().x - cameraPos.x(), transform.position().y - cameraPos.y(), transform.position().z - cameraPos.z());
			poseStack.mulPose(transform.orientation());
			poseStack.scale((float) transform.scale().x, (float) transform.scale().y, (float) transform.scale().z);
			poseStack.translate(-transform.rotationPoint().x, -transform.rotationPoint().y, -transform.rotationPoint().z);
		}
		poseStack.translate(highlight.pivot().x, highlight.pivot().y, highlight.pivot().z);
		float scale = 1 + Easing.EASE_IN_OUT_CUBIC.ease((float) BlockHighlightRenderer.tri01(mc.level.getGameTime(), 15, partialTick)) * 0.05f;
		poseStack.scale(scale, scale, scale);
		poseStack.translate(-highlight.pivot().x, -highlight.pivot().y, -highlight.pivot().z);
		BlockHighlightRenderHelper.submitThickEdges(submitNodeCollector, poseStack, color, highlight.edges(), 0, 0, 0);
		poseStack.popPose();
	}
}
