package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.RandHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ClaimProtectionTest {
	private static final Identifier HANDLER_ID = Identifier.parse("test:claim_protection");
	private static final BlockPos ALLOWED_POS = new BlockPos(1, 64, 1);
	private static final BlockPos DENIED_POS = new BlockPos(17, 64, 1);
	private List<BiPredicate<Player, BlockPos>> blockChecks;
	private List<BiPredicate<Player, Entity>> entityChecks;
	private List<BiPredicate<Player, BlockPos>> originalBlockChecks;
	private List<BiPredicate<Player, Entity>> originalEntityChecks;
	private ServerPlayer player;
	private ServerLevel level;
	private Entity allowedEntity;
	private Entity deniedEntity;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@BeforeEach
	void prepareTargets() throws ReflectiveOperationException {
		blockChecks = getChecks("ADDITIONAL_INTERACTION_CHECKS");
		entityChecks = getChecks("ADDITIONAL_ENTITY_INTERACTION_CHECKS");
		originalBlockChecks = new ArrayList<>(blockChecks);
		originalEntityChecks = new ArrayList<>(entityChecks);
		blockChecks.clear();
		entityChecks.clear();
		player = mock(ServerPlayer.class);
		level = mock(ServerLevel.class);
		allowedEntity = mock(Entity.class);
		deniedEntity = mock(Entity.class);
		when(player.level()).thenReturn(level);
		when(player.position()).thenReturn(Vec3.ZERO);
		when(player.mayInteract(level, ALLOWED_POS)).thenReturn(true);
		when(player.mayInteract(level, DENIED_POS)).thenReturn(true);
		when(level.getEntity(1)).thenReturn(allowedEntity);
		when(level.getEntity(2)).thenReturn(deniedEntity);
		WorldHelper.addAdditionalInteractionCheck((actor, pos) -> !pos.equals(DENIED_POS));
		WorldHelper.addAdditionalEntityInteractionCheck((actor, entity) -> entity != deniedEntity);
	}

	@AfterEach
	void restoreChecks() {
		blockChecks.clear();
		blockChecks.addAll(originalBlockChecks);
		entityChecks.clear();
		entityChecks.addAll(originalEntityChecks);
	}

	@Test
	void collectAndSortDepositHandlersSkipsDeniedBlocksAndEntities() throws ReflectiveOperationException {
		IBlockItemActionHandler blockHandler = mock(IBlockItemActionHandler.class);
		IEntityItemActionHandler entityHandler = mock(IEntityItemActionHandler.class);
		IDepositHandler blockDeposit = mock(IDepositHandler.class);
		IDepositHandler entityDeposit = mock(IDepositHandler.class);
		when(blockDeposit.getPosition()).thenReturn(Vec3.ZERO);
		when(entityDeposit.getPosition()).thenReturn(Vec3.ZERO);
		when(blockHandler.getDepositHandler(player, ALLOWED_POS)).thenReturn(Optional.of(blockDeposit));
		when(entityHandler.getDepositHandler(allowedEntity)).thenReturn(Optional.of(entityDeposit));

		List<?> handlers = collect("collectAndSortDepositHandlers", blockHandler, entityHandler);

		assertEquals(List.of(blockDeposit, entityDeposit), handlers);
		verify(blockHandler, never()).getDepositHandler(player, DENIED_POS);
		verify(entityHandler, never()).getDepositHandler(deniedEntity);
	}

	@Test
	void collectAndSortRestockHandlersSkipsDeniedBlocksAndEntities() throws ReflectiveOperationException {
		IBlockItemActionHandler blockHandler = mock(IBlockItemActionHandler.class);
		IEntityItemActionHandler entityHandler = mock(IEntityItemActionHandler.class);
		IRestockHandler blockRestock = mock(IRestockHandler.class);
		IRestockHandler entityRestock = mock(IRestockHandler.class);
		when(blockRestock.getPosition()).thenReturn(Vec3.ZERO);
		when(entityRestock.getPosition()).thenReturn(Vec3.ZERO);
		when(blockHandler.getRestockHandler(player, ALLOWED_POS)).thenReturn(Optional.of(blockRestock));
		when(entityHandler.getRestockHandler(allowedEntity)).thenReturn(Optional.of(entityRestock));

		List<?> handlers = collect("collectAndSortRestockHandlers", blockHandler, entityHandler);

		assertEquals(List.of(blockRestock, entityRestock), handlers);
		verify(blockHandler, never()).getRestockHandler(player, DENIED_POS);
		verify(entityHandler, never()).getRestockHandler(deniedEntity);
	}

	@Test
	void collectStorageItemHandlerTargetsSkipsDeniedExtensionTargets() throws ReflectiveOperationException {
		IBlockItemActionHandler blockHandler = mock(IBlockItemActionHandler.class);
		IEntityItemActionHandler entityHandler = mock(IEntityItemActionHandler.class);
		ResourceHandler<ItemResource> itemHandler = mock();
		StorageItemHandlerTarget blockTarget = new StorageItemHandlerTarget(ALLOWED_POS, Vec3.ZERO, itemHandler);
		StorageItemHandlerTarget entityTarget = new StorageItemHandlerTarget(null, Vec3.ZERO, itemHandler);
		when(blockHandler.getStorageItemHandlerTargets(player, ALLOWED_POS)).thenReturn(List.of(blockTarget));
		when(entityHandler.getStorageItemHandlerTargets(allowedEntity)).thenReturn(List.of(entityTarget));

		List<?> targets = collect("collectStorageItemHandlerTargets", blockHandler, entityHandler);

		assertEquals(List.of(blockTarget, entityTarget), targets);
		verify(blockHandler, never()).getStorageItemHandlerTargets(player, DENIED_POS);
		verify(entityHandler, never()).getStorageItemHandlerTargets(deniedEntity);
	}

	@Test
	void handleHighlightChecksClaimsBeforeInventoryAndCustomHighlightAccess() {
		IBlockItemActionHandler blockHandler = mock(IBlockItemActionHandler.class);
		IEntityItemActionHandler entityHandler = mock(IEntityItemActionHandler.class);
		ItemStackKey stackKey = mock(ItemStackKey.class);
		when(blockHandler.getItemMatch(player, stackKey, ALLOWED_POS, IBlockItemActionHandler.Action.HIGHLIGHT)).thenReturn(ItemMatchResult.NO_MATCH);
		when(entityHandler.getItemMatch(stackKey, allowedEntity)).thenReturn(ItemMatchResult.NO_MATCH);
		// These scoped mocks suppress packet sends and random access even without explicit stubbing.
		try (MockedStatic<ItemActionHandlerRegistry> registry = mockStatic(ItemActionHandlerRegistry.class);
				MockedStatic<PacketDistributor> packets = mockStatic(PacketDistributor.class);
				MockedStatic<RandHelper> random = mockStatic(RandHelper.class)) {
			registry.when(() -> ItemActionHandlerRegistry.getBlockHandler(HANDLER_ID)).thenReturn(Optional.of(blockHandler));
			registry.when(() -> ItemActionHandlerRegistry.getEntityHandler(HANDLER_ID)).thenReturn(Optional.of(entityHandler));

			HighlightHandler.handleHighlight(player, stackKey, blockTargets(), entityTargets());

			verify(blockHandler).getItemMatch(player, stackKey, ALLOWED_POS, IBlockItemActionHandler.Action.HIGHLIGHT);
			verify(entityHandler).getItemMatch(stackKey, allowedEntity);
			verify(blockHandler, never()).getItemMatch(player, stackKey, DENIED_POS, IBlockItemActionHandler.Action.HIGHLIGHT);
			verify(blockHandler, never()).getHighlightPositions(player, DENIED_POS);
			verify(entityHandler, never()).getCustomRenderedHighlightGroupsWithMatch(stackKey, deniedEntity);
			verify(entityHandler, never()).getCustomHighlightGroupsWithMatch(stackKey, deniedEntity);
			verify(entityHandler, never()).getItemMatch(stackKey, deniedEntity);
		}
	}

	@Test
	void mayInteractPreservesResolvedLevelAndRestoresClaimDenial() {
		ServerLevel storageLevel = mock(ServerLevel.class);
		ISubLevelCompat compat = mock(ISubLevelCompat.class, CALLS_REAL_METHODS);
		when(compat.getLevelForPosition(level, DENIED_POS)).thenReturn(storageLevel);
		when(player.mayInteract(storageLevel, DENIED_POS)).thenReturn(true);

		boolean allowed = compat.mayInteract(player, level, DENIED_POS);

		assertFalse(allowed);
		verify(player).mayInteract(storageLevel, DENIED_POS);
	}

	@Test
	void mayInteractClientDiscoveryDoesNotInvokeServerClaimChecks() {
		Level clientLevel = mock(Level.class);
		Player clientPlayer = mock(Player.class);
		when(clientLevel.isClientSide()).thenReturn(true);
		ISubLevelCompat compat = mock(ISubLevelCompat.class, CALLS_REAL_METHODS);
		WorldHelper.addAdditionalInteractionCheck((actor, pos) -> {
			throw new AssertionError("Server claim checks must not run during client discovery");
		});

		boolean allowed = compat.mayInteract(clientPlayer, clientLevel, DENIED_POS);

		assertTrue(allowed);
		verify(compat).getLevelForPosition(clientLevel, DENIED_POS);
		verifyNoInteractions(clientPlayer);
	}

	private List<?> collect(String name, IBlockItemActionHandler blockHandler, IEntityItemActionHandler entityHandler) throws ReflectiveOperationException {
		try (MockedStatic<ItemActionHandlerRegistry> registry = mockStatic(ItemActionHandlerRegistry.class)) {
			registry.when(() -> ItemActionHandlerRegistry.getBlockHandler(HANDLER_ID)).thenReturn(Optional.of(blockHandler));
			registry.when(() -> ItemActionHandlerRegistry.getEntityHandler(HANDLER_ID)).thenReturn(Optional.of(entityHandler));
			Method method = ItemTransferHandler.class.getDeclaredMethod(name, Player.class, Map.class, Map.class, ServerPlayer.class);
			method.setAccessible(true);
			return (List<?>) method.invoke(null, player, blockTargets(), entityTargets(), player);
		}
	}

	private static Map<Identifier, List<BlockPos>> blockTargets() {
		return Map.of(HANDLER_ID, List.of(DENIED_POS, ALLOWED_POS));
	}

	private static Map<Identifier, List<Integer>> entityTargets() {
		return Map.of(HANDLER_ID, List.of(2, 3, 1));
	}

	@SuppressWarnings("unchecked")
	private static <T> List<BiPredicate<Player, T>> getChecks(String name) throws ReflectiveOperationException {
		Field field = WorldHelper.class.getDeclaredField(name);
		field.setAccessible(true);
		return (List<BiPredicate<Player, T>>) field.get(null);
	}
}
