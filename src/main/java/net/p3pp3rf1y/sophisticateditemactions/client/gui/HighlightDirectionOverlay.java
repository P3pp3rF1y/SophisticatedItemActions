package net.p3pp3rf1y.sophisticateditemactions.client.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class HighlightDirectionOverlay {
	private static final ResourceLocation CONTROLS = SophisticatedItemActions.getRL("textures/gui/controls.png");
	private static final int TEXTURE_WIDTH = 256;
	private static final int TEXTURE_HEIGHT = 16;
	private static final int LINE_WIDTH = 182;
	private static final int LINE_HEIGHT = 5;
	private static final int DOT_SIZE = 5;
	private static final int DOT_V = 5;
	private static final int HUD_Y_OFFSET = 80;
	private static final int MAX_VISIBILITY_CHECK_POSITIONS = 4;
	private static final float MAX_TRACKED_YAW = 55F;
	private static final float GROUP_YAW_DEGREES = 7F;
	private static final float HIDE_CENTER_YAW = 25F;
	private static final double HIDE_DISTANCE = 10;
	private static final float FADE_STEP = 0.04F;

	private static final List<TrackedBlockTarget> BLOCK_TARGETS = new ArrayList<>();
	private static final List<TrackedEntityTarget> ENTITY_TARGETS = new ArrayList<>();
	private static long highlightExpireTime = 0;
	private static float visibilityAlpha = 0;

	public static final GuiLayer HUD_HIGHLIGHT_DIRECTIONS = HighlightDirectionOverlay::render;

	private HighlightDirectionOverlay() {
	}

	public static void addHighlightTargets(Map<Integer, List<List<BlockPos>>> blockTargets, Map<Integer, List<Integer>> entityTargets, int durationTicks) {
		BLOCK_TARGETS.clear();
		ENTITY_TARGETS.clear();

		blockTargets.forEach((color, groups) -> groups.forEach(group -> BLOCK_TARGETS.add(new TrackedBlockTarget(color, getCenter(group), group))));
		entityTargets.forEach((color, entityIds) -> entityIds.forEach(entityId -> ENTITY_TARGETS.add(new TrackedEntityTarget(color, entityId))));

		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null) {
			highlightExpireTime = mc.level.getGameTime() + durationTicks;
		}
	}

	private static Vec3 getCenter(List<BlockPos> positions) {
		double x = 0;
		double y = 0;
		double z = 0;
		for (BlockPos pos : positions) {
			x += pos.getX() + 0.5;
			y += pos.getY() + 0.5;
			z += pos.getZ() + 0.5;
		}
		int count = Math.max(positions.size(), 1);
		return new Vec3(x / count, y / count, z / count);
	}

	private static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null || mc.options.hideGui || mc.screen != null) {
			return;
		}

		boolean expired = highlightExpireTime < mc.level.getGameTime();
		List<Dot> dots = getDots(mc, player);
		if (dots.isEmpty()) {
			updateVisibilityAlpha(false);
			if (visibilityAlpha <= 0.01F) {
				clearTargets();
			}
			return;
		}
		updateVisibilityAlpha(!expired && dots.stream().anyMatch(Dot::guidanceNeeded));
		if (visibilityAlpha <= 0.01F) {
			if (expired) {
				clearTargets();
			}
			return;
		}

		int x = (guiGraphics.guiWidth() - LINE_WIDTH) / 2;
		int y = guiGraphics.guiHeight() - HUD_Y_OFFSET;
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CONTROLS, x, y, 0, 0, LINE_WIDTH, LINE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT,
				getColor(0xFFFFFF, visibilityAlpha));

		for (Dot dot : groupDots(dots)) {
			int dotX = x + Math.round((dot.normalizedPosition() + 1F) * (LINE_WIDTH - DOT_SIZE) / 2F);
			guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CONTROLS, dotX, y, 0, DOT_V, DOT_SIZE, DOT_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT,
					getColor(dot.color(), visibilityAlpha));
		}

		dots.stream().min(Comparator.comparingDouble(Dot::distance)).ifPresent(closest -> renderDistance(guiGraphics, mc.font, x, y, closest));
	}

	private static void clearTargets() {
		BLOCK_TARGETS.clear();
		ENTITY_TARGETS.clear();
		visibilityAlpha = 0;
	}

	private static List<Dot> getDots(Minecraft mc, LocalPlayer player) {
		List<Dot> dots = new ArrayList<>();
		Vec3 playerPos = player.position();
		Vec3 eyePos = player.getEyePosition();

		for (TrackedBlockTarget target : BLOCK_TARGETS) {
			dots.add(createDot(player, playerPos, target.color(), target.center(), !isCloseCenteredAndVisible(player, playerPos, eyePos, target)));
		}
		for (TrackedEntityTarget target : ENTITY_TARGETS) {
			Entity entity = mc.level.getEntity(target.entityId());
			if (entity != null) {
				Vec3 targetPos = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
				dots.add(createDot(player, playerPos, target.color(), targetPos, !isCloseCenteredAndVisible(player, playerPos, eyePos, targetPos)));
			}
		}
		return dots;
	}

	private static Dot createDot(LocalPlayer player, Vec3 playerPos, int color, Vec3 targetPos, boolean guidanceNeeded) {
		double dx = targetPos.x() - playerPos.x();
		double dz = targetPos.z() - playerPos.z();
		float targetYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
		float relativeYaw = Mth.wrapDegrees(targetYaw - player.getYRot());
		float normalizedPosition = Mth.clamp(relativeYaw / MAX_TRACKED_YAW, -1F, 1F);
		return new Dot(normalizedPosition, relativeYaw, Math.sqrt(playerPos.distanceToSqr(targetPos)), color, guidanceNeeded);
	}

	private static boolean isCloseCenteredAndVisible(LocalPlayer player, Vec3 playerPos, Vec3 eyePos, TrackedBlockTarget target) {
		return playerPos.distanceToSqr(target.center()) <= HIDE_DISTANCE * HIDE_DISTANCE && isCentered(player, target.center())
				&& isBlockTargetVisible(player, eyePos, target.positions());
	}

	private static boolean isCloseCenteredAndVisible(LocalPlayer player, Vec3 playerPos, Vec3 eyePos, Vec3 targetPos) {
		return playerPos.distanceToSqr(targetPos) <= HIDE_DISTANCE * HIDE_DISTANCE && isCentered(player, targetPos)
				&& isPointVisible(player, eyePos, targetPos, null);
	}

	private static boolean isCentered(LocalPlayer player, Vec3 targetPos) {
		double dx = targetPos.x() - player.getX();
		double dz = targetPos.z() - player.getZ();
		float targetYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
		return Math.abs(Mth.wrapDegrees(targetYaw - player.getYRot())) <= HIDE_CENTER_YAW;
	}

	private static boolean isBlockTargetVisible(LocalPlayer player, Vec3 eyePos, List<BlockPos> positions) {
		for (BlockPos pos : positions.subList(0, Math.min(positions.size(), MAX_VISIBILITY_CHECK_POSITIONS))) {
			if (isPointVisible(player, eyePos, Vec3.atCenterOf(pos), pos)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isPointVisible(LocalPlayer player, Vec3 eyePos, Vec3 targetPos, @Nullable BlockPos targetBlockPos) {
		BlockHitResult hitResult = player.level().clip(new ClipContext(eyePos, targetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (hitResult.getType() == HitResult.Type.MISS) {
			return true;
		}
		if (targetBlockPos != null) {
			return hitResult.getBlockPos().equals(targetBlockPos);
		}
		return hitResult.getLocation().distanceToSqr(eyePos) >= targetPos.distanceToSqr(eyePos) - 0.25;
	}

	private static void updateVisibilityAlpha(boolean guidanceNeeded) {
		visibilityAlpha = Mth.clamp(visibilityAlpha + (guidanceNeeded ? FADE_STEP : -FADE_STEP), 0, 1);
	}

	private static List<Dot> groupDots(List<Dot> dots) {
		List<Dot> groupedDots = new ArrayList<>();
		dots.stream().sorted(Comparator.comparingDouble(Dot::normalizedPosition)).forEach(dot -> {
			for (int i = 0; i < groupedDots.size(); i++) {
				Dot grouped = groupedDots.get(i);
				if (grouped.color() == dot.color() && (Math.abs(grouped.relativeYaw() - dot.relativeYaw()) <= GROUP_YAW_DEGREES
						|| grouped.normalizedPosition() == dot.normalizedPosition())) {
					if (dot.distance() < grouped.distance()) {
						groupedDots.set(i, dot);
					}
					return;
				}
			}
			groupedDots.add(dot);
		});
		return groupedDots;
	}

	private static void renderDistance(GuiGraphics guiGraphics, Font font, int lineX, int lineY, Dot closest) {
		String distanceText = Math.round(closest.distance()) + "m";
		int dotCenterX = lineX + Math.round((closest.normalizedPosition() + 1F) * (LINE_WIDTH - DOT_SIZE) / 2F) + DOT_SIZE / 2;
		int textX = Mth.clamp(dotCenterX - font.width(distanceText) / 2, 0, guiGraphics.guiWidth() - font.width(distanceText));
		guiGraphics.drawString(font, distanceText, textX, lineY - 10, getColor(closest.color(), visibilityAlpha), true);
	}

	private static int getColor(int color, float alpha) {
		return Math.round(alpha * 255) << 24 | color;
	}

	private record TrackedBlockTarget(int color, Vec3 center, List<BlockPos> positions) {
	}

	private record TrackedEntityTarget(int color, int entityId) {
	}

	private record Dot(float normalizedPosition, float relativeYaw, double distance, int color, boolean guidanceNeeded) {
	}
}
