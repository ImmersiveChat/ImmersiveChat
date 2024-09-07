package me.slide.immersivechat.service;

import com.google.inject.Singleton;
import me.slide.immersivechat.model.SynchronizedImmersiveChatPlayer;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

@Singleton
public class ProxyPlayerApiService {
	private final HashMap<UUID, SynchronizedImmersiveChatPlayer> proxyPlayerMap = new HashMap<>();

	public void addSynchronizedMineverseChatPlayerToMap(SynchronizedImmersiveChatPlayer smcp) {
		proxyPlayerMap.put(smcp.getUuid(), smcp);
	}

	public void clearProxyPlayerMap() {
		proxyPlayerMap.clear();
	}

	public Collection<SynchronizedImmersiveChatPlayer> getSynchronizedMineverseChatPlayers() {
		return proxyPlayerMap.values();
	}

	/**
	 * Get a SynchronizedMineverseChatPlayer from a UUID.
	 *
	 * @param uuid {@link UUID}
	 * @return {@link SynchronizedImmersiveChatPlayer}
	 */
	public SynchronizedImmersiveChatPlayer getSynchronizedMineverseChatPlayer(UUID uuid) {
		return proxyPlayerMap.get(uuid);
	}
}
