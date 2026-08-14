
package net.p3pp3rf1y.sophisticateditemactions.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.util.RandHelper;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;
import net.p3pp3rf1y.sophisticateditemactions.client.ChestOpeningAnimator;
import net.p3pp3rf1y.sophisticateditemactions.client.ClientEventHandler;
import net.p3pp3rf1y.sophisticateditemactions.client.render.ItemFlightAnimator;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemTransferData;

import java.util.List;

public record SyncItemTransfersPayload(List<ItemTransferData> itemTransferData, Vec3 playerPos, boolean fromPlayer,
		boolean recipeRestock) implements CustomPacketPayload {
	public static final Type<SyncItemTransfersPayload> TYPE = new Type<>(SophisticatedCore.getRL("sync_item_transfers"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SyncItemTransfersPayload> STREAM_CODEC = StreamCodec.composite(
			ItemTransferData.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncItemTransfersPayload::itemTransferData, StreamCodecHelper.VEC3,
			SyncItemTransfersPayload::playerPos, ByteBufCodecs.BOOL, SyncItemTransfersPayload::fromPlayer, ByteBufCodecs.BOOL,
			SyncItemTransfersPayload::recipeRestock, SyncItemTransfersPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SyncItemTransfersPayload payload, IPayloadContext context) {
		payload.itemTransferData().forEach(itemTransferData -> {
			Player player = context.player();
			Vec3 from = payload.fromPlayer() ? payload.playerPos : itemTransferData.itemFlightPos();
			Vec3 to = payload.fromPlayer() ? itemTransferData.itemFlightPos() : payload.playerPos;
			Level level = player.level();
			if (itemTransferData.positionToOpen() != null) {
				ChestOpeningAnimator.animateOpeningIfOpenable(level, itemTransferData.positionToOpen());
			}
			for (ItemStack stack : itemTransferData.itemsTransferred()) {
				ItemFlightAnimator.startFlight(stack, from, to, level.getGameTime(), payload.fromPlayer() ? 15 : 10, level.getRandom());
			}
			float pitch = payload.fromPlayer()
					? RandHelper.getRandomMinusOneToOne(level.random) * 0.1F + 0.2F
					: RandHelper.getRandomMinusOneToOne(level.random) * 1.4F + 2.0F;
			level.playSound(player, to.x(), to.y(), to.z(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.3F, pitch);
		});
		if (payload.recipeRestock()) {
			if (payload.itemTransferData().isEmpty()) {
				Level level = context.player().level();
				level.playSound(context.player(), context.player().getX(), context.player().getY(), context.player().getZ(), SoundEvents.ITEM_PICKUP,
						SoundSource.PLAYERS, 0.3F, RandHelper.getRandomMinusOneToOne(level.random) * 1.4F + 2.0F);
			}
			ClientEventHandler.handleRecipeRestockSync();
		}
	}
}
