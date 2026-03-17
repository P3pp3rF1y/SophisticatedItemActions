package net.p3pp3rf1y.sophisticateditemactions.client.discovery;

import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticateditemactions.client.gui.ItemActionsTranslationHelper;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

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

		if (minecraft.gui != null) {
			minecraft.gui.setOverlayMessage(description, false);
			return true;
		}

		if (minecraft.player != null) {
			minecraft.player.displayClientMessage(description, true);
			return true;
		}

		return false;
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
		try {
			Object toastManager = getToastManager(minecraft);
			if (toastManager == null) {
				return false;
			}

			Class<?> systemToastClass = Class.forName("net.minecraft.client.gui.components.toasts.SystemToast");
			Object toastId = getToastId(systemToastClass);
			if (toastId == null) {
				return false;
			}

			Object toast = createSystemToast(systemToastClass, toastManager, toastId, title, description);
			if (toast == null) {
				return false;
			}

			return addToast(toastManager, toast);
		} catch (Exception ignored) {
			return false;
		}
	}

	private Object getToastManager(Minecraft minecraft) {
		try {
			return Minecraft.class.getMethod("getToasts").invoke(minecraft);
		} catch (Exception ignored) {
			try {
				return Minecraft.class.getMethod("getToastManager").invoke(minecraft);
			} catch (Exception ignoredAgain) {
				return null;
			}
		}
	}

	private Object getToastId(Class<?> systemToastClass) {
		for (Class<?> declaredClass : systemToastClass.getDeclaredClasses()) {
			if (!declaredClass.isEnum()) {
				continue;
			}
			Object[] constants = declaredClass.getEnumConstants();
			if (constants == null || constants.length == 0) {
				continue;
			}
			for (Object constant : constants) {
				if (constant.toString().contains("TUTORIAL") || constant.toString().contains("PERIODIC")) {
					return constant;
				}
			}
			return constants[0];
		}
		return null;
	}

	private Object createSystemToast(Class<?> systemToastClass, Object toastManager, Object toastId, Component title, Component description) throws ReflectiveOperationException {
		for (Method method : systemToastClass.getMethods()) {
			if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != systemToastClass) {
				continue;
			}
			Class<?>[] parameterTypes = method.getParameterTypes();
			if (parameterTypes.length == 4
					&& parameterTypes[0].isAssignableFrom(toastManager.getClass())
					&& parameterTypes[1].isAssignableFrom(toastId.getClass())
					&& parameterTypes[2] == Component.class
					&& parameterTypes[3] == Component.class) {
				return method.invoke(null, toastManager, toastId, title, description);
			}
		}
		return null;
	}

	private boolean addToast(Object toastManager, Object toast) throws ReflectiveOperationException {
		for (Method method : toastManager.getClass().getMethods()) {
			if (method.getParameterCount() != 1) {
				continue;
			}
			if (method.getName().equals("addToast") || method.getName().equals("add")) {
				method.invoke(toastManager, toast);
				return true;
			}
		}

		return false;
	}
}
