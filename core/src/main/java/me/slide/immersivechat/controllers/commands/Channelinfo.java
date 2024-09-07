package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import me.slide.immersivechat.model.ChatChannel;
import me.slide.immersivechat.model.UniversalCommand;
import me.slide.immersivechat.service.ConfigService;
import me.slide.immersivechat.utilities.FormatUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Channelinfo extends UniversalCommand {
	@Inject
	private ConfigService configService;

	@Inject
	public Channelinfo(String name) {
		super(name);
	}

	@Override
	protected void executeCommand(CommandSender sender, String command, String[] args) {
		if (sender.hasPermission("venturechat.channelinfo")) {
			if (args.length < 1) {
				sender.sendMessage(ChatColor.RED + "Invalid command: /channelinfo [channel]");
				return;
			}
			ChatChannel chname = configService.getChannel(args[0]);
			if (chname == null) {
				sender.sendMessage(ChatColor.RED + "Invalid channel: " + args[0]);
				return;
			}
			if (chname.isPermissionRequired()) {
				if (!sender.hasPermission(chname.getPermission())) {
					sender.sendMessage(ChatColor.RED + "You do not have permission to look at this channel.");
					return;
				}
			}
			sender.sendMessage(ChatColor.GOLD + "Channel: " + chname.getColor() + chname.getName());
			sender.sendMessage(ChatColor.GOLD + "Alias: " + chname.getColor() + chname.getAlias());
			sender.sendMessage(ChatColor.GOLD + "Color: " + chname.getColor() + chname.getColorRaw());
			sender.sendMessage(ChatColor.GOLD + "ChatColor: " + (chname.getChatColor().equalsIgnoreCase("None") ? FormatUtils.DEFAULT_COLOR_CODE : chname.getChatColor())
					+ chname.getChatColorRaw());
			if (chname.isPermissionRequired()) {
				sender.sendMessage(ChatColor.GOLD + "Permission: " + chname.getColor() + chname.getPermission());
			} else {
				sender.sendMessage(ChatColor.GOLD + "Permission: " + chname.getColor() + "None");
			}
			if (chname.isSpeakPermissionRequired()) {
				sender.sendMessage(ChatColor.GOLD + "Speak Permission: " + chname.getColor() + chname.getSpeakPermission());
			} else {
				sender.sendMessage(ChatColor.GOLD + "Speak Permission: " + chname.getColor() + "None");
			}
			sender.sendMessage(ChatColor.GOLD + "Autojoin: " + chname.getColor() + chname.isAutoJoinEnabled());
			sender.sendMessage(ChatColor.GOLD + "Default: " + chname.getColor() + chname.isDefaultChannel());
			if (chname.getDistance() <= 0 || chname.isBungeeEnabled()) {
				sender.sendMessage(ChatColor.GOLD + "Distance: " + ChatColor.RED + "N/A");
			} else {
				sender.sendMessage(ChatColor.GOLD + "Distance: " + chname.getColor() + chname.getDistance());
			}
			if (chname.getCooldown() <= 0) {
				sender.sendMessage(ChatColor.GOLD + "Cooldown: " + ChatColor.RED + "N/A");
			} else {
				sender.sendMessage(ChatColor.GOLD + "Cooldown: " + chname.getColor() + chname.getCooldown());
			}
			sender.sendMessage(ChatColor.GOLD + "Bungeecord: " + chname.getColor() + chname.isBungeeEnabled());
			sender.sendMessage(ChatColor.GOLD + "Format: " + chname.getColor() + chname.getFormat());
			return;
		} else {
			sender.sendMessage(ChatColor.RED + "You do not have permission for this command.");
			return;
		}
	}
}
