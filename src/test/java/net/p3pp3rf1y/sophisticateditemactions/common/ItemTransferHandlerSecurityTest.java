package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ItemTransferHandlerSecurityTest {
	@Test
	void handleDepositRejectsOversizedIntervalBeforeAccessingPlayerOrTargets() {
		ServerPlayer player = mock(ServerPlayer.class);
		Map<Identifier, List<BlockPos>> blocks = Map.of();
		Map<Identifier, List<Integer>> entities = Map.of();

		ItemTransferHandler.handleDeposit(player, 0, Integer.MAX_VALUE, blocks, entities, true);

		verifyNoInteractions(player);
	}

	@Test
	void handleDepositRejectsNegativeSlotBeforeAccessingPlayerOrTargets() {
		ServerPlayer player = mock(ServerPlayer.class);
		Map<Identifier, List<BlockPos>> blocks = Map.of();
		Map<Identifier, List<Integer>> entities = Map.of();

		ItemTransferHandler.handleDeposit(player, -1, 1, blocks, entities, true);

		verifyNoInteractions(player);
	}

	@Test
	void handleRestockRejectsEquipmentIntervalBeforeAccessingPlayerOrTargets() {
		ServerPlayer player = mock(ServerPlayer.class);
		Map<Identifier, List<BlockPos>> blocks = Map.of();
		Map<Identifier, List<Integer>> entities = Map.of();

		ItemTransferHandler.handleRestock(player, blocks, entities, 36, 40, ItemStack.EMPTY, true, false);

		verifyNoInteractions(player);
	}

	@Test
	void handleAlternativeRestockRejectsReversedIntervalBeforeAccessingPlayerOrTargets() {
		ServerPlayer player = mock(ServerPlayer.class);
		Map<Identifier, List<BlockPos>> blocks = Map.of();
		Map<Identifier, List<Integer>> entities = Map.of();

		ItemTransferHandler.handleAlternativeRestock(player, blocks, entities, List.of(ItemStack.EMPTY), 4, 3, true, false);

		verifyNoInteractions(player);
	}
}
