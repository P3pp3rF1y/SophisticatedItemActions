package net.p3pp3rf1y.sophisticateditemactions.client.discovery;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticateditemactions.Config;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;
import net.p3pp3rf1y.sophisticateditemactions.client.ClientEventHandler;

public class ItemActionNudgeManager {
	private final NudgeSessionState sessionState = new NudgeSessionState();
	private final NudgeEligibilityDetector eligibilityDetector = new NudgeEligibilityDetector();
	private final NudgeToastDisplayer nudgeToastDisplayer = new NudgeToastDisplayer();

	private long lastEvaluationTick = Long.MIN_VALUE;

	private boolean storageScreenOpen;
	private boolean transfersObservedInSession;
	private boolean restockActionCountedInSession;
	private boolean depositActionCountedInSession;
	private long previousStorageItemCount;

	private int opensWithoutTransfers;
	private int restockActionUnits;
	private int depositActionUnits;

	public void tick(Minecraft minecraft) {
		if (minecraft.player == null || minecraft.level == null) {
			return;
		}

		syncActionUsageSuppressions();
		trackStorageSession(minecraft);

		Config.DiscoveryNudges config = Config.CLIENT.discoveryNudges;
		if (!config.enabled.get() || !shouldEvaluate(minecraft, config.checkIntervalTicks.get())) {
			return;
		}

		if (!eligibilityDetector.isDisplayContextAllowed(minecraft)) {
			return;
		}

		maybeShowNudge(minecraft, config);
	}

	private boolean shouldEvaluate(Minecraft minecraft, int checkInterval) {
		long gameTime = minecraft.level.getGameTime();
		if (lastEvaluationTick == Long.MIN_VALUE || gameTime - lastEvaluationTick >= checkInterval) {
			lastEvaluationTick = gameTime;
			return true;
		}
		return false;
	}

	private void maybeShowNudge(Minecraft minecraft, Config.DiscoveryNudges config) {
		if (sessionState.getNudgesShown() >= config.maxNudgesPerSession.get()) {
			return;
		}

		long gameTime = minecraft.level.getGameTime();
		if (sessionState.getLastNudgeTick() != Long.MIN_VALUE && gameTime - sessionState.getLastNudgeTick() < config.globalCooldownTicks.get()) {
			return;
		}

		if (shouldShow(config.highlightEnabled.get(), config.highlightActionThreshold.get(), opensWithoutTransfers, NudgeHintType.HIGHLIGHT)) {
			show(NudgeHintType.HIGHLIGHT, gameTime);
			return;
		}
		if (shouldShow(config.restockEnabled.get(), config.restockActionThreshold.get(), restockActionUnits, NudgeHintType.RESTOCK)) {
			show(NudgeHintType.RESTOCK, gameTime);
			return;
		}
		if (shouldShow(config.depositEnabled.get(), config.depositActionThreshold.get(), depositActionUnits, NudgeHintType.DEPOSIT)) {
			show(NudgeHintType.DEPOSIT, gameTime);
		}
	}

	private boolean shouldShow(boolean hintEnabled, int threshold, int value, NudgeHintType hintType) {
		return hintEnabled
				&& threshold >= 0
				&& value >= threshold
				&& !sessionState.wasShown(hintType)
				&& !sessionState.isSuppressed(hintType);
	}

	private void show(NudgeHintType hintType, long gameTime) {
		if (!nudgeToastDisplayer.showHint(hintType, getKeybindName(hintType))) {
			if (Config.CLIENT.discoveryNudges.debugLogging.get()) {
				SophisticatedItemActions.LOGGER.debug("Discovery nudge for {} was not displayed", hintType);
			}
			return;
		}

		sessionState.markShown(hintType, gameTime);
		if (Config.CLIENT.discoveryNudges.debugLogging.get()) {
			SophisticatedItemActions.LOGGER.debug("Discovery nudge shown for {}", hintType);
		}
	}

	private Component getKeybindName(NudgeHintType hintType) {
		return switch (hintType) {
			case HIGHLIGHT -> ClientEventHandler.ITEM_HIGHLIGHT_KEYBIND.getTranslatedKeyMessage();
			case RESTOCK -> ClientEventHandler.ITEM_RESTOCK_KEYBIND.getTranslatedKeyMessage();
			case DEPOSIT -> ClientEventHandler.ITEM_DEPOSIT_KEYBIND.getTranslatedKeyMessage();
		};
	}

