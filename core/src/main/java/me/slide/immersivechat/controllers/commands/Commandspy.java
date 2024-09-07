package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import me.slide.immersivechat.model.PlayerCommand;
import me.slide.immersivechat.service.ConfigService;
import me.slide.immersivechat.service.PlayerApiService;
import org.bukkit.entity.Player;

public class Commandspy extends PlayerCommand {
	@Inject
	private PlayerApiService playerApiService;
	@Inject
	private ConfigService configService;

	@Inject
	public Commandspy(String name) {
		super(name);
	}

	@Override
	public void executeCommand(Player sender, String command, String[] args) {
		IImmersiveChatPlayer mcp = playerApiService.getOnlineImmersiveChatPlayer((Player) sender);
		if (mcp.getPlayer().hasPermission("venturechat.commandspy")) {
			if (!configService.isCommandSpy(mcp)) {
				mcp.setCommandSpy(true);
				mcp.getPlayer().sendMessage(LocalizedMessage.COMMANDSPY_ON.toString());
				return;
			}
			mcp.setCommandSpy(false);
			mcp.getPlayer().sendMessage(LocalizedMessage.COMMANDSPY_OFF.toString());
			return;
		}
		mcp.getPlayer().sendMessage(LocalizedMessage.COMMAND_NO_PERMISSION.toString());
	}
}
