package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import me.slide.immersivechat.controllers.PluginMessageController;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import me.slide.immersivechat.model.PlayerCommand;
import me.slide.immersivechat.service.PlayerApiService;
import org.bukkit.entity.Player;

public class MessageToggle extends PlayerCommand {
	@Inject
	private PluginMessageController pluginMessageController;
	@Inject
	private PlayerApiService playerApiService;

	@Inject
	public MessageToggle(String name) {
		super(name);
	}

	@Override
	public void executeCommand(Player player, String command, String[] args) {
		IImmersiveChatPlayer mcp = playerApiService.getOnlineImmersiveChatPlayer(player);
		if (mcp.getPlayer().hasPermission("venturechat.messagetoggle")) {
			if (!mcp.isMessageToggle()) {
				mcp.setMessageToggle(true);
				mcp.getPlayer().sendMessage(LocalizedMessage.MESSAGE_TOGGLE_ON.toString());
				pluginMessageController.synchronize(mcp, true);
				return;
			}
			mcp.setMessageToggle(false);
			mcp.getPlayer().sendMessage(LocalizedMessage.MESSAGE_TOGGLE_OFF.toString());
			pluginMessageController.synchronize(mcp, true);
			return;
		}
		mcp.getPlayer().sendMessage(LocalizedMessage.COMMAND_NO_PERMISSION.toString());
		return;
	}
}
