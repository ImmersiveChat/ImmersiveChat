package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.UniversalCommand;
import me.slide.immersivechat.service.FormatService;
import me.slide.immersivechat.service.PlayerApiService;
import me.slide.immersivechat.utilities.FormatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Me extends UniversalCommand {
	@Inject
	private FormatService formatService;
	@Inject
	private PlayerApiService playerApiService;

	@Inject
	public Me(String name) {
		super(name);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void executeCommand(CommandSender sender, String command, String[] args) {
		if (sender.hasPermission("venturechat.me")) {
			if (args.length > 0) {
				String msg = "";
				for (int x = 0; x < args.length; x++)
					if (args[x].length() > 0)
						msg += " " + args[x];
				if (sender instanceof Player && playerApiService.getOnlineImmersiveChatPlayer((Player) sender).isFilterEnabled()) {
					msg = formatService.filterChat(msg);
				}
				if (sender.hasPermission("venturechat.color.legacy")) {
					msg = FormatUtils.FormatStringLegacyColor(msg);
				}
				if (sender.hasPermission("venturechat.color"))
					msg = FormatUtils.FormatStringColor(msg);
				if (sender.hasPermission("venturechat.format"))
					msg = FormatUtils.FormatString(msg);
				if (sender instanceof Player) {
					Player p = (Player) sender;
					formatService.broadcastToServer("* " + p.getDisplayName() + msg);
					return;
				}
				formatService.broadcastToServer("* " + sender.getName() + msg);
				return;
			}
			sender.sendMessage(LocalizedMessage.COMMAND_INVALID_ARGUMENTS.toString().replace("{command}", "/me").replace("{args}", "[message]"));
			return;
		}
		sender.sendMessage(LocalizedMessage.COMMAND_NO_PERMISSION.toString());
	}
}
