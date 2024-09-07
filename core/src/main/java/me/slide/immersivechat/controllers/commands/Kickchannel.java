package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import me.slide.immersivechat.api.events.ChannelLeaveEvent;
import me.slide.immersivechat.api.events.KickChannelPlayerEvent;
import me.slide.immersivechat.controllers.PluginMessageController;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.ChatChannel;
import me.slide.immersivechat.model.ImmersiveChatPlayer;
import me.slide.immersivechat.model.UniversalCommand;
import me.slide.immersivechat.service.ConfigService;
import me.slide.immersivechat.service.PlayerApiService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;

public class Kickchannel extends UniversalCommand {
	@Inject
	private PluginMessageController pluginMessageController;
	@Inject
	private PlayerApiService playerApiService;
	@Inject
	private ConfigService configService;

	@Inject
	public Kickchannel(String name) {
		super(name);
	}

	@Override
	public void executeCommand(CommandSender sender, String command, String[] args) {
		KickChannelPlayerEvent kickChannelPlayerEvent;
		if (sender.hasPermission("venturechat.kickchannel")) {
			if (args.length < 2) {
				sender.sendMessage(LocalizedMessage.COMMAND_INVALID_ARGUMENTS.toString().replace("{command}", "/kickchannel").replace("{args}", "[player] [channel]"));
				return;
			}
			ImmersiveChatPlayer player = playerApiService.getImmersiveChatPlayer(args[0]);
			if (player == null) {
				sender.sendMessage(LocalizedMessage.PLAYER_OFFLINE.toString().replace("{args}", args[0]));
				return;
			}
			ChatChannel channel = configService.getChannel(args[1]);
			if (channel == null) {
				sender.sendMessage(LocalizedMessage.INVALID_CHANNEL.toString().replace("{args}", args[1]));
				return;
			}

			if(sender instanceof Player) {
				kickChannelPlayerEvent = new KickChannelPlayerEvent(player, playerApiService.getImmersiveChatPlayer(sender.getName()), Collections.singleton(channel));
			} else {
				kickChannelPlayerEvent = new KickChannelPlayerEvent(player, null, Collections.singleton(channel));
			}
			kickChannelPlayerEvent.callEvent();
			if(kickChannelPlayerEvent.isCancelled()){
				return;
			}

			ChannelLeaveEvent channelLeaveEvent = new ChannelLeaveEvent(player, channel);
			channelLeaveEvent.callEvent();
			if(channelLeaveEvent.isCancelled()){
				return;
			}

			sender.sendMessage(LocalizedMessage.KICK_CHANNEL.toString().replace("{player}", args[0]).replace("{channel_color}", channel.getColor() + "").replace("{channel_name}",
					channel.getName()));
			player.getListening().remove(channel.getName());
			if (player.isOnline()) {
				player.getPlayer()
						.sendMessage(LocalizedMessage.LEAVE_CHANNEL.toString().replace("{channel_color}", channel.getColor() + "").replace("{channel_name}", channel.getName()));
			} else {
				player.setModified(true);
			}
			boolean isThereABungeeChannel = channel.isBungeeEnabled();
			if (player.getListening().size() == 0) {
				player.getListening().add(configService.getDefaultChannel().getName());
				player.setCurrentChannel(configService.getDefaultChannel());
				if (configService.getDefaultChannel().isBungeeEnabled()) {
					isThereABungeeChannel = true;
				}
				if (player.isOnline()) {
					player.getPlayer().sendMessage(LocalizedMessage.MUST_LISTEN_ONE_CHANNEL.toString());
					player.getPlayer()
							.sendMessage(LocalizedMessage.SET_CHANNEL.toString().replace("{channel_color}", ChatColor.valueOf(configService.getDefaultColor().toUpperCase()) + "")
									.replace("{channel_name}", configService.getDefaultChannel().getName()));
				} else
					player.setModified(true);
			}
			if (isThereABungeeChannel) {
				pluginMessageController.synchronize(player, true);
			}
			return;
		}
		sender.sendMessage(LocalizedMessage.COMMAND_NO_PERMISSION.toString());
	}
}
