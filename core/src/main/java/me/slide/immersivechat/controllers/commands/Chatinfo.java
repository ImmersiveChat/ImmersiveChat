package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import me.slide.immersivechat.model.*;
import me.slide.immersivechat.service.ConfigService;
import me.slide.immersivechat.service.PlayerApiService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Chatinfo extends UniversalCommand {
	@Inject
	private PlayerApiService playerApiService;
	@Inject
	private ConfigService configService;

	@Inject
	public Chatinfo(String name) {
		super(name);
	}

	@Override
	public void executeCommand(CommandSender sender, String command, String[] args) {
		if (sender.hasPermission("venturechat.chatinfo")) {
			if (args.length == 0) {
				if (!(sender instanceof Player)) {
					plugin.getServer().getConsoleSender().sendMessage(ChatColor.RED + "This command must be run by a player; use /ci [name]");
					return;
				}
				IImmersiveChatPlayer mcp = playerApiService.getOnlineImmersiveChatPlayer((Player) sender);
				String listen = "";
				String mute = "";
				String blockedcommands = "";
				if (args.length < 1) {
					mcp.getPlayer().sendMessage(ChatColor.GOLD + "Player: " + ChatColor.GREEN + mcp.getName());
					for (String c : mcp.getListening()) {
						ChatChannel channel = configService.getChannel(c);
						listen += channel.getColor() + channel.getName() + " ";
					}
					for (IMuteContainer muteContainer : mcp.getMutes().values()) {
						ChatChannel channel = configService.getChannel(muteContainer.getChannel());
						mute += channel.getColor() + channel.getName() + " ";
					}
					for (String bc : mcp.getBlockedCommands()) {
						blockedcommands += bc + " ";
					}
					mcp.getPlayer().sendMessage(ChatColor.GOLD + "Listening: " + listen);
					if (mute.length() > 0) {
						mcp.getPlayer().sendMessage(ChatColor.GOLD + "Mutes: " + mute);
					} else {
						mcp.getPlayer().sendMessage(ChatColor.GOLD + "Mutes: " + ChatColor.RED + "N/A");
					}
					if (blockedcommands.length() > 0) {
						mcp.getPlayer().sendMessage(ChatColor.GOLD + "Blocked Commands: " + ChatColor.RED + blockedcommands);
					} else {
						mcp.getPlayer().sendMessage(ChatColor.GOLD + "Blocked Commands: " + ChatColor.RED + "N/A");
					}
					if (mcp.getConversation() != null) {
						mcp.getPlayer().sendMessage(
								ChatColor.GOLD + "Private conversation: " + ChatColor.GREEN + playerApiService.getImmersiveChatPlayer(mcp.getConversation()).getName());
					} else {
						mcp.getPlayer().sendMessage(ChatColor.GOLD + "Private conversation: " + ChatColor.RED + "N/A");
					}
					if (configService.isSpy(mcp)) {
						mcp.getPlayer().sendMessage(ChatColor.GOLD + "Spy: " + ChatColor.GREEN + "true");
					} else {
						mcp.getPlayer().sendMessage(ChatColor.GOLD + "Spy: " + ChatColor.RED + "false");
					}
					if (configService.isCommandSpy(mcp)) {
						mcp.getPlayer().sendMessage(ChatColor.GOLD + "Command spy: " + ChatColor.GREEN + "true");
					} else {
						mcp.getPlayer().sendMessage(ChatColor.GOLD + "Command spy: " + ChatColor.RED + "false");
					}
					if (configService.isRangedSpy(mcp)) {
						mcp.getPlayer().sendMessage(ChatColor.GOLD + "Ranged spy: " + ChatColor.GREEN + "true");
					} else {
						mcp.getPlayer().sendMessage(ChatColor.GOLD + "Ranged spy: " + ChatColor.RED + "false");
					}
					if (mcp.isFilterEnabled()) {
						mcp.getPlayer().sendMessage(ChatColor.GOLD + "Filter: " + ChatColor.GREEN + "true");
					} else {
						mcp.getPlayer().sendMessage(ChatColor.GOLD + "Filter: " + ChatColor.RED + "false");
					}
					return;
				}
			}
			if (sender.hasPermission("venturechat.chatinfo.others")) {
				String listen = "";
				String mute = "";
				String blockedcommands = "";
				ImmersiveChatPlayer p = playerApiService.getImmersiveChatPlayer(args[0]);
				if (p == null) {
					sender.sendMessage(ChatColor.RED + "Player: " + ChatColor.GOLD + args[0] + ChatColor.RED + " is not online.");
					return;
				}
				sender.sendMessage(ChatColor.GOLD + "Player: " + ChatColor.GREEN + p.getName());
				for (String c : p.getListening()) {
					ChatChannel channel = configService.getChannel(c);
					listen += channel.getColor() + channel.getName() + " ";
				}
				for (IMuteContainer muteContainer : p.getMutes().values()) {
					ChatChannel channel = configService.getChannel(muteContainer.getChannel());
					mute += channel.getColor() + channel.getName() + " ";
				}
				for (String bc : p.getBlockedCommands()) {
					blockedcommands += bc + " ";
				}
				sender.sendMessage(ChatColor.GOLD + "Listening: " + listen);
				if (mute.length() > 0) {
					sender.sendMessage(ChatColor.GOLD + "Mutes: " + mute);
				} else {
					sender.sendMessage(ChatColor.GOLD + "Mutes: " + ChatColor.RED + "N/A");
				}
				if (blockedcommands.length() > 0) {
					sender.sendMessage(ChatColor.GOLD + "Blocked Commands: " + ChatColor.RED + blockedcommands);
				} else {
					sender.sendMessage(ChatColor.GOLD + "Blocked Commands: " + ChatColor.RED + "N/A");
				}
				if (p.getConversation() != null) {
					sender.sendMessage(ChatColor.GOLD + "Private conversation: " + ChatColor.GREEN + playerApiService.getImmersiveChatPlayer(p.getConversation()).getName());
				} else {
					sender.sendMessage(ChatColor.GOLD + "Private conversation: " + ChatColor.RED + "N/A");
				}
				if (configService.isSpy(p)) {
					sender.sendMessage(ChatColor.GOLD + "Spy: " + ChatColor.GREEN + "true");
				} else {
					sender.sendMessage(ChatColor.GOLD + "Spy: " + ChatColor.RED + "false");
				}
				if (configService.isCommandSpy(p)) {
					sender.sendMessage(ChatColor.GOLD + "Command spy: " + ChatColor.GREEN + "true");
				} else {
					sender.sendMessage(ChatColor.GOLD + "Command spy: " + ChatColor.RED + "false");
				}
				if (configService.isRangedSpy(p)) {
					sender.sendMessage(ChatColor.GOLD + "Ranged spy: " + ChatColor.GREEN + "true");
				} else {
					sender.sendMessage(ChatColor.GOLD + "Ranged spy: " + ChatColor.RED + "false");
				}
				if (p.isFilterEnabled()) {
					sender.sendMessage(ChatColor.GOLD + "Filter: " + ChatColor.GREEN + "true");
				} else {
					sender.sendMessage(ChatColor.GOLD + "Filter: " + ChatColor.RED + "false");
				}
				return;
			} else {
				sender.sendMessage(ChatColor.RED + "You do not have permission to check the chat info of others.");
			}
			return;
		} else {
			sender.sendMessage(ChatColor.RED + "You do not have permission for this command.");
			return;
		}
	}
}
