package net.p3pp3rf1y.sophisticateditemactions.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.util.RandHelper;
import net.p3pp3rf1y.sophisticateditemactions.client.ChestOpeningAnimator;
import net.p3pp3rf1y.sophisticateditemactions.client.ClientEventHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemTransferData;

import java.util.List;

public class ItemTransferClientHandler {
	private ItemTransferClientHandler() {
	}

	public static void handleItemTransfers(List<ItemTransferData> itd, Vec3 playerPos, boolean fromPlayer, boolean recipeRestock) {
		LocalPlayer player = Minecraft.getInstance().player;
		Level level = player.level();

		itd.forEach(itemTransferData -> {
			Vec3 from = fromPlayer ? playerPos : itemTransferData.itemFlightPos();
			Vec3 to = fromPlayer ? itemTransferData.itemFlightPos() : playerPos;
			if (itemTransferData.positionToOpen() != null) {
				ChestOpeningAnimator.animateOpeningIfOpenable(level, itemTransferData.positionToOpen());
			}
			for (ItemStack stack : itemTransferData.itemsTransferred()) {
				ItemFlightAnimator.startFlight(stack, from, to, level.getGameTime(), fromPlayer ? 15 : 10, level.getRandom());
			}
			float pitch = fromPlayer
					? RandHelper.getRandomMinusOneToOne(level.random) * 0.1F + 0.2F
					: RandHelper.getRandomMinusOneToOne(level.random) * 1.4F + 2.0F;
			level.playSound(player, to.x(), to.y(), to.z(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.3F, pitch);
		});
		if (recipeRestock) {
			if (itd.isEmpty()) {
				level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.3F,
						RandHelper.getRandomMinusOneToOne(level.random) * 1.4F + 2.0F);
			}
			ClientEventHandler.handleRecipeRestockSync();
		}
	}
}
