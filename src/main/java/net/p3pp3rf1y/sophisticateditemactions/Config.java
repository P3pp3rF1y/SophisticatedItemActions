package net.p3pp3rf1y.sophisticateditemactions;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.p3pp3rf1y.sophisticateditemactions.client.gui.ItemActionsTranslationHelper;
import org.apache.commons.lang3.tuple.Pair;

public class Config {
	private Config() {
	}

	public static final Client CLIENT;
	public static final ModConfigSpec CLIENT_SPEC;

	static {
		final Pair<Client, ModConfigSpec> clientSpec = new ModConfigSpec.Builder().configure(Client::new);
		CLIENT_SPEC = clientSpec.getRight();
		CLIENT = clientSpec.getLeft();
	}

	public static class Client {
		public final DiscoveryNudges discoveryNudges;

		public Client(ModConfigSpec.Builder builder) {
			discoveryNudges = new DiscoveryNudges(builder);
		}
	}

	public static class DiscoveryNudges {
		public final ModConfigSpec.BooleanValue enabled;
		public final ModConfigSpec.IntValue globalCooldownTicks;
		public final ModConfigSpec.IntValue maxNudgesPerSession;
		public final ModConfigSpec.IntValue actionCooldownTicks;
		public final ModConfigSpec.IntValue maxSuccessfulUsesBeforeSuppressing;
		public final ModConfigSpec.IntValue highlightActionThreshold;
		public final ModConfigSpec.IntValue restockActionThreshold;
		public final ModConfigSpec.IntValue depositActionThreshold;
		public final ModConfigSpec.BooleanValue highlightEnabled;
		public final ModConfigSpec.BooleanValue restockEnabled;
		public final ModConfigSpec.BooleanValue depositEnabled;
		public final ModConfigSpec.IntValue checkIntervalTicks;
		public final ModConfigSpec.BooleanValue debugLogging;

		public DiscoveryNudges(ModConfigSpec.Builder builder) {
			builder.comment("Client-only discoverability nudges for highlight, restock and deposit keybind actions")
					.translation(ItemActionsTranslationHelper.INSTANCE.translConfig("discoveryNudges")).push("discoveryNudges");

			enabled = builder.comment("Master toggle for discovery nudges").translation(ItemActionsTranslationHelper.INSTANCE.translConfig("enabled"))
					.define("enabled", true);
			globalCooldownTicks = builder.comment("Global cooldown between any two nudges in ticks (default: 2 hours)")
					.translation(ItemActionsTranslationHelper.INSTANCE.translConfig("globalCooldownTicks"))
					.defineInRange("globalCooldownTicks", 144000, 0, Integer.MAX_VALUE);
			maxNudgesPerSession = builder.comment("Maximum total number of nudges shown during one world connection session")
					.translation(ItemActionsTranslationHelper.INSTANCE.translConfig("maxNudgesPerSession"))
					.defineInRange("maxNudgesPerSession", 3, 0, Integer.MAX_VALUE);
			actionCooldownTicks = builder.comment("Cooldown between repeated nudges or successful uses of the same action in ticks (default: 4 hours)")
					.translation(ItemActionsTranslationHelper.INSTANCE.translConfig("actionCooldownTicks"))
					.defineInRange("actionCooldownTicks", 288000, 0, Integer.MAX_VALUE);
			maxSuccessfulUsesBeforeSuppressing = builder.comment("Maximum successful uses of one action before its nudge stops appearing")
					.translation(ItemActionsTranslationHelper.INSTANCE.translConfig("maxSuccessfulUsesBeforeSuppressing"))
					.defineInRange("maxSuccessfulUsesBeforeSuppressing", 5, 0, Integer.MAX_VALUE);
			highlightActionThreshold = builder.comment("Number of storage opens without transfers before showing highlight hint (-1 disables)")
					.translation(ItemActionsTranslationHelper.INSTANCE.translConfig("highlightActionThreshold"))
					.defineInRange("highlightActionThreshold", 4, -1, Integer.MAX_VALUE);
			restockActionThreshold = builder
					.comment("Number of storage sessions with transfer from storage to inventory before showing restock hint (-1 disables)")
					.translation(ItemActionsTranslationHelper.INSTANCE.translConfig("restockActionThreshold"))
					.defineInRange("restockActionThreshold", 3, -1, Integer.MAX_VALUE);
			depositActionThreshold = builder
					.comment("Number of storage sessions with transfer from inventory to storage before showing deposit hint (-1 disables)")
					.translation(ItemActionsTranslationHelper.INSTANCE.translConfig("depositActionThreshold"))
					.defineInRange("depositActionThreshold", 3, -1, Integer.MAX_VALUE);
			highlightEnabled = builder.comment("Whether highlight hint can be shown")
					.translation(ItemActionsTranslationHelper.INSTANCE.translConfig("highlightEnabled")).define("highlightEnabled", true);
			restockEnabled = builder.comment("Whether restock hint can be shown")
					.translation(ItemActionsTranslationHelper.INSTANCE.translConfig("restockEnabled")).define("restockEnabled", true);
			depositEnabled = builder.comment("Whether deposit hint can be shown")
					.translation(ItemActionsTranslationHelper.INSTANCE.translConfig("depositEnabled")).define("depositEnabled", true);
			checkIntervalTicks = builder.comment("How often to evaluate nudge state in ticks")
					.translation(ItemActionsTranslationHelper.INSTANCE.translConfig("checkIntervalTicks"))
					.defineInRange("checkIntervalTicks", 40, 1, Integer.MAX_VALUE);
			debugLogging = builder.comment("Logs nudge state transitions to debug log")
					.translation(ItemActionsTranslationHelper.INSTANCE.translConfig("debugLogging")).define("debugLogging", false);

			builder.pop();
		}
	}
}
