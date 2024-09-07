package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import me.slide.immersivechat.api.events.ChannelLeaveEvent;
import me.slide.immersivechat.controllers.PluginMessageController;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.ChatChannel;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import me.slide.immersivechat.model.PlayerCommand;
import me.slide.immersivechat.service.ConfigService;
import me.slide.immersivechat.service.PlayerApiService;
import org.bukkit.entity.Player;

public class ChannelAlias extends PlayerCommand {
	@Inject
	private PlayerApiService playerApiService;
	@Inject
	private ConfigService configService;
	@Inject
	private PluginMessageController pluginMessageController;

	public ChannelAlias() {
		super("channelalias");
	}

	@Override
	protected void executeCommand(final Player player, final String commandLabel, final String[] args) {
		IImmersiveChatPlayer mcp = playerApiService.getOnlineImmersiveChatPlayer(player);
		for (ChatChannel channel : configService.getChatChannels()) {
			if (commandLabel.toLowerCase().equals(channel.getAlias())) {
				if (args.length == 0) {
					ChannelLeaveEvent channelLeaveEvent = new ChannelLeaveEvent(mcp, mcp.getCurrentChannel());
					channelLeaveEvent.callEvent();
					if(channelLeaveEvent.isCancelled()){
						return;
					}

					mcp.getPlayer()
							.sendMessage(LocalizedMessage.SET_CHANNEL.toString().replace("{channel_color}", channel.getColor() + "").replace("{channel_name}", channel.getName()));
					if (mcp.getConversation() != null) {
						for (IImmersiveChatPlayer p : playerApiService.getOnlineMineverseChatPlayers()) {
							if (configService.isSpy(p)) {
								p.getPlayer().sendMessage(LocalizedMessage.EXIT_PRIVATE_CONVERSATION_SPY.toString().replace("{player_sender}", mcp.getName())
										.replace("{player_receiver}", playerApiService.getImmersiveChatPlayer(mcp.getConversation()).getName()));
							}
						}
						mcp.getPlayer().sendMessage(LocalizedMessage.EXIT_PRIVATE_CONVERSATION.toString().replace("{player_receiver}",
								playerApiService.getImmersiveChatPlayer(mcp.getConversation()).getName()));
						mcp.setConversation(null);
					}
					mcp.getListening().add(channel.getName());
					mcp.setCurrentChannel(channel);
					if (channel.isBungeeEnabled()) {
						pluginMessageController.synchronize(mcp, true);
					}
					return;
				} else {
					mcp.setQuickChat(true);
					mcp.setQuickChannel(channel);
					mcp.getListening().add(channel.getName());
					if (channel.isBungeeEnabled()) {
						pluginMessageController.synchronize(mcp, true);
					}
					String msg = "";
					for (int x = 0; x < args.length; x++) {
						if (args[x].length() > 0)
							msg += " " + args[x];
					}
					mcp.getPlayer().chat(msg);
					return;
				}
			}
		}
	}
}
