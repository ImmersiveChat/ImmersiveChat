package me.slide.immersivechat.controllers;

import com.google.inject.Inject;
import me.slide.immersivechat.model.ImmersiveChatProxySource;
import me.slide.immersivechat.model.MuteContainer;
import me.slide.immersivechat.model.SynchronizedImmersiveChatPlayer;
import me.slide.immersivechat.service.ProxyPlayerApiService;
import me.slide.immersivechat.service.ProxyUuidService;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Class for reading and writing proxy player data.
 *
 * @author Aust1n46
 */
public class ProxyFlatFileController {
	@Inject
	private ProxyUuidService uuidService;
	@Inject
	private ProxyPlayerApiService playerApiService;

	public void loadLegacyBungeePlayerData(File dataFolder, ImmersiveChatProxySource source) {
		File sync = new File(dataFolder, "BungeePlayers.yml");
		if (!sync.exists()) {
			return;
		}
		try {
			source.sendConsoleMessage("&8[&eVentureChat&8]&c - Detected Legacy Player Data!");
			source.sendConsoleMessage("&8[&eVentureChat&8]&c - Converting to new structure and deleting old BungeePlayers.yml file!");
			Configuration playerData = ConfigurationProvider.getProvider(YamlConfiguration.class).load(sync);
			for (String uuidString : playerData.getKeys()) {
				UUID uuid = UUID.fromString(uuidString);
				if (uuidService.shouldSkipOfflineUUIDProxy(uuid, source)) {
					source.sendConsoleMessage("&8[&eVentureChat&8]&c - Skipping Offline UUID: " + uuid);
					continue;
				}
				Set<String> listening = new HashSet<String>();
				StringTokenizer l = new StringTokenizer(playerData.getString(uuidString + ".channels"), ",");
				while (l.hasMoreTokens()) {
					String channel = l.nextToken();
					listening.add(channel);
				}
				HashMap<String, MuteContainer> mutes = new HashMap<String, MuteContainer>();
				StringTokenizer m = new StringTokenizer(playerData.getString(uuidString + ".mutes"), ",");
				while (m.hasMoreTokens()) {
					String[] parts = m.nextToken().split(":");
					String channelName = parts[0];
					mutes.put(channelName, new MuteContainer(channelName, Long.parseLong(parts[1]), ""));
				}
				HashSet<UUID> ignores = new HashSet<UUID>();
				StringTokenizer n = new StringTokenizer(playerData.getString(uuidString + ".ignores"), ",");
				while (n.hasMoreTokens()) {
					String ignore = n.nextToken();
					ignores.add(UUID.fromString(ignore));
				}
				boolean spy = playerData.getBoolean(uuidString + ".spy");
				boolean messageToggle = playerData.getBoolean(uuidString + ".messagetoggle");
				playerApiService.addSynchronizedMineverseChatPlayerToMap(
						SynchronizedImmersiveChatPlayer.builder().uuid(uuid).listening(listening).mutes(mutes).ignores(ignores).spy(spy).messageToggleEnabled(messageToggle).build());
			}
		} catch (Exception e) {
			playerApiService.clearProxyPlayerMap();
			source.sendConsoleMessage("&8[&eVentureChat&8]&c - Error Loading Legacy Player Data!");
			source.sendConsoleMessage("&8[&eVentureChat&8]&c - Deleted BungeePlayers.yml file!");
		} finally {
			sync.delete();
		}
	}

