package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import me.slide.immersivechat.model.PlayerCommand;
import me.slide.immersivechat.service.ConfigService;
import me.slide.immersivechat.service.PlayerApiService;
import org.bukkit.entity.Player;

public class RangedSpy extends PlayerCommand {
	@Inject
	private PlayerApiService playerApiService;
	@Inject
	private ConfigService configService;

	@Inject
	public RangedSpy(String name) {
		super(name);
	}

	@Override
	public void executeCommand(Player player, String command, String[] args) {
		IImmersiveChatPlayer mcp = playerApiService.getOnlineImmersiveChatPlayer((Player) player);
		if (mcp.getPlayer().hasPermission("venturechat.rangedspy")) {
			if (!configService.isRangedSpy(mcp)) {
				mcp.setRangedSpy(true);
				mcp.getPlayer().sendMessage(LocalizedMessage.RANGED_SPY_ON.toString());
				return;
			}
			mcp.setRangedSpy(false);
			mcp.getPlayer().sendMessage(LocalizedMessage.RANGED_SPY_OFF.toString());
			return;
		}
		mcp.getPlayer().sendMessage(LocalizedMessage.COMMAND_NO_PERMISSION.toString());
		return;
	}
}
