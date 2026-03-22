package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;

import java.util.Optional;

public interface IDepositHandler {
	Optional<BlockPos> getPositionToOpen();
	Vec3 getPosition();
	ItemMatchResult getItemMatch(ItemStackKey stackKey);
	ItemStack insertItem(ItemStack stack);
}
