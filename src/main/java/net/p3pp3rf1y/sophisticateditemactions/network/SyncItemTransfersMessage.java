
package net.p3pp3rf1y.sophisticateditemactions.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.network.ISplittableMessage;
import net.p3pp3rf1y.sophisticatedcore.util.RandHelper;
import net.p3pp3rf1y.sophisticateditemactions.client.ClientEventHandler;
import net.p3pp3rf1y.sophisticateditemactions.client.render.ItemTransferClientHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemTransferData;

import java.util.List;
import java.util.function.Supplier;

public record SyncItemTransfersMessage(List<ItemTransferData> itemTransferData, Vec3 playerPos, boolean fromPlayer,
		boolean recipeRestock) implements ISplittableMessage {
	public static void encode(SyncItemTransfersMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeCollection(msg.itemTransferData, (friendlyByteBuf, itemTransferData) -> itemTransferData.encode(friendlyByteBuf));
		packetBuffer.writeDouble(msg.playerPos.x());
		packetBuffer.writeDouble(msg.playerPos.y());
		packetBuffer.writeDouble(msg.playerPos.z());
		packetBuffer.writeBoolean(msg.fromPlayer);
		packetBuffer.writeBoolean(msg.recipeRestock);
	}

	public static SyncItemTransfersMessage decode(FriendlyByteBuf packetBuffer) {
		return new SyncItemTransfersMessage(packetBuffer.readList(ItemTransferData::decode),
				new Vec3(packetBuffer.readDouble(), packetBuffer.readDouble(), packetBuffer.readDouble()), packetBuffer.readBoolean(),
				packetBuffer.readBoolean());
	}

	static void onMessage(SyncItemTransfersMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg, context));
		context.setPacketHandled(true);
	}

	public static void handleMessage(SyncItemTransfersMessage payload, NetworkEvent.Context context) {
		ItemTransferClientHandler.handleItemTransfers(payload.itemTransferData(), payload.playerPos(), payload.fromPlayer());
		if (payload.recipeRestock()) {
			if (payload.itemTransferData().isEmpty()) {
				LocalPlayer player = Minecraft.getInstance().player;
				Level level = player.level();
				level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.3F,
						RandHelper.getRandomMinusOneToOne(level.random) * 1.4F + 2.0F);
			}
			ClientEventHandler.handleRecipeRestockSync();
		}
	}
}
