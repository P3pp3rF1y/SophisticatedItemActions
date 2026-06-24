package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record ItemTransferData(@Nullable BlockPos positionToOpen, Vec3 itemFlightPos, List<ItemStack> itemsTransferred) {
	public static final StreamCodec<RegistryFriendlyByteBuf, ItemTransferData> STREAM_CODEC = StreamCodec.composite(
			StreamCodecHelper.ofNullable(BlockPos.STREAM_CODEC), ItemTransferData::positionToOpen, StreamCodecHelper.VEC3, ItemTransferData::itemFlightPos,
			ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), ItemTransferData::itemsTransferred, ItemTransferData::new);
}
