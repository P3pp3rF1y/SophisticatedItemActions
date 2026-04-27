package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

public record EntityBlockHighlightData(int entityId, List<BlockPos> positions) {
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeInt(entityId);
		buffer.writeCollection(positions, FriendlyByteBuf::writeBlockPos);
	}

	public static EntityBlockHighlightData decode(FriendlyByteBuf buffer) {
		return new EntityBlockHighlightData(buffer.readInt(), buffer.readList(FriendlyByteBuf::readBlockPos));
	}
}
