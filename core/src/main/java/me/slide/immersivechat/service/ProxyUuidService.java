package me.slide.immersivechat.service;

import com.google.inject.Singleton;
import me.slide.immersivechat.model.ImmersiveChatProxySource;
import me.slide.immersivechat.utilities.FormatUtils;

import java.util.UUID;

@Singleton
public class ProxyUuidService {

	public boolean shouldSkipOfflineUUIDProxy(UUID uuid, ImmersiveChatProxySource source) {
		return (FormatUtils.uuidIsOffline(uuid) && !source.isOfflineServerAcknowledgementSet());
	}

	public void checkOfflineUUIDWarningProxy(UUID uuid, ImmersiveChatProxySource source) {
		if (shouldSkipOfflineUUIDProxy(uuid, source)) {
			source.sendConsoleMessage("&8[&eVentureChat&8]&c - Detected Offline UUID!");
			source.sendConsoleMessage("&8[&eVentureChat&8]&c - If you are using BungeeCord, make sure you have properly setup IP Forwarding.");
			source.sendConsoleMessage("&8[&eVentureChat&8]&c - https://www.spigotmc.org/wiki/bungeecord-ip-forwarding/");
			source.sendConsoleMessage("&8[&eVentureChat&8]&c - You can access this wiki page from the log file or just Google it.");
			source.sendConsoleMessage("&8[&eVentureChat&8]&c - If you're running a \"cracked\" server, player data might not be stored properly, and thus, you are on your own.");
			source.sendConsoleMessage("&8[&eVentureChat&8]&c - If you run your server in offline mode, you will probably lose your player data when switching to online mode!");
			source.sendConsoleMessage("&8[&eVentureChat&8]&c - No player data will be saved in offline mode unless you set the \"cracked\" server acknowledgement in the config!");
			return;
		}
	}
}
