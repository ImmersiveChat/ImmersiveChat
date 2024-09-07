package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.slide.immersivechat.api.events.ChannelJoinEvent;
import me.slide.immersivechat.api.events.ChannelLeaveEvent;
import me.slide.immersivechat.controllers.PluginMessageController;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.ChatChannel;
import me.slide.immersivechat.model.IChatChannel;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import me.slide.immersivechat.model.PlayerCommand;
import me.slide.immersivechat.service.ConfigService;
import me.slide.immersivechat.service.PlayerApiService;
import org.bukkit.entity.Player;

@Singleton
public class Channel extends PlayerCommand {
	@Inject
	private PluginMessageController pluginMessageController;
	@Inject
	private PlayerApiService playerApiService;
	@Inject
	private ConfigService configService;

	@Inject
	public Channel(String name) {
		super(name);
	}

	@Override
	protected void executeCommand(final Player player, final String commandLabel, final String[] args) {
		final IImmersiveChatPlayer mcp = playerApiService.getOnlineImmersiveChatPlayer(player);
		if (args.length > 0) {
			if (!configService.isChannel(args[0])) {
				mcp.getPlayer().sendMessage(LocalizedMessage.INVALID_CHANNEL.toString().replace("{args}", args[0]));
				return;
			}

			ChannelLeaveEvent channelLeaveEvent = new ChannelLeaveEvent(mcp, mcp.getCurrentChannel());
			channelLeaveEvent.callEvent();
			if(channelLeaveEvent.isCancelled()){
				return;
			}

			ChatChannel channel = configService.getChannel(args[0]);
			ChannelJoinEvent channelJoinEvent = new ChannelJoinEvent(mcp, channel,
					LocalizedMessage.SET_CHANNEL.toString().replace("{channel_color}", channel.getColor() + "").replace("{channel_name}", channel.getName()));
			plugin.getServer().getPluginManager().callEvent(channelJoinEvent);
			handleChannelJoinEvent(channelJoinEvent);
			return;
		}
		mcp.getPlayer().sendMessage(LocalizedMessage.COMMAND_INVALID_ARGUMENTS.toString().replace("{command}", "/channel").replace("{args}", "[channel]"));
	}

	private void handleChannelJoinEvent(final ChannelJoinEvent event) {
		if (event.isCancelled())
			return;
		IChatChannel channel = event.getChannel();
		IImmersiveChatPlayer mcp = playerApiService.getOnlineImmersiveChatPlayer(event.getPlayer().getPlayer());
		if (channel.isPermissionRequired()) {
			if (!mcp.getPlayer().hasPermission(channel.getPermission())) {
				mcp.getListening().remove(channel.getName());
				mcp.getPlayer().sendMessage(LocalizedMessage.CHANNEL_NO_PERMISSION.toString());
				return;
			}
		}
		if (mcp.getConversation() != null) {
			for (IImmersiveChatPlayer p : playerApiService.getOnlineMineverseChatPlayers()) {
				if (configService.isSpy(p)) {
					p.getPlayer().sendMessage(LocalizedMessage.EXIT_PRIVATE_CONVERSATION_SPY.toString().replace("{player_sender}", mcp.getName()).replace("{player_receiver}",
							playerApiService.getImmersiveChatPlayer(mcp.getConversation()).getName()));
				}
			}
			mcp.getPlayer().sendMessage(
					LocalizedMessage.EXIT_PRIVATE_CONVERSATION.toString().replace("{player_receiver}", playerApiService.getImmersiveChatPlayer(mcp.getConversation()).getName()));
			mcp.setConversation(null);
		}
		mcp.getListening().add(channel.getName());
		mcp.setCurrentChannel(channel);
		mcp.getPlayer().sendMessage(event.getMessage());
		if (channel.isBungeeEnabled()) {
			pluginMessageController.synchronize(mcp, true);
		}
	}
}
