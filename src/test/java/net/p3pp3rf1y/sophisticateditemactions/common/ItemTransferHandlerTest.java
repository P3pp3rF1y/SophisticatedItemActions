package net.p3pp3rf1y.sophisticateditemactions.common;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemTransferHandlerTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Bootstrap.validate();
		bindTestComponents(Items.OAK_PLANKS, Items.BIRCH_PLANKS, Items.STICK);
	}

	@Test
	void getMissingRecipeIngredientOptionsCombinesRepeatedRecipeInputsBeforeSubtractingInventory() {
		List<List<ItemStack>> missingIngredients = ItemTransferHandler.getMissingRecipeIngredientOptions(
				List.of(List.of(new ItemStack(Items.OAK_PLANKS)), List.of(new ItemStack(Items.OAK_PLANKS)), List.of(new ItemStack(Items.STICK, 2))),
				List.of(new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.STICK)));

		assertEquals(2, missingIngredients.size());
		assertIngredientCount(missingIngredients, Items.OAK_PLANKS, 1);
		assertIngredientCount(missingIngredients, Items.STICK, 1);
	}

	@Test
	void getMissingRecipeIngredientOptionsKeepsOnlyTheDeficitForFullStackTargets() {
		List<List<ItemStack>> missingIngredients = ItemTransferHandler.getMissingRecipeIngredientOptions(List.of(List.of(new ItemStack(Items.OAK_PLANKS, 64)),
				List.of(new ItemStack(Items.OAK_PLANKS, 64)), List.of(new ItemStack(Items.OAK_PLANKS, 64))), List.of(new ItemStack(Items.OAK_PLANKS, 64)));

		assertEquals(2, missingIngredients.size());
		assertEquals(128, missingIngredients.stream().flatMap(List::stream).filter(stack -> stack.is(Items.OAK_PLANKS)).mapToInt(ItemStack::getCount).sum());
	}

	@Test
	void getMissingRecipeIngredientOptionsAcceptsAnyTagAlternativeAlreadyInInventory() {
		List<List<ItemStack>> missingIngredients = ItemTransferHandler.getMissingRecipeIngredientOptions(
				List.of(List.of(new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.BIRCH_PLANKS))), List.of(new ItemStack(Items.BIRCH_PLANKS)));

		assertTrue(missingIngredients.isEmpty());
	}

	@Test
	void getMissingRecipeIngredientOptionsRemovesDuplicateIngredientAlternatives() {
		List<List<ItemStack>> missingIngredients = ItemTransferHandler.getMissingRecipeIngredientOptions(
				List.of(List.of(new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.BIRCH_PLANKS), new ItemStack(Items.OAK_PLANKS))), List.of());

		assertEquals(1, missingIngredients.size());
		assertEquals(2, missingIngredients.getFirst().size());
	}

	@Test
	void getMissingRecipeIngredientOptionsReservesAlternativesForMoreConstrainedRecipeInputs() {
		List<List<ItemStack>> missingIngredients = ItemTransferHandler.getMissingRecipeIngredientOptions(
				List.of(List.of(new ItemStack(Items.OAK_PLANKS)), List.of(new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.BIRCH_PLANKS))),
				List.of(new ItemStack(Items.OAK_PLANKS)));

		assertEquals(1, missingIngredients.size());
		assertEquals(2, missingIngredients.getFirst().size());
		assertIngredientCount(missingIngredients, Items.OAK_PLANKS, 1);
		assertIngredientCount(missingIngredients, Items.BIRCH_PLANKS, 1);
	}

	private static void assertIngredientCount(List<List<ItemStack>> ingredients, Item item, int count) {
		assertTrue(ingredients.stream().flatMap(List::stream).anyMatch(stack -> stack.is(item) && stack.getCount() == count));
	}

	private static void bindTestComponents(Item... items) {
		DataComponentMap components = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
		for (Item item : items) {
			item.builtInRegistryHolder().bindComponents(components);
		}
	}
}
