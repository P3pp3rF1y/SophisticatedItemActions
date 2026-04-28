package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

public record EntityBlockHighlightData(int entityId, List<List<BlockPos>> positionGroups) {
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeInt(entityId);
		buffer.writeCollection(positionGroups, (groupBuffer, group) -> groupBuffer.writeCollection(group, FriendlyByteBuf::writeBlockPos));
	}

	public static EntityBlockHighlightData decode(FriendlyByteBuf buffer) {
		return new EntityBlockHighlightData(buffer.readInt(), buffer.readList(groupBuffer -> groupBuffer.readList(FriendlyByteBuf::readBlockPos)));
	}
}
