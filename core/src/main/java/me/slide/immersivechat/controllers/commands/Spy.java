package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import me.slide.immersivechat.controllers.PluginMessageController;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import me.slide.immersivechat.model.PlayerCommand;
import me.slide.immersivechat.service.ConfigService;
import me.slide.immersivechat.service.PlayerApiService;
import org.bukkit.entity.Player;

public class Spy extends PlayerCommand {
	@Inject
	private PluginMessageController pluginMessageController;
	@Inject
	private PlayerApiService playerApiService;
	@Inject
	private ConfigService configService;

	@Inject
	public Spy(String name) {
		super(name);
	}

	@Override
	public void executeCommand(Player player, String command, String[] args) {
		IImmersiveChatPlayer mcp = playerApiService.getOnlineImmersiveChatPlayer(player);
		if (mcp.getPlayer().hasPermission("venturechat.spy")) {
			if (!configService.isSpy(mcp)) {
				mcp.setSpy(true);
				mcp.getPlayer().sendMessage(LocalizedMessage.SPY_ON.toString());
				pluginMessageController.synchronize(mcp, true);
				return;
			}
			mcp.setSpy(false);
			mcp.getPlayer().sendMessage(LocalizedMessage.SPY_OFF.toString());
			pluginMessageController.synchronize(mcp, true);
			return;
		}
		mcp.getPlayer().sendMessage(LocalizedMessage.COMMAND_NO_PERMISSION.toString());
		return;
	}
}
