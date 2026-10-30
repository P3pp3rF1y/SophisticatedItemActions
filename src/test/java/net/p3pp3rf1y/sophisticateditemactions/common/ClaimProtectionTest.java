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
import net.minecraft.world.level.block.entity.BlockEntity;
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
import static org.mockito.ArgumentMatchers.*;
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
	private static final BlockPos DENIED_POS = new BlockPos(2, 64, 1);
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
		when(player.position()).thenReturn(new Vec3(0, 64, 0));
		when(level.hasChunkAt(any())).thenReturn(true);
		when(level.getBlockEntity(any())).thenReturn(mock(BlockEntity.class));
		when(allowedEntity.isAlive()).thenReturn(true);
		when(deniedEntity.isAlive()).thenReturn(true);
		when(allowedEntity.position()).thenReturn(new Vec3(1, 64, 1));
		when(deniedEntity.position()).thenReturn(new Vec3(2, 64, 1));
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
	void collectAndSortDepositHandlersSkipsDeniedBlocks() throws ReflectiveOperationException {
		IBlockItemActionHandler blockHandler = mock(IBlockItemActionHandler.class);
		IEntityItemActionHandler entityHandler = mock(IEntityItemActionHandler.class);
		IDepositHandler blockDeposit = mock(IDepositHandler.class);
		when(blockDeposit.getPosition()).thenReturn(Vec3.ZERO);
		when(blockHandler.getDepositHandler(player, ALLOWED_POS)).thenReturn(Optional.of(blockDeposit));

		List<?> handlers = collect("collectAndSortDepositHandlers", blockHandler, entityHandler, blockTargets(), Map.of());

		assertEquals(List.of(blockDeposit), handlers);
		verify(blockHandler, never()).getDepositHandler(player, DENIED_POS);
	}

	@Test
	void collectAndSortDepositHandlersSkipsDeniedEntities() throws ReflectiveOperationException {
		IEntityItemActionHandler entityHandler = mock(IEntityItemActionHandler.class);
		IDepositHandler entityDeposit = mock(IDepositHandler.class);
		IDepositHandler deniedDeposit = mock(IDepositHandler.class);
		when(entityDeposit.getPosition()).thenReturn(Vec3.ZERO);
		when(deniedDeposit.getPosition()).thenReturn(Vec3.ZERO);
		when(entityHandler.getDepositHandler(allowedEntity)).thenReturn(Optional.of(entityDeposit));
		when(entityHandler.getDepositHandler(deniedEntity)).thenReturn(Optional.of(deniedDeposit));
		when(entityHandler.id()).thenReturn(HANDLER_ID);
		try (MockedStatic<ItemActionHandlerRegistry> registry = mockStatic(ItemActionHandlerRegistry.class, CALLS_REAL_METHODS)) {
			registry.when(() -> ItemActionHandlerRegistry.getEntityHandler(HANDLER_ID)).thenReturn(Optional.of(entityHandler));
			registry.when(() -> ItemActionHandlerRegistry.getEntityHandlerIdFor(allowedEntity)).thenReturn(Optional.of(HANDLER_ID));
			registry.when(() -> ItemActionHandlerRegistry.getEntityHandlerIdFor(deniedEntity)).thenReturn(Optional.of(HANDLER_ID));

			List<?> handlers = invokeCollect("collectAndSortDepositHandlers", Map.of(), entityTargets());

			assertEquals(List.of(entityDeposit), handlers);
			verify(entityHandler, never()).getDepositHandler(deniedEntity);
		}
	}

	@Test
	void collectAndSortRestockHandlersSkipsDeniedBlocks() throws ReflectiveOperationException {
		IBlockItemActionHandler blockHandler = mock(IBlockItemActionHandler.class);
		IEntityItemActionHandler entityHandler = mock(IEntityItemActionHandler.class);
		IRestockHandler blockRestock = mock(IRestockHandler.class);
		when(blockRestock.getPosition()).thenReturn(Vec3.ZERO);
		when(blockHandler.getRestockHandler(player, ALLOWED_POS)).thenReturn(Optional.of(blockRestock));

		List<?> handlers = collect("collectAndSortRestockHandlers", blockHandler, entityHandler, blockTargets(), Map.of());

		assertEquals(List.of(blockRestock), handlers);
		verify(blockHandler, never()).getRestockHandler(player, DENIED_POS);
	}

	@Test
	void collectAndSortRestockHandlersSkipsDeniedEntities() throws ReflectiveOperationException {
		IEntityItemActionHandler entityHandler = mock(IEntityItemActionHandler.class);
		IRestockHandler entityRestock = mock(IRestockHandler.class);
		IRestockHandler deniedRestock = mock(IRestockHandler.class);
		when(entityRestock.getPosition()).thenReturn(Vec3.ZERO);
		when(deniedRestock.getPosition()).thenReturn(Vec3.ZERO);
		when(entityHandler.getRestockHandler(allowedEntity)).thenReturn(Optional.of(entityRestock));
		when(entityHandler.getRestockHandler(deniedEntity)).thenReturn(Optional.of(deniedRestock));
		when(entityHandler.id()).thenReturn(HANDLER_ID);
		try (MockedStatic<ItemActionHandlerRegistry> registry = mockStatic(ItemActionHandlerRegistry.class, CALLS_REAL_METHODS)) {
			registry.when(() -> ItemActionHandlerRegistry.getEntityHandler(HANDLER_ID)).thenReturn(Optional.of(entityHandler));
			registry.when(() -> ItemActionHandlerRegistry.getEntityHandlerIdFor(allowedEntity)).thenReturn(Optional.of(HANDLER_ID));
			registry.when(() -> ItemActionHandlerRegistry.getEntityHandlerIdFor(deniedEntity)).thenReturn(Optional.of(HANDLER_ID));

			List<?> handlers = invokeCollect("collectAndSortRestockHandlers", Map.of(), entityTargets());

			assertEquals(List.of(entityRestock), handlers);
			verify(entityHandler, never()).getRestockHandler(deniedEntity);
		}
	}

	@Test
	void collectStorageItemHandlerTargetsSkipsDeniedBlockExtensionTargets() throws ReflectiveOperationException {
		IBlockItemActionHandler blockHandler = mock(IBlockItemActionHandler.class);
		IEntityItemActionHandler entityHandler = mock(IEntityItemActionHandler.class);
		ResourceHandler<ItemResource> itemHandler = mock();
		StorageItemHandlerTarget blockTarget = new StorageItemHandlerTarget(ALLOWED_POS, Vec3.ZERO, itemHandler);
		when(blockHandler.getStorageItemHandlerTargets(player, ALLOWED_POS)).thenReturn(List.of(blockTarget));

		List<?> targets = collect("collectStorageItemHandlerTargets", blockHandler, entityHandler, blockTargets(), Map.of());

		assertEquals(List.of(blockTarget), targets);
		verify(blockHandler, never()).getStorageItemHandlerTargets(player, DENIED_POS);
	}

	@Test
	void collectStorageItemHandlerTargetsSkipsDeniedEntityExtensionTargets() throws ReflectiveOperationException {
		IEntityItemActionHandler entityHandler = mock(IEntityItemActionHandler.class);
		ResourceHandler<ItemResource> itemHandler = mock();
		StorageItemHandlerTarget entityTarget = new StorageItemHandlerTarget(null, Vec3.ZERO, itemHandler);
		StorageItemHandlerTarget deniedTarget = new StorageItemHandlerTarget(null, Vec3.ZERO, itemHandler);
		when(entityHandler.getStorageItemHandlerTargets(allowedEntity)).thenReturn(List.of(entityTarget));
		when(entityHandler.getStorageItemHandlerTargets(deniedEntity)).thenReturn(List.of(deniedTarget));
		when(entityHandler.id()).thenReturn(HANDLER_ID);
		try (MockedStatic<ItemActionHandlerRegistry> registry = mockStatic(ItemActionHandlerRegistry.class, CALLS_REAL_METHODS)) {
			registry.when(() -> ItemActionHandlerRegistry.getEntityHandler(HANDLER_ID)).thenReturn(Optional.of(entityHandler));
			registry.when(() -> ItemActionHandlerRegistry.getEntityHandlerIdFor(allowedEntity)).thenReturn(Optional.of(HANDLER_ID));
			registry.when(() -> ItemActionHandlerRegistry.getEntityHandlerIdFor(deniedEntity)).thenReturn(Optional.of(HANDLER_ID));

			List<?> targets = invokeCollect("collectStorageItemHandlerTargets", Map.of(), entityTargets());

			assertEquals(List.of(entityTarget), targets);
			verify(entityHandler, never()).getStorageItemHandlerTargets(deniedEntity);
		}
	}

	@Test
	void handleHighlightChecksBlockClaimsBeforeInventoryAndCustomHighlightAccess() {
		IBlockItemActionHandler blockHandler = mock(IBlockItemActionHandler.class);
		IEntityItemActionHandler entityHandler = mock(IEntityItemActionHandler.class);
		ItemStackKey stackKey = mock(ItemStackKey.class);
		when(blockHandler.getItemMatch(player, stackKey, ALLOWED_POS, IBlockItemActionHandler.Action.HIGHLIGHT)).thenReturn(ItemMatchResult.NO_MATCH);

		highlight(stackKey, blockHandler, entityHandler, blockTargets(), Map.of());

		verify(blockHandler).getItemMatch(player, stackKey, ALLOWED_POS, IBlockItemActionHandler.Action.HIGHLIGHT);
		verify(blockHandler, never()).getItemMatch(player, stackKey, DENIED_POS, IBlockItemActionHandler.Action.HIGHLIGHT);
		verify(blockHandler, never()).getHighlightPositions(player, DENIED_POS);
	}

	@Test
	void handleHighlightChecksEntityClaimsBeforeInventoryAndCustomHighlightAccess() {
		IEntityItemActionHandler entityHandler = mock(IEntityItemActionHandler.class);
		ItemStackKey stackKey = mock(ItemStackKey.class);
		when(entityHandler.getItemMatch(stackKey, allowedEntity)).thenReturn(ItemMatchResult.NO_MATCH);
		when(entityHandler.getItemMatch(stackKey, deniedEntity)).thenReturn(ItemMatchResult.NO_MATCH);
		when(entityHandler.id()).thenReturn(HANDLER_ID);
		try (MockedStatic<ItemActionHandlerRegistry> registry = mockStatic(ItemActionHandlerRegistry.class, CALLS_REAL_METHODS);
				MockedStatic<PacketDistributor> packets = mockStatic(PacketDistributor.class);
				MockedStatic<RandHelper> random = mockStatic(RandHelper.class)) {
			registry.when(() -> ItemActionHandlerRegistry.getEntityHandler(HANDLER_ID)).thenReturn(Optional.of(entityHandler));
			registry.when(() -> ItemActionHandlerRegistry.getEntityHandlerIdFor(allowedEntity)).thenReturn(Optional.of(HANDLER_ID));
			registry.when(() -> ItemActionHandlerRegistry.getEntityHandlerIdFor(deniedEntity)).thenReturn(Optional.of(HANDLER_ID));

			HighlightHandler.handleHighlight(player, stackKey, Map.of(), entityTargets());

			verify(entityHandler).getItemMatch(stackKey, allowedEntity);
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

	private List<?> collect(String name, IBlockItemActionHandler blockHandler, IEntityItemActionHandler entityHandler, Map<Identifier, List<BlockPos>> blocks,
			Map<Identifier, List<Integer>> entities) throws ReflectiveOperationException {
		try (MockedStatic<ItemActionHandlerRegistry> registry = mockStatic(ItemActionHandlerRegistry.class, CALLS_REAL_METHODS)) {
			registry.when(() -> ItemActionHandlerRegistry.getBlockHandler(HANDLER_ID)).thenReturn(Optional.of(blockHandler));
			registry.when(() -> ItemActionHandlerRegistry.getEntityHandler(HANDLER_ID)).thenReturn(Optional.of(entityHandler));
			stubCanonicalHandlers(registry, blockHandler, entityHandler);
			return invokeCollect(name, blocks, entities);
		}
	}

	private List<?> invokeCollect(String name, Map<Identifier, List<BlockPos>> blocks, Map<Identifier, List<Integer>> entities)
			throws ReflectiveOperationException {
		Method method = ItemTransferHandler.class.getDeclaredMethod(name, Player.class, Map.class, Map.class, ServerPlayer.class);
		method.setAccessible(true);
		return (List<?>) method.invoke(null, player, blocks, entities, player);
	}

	private void highlight(ItemStackKey stackKey, IBlockItemActionHandler blockHandler, IEntityItemActionHandler entityHandler,
			Map<Identifier, List<BlockPos>> blocks, Map<Identifier, List<Integer>> entities) {
		try (MockedStatic<ItemActionHandlerRegistry> registry = mockStatic(ItemActionHandlerRegistry.class, CALLS_REAL_METHODS);
				MockedStatic<PacketDistributor> packets = mockStatic(PacketDistributor.class);
				MockedStatic<RandHelper> random = mockStatic(RandHelper.class)) {
			registry.when(() -> ItemActionHandlerRegistry.getBlockHandler(HANDLER_ID)).thenReturn(Optional.of(blockHandler));
			registry.when(() -> ItemActionHandlerRegistry.getEntityHandler(HANDLER_ID)).thenReturn(Optional.of(entityHandler));
			stubCanonicalHandlers(registry, blockHandler, entityHandler);
			HighlightHandler.handleHighlight(player, stackKey, blocks, entities);
		}
	}

	private void stubCanonicalHandlers(MockedStatic<ItemActionHandlerRegistry> registry, IBlockItemActionHandler blockHandler,
			IEntityItemActionHandler entityHandler) {
		when(blockHandler.id()).thenReturn(HANDLER_ID);
		when(entityHandler.id()).thenReturn(HANDLER_ID);
		registry.when(() -> ItemActionHandlerRegistry.getBlockHandlerFor(eq(level), any(), any(), any())).thenReturn(Optional.of(blockHandler));
		registry.when(() -> ItemActionHandlerRegistry.getEntityHandlerIdFor(allowedEntity)).thenReturn(Optional.of(HANDLER_ID));
	}

	@Test
	void canUseBlockHandlerRejectsDistantTargetBeforeLookingUpBlockEntity() {
		IBlockItemActionHandler blockHandler = mock(IBlockItemActionHandler.class);
		when(player.position()).thenReturn(Vec3.ZERO);

		boolean allowed = ItemActionHandlerRegistry.canUseBlockHandler(player, ALLOWED_POS, blockHandler, IBlockItemActionHandler.Action.RESTOCK, 10);

		assertFalse(allowed);
		verify(level, never()).getBlockEntity(any());
	}

	@Test
	void canUseEntityHandlerRejectsDistantTarget() {
		IEntityItemActionHandler entityHandler = mock(IEntityItemActionHandler.class);
		when(player.position()).thenReturn(Vec3.ZERO);

		boolean allowed = ItemActionHandlerRegistry.canUseEntityHandler(player, allowedEntity, entityHandler, 10);

		assertFalse(allowed);
	}

	@Test
	void canUseBlockHandlerRejectsUnloadedTargetBeforeLookingUpBlockEntity() {
		IBlockItemActionHandler blockHandler = mock(IBlockItemActionHandler.class);
		when(level.hasChunkAt(ALLOWED_POS)).thenReturn(false);

		boolean allowed = ItemActionHandlerRegistry.canUseBlockHandler(player, ALLOWED_POS, blockHandler, IBlockItemActionHandler.Action.RESTOCK, 10);

		assertFalse(allowed);
		verify(level, never()).getBlockEntity(any());
	}

	@Test
	void canUseBlockHandlerRejectsNonCanonicalHandler() {
		IBlockItemActionHandler blockHandler = mock(IBlockItemActionHandler.class);
		when(blockHandler.id()).thenReturn(Identifier.parse("test:forged"));
		try (MockedStatic<ItemActionHandlerRegistry> registry = mockStatic(ItemActionHandlerRegistry.class, CALLS_REAL_METHODS)) {
			IBlockItemActionHandler canonical = mock(IBlockItemActionHandler.class);
			when(canonical.id()).thenReturn(HANDLER_ID);
			registry.when(() -> ItemActionHandlerRegistry.getBlockHandlerFor(eq(level), any(), any(), any())).thenReturn(Optional.of(canonical));

			boolean allowed = ItemActionHandlerRegistry.canUseBlockHandler(player, ALLOWED_POS, blockHandler, IBlockItemActionHandler.Action.RESTOCK, 10);

			assertFalse(allowed);
		}
	}

	@Test
	void canUseEntityHandlerRejectsNonCanonicalHandler() {
		IEntityItemActionHandler entityHandler = mock(IEntityItemActionHandler.class);
		when(entityHandler.id()).thenReturn(Identifier.parse("test:forged"));
		try (MockedStatic<ItemActionHandlerRegistry> registry = mockStatic(ItemActionHandlerRegistry.class, CALLS_REAL_METHODS)) {
			registry.when(() -> ItemActionHandlerRegistry.getEntityHandlerIdFor(allowedEntity)).thenReturn(Optional.of(HANDLER_ID));

			boolean allowed = ItemActionHandlerRegistry.canUseEntityHandler(player, allowedEntity, entityHandler, 10);

			assertFalse(allowed);
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
