package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import me.slide.immersivechat.model.PlayerCommand;
import me.slide.immersivechat.service.PlayerApiService;
import org.bukkit.entity.Player;

public class Notifications extends PlayerCommand {
	@Inject
	private PlayerApiService playerApiService;

	@Inject
	public Notifications(String name) {
		super(name);
	}

	@Override
	public void executeCommand(Player player, String command, String[] args) {
		IImmersiveChatPlayer mcp = playerApiService.getOnlineImmersiveChatPlayer(player);
		if (!mcp.isNotifications()) {
			mcp.setNotifications(true);
			mcp.getPlayer().sendMessage(LocalizedMessage.NOTIFICATIONS_ON.toString());
			return;
		}
		mcp.setNotifications(false);
		mcp.getPlayer().sendMessage(LocalizedMessage.NOTIFICATIONS_OFF.toString());
		return;
	}
}
