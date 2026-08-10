package net.p3pp3rf1y.sophisticateditemactions.compat.recipeviewers.rei;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.DisplayRenderer;
import me.shedaniel.rei.api.client.gui.widgets.Button;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.WidgetWithBounds;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategoryView;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.plugins.PluginManager;
import me.shedaniel.rei.api.common.registry.ReloadStage;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticateditemactions.Config;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;
import net.p3pp3rf1y.sophisticateditemactions.client.discovery.NudgeActionUsageTracker;
import net.p3pp3rf1y.sophisticateditemactions.client.discovery.NudgeHintType;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemTransferHandler;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@REIPluginClient
public class ItemActionsReiPlugin implements REIClientPlugin {
	private static final int RESTOCK_BUTTON_SIZE = 10;
	private static final int RESTOCK_ICON_SIZE = 7;
	private static final Identifier RESTOCK_ICON_TEXTURE = SophisticatedItemActions.getIdentifier("textures/gui/restock.png");

	@Override
	public void postStage(PluginManager<REIClientPlugin> manager, ReloadStage stage) {
		if (stage == ReloadStage.END) {
			CategoryRegistry.getInstance().forEach(ItemActionsReiPlugin::registerRestockButton);
		}
	}

	private static void registerRestockButton(CategoryRegistry.CategoryConfiguration<?> categoryConfiguration) {
		registerRestockButtonForCategory(categoryConfiguration);
	}

	private static <T extends Display> void registerRestockButtonForCategory(CategoryRegistry.CategoryConfiguration<T> categoryConfiguration) {
		categoryConfiguration.registerExtension((display, category, lastView) -> new DisplayCategoryView<>() {
			@Override
			public DisplayRenderer getDisplayRenderer(T display) {
				return lastView.getDisplayRenderer(display);
			}

			@Override
			public List<Widget> setupDisplay(T display, Rectangle bounds) {
				List<Widget> widgets = new ArrayList<>(lastView.setupDisplay(display, bounds));
				if (Config.SERVER.recipeRestockEnabled.get() && Minecraft.getInstance().player != null) {
					Rectangle buttonBounds = new Rectangle(bounds.getMaxX() + 2, bounds.getMaxY() - RESTOCK_BUTTON_SIZE * 2 - 8, RESTOCK_BUTTON_SIZE,
							RESTOCK_BUTTON_SIZE);
					widgets.add(new RecipeRestockWidget(display, buttonBounds));
				}
				return widgets;
			}
		});
	}

	private static List<List<ItemStack>> getIngredientOptions(Display display, boolean fullStacks) {
		List<List<ItemStack>> ingredientOptions = new ArrayList<>();
		for (EntryIngredient ingredient : display.getInputEntries()) {
			List<ItemStack> options = new ArrayList<>();
			for (EntryStack<?> entryStack : ingredient) {
				if (entryStack.getType() != VanillaEntryTypes.ITEM || entryStack.isEmpty()) {
					continue;
				}

				ItemStack stack = entryStack.castValue();
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

	private static class RecipeRestockWidget extends WidgetWithBounds {
		private final Rectangle bounds;
		private final Button button;

		private RecipeRestockWidget(Display display, Rectangle bounds) {
			this.bounds = bounds;
			button = Widgets.createButton(bounds, Component.empty()).focusable(false).onClick(ignored -> restockRecipeItems(display))
					.tooltipLine(Component.translatable("gui.sophisticateditemactions.recipe_restock"));
		}

		@Override
		public Rectangle getBounds() {
			return bounds;
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(button);
		}

		@Override
		public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
			button.render(guiGraphics, mouseX, mouseY, delta);
			guiGraphics.blit(RESTOCK_ICON_TEXTURE, bounds.x + (RESTOCK_BUTTON_SIZE - RESTOCK_ICON_SIZE) / 2,
					bounds.y + (RESTOCK_BUTTON_SIZE - RESTOCK_ICON_SIZE) / 2, 0, 0, RESTOCK_ICON_SIZE, RESTOCK_ICON_SIZE, RESTOCK_ICON_SIZE, RESTOCK_ICON_SIZE);
		}

		private static void restockRecipeItems(Display display) {
			Minecraft minecraft = Minecraft.getInstance();
			List<List<ItemStack>> ingredientOptions = getIngredientOptions(display, false);
			if (!Config.SERVER.recipeRestockEnabled.get() || minecraft.player == null || ingredientOptions.isEmpty()) {
				return;
			}

			ItemTransferHandler.restockRecipeItems(minecraft.player, ingredientOptions);
			NudgeActionUsageTracker.markUsed(NudgeHintType.RESTOCK);
		}
	}
}
