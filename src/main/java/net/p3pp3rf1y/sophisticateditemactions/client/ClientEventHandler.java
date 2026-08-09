package net.p3pp3rf1y.sophisticateditemactions.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;
import net.p3pp3rf1y.sophisticateditemactions.client.discovery.ItemActionNudgeManager;
import net.p3pp3rf1y.sophisticateditemactions.client.discovery.NudgeActionUsageTracker;
import net.p3pp3rf1y.sophisticateditemactions.client.discovery.NudgeHintType;
import net.p3pp3rf1y.sophisticateditemactions.client.gui.HighlightDirectionOverlay;
import net.p3pp3rf1y.sophisticateditemactions.client.gui.ItemActionsTranslationHelper;
import net.p3pp3rf1y.sophisticateditemactions.client.render.EntityHighlightRenderer;
import net.p3pp3rf1y.sophisticateditemactions.client.render.ItemFlightAnimator;
import net.p3pp3rf1y.sophisticateditemactions.client.render.RenderedBlockHighlightRenderer;
import net.p3pp3rf1y.sophisticateditemactions.common.HighlightHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemTransferHandler;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.neoforged.neoforge.client.settings.KeyConflictContext.GUI;
import static net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME;

public class ClientEventHandler {
	private static final String KEYBIND_SOPHISTICATEDITEMACTIONS_CATEGORY = "key.category.sophisticateditemactions.main";
	public static final KeyMapping ITEM_HIGHLIGHT_KEYBIND = new KeyMapping(ItemActionsTranslationHelper.INSTANCE.translKeybind("item_highlight"),
			ClientEventHandler.ItemHighlightKeyConflictContext.INSTANCE, InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_SEMICOLON),
			KEYBIND_SOPHISTICATEDITEMACTIONS_CATEGORY);
	public static final KeyMapping ITEM_DEPOSIT_KEYBIND = new KeyMapping(ItemActionsTranslationHelper.INSTANCE.translKeybind("deposit_item"),
			ClientEventHandler.ItemHighlightKeyConflictContext.INSTANCE, InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_APOSTROPHE),
			KEYBIND_SOPHISTICATEDITEMACTIONS_CATEGORY);
	public static final KeyMapping ITEM_RESTOCK_KEYBIND = new KeyMapping(ItemActionsTranslationHelper.INSTANCE.translKeybind("restock_item"),
			ClientEventHandler.ItemHighlightKeyConflictContext.INSTANCE, InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_BACKSLASH),
			KEYBIND_SOPHISTICATEDITEMACTIONS_CATEGORY);
	private static final List<IHoveredStackProvider> HOVERED_STACK_PROVIDERS = new ArrayList<>();
	private static final List<IFocusedScreenProvider> FOCUSED_SCREEN_PROVIDERS = new ArrayList<>();
	private static final ItemActionNudgeManager NUDGE_MANAGER = new ItemActionNudgeManager();
	private static int guiHighlightRequestsPendingResult = 0;
	@Nullable
	private static Screen guiHighlightRequestScreen = null;
	private static final IHoveredStackProvider DEFAULT_HOVERED_STACK_PROVIDER = new IHoveredStackProvider() {
		@Override
		public ItemStack getHoveredStack(Screen screen) {
			if (screen instanceof AbstractContainerScreen<?> containerScreen) {
				Slot slotUnderMouse = containerScreen.getSlotUnderMouse();
				if (slotUnderMouse != null) {
					return slotUnderMouse.getItem();
				}
			}
			return ItemStack.EMPTY;
		}

		@Override
		public boolean restockSingle(Screen screen) {
			return false;
		}

		@Override
		public int getRestockSlot(Screen screen, Player player, ItemStack filter) {
			if (!(screen instanceof AbstractContainerScreen<?> containerScreen) || containerScreen.getSlotUnderMouse() == null) {
				return -1;
			}
			return containerScreen.getSlotUnderMouse().getSlotIndex();
		}
	};

	public static void registerHoveredStackProvider(IHoveredStackProvider provider) {
		HOVERED_STACK_PROVIDERS.add(provider);
	}

	public static void registerFocusedScreenProvider(IFocusedScreenProvider provider) {
		FOCUSED_SCREEN_PROVIDERS.add(provider);
	}

	public static void registerHandlers(IEventBus modBus) {
		modBus.addListener(ClientEventHandler::registerKeyMappings);
		modBus.addListener(ClientEventHandler::registerOverlay);

		IEventBus eventBus = NeoForge.EVENT_BUS;
		eventBus.addListener(ClientEventHandler::handleKeyInput);
		eventBus.addListener(ClientEventHandler::onPostClientTick);
		eventBus.addListener(ClientEventHandler::handleGuiKeyPress);
		eventBus.addListener(ClientEventHandler::handleGuiMouseKeyPress);
		eventBus.addListener(ClientEventHandler::onPlayerLoggingOut);
		eventBus.addListener(ClientEventHandler::renderLevelStage);
		eventBus.addListener(ClientEventHandler::tickLevel);
	}

	private static void onPlayerLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		guiHighlightRequestsPendingResult = 0;
		guiHighlightRequestScreen = null;
		NUDGE_MANAGER.onWorldLeft(Minecraft.getInstance());
	}

	private static void renderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
			return;
		}
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		ItemFlightAnimator.render(event.getPoseStack(), partialTick, event.getCamera().getPosition());
		RenderedBlockHighlightRenderer.render(event.getPoseStack(), partialTick, event.getCamera().getPosition());
		ClientCompatRenderHelper.renderLevelStage(event.getPoseStack(), partialTick, event.getCamera().getPosition());
		EntityHighlightRenderer.render(event.getPoseStack(), partialTick, event.getCamera().getPosition());
	}

	private static void tickLevel(ClientTickEvent.Post event) {
		ChestOpeningAnimator.tick(Minecraft.getInstance().level);
	}

	private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(ITEM_HIGHLIGHT_KEYBIND);
		event.register(ITEM_DEPOSIT_KEYBIND);
		event.register(ITEM_RESTOCK_KEYBIND);
	}

	private static void registerOverlay(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.HOTBAR, SophisticatedItemActions.getRL("highlight_directions"),
				HighlightDirectionOverlay.HUD_HIGHLIGHT_DIRECTIONS);
	}

	public static void handleGuiKeyPress(ScreenEvent.KeyPressed.Pre event) {
		InputConstants.Key key = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
		if (ITEM_HIGHLIGHT_KEYBIND.isActiveAndMatches(key) && !shouldSkipGuiItemAction(event.getScreen())
				&& event.getScreen() instanceof AbstractContainerScreen<?> screen && tryHighlightGuiItem(screen.getSlotUnderMouse())) {
			NudgeActionUsageTracker.markUsed(NudgeHintType.HIGHLIGHT);
			event.setCanceled(true);
		}
	}

	public static void handleGuiMouseKeyPress(ScreenEvent.MouseButtonPressed.Pre event) {
		InputConstants.Key input = InputConstants.Type.MOUSE.getOrCreate(event.getButton());
		if (ITEM_HIGHLIGHT_KEYBIND.isActiveAndMatches(input) && !shouldSkipGuiItemAction(event.getScreen())
				&& event.getScreen() instanceof AbstractContainerScreen<?> screen && tryHighlightGuiItem(screen.getSlotUnderMouse())) {
			NudgeActionUsageTracker.markUsed(NudgeHintType.HIGHLIGHT);
			event.setCanceled(true);
		}
	}

	public static void onPostClientTick(ClientTickEvent.Post event) {
		if (isInWorld() && ITEM_HIGHLIGHT_KEYBIND.consumeClick() && tryHighlightItem()) {
			NudgeActionUsageTracker.markUsed(NudgeHintType.HIGHLIGHT);
		}
		NUDGE_MANAGER.tick(Minecraft.getInstance());
	}

	public static boolean isInWorld() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && mc.level != null;
	}

	public static boolean shouldSkipGuiItemAction(Screen screen) {
		return !isInWorld() || isTextInputFocused(screen);
	}

	private static boolean isTextInputFocused(Screen screen) {
		return screen.isFocused() || FOCUSED_SCREEN_PROVIDERS.stream().anyMatch(provider -> provider.isFocused(screen));
	}

	public static boolean tryHighlightGuiItem(ItemStack stack) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || stack.isEmpty()) {
			return false;
		}

		if (HighlightHandler.highlightItem(player, stack)) {
			guiHighlightRequestsPendingResult++;
			guiHighlightRequestScreen = Minecraft.getInstance().screen;
		}
		return true;
	}

	private static boolean tryHighlightGuiItem(@Nullable Slot slot) {
		return slot != null && tryHighlightGuiItem(slot.getItem());
	}

	public static void handleHighlightResult(boolean hasMatches) {
		if (guiHighlightRequestsPendingResult <= 0) {
			return;
		}

		guiHighlightRequestsPendingResult--;
		if (hasMatches && Minecraft.getInstance().screen == guiHighlightRequestScreen) {
			Minecraft.getInstance().setScreen(null);
		}
		if (guiHighlightRequestsPendingResult == 0) {
			guiHighlightRequestScreen = null;
		}
	}

	private static boolean tryHighlightItem() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || player.getMainHandItem().isEmpty()) {
			return false;
		}

		HighlightHandler.highlightItem(player, player.getMainHandItem());
		return true;
	}

	public static void handleKeyInput(InputEvent.Key event) {
		Screen screen = Minecraft.getInstance().screen;
		if (!isInWorld() || screen != null && isTextInputFocused(screen)) {
			return;
		}

		if (!ITEM_DEPOSIT_KEYBIND.isUnbound() && ITEM_DEPOSIT_KEYBIND.getKey().getValue() == event.getKey() && event.getAction() == GLFW.GLFW_PRESS) {
			if (tryDepositItem(event)) {
				NudgeActionUsageTracker.markUsed(NudgeHintType.DEPOSIT);
			}
		} else if (!ITEM_RESTOCK_KEYBIND.isUnbound() && ITEM_RESTOCK_KEYBIND.getKey().getValue() == event.getKey() && event.getAction() == GLFW.GLFW_PRESS) {
			if (tryRestockItem(event)) {
				NudgeActionUsageTracker.markUsed(NudgeHintType.RESTOCK);
			}
		}
	}

	private static boolean tryRestockItem(InputEvent.Key event) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return false;
		}

		int mods = event.getModifiers();
		boolean mainInventory = (mods & GLFW.GLFW_MOD_SHIFT) != 0;
		boolean hotbar = (mods & GLFW.GLFW_MOD_ALT) != 0;
		boolean fillEmpty = (mods & GLFW.GLFW_MOD_CONTROL) != 0;

		Screen screen = Minecraft.getInstance().screen;
		ItemStack filter = ItemStack.EMPTY;
		List<ItemStack> filters = List.of();
		int slot = -1;
		boolean refillSingle = false;
		if (screen != null) {
			IHoveredStackProvider provider = getHoveredStackProvider(screen);
			if (provider != null) {
				filters = provider.getHoveredStackAlternatives(screen);
				filter = filters.isEmpty() ? ItemStack.EMPTY : filters.getFirst();
				slot = provider.getRestockSlot(screen, player, filters);
				fillEmpty |= provider.restockEmptySlot();
				refillSingle = provider.restockSingle(screen);
			}
		} else {
			filter = player.getMainHandItem();
			filters = List.of(filter);
			slot = player.getInventory().getSelectedSlot();
		}

		if (slot == -1) {
			return false;
		}

		if (filters.size() > 1) {
			if (mainInventory || hotbar) {
				ItemTransferHandler.restockAlternativeItems(player, filters, mainInventory, hotbar, fillEmpty, refillSingle);
			} else {
				ItemTransferHandler.restockAlternativeItem(player, filters, slot, fillEmpty, refillSingle);
			}
		} else if (mainInventory || hotbar) {
			ItemTransferHandler.restockMultipleItems(player, filter, mainInventory, hotbar, fillEmpty, refillSingle);
		} else {
			ItemTransferHandler.restockItem(player, filter, slot, fillEmpty, refillSingle);
		}

		return true;
	}

	@Nullable
	private static IHoveredStackProvider getHoveredStackProvider(Screen screen) {
		for (IHoveredStackProvider provider : HOVERED_STACK_PROVIDERS) {
			if (!provider.getHoveredStack(screen).isEmpty()) {
				return provider;
			}
		}
		if (screen instanceof AbstractContainerScreen<?>) {
			return DEFAULT_HOVERED_STACK_PROVIDER;
		}
		return null;
	}

	private static boolean tryDepositItem(InputEvent.Key event) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return false;
		}

		int mods = event.getModifiers();
		boolean mainInventory = (mods & GLFW.GLFW_MOD_SHIFT) != 0;
		boolean onlyMatching = (mods & GLFW.GLFW_MOD_CONTROL) == 0;
		boolean hotbar = (mods & GLFW.GLFW_MOD_ALT) != 0;

		if (mainInventory || hotbar) {
			return tryDepositMultipleItems(player, mainInventory, hotbar, onlyMatching);
		}

		Screen screen = Minecraft.getInstance().screen;
		if (screen != null) {
			if (screen instanceof AbstractContainerScreen<?> containerScreen) {
				return tryDepositItem(player, containerScreen.getSlotUnderMouse(), onlyMatching);
			}
			return false;
		}

		return tryDepositItem(player, onlyMatching);
	}

	private static boolean tryDepositMultipleItems(Player player, boolean mainInventory, boolean hotbar, boolean onlyMatching) {
		ItemTransferHandler.depositMultipleItems(player, mainInventory, hotbar, onlyMatching);
		return true;
	}

	private static boolean tryDepositItem(Player player, boolean onlyMatching) {
		ItemStack item = player.getMainHandItem();
		if (!item.isEmpty()) {
			ItemTransferHandler.depositItem(player, player.getInventory().getSelectedSlot(), onlyMatching);
			return true;
		}
		return false;
	}

	private static boolean tryDepositItem(Player player, @Nullable Slot slot, boolean onlyMatching) {
		if (slot == null || slot.getItem().isEmpty() || !(slot.container instanceof Inventory)) {
			return false;
		}
		ItemTransferHandler.depositItem(player, slot.getSlotIndex(), onlyMatching);
		return true;
	}

	private static class ItemHighlightKeyConflictContext implements IKeyConflictContext {
		public static final ItemHighlightKeyConflictContext INSTANCE = new ItemHighlightKeyConflictContext();

		@Override
		public boolean isActive() {
			return isInWorld() && ((IN_GAME.isActive() && !Minecraft.getInstance().player.getMainHandItem().isEmpty()) || GUI.isActive());
		}

		@Override
		public boolean conflicts(IKeyConflictContext other) {
			return this == other;
		}
	}

	public interface IHoveredStackProvider {
		ItemStack getHoveredStack(Screen screen);

		default List<ItemStack> getHoveredStackAlternatives(Screen screen) {
			return List.of(getHoveredStack(screen));
		}

		boolean restockSingle(Screen screen);

		default int getRestockSlot(Screen screen, Player player, ItemStack filter) {
			NonNullList<ItemStack> items = player.getInventory().getNonEquipmentItems();
			for (int slot = 0; slot < items.size(); ++slot) {
				ItemStack stack = items.get(slot);
				if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(filter, stack) && stack.getCount() < stack.getMaxStackSize()) {
					return slot;
				}
			}

			return player.getInventory().getFreeSlot();
		}

		default int getRestockSlot(Screen screen, Player player, List<ItemStack> filters) {
			return getRestockSlot(screen, player, filters.getFirst());
		}

		default boolean restockEmptySlot() {
			return false;
		}
	}

	public interface IFocusedScreenProvider {
		boolean isFocused(Screen screen);
	}
}
