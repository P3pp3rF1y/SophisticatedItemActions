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
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.p3pp3rf1y.sophisticateditemactions.client.discovery.ItemActionNudgeManager;
import net.p3pp3rf1y.sophisticateditemactions.client.discovery.NudgeActionUsageTracker;
import net.p3pp3rf1y.sophisticateditemactions.client.discovery.NudgeHintType;
import net.p3pp3rf1y.sophisticateditemactions.client.gui.HighlightDirectionOverlay;
import net.p3pp3rf1y.sophisticateditemactions.client.gui.ItemActionsTranslationHelper;
import net.p3pp3rf1y.sophisticateditemactions.client.render.EntityHighlightRenderer;
import net.p3pp3rf1y.sophisticateditemactions.client.render.ItemFlightAnimator;
import net.p3pp3rf1y.sophisticateditemactions.common.HighlightHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemTransferHandler;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.minecraftforge.client.settings.KeyConflictContext.GUI;
import static net.minecraftforge.client.settings.KeyConflictContext.IN_GAME;

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
	private static final ItemActionNudgeManager NUDGE_MANAGER = new ItemActionNudgeManager();
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

	public static void registerHandlers(IEventBus modBus) {
		modBus.addListener(ClientEventHandler::registerKeyMappings);
		modBus.addListener(ClientEventHandler::registerOverlay);

		IEventBus eventBus = MinecraftForge.EVENT_BUS;
		eventBus.addListener(ClientEventHandler::handleKeyInput);
		eventBus.addListener(ClientEventHandler::onPostClientTick);
		eventBus.addListener(ClientEventHandler::handleGuiKeyPress);
		eventBus.addListener(ClientEventHandler::handleGuiMouseKeyPress);
		eventBus.addListener(ClientEventHandler::onPlayerLoggingOut);
		eventBus.addListener(ClientEventHandler::renderLevelStage);
		eventBus.addListener(ClientEventHandler::tickLevel);
	}

	private static void onPlayerLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		NUDGE_MANAGER.onWorldLeft(Minecraft.getInstance());
	}

	private static void renderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
			return;
		}
		ItemFlightAnimator.render(event.getPoseStack(), event.getPartialTick(), event.getCamera().getPosition());
		ClientCompatRenderHelper.renderLevelStage(event.getPoseStack(), event.getPartialTick(), event.getCamera().getPosition());
		EntityHighlightRenderer.render(event.getPoseStack(), event.getPartialTick(), event.getCamera().getPosition());
	}

	private static void tickLevel(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		ChestOpeningAnimator.tick(Minecraft.getInstance().level);
	}

	private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(ITEM_HIGHLIGHT_KEYBIND);
		event.register(ITEM_DEPOSIT_KEYBIND);
		event.register(ITEM_RESTOCK_KEYBIND);
	}

	private static void registerOverlay(RegisterGuiOverlaysEvent event) {
		event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "highlight_directions", HighlightDirectionOverlay.HUD_HIGHLIGHT_DIRECTIONS);
	}

	public static void handleGuiKeyPress(ScreenEvent.KeyPressed.Pre event) {
		InputConstants.Key key = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
		if (ITEM_HIGHLIGHT_KEYBIND.isActiveAndMatches(key) && event.getScreen() instanceof AbstractContainerScreen<?> screen
				&& tryHighlightItem(screen.getSlotUnderMouse())) {
			NudgeActionUsageTracker.markUsed(NudgeHintType.HIGHLIGHT);
			event.getScreen().getMinecraft().setScreen(null);
			event.setCanceled(true);
		}
	}

	public static void handleGuiMouseKeyPress(ScreenEvent.MouseButtonPressed.Pre event) {
		InputConstants.Key input = InputConstants.Type.MOUSE.getOrCreate(event.getButton());
		if (ITEM_HIGHLIGHT_KEYBIND.isActiveAndMatches(input) && event.getScreen() instanceof AbstractContainerScreen<?> screen
				&& tryHighlightItem(screen.getSlotUnderMouse())) {
			NudgeActionUsageTracker.markUsed(NudgeHintType.HIGHLIGHT);
			event.getScreen().getMinecraft().setScreen(null);
			event.setCanceled(true);
		}
	}

	public static void onPostClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}

		if (ITEM_HIGHLIGHT_KEYBIND.consumeClick() && tryHighlightItem()) {
			NudgeActionUsageTracker.markUsed(NudgeHintType.HIGHLIGHT);
		}
		NUDGE_MANAGER.tick(Minecraft.getInstance());
	}

	private static boolean tryHighlightItem(@Nullable Slot slot) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (slot == null || player == null || slot.getItem().isEmpty()) {
			return false;
		}

		HighlightHandler.highlightItem(player, slot.getItem());

		return true;
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
		if (screen != null && screen.isFocused()) {
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
		int slot = -1;
		boolean refillSingle = false;
		if (screen != null) {
			IHoveredStackProvider provider = getHoveredStackProvider(screen);
			if (provider != null) {
				filter = provider.getHoveredStack(screen);
				slot = provider.getRestockSlot(screen, player, filter);
				fillEmpty |= provider.restockEmptySlot();
				refillSingle = provider.restockSingle(screen);
			}
		} else {
			filter = player.getMainHandItem();
			slot = player.getInventory().selected;
		}

		if (slot == -1) {
			return false;
		}

		if (mainInventory || hotbar) {
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
			ItemTransferHandler.depositItem(player, player.getInventory().selected, onlyMatching);
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
			return (IN_GAME.isActive() && Minecraft.getInstance().player != null && !Minecraft.getInstance().player.getMainHandItem().isEmpty())
					|| GUI.isActive();
		}

		@Override
		public boolean conflicts(IKeyConflictContext other) {
			return this == other;
		}
	}

	public interface IHoveredStackProvider {
		ItemStack getHoveredStack(Screen screen);

		boolean restockSingle(Screen screen);

		default int getRestockSlot(Screen screen, Player player, ItemStack filter) {
			NonNullList<ItemStack> items = player.getInventory().items;
			for (int slot = 0; slot < items.size(); ++slot) {
				ItemStack stack = items.get(slot);
				if (!stack.isEmpty() && ItemStack.isSameItemSameTags(filter, stack) && stack.getCount() < stack.getMaxStackSize()) {
					return slot;
				}
			}

			return player.getInventory().getFreeSlot();
		}

		default boolean restockEmptySlot() {
			return false;
		}
	}
}
