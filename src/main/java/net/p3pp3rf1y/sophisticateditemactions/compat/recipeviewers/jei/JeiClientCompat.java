package net.p3pp3rf1y.sophisticateditemactions.compat.recipeviewers.jei;

import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.p3pp3rf1y.sophisticateditemactions.client.ClientEventHandler;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class JeiClientCompat {
	@Nullable
	private static IJeiRuntime runtime = null;

	public static void setRuntime(@Nullable IJeiRuntime runtime) {
		JeiClientCompat.runtime = runtime;
	}

	public static void init() {
		IEventBus eventBus = NeoForge.EVENT_BUS;
		eventBus.addListener(JeiClientCompat::handleGuiKeyPress);
		eventBus.addListener(JeiClientCompat::handleGuiMouseKeyPress);
		ClientEventHandler.registerHoveredStackProvider(new ClientEventHandler.IHoveredStackProvider() {
			@Override
			public ItemStack getHoveredStack(Screen screen) {
				List<ItemStack> alternatives = getHoveredRecipeIngredientAlternatives();
				return alternatives.isEmpty() ? getStack().orElse(ItemStack.EMPTY) : alternatives.getFirst();
			}

			@Override
			public List<ItemStack> getHoveredStackAlternatives(Screen screen) {
				List<ItemStack> alternatives = getHoveredRecipeIngredientAlternatives();
				return alternatives.isEmpty() ? List.of(getStack().orElse(ItemStack.EMPTY)) : alternatives;
			}

			@Override
			public boolean restockSingle(Screen screen) {
				// in case of crafting grid return single
				return runtime != null && runtime.getRecipesGui().getIngredientUnderMouse(VanillaTypes.ITEM_STACK).isPresent();
			}

			@Override
			public int getRestockSlot(Screen screen, Player player, List<ItemStack> filters) {
				for (int slot = 0; slot < player.getInventory().getNonEquipmentItems().size(); slot++) {
					ItemStack stack = player.getInventory().getNonEquipmentItems().get(slot);
					if (!stack.isEmpty() && filters.stream().anyMatch(filter -> ItemStack.isSameItemSameComponents(stack, filter))
							&& stack.getCount() < stack.getMaxStackSize()) {
						return slot;
					}
				}
				return player.getInventory().getFreeSlot();
			}

			@Override
			public boolean restockEmptySlot() {
				return true;
			}
		});
	}

	private static Optional<ItemStack> getStack() {
		return runtime == null
				? Optional.empty()
				: runtime.getIngredientListOverlay().getIngredientUnderMouse().or(() -> runtime.getBookmarkOverlay().getIngredientUnderMouse())
						.flatMap(ITypedIngredient::getItemStack).or(() -> runtime.getRecipesGui().getIngredientUnderMouse(VanillaTypes.ITEM_STACK));
	}

	private static List<ItemStack> getHoveredRecipeIngredientAlternatives() {
		return ItemActionsPlugin.getHoveredRecipeIngredientAlternatives();
	}

	public static void handleGuiKeyPress(ScreenEvent.KeyPressed.Pre event) {
		if (runtime == null || ClientEventHandler.shouldSkipGuiItemAction(event.getScreen())) {
			return;
		}
		InputConstants.Key key = InputConstants.getKey(event.getKeyEvent());
		if (ClientEventHandler.ITEM_HIGHLIGHT_KEYBIND.isActiveAndMatches(key) && getStack().map(ClientEventHandler::tryHighlightGuiItem).orElse(false)) {
			event.setCanceled(true);
		}
	}

	public static void handleGuiMouseKeyPress(ScreenEvent.MouseButtonPressed.Pre event) {
		if (runtime == null || ClientEventHandler.shouldSkipGuiItemAction(event.getScreen())) {
			return;
		}
		InputConstants.Key input = InputConstants.Type.MOUSE.getOrCreate(event.getButton());
		if (ClientEventHandler.ITEM_HIGHLIGHT_KEYBIND.isActiveAndMatches(input) && getStack().map(ClientEventHandler::tryHighlightGuiItem).orElse(false)) {
			event.setCanceled(true);
		}
	}

}
