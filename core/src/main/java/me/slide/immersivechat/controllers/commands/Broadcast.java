package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import me.slide.immersivechat.initiators.application.ImmersiveChat;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.UniversalCommand;
import me.slide.immersivechat.service.FormatService;
import me.slide.immersivechat.utilities.FormatUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

public class Broadcast extends UniversalCommand {
	@Inject
	private ImmersiveChat plugin;
	@Inject
	private FormatService formatService;

	@Inject
	public Broadcast(String name) {
		super(name);
	}

	@Override
	protected void executeCommand(CommandSender sender, String command, String[] args) {
		ConfigurationSection bs = plugin.getConfig().getConfigurationSection("broadcast");
		String broadcastColor = bs.getString("color", "white");
		String broadcastPermissions = bs.getString("permissions", "None");
		String broadcastDisplayTag = FormatUtils.FormatStringAll(bs.getString("displaytag", "[Broadcast]"));
		if (broadcastPermissions.equalsIgnoreCase("None") || sender.hasPermission(broadcastPermissions)) {
			if (args.length > 0) {
				String bc = "";
				for (int x = 0; x < args.length; x++) {
					if (args[x].length() > 0)
						bc += args[x] + " ";
				}
				bc = FormatUtils.FormatStringAll(bc);
				formatService.broadcastToServer(broadcastDisplayTag + ChatColor.valueOf(broadcastColor.toUpperCase()) + " " + bc);
				return;
			} else {
				sender.sendMessage(LocalizedMessage.COMMAND_INVALID_ARGUMENTS.toString().replace("{command}", "/broadcast").replace("{args}", "[msg]"));
				return;
			}
		} else {
			sender.sendMessage(LocalizedMessage.COMMAND_NO_PERMISSION.toString());
			return;
		}
	}
}
