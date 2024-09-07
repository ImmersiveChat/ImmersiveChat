package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import me.slide.immersivechat.controllers.SpigotFlatFileController;
import me.slide.immersivechat.initiators.application.ImmersiveChat;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import me.slide.immersivechat.model.ImmersiveChatPlayer;
import me.slide.immersivechat.model.JsonFormat;
import me.slide.immersivechat.model.UniversalCommand;
import me.slide.immersivechat.service.ConfigService;
import me.slide.immersivechat.service.PlayerApiService;
import me.slide.immersivechat.utilities.FormatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Chatreload extends UniversalCommand {
	@Inject
	private ImmersiveChat plugin;
	@Inject
	private SpigotFlatFileController spigotFlatFileController;
	@Inject
	private PlayerApiService playerApiService;
	@Inject
	private ConfigService configService;

	@Inject
	public Chatreload(String name) {
		super(name);
	}

	@Override
	public void executeCommand(CommandSender sender, String command, String[] args) {
		if (sender.hasPermission("venturechat.reload")) {
			spigotFlatFileController.savePlayerData();
			playerApiService.clearMineverseChatPlayerMap();
			playerApiService.clearNameMap();
			playerApiService.clearOnlineMineverseChatPlayerMap();

			plugin.reloadConfig();
			configService.postConstruct();

			spigotFlatFileController.loadLegacyPlayerData();
			spigotFlatFileController.loadPlayerData();
			for (Player p : plugin.getServer().getOnlinePlayers()) {
				ImmersiveChatPlayer mcp = playerApiService.getImmersiveChatPlayer(p);
				if (mcp == null) {
					plugin.getServer().getConsoleSender()
							.sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&c - Could not find player data post reload for currently online player: " + p.getName()));
					plugin.getServer().getConsoleSender().sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&c - There could be an issue with your player data saving."));
					String name = p.getName();
					UUID uuid = p.getUniqueId();
					mcp = ImmersiveChatPlayer.builder().uuid(uuid).name(name).currentChannel(configService.getDefaultChannel()).build();
					mcp.getListening().add(configService.getDefaultChannel().getName());
				}
				mcp.setOnline(true);
				mcp.setPlayer(plugin.getServer().getPlayer(mcp.getUuid()));
				mcp.setHasPlayed(false);
				String jsonFormat = mcp.getJsonFormat();
				for (JsonFormat j : configService.getJsonFormats()) {
					if (mcp.getPlayer().hasPermission("venturechat.json." + j.getName())) {
						if (configService.getJsonFormat(mcp.getJsonFormat()).getPriority() > j.getPriority()) {
							jsonFormat = j.getName();
						}
					}
				}
				mcp.setJsonFormat(jsonFormat);
				playerApiService.addMineverseChatOnlinePlayerToMap(mcp);
				playerApiService.addNameToMap(mcp);
			}

			plugin.getServer().getConsoleSender().sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - Config reloaded"));
			for (IImmersiveChatPlayer player : playerApiService.getOnlineMineverseChatPlayers()) {
				if (player.getPlayer().hasPermission("venturechat.reload")) {
					player.getPlayer().sendMessage(LocalizedMessage.CONFIG_RELOADED.toString());
				}
			}
			return;
		}
		sender.sendMessage(LocalizedMessage.COMMAND_NO_PERMISSION.toString());
		return;
	}
}
