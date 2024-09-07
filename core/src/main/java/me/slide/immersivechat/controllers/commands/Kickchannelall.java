package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import me.slide.immersivechat.api.events.ChannelLeaveEvent;
import me.slide.immersivechat.api.events.KickChannelPlayerEvent;
import me.slide.immersivechat.controllers.PluginMessageController;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.ChatChannel;
import me.slide.immersivechat.model.IChatChannel;
import me.slide.immersivechat.model.ImmersiveChatPlayer;
import me.slide.immersivechat.model.UniversalCommand;
import me.slide.immersivechat.service.ConfigService;
import me.slide.immersivechat.service.PlayerApiService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.stream.Collectors;

public class Kickchannelall extends UniversalCommand {
	@Inject
	private PluginMessageController pluginMessageController;
	@Inject
	private PlayerApiService playerApiService;
	@Inject
	private ConfigService configService;

	@Inject
	public Kickchannelall(String name) {
		super(name);
	}

	@Override
	public void executeCommand(CommandSender sender, String command, String[] args) {
		KickChannelPlayerEvent kickChannelPlayerEvent;
		if (sender.hasPermission("venturechat.kickchannelall")) {
			if (args.length < 1) {
				sender.sendMessage(LocalizedMessage.COMMAND_INVALID_ARGUMENTS.toString().replace("{command}", "/kickchannelall").replace("{args}", "[player]"));
				return;
			}
			ImmersiveChatPlayer player = playerApiService.getImmersiveChatPlayer(args[0]);
			if (player == null) {
				sender.sendMessage(LocalizedMessage.PLAYER_OFFLINE.toString().replace("{args}", args[0]));
				return;
			}
			boolean isThereABungeeChannel = false;
			for (String channel : player.getListening()) {
				if (configService.isChannel(channel)) {
					ChatChannel chatChannelObj = configService.getChannel(channel);
					if (chatChannelObj.isBungeeEnabled()) {
						isThereABungeeChannel = true;
					}
				}
			}

			Set<IChatChannel> kickedChannels = player.getListening()
					.stream()
					.map(configService::getChannel)
					.collect(Collectors.toSet());

			if(sender instanceof Player) {
				kickChannelPlayerEvent = new KickChannelPlayerEvent(player, playerApiService.getImmersiveChatPlayer(sender.getName()), kickedChannels);
			} else {
				kickChannelPlayerEvent = new KickChannelPlayerEvent(player, null, kickedChannels);
			}
			kickChannelPlayerEvent.callEvent();
			if(kickChannelPlayerEvent.isCancelled()){
				return;
			}

			ChannelLeaveEvent channelLeaveEvent = new ChannelLeaveEvent(player, player.getCurrentChannel());
			channelLeaveEvent.callEvent();
			if(channelLeaveEvent.isCancelled()){
				return;
			}

			player.getListening().clear();
			sender.sendMessage(LocalizedMessage.KICK_CHANNEL_ALL_SENDER.toString().replace("{player}", player.getName()));
			player.getListening().add(configService.getDefaultChannel().getName());
			player.setCurrentChannel(configService.getDefaultChannel());
			if (configService.getDefaultChannel().isBungeeEnabled()) {
				isThereABungeeChannel = true;
			}
			if (isThereABungeeChannel) {
				pluginMessageController.synchronize(player, true);
			}
			if (player.isOnline()) {
				player.getPlayer().sendMessage(LocalizedMessage.KICK_CHANNEL_ALL_PLAYER.toString());
				player.getPlayer().sendMessage(LocalizedMessage.MUST_LISTEN_ONE_CHANNEL.toString());
				player.getPlayer()
						.sendMessage(LocalizedMessage.SET_CHANNEL.toString().replace("{channel_color}", ChatColor.valueOf(configService.getDefaultColor().toUpperCase()) + "")
								.replace("{channel_name}", configService.getDefaultChannel().getName()));
			} else {
				player.setModified(true);
			}
			return;
		}
		sender.sendMessage(LocalizedMessage.COMMAND_NO_PERMISSION.toString());
	}
}
