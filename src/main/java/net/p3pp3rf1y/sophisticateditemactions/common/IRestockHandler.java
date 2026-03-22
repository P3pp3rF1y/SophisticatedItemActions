package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public interface IRestockHandler {
	Optional<BlockPos> getPositionToOpen();
	Vec3 getPosition();
	ItemStack extractItem(ItemStack stack);
}
