package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import me.slide.immersivechat.api.events.MutePlayerEvent;
import me.slide.immersivechat.controllers.PluginMessageController;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.*;
import me.slide.immersivechat.service.ConfigService;
import me.slide.immersivechat.service.PlayerApiService;
import me.slide.immersivechat.utilities.FormatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.stream.Collectors;

public class Muteall extends UniversalCommand {
	@Inject
	private PluginMessageController pluginMessageController;
	@Inject
	private PlayerApiService playerApiService;
	@Inject
	private ConfigService configService;

	@Inject
	public Muteall(String name) {
		super(name);
	}

	@Override
	public void executeCommand(CommandSender sender, String command, String[] args) {
		MutePlayerEvent mutePlayerEvent;
		if (sender.hasPermission("venturechat.mute")) {
			if (args.length < 1) {
				sender.sendMessage(LocalizedMessage.COMMAND_INVALID_ARGUMENTS.toString().replace("{command}", "/muteall").replace("{args}", "[player] {reason}"));
				return;
			}
			ImmersiveChatPlayer player = playerApiService.getImmersiveChatPlayer(args[0]);
			if (player == null || (!player.isOnline() && !sender.hasPermission("venturechat.mute.offline"))) {
				sender.sendMessage(LocalizedMessage.PLAYER_OFFLINE.toString().replace("{args}", args[0]));
				return;
			}
			String reason = "";
			if (args.length > 1) {
				StringBuilder reasonBuilder = new StringBuilder();
				for (int a = 1; a < args.length; a++) {
					reasonBuilder.append(args[a] + " ");
				}
				reason = FormatUtils.FormatStringAll(reasonBuilder.toString().trim());
			}

			Set<IChatChannel> mutedChannels = player.getMutes()
					.keySet()
					.stream()
					.map(configService::getChannel)
					.collect(Collectors.toSet());
			if (reason.isEmpty()) {
				if(sender instanceof Player) {
					mutePlayerEvent = new MutePlayerEvent(player, playerApiService.getImmersiveChatPlayer(sender.getName()), mutedChannels, 0);
				} else {
					mutePlayerEvent = new MutePlayerEvent(player, null, mutedChannels, 0);
				}
				mutePlayerEvent.callEvent();
				if(mutePlayerEvent.isCancelled()){
					return;
				}

				boolean bungee = false;
				for (ChatChannel channel : configService.getChatChannels()) {
					if (channel.isMutable()) {
						player.getMutes().put(channel.getName(), new MuteContainer(channel.getName(), 0, ""));
						if (channel.isBungeeEnabled()) {
							bungee = true;
						}
					}
				}
				if (bungee) {
					pluginMessageController.synchronize(player, true);
				}
				sender.sendMessage(LocalizedMessage.MUTE_PLAYER_ALL_SENDER.toString().replace("{player}", player.getName()));
				if (player.isOnline()) {
					player.getPlayer().sendMessage(LocalizedMessage.MUTE_PLAYER_ALL_PLAYER.toString());
				} else
					player.setModified(true);
				return;
			} else {
				if(sender instanceof Player) {
					mutePlayerEvent = new MutePlayerEvent(player, playerApiService.getImmersiveChatPlayer(sender.getName()), mutedChannels, 0, reason);
				} else {
					mutePlayerEvent = new MutePlayerEvent(player, null, mutedChannels, 0, reason);
				}
				mutePlayerEvent.callEvent();
				if(mutePlayerEvent.isCancelled()){
					return;
				}

				boolean bungee = false;
				for (ChatChannel channel : configService.getChatChannels()) {
					if (channel.isMutable()) {
						player.getMutes().put(channel.getName(), new MuteContainer(channel.getName(), 0, reason));
						if (channel.isBungeeEnabled()) {
							bungee = true;
						}
					}
				}
				if (bungee) {
					pluginMessageController.synchronize(player, true);
				}
				sender.sendMessage(LocalizedMessage.MUTE_PLAYER_ALL_SENDER_REASON.toString().replace("{player}", player.getName()).replace("{reason}", reason));
				if (player.isOnline()) {
					player.getPlayer().sendMessage(LocalizedMessage.MUTE_PLAYER_ALL_PLAYER_REASON.toString().replace("{reason}", reason));
				} else
					player.setModified(true);
				return;
			}
		} else {
			sender.sendMessage(LocalizedMessage.COMMAND_NO_PERMISSION.toString());
			return;
		}
	}
}
