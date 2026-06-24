package net.p3pp3rf1y.sophisticateditemactions.compat.recipeviewers.rei;

import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.math.impl.PointHelper;
import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.impl.client.gui.screen.AbstractDisplayViewingScreen;
import me.shedaniel.rei.impl.client.gui.widget.EntryWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.p3pp3rf1y.sophisticateditemactions.client.ClientEventHandler;
import net.p3pp3rf1y.sophisticateditemactions.common.HighlightHandler;

import java.util.Optional;

public class ReiClientCompat {
	public static void init() {
		IEventBus eventBus = MinecraftForge.EVENT_BUS;
		eventBus.addListener(ReiClientCompat::handleGuiKeyPress);
		eventBus.addListener(ReiClientCompat::handleGuiMouseKeyPress);
		ClientEventHandler.registerHoveredStackProvider(new ClientEventHandler.IHoveredStackProvider() {
			@Override
			public ItemStack getHoveredStack(Screen screen) {
				return getStack();
			}

			@Override
			public boolean restockSingle(Screen screen) {
				return Minecraft.getInstance().screen instanceof AbstractDisplayViewingScreen;
			}

			@Override
			public boolean restockEmptySlot() {
				return true;
			}
		});
	}

	private static ItemStack getStack() {
		return REIRuntime.getInstance().getOverlay().<ItemStack>flatMap(overlay -> {
			EntryStack<?> focusedStack = overlay.getEntryList().getFocusedStack();
			if (!focusedStack.isEmpty()) {
				return Optional.of(focusedStack.castValue());
			}

			if (overlay.getFavoritesList().isPresent()) {
				focusedStack = overlay.getFavoritesList().get().getFocusedStack();
				if (!focusedStack.isEmpty()) {
					return Optional.of(focusedStack.castValue());
				}
			}

			return Optional.empty();
		}).orElseGet(ReiClientCompat::getRecipeViewStack);
	}

	private static ItemStack getRecipeViewStack() {
		if (!(Minecraft.getInstance().screen instanceof AbstractDisplayViewingScreen displayScreen)) {
			return ItemStack.EMPTY;
		}

		for (EntryWidget widget : Widgets.<EntryWidget>walk(displayScreen.children(), EntryWidget.class::isInstance)) {
			if (widget.containsMouse(PointHelper.ofMouse())) {
				EntryStack<?> currentEntry = widget.getCurrentEntry();
				return currentEntry.isEmpty() ? ItemStack.EMPTY : currentEntry.castValue();
			}
		}
		return ItemStack.EMPTY;
	}

	public static void handleGuiKeyPress(ScreenEvent.KeyPressed.Pre event) {
		InputConstants.Key key = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
		if (ClientEventHandler.ITEM_HIGHLIGHT_KEYBIND.isActiveAndMatches(key)) {
			ItemStack stack = getStack();
			if (!stack.isEmpty() && tryHighlightItem(stack)) {
				event.getScreen().getMinecraft().setScreen(null);
				event.setCanceled(true);
			}
		}
	}

	public static void handleGuiMouseKeyPress(ScreenEvent.MouseButtonPressed.Pre event) {
		InputConstants.Key input = InputConstants.Type.MOUSE.getOrCreate(event.getButton());
		if (ClientEventHandler.ITEM_HIGHLIGHT_KEYBIND.isActiveAndMatches(input)) {
			ItemStack stack = getStack();
			if (!stack.isEmpty() && tryHighlightItem(stack)) {
				event.getScreen().getMinecraft().setScreen(null);
				event.setCanceled(true);
			}
		}
	}

	private static boolean tryHighlightItem(ItemStack stack) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) {
			return false;
		}

		HighlightHandler.highlightItem(player, stack);

		return true;
	}
}
