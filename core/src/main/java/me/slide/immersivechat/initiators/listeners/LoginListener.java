package me.slide.immersivechat.initiators.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.slide.immersivechat.controllers.PluginMessageController;
import me.slide.immersivechat.controllers.SpigotFlatFileController;
import me.slide.immersivechat.initiators.application.ImmersiveChat;
import me.slide.immersivechat.model.ChatChannel;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import me.slide.immersivechat.model.ImmersiveChatPlayer;
import me.slide.immersivechat.model.JsonFormat;
import me.slide.immersivechat.service.ConfigService;
import me.slide.immersivechat.service.PlayerApiService;
import me.slide.immersivechat.service.UuidService;
import me.slide.immersivechat.utilities.FormatUtils;
import me.slide.immersivechat.xcut.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Manages player login and logout events.
 * 
 * @author Aust1n46
 */
@Singleton
public class LoginListener implements Listener {
	@Inject
	private ImmersiveChat plugin;
	@Inject
	private UuidService uuidService;
	@Inject
	private SpigotFlatFileController spigotFlatFileController;
	@Inject
	private PluginMessageController pluginMessageController;
	@Inject
	private PlayerApiService playerApiService;
	@Inject
	private ConfigService configService;
	@Inject
	private Logger log;

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerQuit(PlayerQuitEvent playerQuitEvent) {
		IImmersiveChatPlayer ventureChatPlayer = playerApiService.getOnlineImmersiveChatPlayer(playerQuitEvent.getPlayer());
		if (ventureChatPlayer == null) {
			log.warn("onPlayerQuit() Could not find VentureChatPlayer");
		} else {
			spigotFlatFileController.savePlayerData(ventureChatPlayer);
			ventureChatPlayer.getMessages().clear();
			ventureChatPlayer.setOnline(false);
			ventureChatPlayer.setPlayer((Player) null);
			playerApiService.removeMineverseChatOnlinePlayerToMap(ventureChatPlayer);
			log.debug("onPlayerQuit() ventureChatPlayer:{} quit", ventureChatPlayer);
		}
	}

	private void handleNameChange(ImmersiveChatPlayer mcp, Player eventPlayerInstance) {
		plugin.getServer().getConsoleSender().sendMessage(
				FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - Detected Name Change. Old Name:&c " + mcp.getName() + " &eNew Name:&c " + eventPlayerInstance.getName()));
		playerApiService.removeNameFromMap(mcp.getName());
		mcp.setName(eventPlayerInstance.getName());
		playerApiService.addNameToMap(mcp);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerJoin(PlayerJoinEvent event) {
		ImmersiveChatPlayer mcp = playerApiService.getImmersiveChatPlayer(event.getPlayer());
		Player player = event.getPlayer();
		String name = player.getName();
		if (mcp == null) {
			UUID uuid = player.getUniqueId();
			mcp = ImmersiveChatPlayer.builder().uuid(uuid).name(name).currentChannel(configService.getDefaultChannel()).build();
			mcp.getListening().add(configService.getDefaultChannel().getName());
			playerApiService.addMineverseChatPlayerToMap(mcp);
			playerApiService.addNameToMap(mcp);
		}
		uuidService.checkOfflineUUIDWarning(mcp.getUuid());
		// check for name change
		if (!mcp.getName().equals(name)) {
			handleNameChange(mcp, event.getPlayer());
		}
		mcp.setOnline(true);
		mcp.setPlayer(player);
		mcp.setHasPlayed(false);
		playerApiService.addMineverseChatOnlinePlayerToMap(mcp);
		String jsonFormat = mcp.getJsonFormat();
		for (JsonFormat j : configService.getJsonFormats()) {
			if (mcp.getPlayer().hasPermission("venturechat.json." + j.getName())) {
				if (configService.getJsonFormat(mcp.getJsonFormat()).getPriority() > j.getPriority()) {
					jsonFormat = j.getName();
				}
			}
		}
		mcp.setJsonFormat(jsonFormat);
		for (ChatChannel ch : configService.getAutojoinList()) {
			if (ch.isPermissionRequired()) {
				if (mcp.getPlayer().hasPermission(ch.getPermission())) {
					mcp.getListening().add(ch.getName());
				}
			} else {
				mcp.getListening().add(ch.getName());
			}
		}
		if (configService.isProxyEnabled()) {
			pluginMessageController.synchronizeWithDelay(mcp, false);
		}
	}
}
