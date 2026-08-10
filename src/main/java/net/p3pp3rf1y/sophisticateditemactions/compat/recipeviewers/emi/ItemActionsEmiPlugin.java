package net.p3pp3rf1y.sophisticateditemactions.compat.recipeviewers.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticateditemactions.Config;
import net.p3pp3rf1y.sophisticateditemactions.client.discovery.NudgeActionUsageTracker;
import net.p3pp3rf1y.sophisticateditemactions.client.discovery.NudgeHintType;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemTransferHandler;

import java.util.ArrayList;
import java.util.List;

@EmiEntrypoint
public class ItemActionsEmiPlugin implements EmiPlugin {
	private static final int RESTOCK_BUTTON_SIZE = 12;

	@Override
	public void register(EmiRegistry registry) {
		registry.addRecipeDecorator((recipe, widgets) -> addRestockButton(recipe, widgets));
	}

	private static void addRestockButton(EmiRecipe recipe, WidgetHolder widgets) {
		if (recipe.getCategory() != VanillaEmiRecipeCategories.CRAFTING || !Config.SERVER.recipeRestockEnabled.get() || Minecraft.getInstance().player == null
				|| getIngredientOptions(recipe, false).isEmpty()) {
			return;
		}

		int x = widgets.getWidth() + 5;
		widgets.add(new RecipeRestockWidget(recipe, x, 0));
		widgets.addTooltipText(List.of(Component.translatable("gui.sophisticateditemactions.recipe_restock")), x, 0, RESTOCK_BUTTON_SIZE, RESTOCK_BUTTON_SIZE);
	}

	private static List<List<ItemStack>> getIngredientOptions(EmiRecipe recipe, boolean fullStacks) {
		List<List<ItemStack>> ingredientOptions = new ArrayList<>();
		for (EmiIngredient ingredient : recipe.getInputs()) {
			List<ItemStack> options = new ArrayList<>();
			for (EmiStack emiStack : ingredient.getEmiStacks()) {
				ItemStack stack = emiStack.getItemStack();
				if (!stack.isEmpty() && options.stream().noneMatch(option -> ItemStack.isSameItemSameComponents(option, stack))) {
					options.add(stack.copyWithCount(fullStacks ? stack.getMaxStackSize() : stack.getCount()));
				}
			}
			if (!options.isEmpty()) {
				ingredientOptions.add(options);
			}
		}
		return ingredientOptions;
	}

	private static class RecipeRestockWidget extends Widget {
		private final EmiRecipe recipe;
		private final Bounds bounds;

		private RecipeRestockWidget(EmiRecipe recipe, int x, int y) {
			this.recipe = recipe;
			bounds = new Bounds(x, y, RESTOCK_BUTTON_SIZE, RESTOCK_BUTTON_SIZE);
		}

		@Override
		public Bounds getBounds() {
			return bounds;
		}

		@Override
		public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
			int backgroundColor = bounds.contains(mouseX, mouseY) ? 0xFFA0A0A0 : 0xFF6E6E6E;
			int iconColor = bounds.contains(mouseX, mouseY) ? 0xFFFFFFA0 : 0xFFE0E0E0;
			guiGraphics.fill(bounds.x(), bounds.y(), bounds.x() + RESTOCK_BUTTON_SIZE, bounds.y() + RESTOCK_BUTTON_SIZE, backgroundColor);
			guiGraphics.fill(bounds.x() + 4, bounds.y() + 2, bounds.x() + 8, bounds.y() + 6, iconColor);
			guiGraphics.fill(bounds.x() + 3, bounds.y() + 5, bounds.x() + 9, bounds.y() + 8, iconColor);
			guiGraphics.fill(bounds.x() + 5, bounds.y() + 8, bounds.x() + 7, bounds.y() + 10, iconColor);
		}

		@Override
		public boolean mouseClicked(int mouseX, int mouseY, int button) {
			Minecraft minecraft = Minecraft.getInstance();
			List<List<ItemStack>> ingredientOptions = getIngredientOptions(recipe, Screen.hasShiftDown());
			if (!bounds.contains(mouseX, mouseY) || !Config.SERVER.recipeRestockEnabled.get() || minecraft.player == null || ingredientOptions.isEmpty()) {
				return false;
			}

			ItemTransferHandler.restockRecipeItems(minecraft.player, ingredientOptions);
			NudgeActionUsageTracker.markUsed(NudgeHintType.RESTOCK);
			return true;
		}
	}
}