	private void trackStorageSession(Minecraft minecraft) {
		Screen screen = minecraft.screen;
		if (screen == null) {
			eligibilityDetector.clearCache();
		}

		boolean isStorageScreen = eligibilityDetector.isStorageContainerScreen(screen);
		if (!storageScreenOpen && isStorageScreen) {
			startStorageSession((AbstractContainerScreen<?>) screen);
			return;
		}

		if (storageScreenOpen && isStorageScreen) {
			refreshTransferSignals((AbstractContainerScreen<?>) screen);
			return;
		}

		if (storageScreenOpen) {
			finishStorageSession();
		}
	}

	private void startStorageSession(AbstractContainerScreen<?> screen) {
		storageScreenOpen = true;
		transfersObservedInSession = false;
		restockActionCountedInSession = false;
		depositActionCountedInSession = false;
		previousStorageItemCount = countStorageItems(screen);
	}

	private void refreshTransferSignals(AbstractContainerScreen<?> screen) {
		long currentStorageItemCount = countStorageItems(screen);

		if (currentStorageItemCount < previousStorageItemCount) {
			transfersObservedInSession = true;
			if (!restockActionCountedInSession) {
				restockActionUnits++;
				restockActionCountedInSession = true;
			}
		}
		if (currentStorageItemCount > previousStorageItemCount) {
			transfersObservedInSession = true;
			if (!depositActionCountedInSession) {
				depositActionUnits++;
				depositActionCountedInSession = true;
			}
		}

		previousStorageItemCount = currentStorageItemCount;
	}

	private void finishStorageSession() {
		if (!transfersObservedInSession) {
			opensWithoutTransfers++;
		} else {
			opensWithoutTransfers = 0;
		}

		if (Config.CLIENT.discoveryNudges.debugLogging.get()) {
			SophisticatedItemActions.LOGGER.debug("Discovery storage session finished: transfersObservedInSession={}, opensWithoutTransfers={}, restockActionUnits={}, depositActionUnits={}",
					transfersObservedInSession, opensWithoutTransfers, restockActionUnits, depositActionUnits);
		}

		storageScreenOpen = false;
	}

	private long countStorageItems(AbstractContainerScreen<?> screen) {
		long storageItems = 0;
		for (Slot slot : screen.getMenu().slots) {
			if (!(slot.container instanceof Inventory)) {
				ItemStack item = slot.getItem();
				if (!item.isEmpty()) {
					storageItems += item.getCount();
				}
			}
		}
		return storageItems;
	}

	public void onWorldLeft(Minecraft minecraft) {
		resetSessionState();

		if (Config.CLIENT.discoveryNudges.debugLogging.get()) {
			String connectionKey = minecraft.getCurrentServer() == null ? "singleplayer" : minecraft.getCurrentServer().ip;
			SophisticatedItemActions.LOGGER.debug("Discovery nudge session reset after world disconnect from {}", connectionKey);
		}
	}

	private void resetSessionState() {
		sessionState.reset();
		eligibilityDetector.clearCache();
		NudgeActionUsageTracker.reset();
		storageScreenOpen = false;
		transfersObservedInSession = false;
		restockActionCountedInSession = false;
		depositActionCountedInSession = false;
		previousStorageItemCount = 0;
		opensWithoutTransfers = 0;
		restockActionUnits = 0;
		depositActionUnits = 0;
		lastEvaluationTick = Long.MIN_VALUE;
	}

	private void syncActionUsageSuppressions() {
		consumeAndSuppress(NudgeHintType.HIGHLIGHT);
		consumeAndSuppress(NudgeHintType.RESTOCK);
		consumeAndSuppress(NudgeHintType.DEPOSIT);
	}

	private void consumeAndSuppress(NudgeHintType hintType) {
		if (!NudgeActionUsageTracker.consumeUsed(hintType) || sessionState.isSuppressed(hintType)) {
			return;
		}

		sessionState.suppress(hintType);
		if (Config.CLIENT.discoveryNudges.debugLogging.get()) {
			SophisticatedItemActions.LOGGER.debug("Discovery nudge for {} suppressed after successful keybind use", hintType);
		}
	}
}
