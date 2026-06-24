package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

import java.util.List;

public record ItemTransferData(@Nullable BlockPos positionToOpen, Vec3 itemFlightPos, List<ItemStack> itemsTransferred) {
	public void encode(FriendlyByteBuf packetBuffer) {
		packetBuffer.writeBoolean(positionToOpen != null);
		if (positionToOpen != null) {
			packetBuffer.writeBlockPos(positionToOpen);
		}
		packetBuffer.writeDouble(itemFlightPos.x);
		packetBuffer.writeDouble(itemFlightPos.y);
		packetBuffer.writeDouble(itemFlightPos.z);
		packetBuffer.writeCollection(itemsTransferred, FriendlyByteBuf::writeItem);
	}

	public static ItemTransferData decode(FriendlyByteBuf packetBuffer) {
		BlockPos positionToOpen = null;
		if (packetBuffer.readBoolean()) {
			positionToOpen = packetBuffer.readBlockPos();
		}
		Vec3 itemFlightPos = new Vec3(packetBuffer.readDouble(), packetBuffer.readDouble(), packetBuffer.readDouble());
		List<ItemStack> itemsTransferred = packetBuffer.readList(FriendlyByteBuf::readItem);
		return new ItemTransferData(positionToOpen, itemFlightPos, itemsTransferred);
	}
}
