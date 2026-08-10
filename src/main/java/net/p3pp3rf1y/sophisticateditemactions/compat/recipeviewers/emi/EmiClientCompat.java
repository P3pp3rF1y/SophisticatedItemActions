package net.p3pp3rf1y.sophisticateditemactions.compat.recipeviewers.emi;

import com.mojang.blaze3d.platform.InputConstants;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.emi.screen.RecipeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.p3pp3rf1y.sophisticateditemactions.client.ClientEventHandler;

import java.util.List;

public class EmiClientCompat {
	private static boolean recipeRestockRefreshPending = false;

	public static void init() {
		IEventBus eventBus = NeoForge.EVENT_BUS;
		eventBus.addListener(EmiClientCompat::handleGuiKeyPress);
		eventBus.addListener(EmiClientCompat::handleGuiMouseKeyPress);
		eventBus.addListener(EmiClientCompat::refreshRecipeScreenAfterRestock);
		ClientEventHandler.registerRecipeRestockSyncHandler(() -> recipeRestockRefreshPending = true);
		ClientEventHandler.registerHoveredStackProvider(new ClientEventHandler.IHoveredStackProvider() {
			@Override
			public ItemStack getHoveredStack(Screen screen) {
				return getStack();
			}

			@Override
			public boolean restockSingle(Screen screen) {
				// in case of crafting grid return single
				return Minecraft.getInstance().screen instanceof RecipeScreen;
			}

			@Override
			public boolean restockEmptySlot() {
				return true;
			}
		});
	}

	private static void refreshRecipeScreenAfterRestock(ClientTickEvent.Post event) {
		if (!recipeRestockRefreshPending) {
			return;
		}

		recipeRestockRefreshPending = false;
		Minecraft minecraft = Minecraft.getInstance();
		EmiScreenManager.forceRecalculate();
		if (minecraft.screen instanceof RecipeScreen recipeScreen) {
			recipeScreen.init(minecraft, recipeScreen.width, recipeScreen.height);
		}
	}

	private static ItemStack getStack() {
		List<EmiStack> emiStacks;
		if (Minecraft.getInstance().screen instanceof RecipeScreen recipeScreen) {
			emiStacks = recipeScreen.getHoveredStack().getEmiStacks();
		} else {
			emiStacks = EmiApi.getHoveredStack(true).getStack().getEmiStacks();
		}
		return emiStacks.isEmpty() ? ItemStack.EMPTY : emiStacks.getFirst().getItemStack();
	}

	public static void handleGuiKeyPress(ScreenEvent.KeyPressed.Pre event) {
		if (ClientEventHandler.shouldSkipGuiItemAction(event.getScreen())) {
			return;
		}
		InputConstants.Key key = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
		if (ClientEventHandler.ITEM_HIGHLIGHT_KEYBIND.isActiveAndMatches(key)) {
			ItemStack stack = getStack();
			if (ClientEventHandler.tryHighlightGuiItem(stack)) {
				event.setCanceled(true);
			}
		}
	}

	public static void handleGuiMouseKeyPress(ScreenEvent.MouseButtonPressed.Pre event) {
		if (ClientEventHandler.shouldSkipGuiItemAction(event.getScreen())) {
			return;
		}
		InputConstants.Key input = InputConstants.Type.MOUSE.getOrCreate(event.getButton());
		if (ClientEventHandler.ITEM_HIGHLIGHT_KEYBIND.isActiveAndMatches(input)) {
			ItemStack stack = getStack();
			if (ClientEventHandler.tryHighlightGuiItem(stack)) {
				event.setCanceled(true);
			}
		}
	}

}
