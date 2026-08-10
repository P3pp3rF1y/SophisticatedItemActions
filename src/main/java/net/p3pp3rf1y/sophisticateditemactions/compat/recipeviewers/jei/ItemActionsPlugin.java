package net.p3pp3rf1y.sophisticateditemactions.compat.recipeviewers.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.buttons.IButtonState;
import mezz.jei.api.gui.buttons.IIconButtonController;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.inputs.RecipeSlotUnderMouse;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.advanced.IRecipeButtonControllerFactory;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticateditemactions.Config;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;
import net.p3pp3rf1y.sophisticateditemactions.client.discovery.NudgeActionUsageTracker;
import net.p3pp3rf1y.sophisticateditemactions.client.discovery.NudgeHintType;
import net.p3pp3rf1y.sophisticateditemactions.common.ItemTransferHandler;

import java.util.*;

@SuppressWarnings("unused")
@JeiPlugin
public class ItemActionsPlugin implements IModPlugin {
	private static final ResourceLocation ID = SophisticatedItemActions.getRL("default");
	private static final ResourceLocation RESTOCK_ICON_TEXTURE = SophisticatedItemActions.getRL("textures/gui/restock.png");
	private static final Set<RecipeType<?>> RECIPE_RESTOCK_RECIPE_TYPES = Set.of(RecipeTypes.CRAFTING, RecipeTypes.ANVIL, RecipeTypes.STONECUTTING,
			RecipeTypes.SMITHING);
	private static final Set<RecipeRestockButtonController> RECIPE_RESTOCK_BUTTON_CONTROLLERS = Collections.newSetFromMap(new WeakHashMap<>());

	@Override
	public ResourceLocation getPluginUid() {
		return ID;
	}

	@Override
	public void registerAdvanced(IAdvancedRegistration registration) {
		IDrawable restockIcon = registration.getJeiHelpers().getGuiHelper().drawableBuilder(RESTOCK_ICON_TEXTURE, 0, 0, 7, 7).setTextureSize(7, 7).build();
		registration.addRecipeButtonFactory(new IRecipeButtonControllerFactory() {
			@Override
			public <T> IIconButtonController createButtonController(IRecipeLayoutDrawable<T> recipeLayoutDrawable) {
				if (!RECIPE_RESTOCK_RECIPE_TYPES.contains(recipeLayoutDrawable.getRecipeCategory().getRecipeType())) {
					return null;
				}
				return new RecipeRestockButtonController(recipeLayoutDrawable, restockIcon);
			}
		});
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
		JeiClientCompat.setRuntime(jeiRuntime);
	}

	public static List<ItemStack> getHoveredRecipeIngredientAlternatives() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return List.of();
		}

		double mouseX = minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
		double mouseY = minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();
		int currentTick = minecraft.player.tickCount;
		for (RecipeRestockButtonController controller : RECIPE_RESTOCK_BUTTON_CONTROLLERS) {
			if (controller.lastUpdateTick < currentTick - 1) {
				continue;
			}
			Optional<RecipeSlotUnderMouse> slot = controller.recipeLayoutDrawable.getSlotUnderMouse(mouseX, mouseY);
			if (slot.isPresent() && slot.get().slot().getRole() == RecipeIngredientRole.INPUT) {
				return controller.getIngredientOptions(slot.get().slot(), false);
			}
		}
		return List.of();
	}

	private static class RecipeRestockButtonController implements IIconButtonController {
		private final IRecipeLayoutDrawable<?> recipeLayoutDrawable;
		private final IDrawable icon;
		private int lastUpdateTick = -1;

		private RecipeRestockButtonController(IRecipeLayoutDrawable<?> recipeLayoutDrawable, IDrawable icon) {
			this.recipeLayoutDrawable = recipeLayoutDrawable;
			this.icon = icon;
			RECIPE_RESTOCK_BUTTON_CONTROLLERS.add(this);
		}

		@Override
		public void initState(IButtonState state) {
			state.setIcon(icon);
			updateState(state);
		}

		@Override
		public void updateState(IButtonState state) {
			Minecraft minecraft = Minecraft.getInstance();
			lastUpdateTick = minecraft.player == null ? -1 : minecraft.player.tickCount;
			boolean restockEnabled = Config.SERVER.recipeRestockEnabled.get();
			state.setVisible(restockEnabled);
			state.setActive(restockEnabled && minecraft.player != null);
		}

		@Override
		public boolean onPress(IJeiUserInput input) {
			if (!input.isSimulate()) {
				Minecraft minecraft = Minecraft.getInstance();
				if (minecraft.player != null) {
					ItemTransferHandler.restockRecipeItems(minecraft.player, getIngredientOptions(Screen.hasShiftDown()));
					NudgeActionUsageTracker.markUsed(NudgeHintType.RESTOCK);
				}
			}
			return true;
		}

		@Override
		public void getTooltips(ITooltipBuilder tooltip) {
			tooltip.add(Component.translatable("gui.sophisticateditemactions.recipe_restock"));
		}

		private List<List<ItemStack>> getIngredientOptions(boolean fullStacks) {
			return recipeLayoutDrawable.getRecipeSlotsView().getSlotViews(RecipeIngredientRole.INPUT).stream()
					.map(slot -> getIngredientOptions(slot, fullStacks)).filter(options -> !options.isEmpty()).toList();
		}

		private List<ItemStack> getIngredientOptions(IRecipeSlotView slot, boolean fullStacks) {
			Optional<ItemStack> displayedStack = slot.getDisplayedItemStack();
			if (displayedStack.isEmpty()) {
				return List.of();
			}

			int count = fullStacks ? displayedStack.get().getMaxStackSize() : displayedStack.get().getCount();
			List<ItemStack> options = new ArrayList<>();
			addOption(options, displayedStack.get(), count);
			slot.getItemStacks().forEach(stack -> addOption(options, stack, count));
			return options;
		}

		private static void addOption(List<ItemStack> options, ItemStack stack, int count) {
			if (!stack.isEmpty() && options.stream().noneMatch(option -> ItemStack.isSameItemSameComponents(option, stack))) {
				options.add(stack.copyWithCount(count));
			}
		}
	}
}
