package net.p3pp3rf1y.sophisticateditemactions;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {
	private Config() {
	}

	public static final Client CLIENT;
	public static final ForgeConfigSpec CLIENT_SPEC;

	static {
		final Pair<Client, ForgeConfigSpec> clientSpec = new ForgeConfigSpec.Builder().configure(Client::new);
		CLIENT_SPEC = clientSpec.getRight();
		CLIENT = clientSpec.getLeft();
	}

	public static class Client {
		public final DiscoveryNudges discoveryNudges;

		public Client(ForgeConfigSpec.Builder builder) {
			builder.comment("Client-side Settings").push("client");

			discoveryNudges = new DiscoveryNudges(builder);

			builder.pop();
		}
	}

	public static class DiscoveryNudges {
		public final ForgeConfigSpec.BooleanValue enabled;
		public final ForgeConfigSpec.IntValue globalCooldownTicks;
		public final ForgeConfigSpec.IntValue maxNudgesPerSession;
		public final ForgeConfigSpec.IntValue actionCooldownTicks;
		public final ForgeConfigSpec.IntValue maxSuccessfulUsesBeforeSuppressing;
		public final ForgeConfigSpec.IntValue highlightActionThreshold;
		public final ForgeConfigSpec.IntValue restockActionThreshold;
		public final ForgeConfigSpec.IntValue depositActionThreshold;
		public final ForgeConfigSpec.BooleanValue highlightEnabled;
		public final ForgeConfigSpec.BooleanValue restockEnabled;
		public final ForgeConfigSpec.BooleanValue depositEnabled;
		public final ForgeConfigSpec.IntValue checkIntervalTicks;
		public final ForgeConfigSpec.BooleanValue debugLogging;

		public DiscoveryNudges(ForgeConfigSpec.Builder builder) {
			builder.comment("Client-only discoverability nudges for highlight, restock and deposit keybind actions").push("discoveryNudges");

			enabled = builder.comment("Master toggle for discovery nudges").define("enabled", true);
			globalCooldownTicks = builder.comment("Global cooldown between any two nudges in ticks (default: 2 hours)").defineInRange("globalCooldownTicks",
					144000, 0, Integer.MAX_VALUE);
			maxNudgesPerSession = builder.comment("Maximum total number of nudges shown during one world connection session")
					.defineInRange("maxNudgesPerSession", 3, 0, Integer.MAX_VALUE);
			actionCooldownTicks = builder.comment("Cooldown between repeated nudges or successful uses of the same action in ticks (default: 4 hours)")
					.defineInRange("actionCooldownTicks", 288000, 0, Integer.MAX_VALUE);
			maxSuccessfulUsesBeforeSuppressing = builder.comment("Maximum successful uses of one action before its nudge stops appearing")
					.defineInRange("maxSuccessfulUsesBeforeSuppressing", 5, 0, Integer.MAX_VALUE);
			highlightActionThreshold = builder.comment("Number of storage opens without transfers before showing highlight hint (-1 disables)")
					.defineInRange("highlightActionThreshold", 4, -1, Integer.MAX_VALUE);
			restockActionThreshold = builder
					.comment("Number of storage sessions with transfer from storage to inventory before showing restock hint (-1 disables)")
					.defineInRange("restockActionThreshold", 3, -1, Integer.MAX_VALUE);
			depositActionThreshold = builder
					.comment("Number of storage sessions with transfer from inventory to storage before showing deposit hint (-1 disables)")
					.defineInRange("depositActionThreshold", 3, -1, Integer.MAX_VALUE);
			highlightEnabled = builder.comment("Whether highlight hint can be shown").define("highlightEnabled", true);
			restockEnabled = builder.comment("Whether restock hint can be shown").define("restockEnabled", true);
			depositEnabled = builder.comment("Whether deposit hint can be shown").define("depositEnabled", true);
			checkIntervalTicks = builder.comment("How often to evaluate nudge state in ticks").defineInRange("checkIntervalTicks", 40, 1, Integer.MAX_VALUE);
			debugLogging = builder.comment("Logs nudge state transitions to debug log").define("debugLogging", false);

			builder.pop();
		}
	}
}
