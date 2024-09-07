package me.slide.immersivechat.service;

import com.google.inject.Singleton;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import me.slide.immersivechat.model.ImmersiveChatPlayer;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * API class for looking up wrapped {@link ImmersiveChatPlayer} objects from
 * {@link Player}, {@link UUID}, or {@link String} user names.
 *
 * @author Aust1n46
 */
@Singleton
public class PlayerApiService {
	private final HashMap<UUID, ImmersiveChatPlayer> playerMap = new HashMap<>();
	private final HashMap<String, UUID> namesMap = new HashMap<>();
	private final HashMap<UUID, IImmersiveChatPlayer> onlinePlayerMap = new HashMap<>();
	private final List<String> networkPlayerNames = new ArrayList<>();

	public void addNameToMap(ImmersiveChatPlayer mcp) {
		namesMap.put(mcp.getName(), mcp.getUuid());
	}

	public void removeNameFromMap(String name) {
		namesMap.remove(name);
	}

	public void clearNameMap() {
		namesMap.clear();
	}

	public void addMineverseChatPlayerToMap(ImmersiveChatPlayer mcp) {
		playerMap.put(mcp.getUuid(), mcp);
	}

	public void clearMineverseChatPlayerMap() {
		playerMap.clear();
	}

	public Collection<ImmersiveChatPlayer> getMineverseChatPlayers() {
		return playerMap.values();
	}

	public void addMineverseChatOnlinePlayerToMap(ImmersiveChatPlayer mcp) {
		onlinePlayerMap.put(mcp.getUuid(), mcp);
	}

	public void removeMineverseChatOnlinePlayerToMap(IImmersiveChatPlayer mcp) {
		onlinePlayerMap.remove(mcp.getUuid());
	}

	public void clearOnlineMineverseChatPlayerMap() {
		onlinePlayerMap.clear();
	}

	public Collection<IImmersiveChatPlayer> getOnlineMineverseChatPlayers() {
		return onlinePlayerMap.values();
	}

	/**
	 * Get a MineverseChatPlayer wrapper from a Bukkit Player instance.
	 *
	 * @param player {@link Player} object.
	 * @return {@link ImmersiveChatPlayer}
	 */
	public ImmersiveChatPlayer getImmersiveChatPlayer(Player player) {
		return getImmersiveChatPlayer(player.getUniqueId());
	}

	/**
	 * Get a MineverseChatPlayer wrapper from a UUID.
	 *
	 * @param uuid {@link UUID}.
	 * @return {@link ImmersiveChatPlayer}
	 */
	public ImmersiveChatPlayer getImmersiveChatPlayer(UUID uuid) {
		return playerMap.get(uuid);
	}

	/**
	 * Get a MineverseChatPlayer wrapper from a user name.
	 *
	 * @param name {@link String}.
	 * @return {@link ImmersiveChatPlayer}
	 */
	public ImmersiveChatPlayer getImmersiveChatPlayer(String name) {
		return getImmersiveChatPlayer(namesMap.get(name));
	}

	/**
	 * Get a MineverseChatPlayer wrapper from a Bukkit Player instance. Only checks
	 * current online players. Much more efficient!
	 *
	 * @param player {@link Player} object.
	 * @return {@link ImmersiveChatPlayer}
	 */
	public IImmersiveChatPlayer getOnlineImmersiveChatPlayer(final Player player) {
		return getOnlineImmersiveChatPlayer(player.getUniqueId());
	}

	/**
	 * Get a MineverseChatPlayer wrapper from a UUID. Only checks current online
	 * players. Much more efficient!
	 *
	 * @param uuid {@link UUID}.
	 * @return {@link ImmersiveChatPlayer}
	 */
	public IImmersiveChatPlayer getOnlineImmersiveChatPlayer(UUID uuid) {
		return onlinePlayerMap.get(uuid);
	}

	/**
	 * Get a MineverseChatPlayer wrapper from a user name. Only checks current
	 * online players. Much more efficient!
	 *
	 * @param name {@link String}.
	 * @return {@link ImmersiveChatPlayer}
	 */
	public IImmersiveChatPlayer getOnlineImmersiveChatPlayer(String name) {
		return getOnlineImmersiveChatPlayer(namesMap.get(name));
	}

	public List<String> getNetworkPlayerNames() {
		return networkPlayerNames;
	}

	public void clearNetworkPlayerNames() {
		networkPlayerNames.clear();
	}

	public void addNetworkPlayerName(String name) {
		networkPlayerNames.add(name);
	}
}
