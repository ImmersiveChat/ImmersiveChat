package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import me.slide.immersivechat.controllers.PluginMessageController;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.ChatChannel;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import me.slide.immersivechat.model.PlayerCommand;
import me.slide.immersivechat.service.ConfigService;
import me.slide.immersivechat.service.PlayerApiService;
import org.bukkit.entity.Player;

public class Listen extends PlayerCommand {
	@Inject
	private PluginMessageController pluginMessageController;
	@Inject
	private PlayerApiService playerApiService;
	@Inject
	private ConfigService configService;

	@Inject
	public Listen(String name) {
		super(name);
	}

	@Override
	public void executeCommand(Player player, String command, String[] args) {
		IImmersiveChatPlayer mcp = playerApiService.getOnlineImmersiveChatPlayer(player);
		if (args.length > 0) {
			ChatChannel channel = configService.getChannel(args[0]);
			if (channel == null) {
				mcp.getPlayer().sendMessage(LocalizedMessage.INVALID_CHANNEL.toString().replace("{args}", args[0]));
				return;
			}
			if (channel.isPermissionRequired()) {
				if (!mcp.getPlayer().hasPermission(channel.getPermission())) {
					mcp.getListening().remove(channel.getName());
					mcp.getPlayer().sendMessage(LocalizedMessage.CHANNEL_NO_PERMISSION.toString());
					return;
				}
			}
			mcp.getListening().add(channel.getName());
			mcp.getPlayer()
					.sendMessage(LocalizedMessage.LISTEN_CHANNEL.toString().replace("{channel_color}", channel.getColor() + "").replace("{channel_name}", channel.getName()));
			if (channel.isBungeeEnabled()) {
				pluginMessageController.synchronize(mcp, true);
			}
			return;
		}
		mcp.getPlayer().sendMessage(LocalizedMessage.COMMAND_INVALID_ARGUMENTS.toString().replace("{command}", "/listen").replace("{args}", "[channel]"));
	}
}
