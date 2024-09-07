package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import me.slide.immersivechat.controllers.PluginMessageController;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.ChatChannel;
import me.slide.immersivechat.model.ImmersiveChatPlayer;
import me.slide.immersivechat.model.UniversalCommand;
import me.slide.immersivechat.service.ConfigService;
import me.slide.immersivechat.service.PlayerApiService;
import org.bukkit.command.CommandSender;

public class Setchannelall extends UniversalCommand {
	@Inject
	private PluginMessageController pluginMessageController;
	@Inject
	private PlayerApiService playerApiService;
	@Inject
	private ConfigService configService;

	@Inject
	public Setchannelall(String name) {
		super(name);
	}

	@Override
	public void executeCommand(CommandSender sender, String command, String[] args) {
		if (sender.hasPermission("venturechat.setchannelall")) {
			if (args.length < 1) {
				sender.sendMessage(LocalizedMessage.COMMAND_INVALID_ARGUMENTS.toString().replace("{command}", "/setchannelall").replace("{args}", "[player]"));
				return;
			}
			ImmersiveChatPlayer player = playerApiService.getImmersiveChatPlayer(args[0]);
			if (player == null) {
				sender.sendMessage(LocalizedMessage.PLAYER_OFFLINE.toString().replace("{args}", args[0]));
				return;
			}
			boolean isThereABungeeChannel = false;
			for (ChatChannel channel : configService.getChatChannels()) {
				if (channel.isPermissionRequired()) {
					if (!player.isOnline()) {
						sender.sendMessage(LocalizedMessage.PLAYER_OFFLINE_NO_PERMISSIONS_CHECK.toString());
						return;
					}
					if (!player.getPlayer().hasPermission(channel.getPermission())) {
						player.getListening().remove(channel.getName());
					} else {
						player.getListening().add(channel.getName());
					}
				} else {
					player.getListening().add(channel.getName());
				}
				if (channel.isBungeeEnabled()) {
					isThereABungeeChannel = true;
				}
			}
			sender.sendMessage(LocalizedMessage.SET_CHANNEL_ALL_SENDER.toString().replace("{player}", player.getName()));
			if (player.isOnline())
				player.getPlayer().sendMessage(LocalizedMessage.SET_CHANNEL_ALL_PLAYER.toString());
			else
				player.setModified(true);
			if (isThereABungeeChannel) {
				pluginMessageController.synchronize(player, true);
			}
			return;
		}
		sender.sendMessage(LocalizedMessage.COMMAND_NO_PERMISSION.toString());
	}
}
