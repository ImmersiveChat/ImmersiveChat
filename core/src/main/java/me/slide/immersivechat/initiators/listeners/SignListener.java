package me.slide.immersivechat.initiators.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import me.slide.immersivechat.service.PlayerApiService;
import me.slide.immersivechat.utilities.FormatUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

@Singleton
public class SignListener implements Listener {
	@Inject
	private PlayerApiService playerApiService;

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGH)
	public void onSignChange(SignChangeEvent event) {
		IImmersiveChatPlayer mcp = playerApiService.getOnlineImmersiveChatPlayer(event.getPlayer());
		for (int a = 0; a < event.getLines().length; a++) {
			String line = event.getLine(a);
			if (mcp.getPlayer().hasPermission("venturechat.color.legacy")) {
				line = FormatUtils.FormatStringLegacyColor(line);
			}
			if (mcp.getPlayer().hasPermission("venturechat.color")) {
				line = FormatUtils.FormatStringColor(line);
			}
			if (mcp.getPlayer().hasPermission("venturechat.format")) {
				line = FormatUtils.FormatString(line);
			}
			event.setLine(a, line);
		}
	}
}
