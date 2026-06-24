package net.p3pp3rf1y.sophisticateditemactions.compat.ae2;

import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.parts.reporting.AbstractTerminalPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;
import net.p3pp3rf1y.sophisticateditemactions.common.IBlockEntityItemActionHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.IBlockItemActionHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.IDepositHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.IRestockHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemMatchResult;

import java.util.Optional;

public class AppliedEnergistics2TerminalItemActionHandler implements IBlockEntityItemActionHandler<CableBusBlockEntity> {
	public static final AppliedEnergistics2TerminalItemActionHandler INSTANCE = new AppliedEnergistics2TerminalItemActionHandler();
	public static final Identifier ID = SophisticatedItemActions.getIdentifier("ae2_terminal");

	@Override
	public Identifier id() {
		return ID;
	}

	@Override
	public boolean canActOn(Level level, BlockPos pos, BlockEntity blockEntity) {
		return blockEntity instanceof CableBusBlockEntity cableBus && getTerminal(cableBus).isPresent();
	}

	@Override
	public Class<CableBusBlockEntity> getObjectClass() {
		return CableBusBlockEntity.class;
	}

	@Override
	public ItemMatchResult getItemMatch(ServerPlayer player, ItemStackKey stackKey, BlockPos pos, IBlockItemActionHandler.Action action) {
		return getFromBlockEntity(player, pos, cableBus -> getTerminal(cableBus)
				.map(terminal -> AppliedEnergistics2ItemActionHelper.getItemMatch(player, stackKey, terminal, action)).orElse(ItemMatchResult.NO_MATCH))
				.orElse(ItemMatchResult.NO_MATCH);
	}

	@Override
	public ItemMatchResult getItemMatch(ItemStackKey stackKey, CableBusBlockEntity cableBus, IBlockItemActionHandler.Action action) {
		return ItemMatchResult.NO_MATCH;
	}

	@Override
	public IDepositHandler getDepositHandler(CableBusBlockEntity cableBus) {
		throw new UnsupportedOperationException("Use server player aware deposit handler lookup");
	}

	@Override
	public Optional<IDepositHandler> getDepositHandler(ServerPlayer player, BlockPos pos) {
		return getFromBlockEntity(player, pos, cableBus -> getTerminal(cableBus).map(terminal -> new IDepositHandler() {
			@Override
			public Optional<BlockPos> getPositionToOpen() {
				return Optional.of(cableBus.getBlockPos());
			}

			@Override
			public Vec3 getPosition() {
				return Vec3.atCenterOf(cableBus.getBlockPos());
			}

			@Override
			public ItemMatchResult getItemMatch(ItemStackKey stackKey) {
				return AppliedEnergistics2ItemActionHelper.getItemMatch(player, stackKey, terminal, IBlockItemActionHandler.Action.DEPOSIT);
			}

			@Override
			public int insertItem(ItemStack stack) {
				return AppliedEnergistics2ItemActionHelper.insertItem(player, stack, terminal);
			}
		})).flatMap(handler -> handler);
	}

	@Override
	public IRestockHandler getRestockHandler(CableBusBlockEntity cableBus) {
		throw new UnsupportedOperationException("Use server player aware restock handler lookup");
	}

	@Override
	public Optional<IRestockHandler> getRestockHandler(ServerPlayer player, BlockPos pos) {
		return getFromBlockEntity(player, pos, cableBus -> getTerminal(cableBus).map(terminal -> new IRestockHandler() {
			@Override
			public Optional<BlockPos> getPositionToOpen() {
				return Optional.of(cableBus.getBlockPos());
			}

			@Override
			public Vec3 getPosition() {
				return Vec3.atCenterOf(cableBus.getBlockPos());
			}

			@Override
			public int extractItem(ItemStack stack) {
				return AppliedEnergistics2ItemActionHelper.extractItem(player, stack, terminal);
			}
		})).flatMap(handler -> handler);
	}

	private static Optional<AbstractTerminalPart> getTerminal(CableBusBlockEntity cableBus) {
		for (Direction direction : Direction.values()) {
			if (cableBus.getPart(direction) instanceof AbstractTerminalPart terminal) {
				return Optional.of(terminal);
			}
		}

		return Optional.empty();
	}
}
