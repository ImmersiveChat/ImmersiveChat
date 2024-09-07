package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import me.slide.immersivechat.model.PlayerCommand;
import me.slide.immersivechat.service.PlayerApiService;
import org.bukkit.entity.Player;

public class Filter extends PlayerCommand {
	@Inject
	private PlayerApiService playerApiService;

	@Inject
	public Filter(String name) {
		super(name);
	}

	@Override
	public void executeCommand(Player sender, String command, String[] args) {
		IImmersiveChatPlayer mcp = playerApiService.getOnlineImmersiveChatPlayer((Player) sender);
		if (mcp.getPlayer().hasPermission("venturechat.ignorefilter")) {
			if (!mcp.isFilterEnabled()) {
				mcp.setFilterEnabled(true);
				mcp.getPlayer().sendMessage(LocalizedMessage.FILTER_ON.toString());
				return;
			}
			mcp.setFilterEnabled(false);
			mcp.getPlayer().sendMessage(LocalizedMessage.FILTER_OFF.toString());
			return;
		}
		mcp.getPlayer().sendMessage(LocalizedMessage.COMMAND_NO_PERMISSION.toString());
	}
}
