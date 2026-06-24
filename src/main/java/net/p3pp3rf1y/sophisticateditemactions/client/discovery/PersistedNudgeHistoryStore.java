package net.p3pp3rf1y.sophisticateditemactions.client.discovery;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.storage.LevelResource;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;

import javax.annotation.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class PersistedNudgeHistoryStore {
	private static final String DATA_FILE_NAME = "discovery_nudges.dat";

	private PersistedNudgeHistoryStore() {
	}

	@Nullable
	public static String resolveConnectionKey(Minecraft minecraft) {
		if (minecraft.level == null || minecraft.player == null) {
			return null;
		}

		if (minecraft.isLocalServer()) {
			if (minecraft.getSingleplayerServer() == null) {
				return null;
			}

			Path worldRoot = minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT).normalize().toAbsolutePath();
			return hashConnectionKey(worldRoot.toString());
		}

		ServerData serverData = minecraft.getCurrentServer();
		if (serverData == null) {
			return null;
		}

		String serverIdentity = serverData.name + "|" + serverData.ip;
		return hashConnectionKey(serverIdentity);
	}

	public static PersistedNudgeHistory load(Minecraft minecraft, String connectionKey) {
		Path path = getDataFilePath(minecraft, connectionKey);
		if (!Files.exists(path)) {
			return new PersistedNudgeHistory();
		}

		try {
			CompoundTag tag = NbtIo.readCompressed(path.toFile());
			return tag == null ? new PersistedNudgeHistory() : PersistedNudgeHistory.fromTag(tag);
		} catch (IOException e) {
			SophisticatedItemActions.LOGGER.warn("Failed to load discovery nudge history from {}", path, e);
			return new PersistedNudgeHistory();
		}
	}

	public static void save(Minecraft minecraft, String connectionKey, PersistedNudgeHistory history) {
		Path path = getDataFilePath(minecraft, connectionKey);
		try {
			Files.createDirectories(path.getParent());
			NbtIo.writeCompressed(history.toTag(), path.toFile());
		} catch (IOException e) {
			SophisticatedItemActions.LOGGER.warn("Failed to save discovery nudge history to {}", path, e);
		}
	}

	private static Path getDataFilePath(Minecraft minecraft, String connectionKey) {
		return minecraft.gameDirectory.toPath().resolve("local").resolve(SophisticatedItemActions.MOD_ID).resolve("data").resolve(connectionKey)
				.resolve(DATA_FILE_NAME);
	}

	private static String hashConnectionKey(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Missing SHA-256 support", e);
		}
	}
}
