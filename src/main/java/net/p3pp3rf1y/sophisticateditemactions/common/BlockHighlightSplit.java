package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;

public record BlockHighlightSplit(Map<Integer, List<List<BlockPos>>> worldHighlights, Map<Integer, List<List<BlockPos>>> subLevelHighlights) {
}
