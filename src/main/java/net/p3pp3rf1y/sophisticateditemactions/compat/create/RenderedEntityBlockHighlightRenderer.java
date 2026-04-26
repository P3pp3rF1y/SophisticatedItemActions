package net.p3pp3rf1y.sophisticateditemactions.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.render.ClientContraption;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.p3pp3rf1y.sophisticatedcore.client.render.BlockHighlightRenderHelper;
import net.p3pp3rf1y.sophisticatedcore.client.render.BlockHighlightRenderer;
import net.p3pp3rf1y.sophisticatedcore.util.Easing;
import net.p3pp3rf1y.sophisticatedcore.util.VoxelOutliner;
import net.p3pp3rf1y.sophisticateditemactions.common.EntityBlockHighlightData;
import net.p3pp3rf1y.sophisticateditemactions.common.RenderedHighlight;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RenderedEntityBlockHighlightRenderer {
	private static final Map<Integer, List<EntityBlockHighlightData>> highlightedPositions = new HashMap<>();
	private static final Map<Integer, List<CachedEntityHighlight>> cachedHighlights = new HashMap<>();
	private static long highlightExpireTime = 0;

	private RenderedEntityBlockHighlightRenderer() {
	}

	public static void addHighlightedPositions(Map<Integer, List<EntityBlockHighlightData>> highlightPositions, int durationTicks) {
		highlightedPositions.clear();
		cachedHighlights.clear();
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
				cachedHighlights.clear();
			}
			return;
		}

		MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
		highlightedPositions.forEach((color, entityHighlightData) -> entityHighlightData.forEach(highlightData -> {
			CachedEntityHighlight cachedHighlight = getOrCreateCachedHighlight(mc, color, highlightData);
			if (cachedHighlight != null) {
				renderHighlightedBlock(poseStack, partialTick, cameraPos, cachedHighlight, mc, buffer, color);
			}
		}));
	}

	@Nullable
	private static CachedEntityHighlight getOrCreateCachedHighlight(Minecraft mc, int color, EntityBlockHighlightData highlightData) {
		List<CachedEntityHighlight> highlightsForColor = cachedHighlights.computeIfAbsent(color, key -> new ArrayList<>());
		for (CachedEntityHighlight cachedHighlight : highlightsForColor) {
			if (cachedHighlight.matches(highlightData)) {
				return cachedHighlight;
			}
		}

		CachedEntityHighlight cachedHighlight = createCachedHighlight(mc, highlightData);
		if (cachedHighlight != null) {
			highlightsForColor.add(cachedHighlight);
		}
		return cachedHighlight;
	}

	@Nullable
	private static CachedEntityHighlight createCachedHighlight(Minecraft mc, EntityBlockHighlightData highlightData) {
		Entity entity = mc.level.getEntity(highlightData.entityId());
		if (!(entity instanceof AbstractContraptionEntity contraptionEntity) || contraptionEntity.getContraption() == null) {
			return null;
		}

		List<VoxelOutliner.Edge> edges = new ArrayList<>();
		VoxelShape shape = Shapes.empty();
		Vec3 pivotSum = Vec3.ZERO;
		int count = 0;
		ClientContraption clientContraption = contraptionEntity.getContraption().getOrCreateClientContraptionLazy();
		Level renderLevel = clientContraption.getRenderLevel();
		for (BlockPos pos : highlightData.positions()) {
			StructureTemplate.StructureBlockInfo blockInfo = contraptionEntity.getContraption().getBlocks().get(pos);
			if (blockInfo == null) {
				continue;
			}

			BlockState blockState = renderLevel.getBlockState(pos);
			shape = Shapes.join(shape, blockState.getShape(renderLevel, pos).move(pos.getX(), pos.getY(), pos.getZ()), BooleanOp.OR);

			pivotSum = pivotSum.add(Vec3.atCenterOf(pos));
			count++;
		}

		if (shape.isEmpty() || count == 0) {
			return null;
		}

		for (VoxelOutliner.Edge edge : VoxelOutliner.linesFromVoxelShapeSimplified(shape, BlockPos.ZERO)) {
			edges.add(new VoxelOutliner.Edge(edge.a(), edge.b()));
		}

		return new CachedEntityHighlight(highlightData.entityId(), List.copyOf(highlightData.positions()), new RenderedHighlight(edges, pivotSum.scale(1D / count)));
	}

	private static void renderHighlightedBlock(PoseStack poseStack, float partialTick, Vec3 cameraPos, CachedEntityHighlight cachedHighlight, Minecraft mc, MultiBufferSource.BufferSource buffer, int color) {
		Entity entity = mc.level.getEntity(cachedHighlight.entityId());
		if (!(entity instanceof AbstractContraptionEntity contraptionEntity) || contraptionEntity.getContraption() == null) {
			return;
		}

		RenderedHighlight highlight = cachedHighlight.highlight();
		poseStack.pushPose();
		poseStack.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());
		poseStack.translate(Mth.lerp(partialTick, entity.xOld, entity.getX()), Mth.lerp(partialTick, entity.yOld, entity.getY()), Mth.lerp(partialTick, entity.zOld, entity.getZ()));
		contraptionEntity.applyLocalTransforms(poseStack, partialTick);
		poseStack.translate(highlight.pivot().x, highlight.pivot().y, highlight.pivot().z);
		float scale = 1 + Easing.EASE_IN_OUT_CUBIC.ease((float) BlockHighlightRenderer.tri01(mc.level.getGameTime(), 15, partialTick)) * 0.05f;
		poseStack.scale(scale, scale, scale);
		poseStack.translate(-highlight.pivot().x, -highlight.pivot().y, -highlight.pivot().z);
		BlockHighlightRenderHelper.renderThickEdges(poseStack, buffer, color, highlight.edges(), 0, 0, 0);
		poseStack.popPose();
	}

	private record CachedEntityHighlight(int entityId, List<BlockPos> positions, RenderedHighlight highlight) {
		private boolean matches(EntityBlockHighlightData highlightData) {
			return entityId == highlightData.entityId() && positions.equals(highlightData.positions());
		}
	}
}