	public void loadProxyPlayerData(File dataFolder, ImmersiveChatProxySource source) {
		try {
			File playerDataDirectory = dataFolder;
			if (!playerDataDirectory.exists()) {
				playerDataDirectory.mkdirs();
			}
			Files.walk(Paths.get(dataFolder.getAbsolutePath())).filter(Files::isRegularFile).forEach((path) -> readProxyPlayerDataFile(path, source));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readProxyPlayerDataFile(Path path, ImmersiveChatProxySource source) {
		SynchronizedImmersiveChatPlayer smcp;
		File proxyPlayerDataFile = path.toFile();
		if (!proxyPlayerDataFile.exists()) {
			return;
		}
		try {
			Configuration proxyPlayerDataFileConfiguration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(proxyPlayerDataFile);
			String uuidString = proxyPlayerDataFile.getName().replace(".yml", "");
			UUID uuid = UUID.fromString(uuidString);
			if (uuidService.shouldSkipOfflineUUIDProxy(uuid, source)) {
				source.sendConsoleMessage("&8[&eVentureChat&8]&c - Skipping Offline UUID: " + uuid);
				source.sendConsoleMessage("&8[&eVentureChat&8]&c - File will be skipped and deleted.");
				proxyPlayerDataFile.delete();
				return;
			}
			Set<String> listening = new HashSet<String>();
			StringTokenizer l = new StringTokenizer(proxyPlayerDataFileConfiguration.getString("channels"), ",");
			while (l.hasMoreTokens()) {
				String channel = l.nextToken();
				listening.add(channel);
			}
			HashMap<String, MuteContainer> mutes = new HashMap<String, MuteContainer>();
			Configuration muteSection = proxyPlayerDataFileConfiguration.getSection("mutes");
			for (String channelName : muteSection.getKeys()) {
				Configuration channelSection = muteSection.getSection(channelName);
				mutes.put(channelName, new MuteContainer(channelName, channelSection.getLong("time"), channelSection.getString("reason")));
			}
			HashSet<UUID> ignores = new HashSet<UUID>();
			StringTokenizer n = new StringTokenizer(proxyPlayerDataFileConfiguration.getString("ignores"), ",");
			while (n.hasMoreTokens()) {
				String ignore = n.nextToken();
				ignores.add(UUID.fromString(ignore));
			}
			boolean spy = proxyPlayerDataFileConfiguration.getBoolean("spy");
			boolean messageToggle = proxyPlayerDataFileConfiguration.getBoolean("messagetoggle");
//            smcp = new SynchronizedVentureChatPlayer(uuid, listening, mutes, ignores, spy, messageToggle);
			smcp = SynchronizedImmersiveChatPlayer.builder().uuid(uuid).listening(listening).mutes(mutes).ignores(ignores).spy(spy).messageToggleEnabled(messageToggle).build();
		} catch (Exception e) {
			source.sendConsoleMessage("&8[&eVentureChat&8]&c - Error Loading Data File: " + proxyPlayerDataFile.getName());
			source.sendConsoleMessage("&8[&eVentureChat&8]&c - File will be skipped and deleted.");
			proxyPlayerDataFile.delete();
			return;
		}
		if (smcp != null) {
			playerApiService.addSynchronizedMineverseChatPlayerToMap(smcp);
		}
	}

	public void saveProxyPlayerData(File dataFolder, ImmersiveChatProxySource source) {
		try {
			for (SynchronizedImmersiveChatPlayer p : playerApiService.getSynchronizedMineverseChatPlayers()) {
				if (uuidService.shouldSkipOfflineUUIDProxy(p.getUuid(), source)) {
					return;
				}
				File proxyPlayerDataFile = new File(dataFolder.getAbsolutePath(), p.getUuid() + ".yml");
				if (!proxyPlayerDataFile.exists()) {
					proxyPlayerDataFile.createNewFile();
				}
				Configuration proxyPlayerDataFileConfiguration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(proxyPlayerDataFile);

				String listen = "";
				for (String s : p.getListening())
					listen += s + ",";
				String ignore = "";
				for (UUID s : p.getIgnores())
					ignore += s.toString() + ",";
				if (listen.length() > 0)
					listen = listen.substring(0, listen.length() - 1);
				if (ignore.length() > 0)
					ignore = ignore.substring(0, ignore.length() - 1);
				proxyPlayerDataFileConfiguration.set("channels", listen);
				Configuration muteSection = createSection(proxyPlayerDataFileConfiguration, "mutes");
				for (MuteContainer mute : p.getMutes().values()) {
					Configuration channelSection = createSection(muteSection, mute.getChannel());
					channelSection.set("time", mute.getDuration());
					channelSection.set("reason", mute.getReason());
				}
				proxyPlayerDataFileConfiguration.set("ignores", ignore);
				proxyPlayerDataFileConfiguration.set("spy", p.isSpy());
				proxyPlayerDataFileConfiguration.set("messagetoggle", p.isMessageToggleEnabled());

				ConfigurationProvider.getProvider(YamlConfiguration.class).save(proxyPlayerDataFileConfiguration, proxyPlayerDataFile);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create a new {@link Configuration} section.
	 *
	 * @param configurationSection
	 * @param sectionKey
	 * @return Configuration
	 */
	private Configuration createSection(Configuration configurationSection, String sectionKey) {
		configurationSection.set(sectionKey, null);
		return configurationSection.getSection(sectionKey);
	}
}
