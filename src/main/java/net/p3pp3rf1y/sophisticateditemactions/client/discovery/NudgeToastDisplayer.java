package net.p3pp3rf1y.sophisticateditemactions.client.discovery;

import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticateditemactions.client.gui.ItemActionsTranslationHelper;

public class NudgeToastDisplayer {
	public boolean showHint(NudgeHintType hintType, Component keybindName) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return false;
		}

		Component title = getTitle(hintType);
		Component description = getDescription(hintType, keybindName);
		if (tryShowSystemToast(minecraft, title, description)) {
			return true;
		}

		minecraft.gui.hud.setOverlayMessage(description, false);
		return true;
	}

	private Component getTitle(NudgeHintType hintType) {
		return switch (hintType) {
			case HIGHLIGHT -> Component.translatable(ItemActionsTranslationHelper.INSTANCE.translGui("discovery.nudge.highlight.title")).withStyle(ChatFormatting.GOLD);
			case RESTOCK -> Component.translatable(ItemActionsTranslationHelper.INSTANCE.translGui("discovery.nudge.restock.title")).withStyle(ChatFormatting.GOLD);
			case DEPOSIT -> Component.translatable(ItemActionsTranslationHelper.INSTANCE.translGui("discovery.nudge.deposit.title")).withStyle(ChatFormatting.GOLD);
		};
	}

	private Component getDescription(NudgeHintType hintType, Component keybindName) {
		Component highlightedKeybind = keybindName.copy().withStyle(ChatFormatting.AQUA);
		return switch (hintType) {
			case HIGHLIGHT -> Component.translatable(ItemActionsTranslationHelper.INSTANCE.translGui("discovery.nudge.highlight.description"), highlightedKeybind);
			case RESTOCK -> Component.translatable(ItemActionsTranslationHelper.INSTANCE.translGui("discovery.nudge.restock.description"), highlightedKeybind);
			case DEPOSIT -> Component.translatable(ItemActionsTranslationHelper.INSTANCE.translGui("discovery.nudge.deposit.description"), highlightedKeybind);
		};
	}

	private boolean tryShowSystemToast(Minecraft minecraft, Component title, Component description) {
		ToastManager toastManager = minecraft.gui.toastManager();
		SystemToast.add(toastManager, SystemToast.SystemToastId.PERIODIC_NOTIFICATION, title, description);
		return true;
	}
}
