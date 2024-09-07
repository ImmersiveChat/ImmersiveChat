package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import me.clip.placeholderapi.PlaceholderAPI;
import me.slide.immersivechat.controllers.PluginMessageController;
import me.slide.immersivechat.initiators.application.ImmersiveChat;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import me.slide.immersivechat.model.PlayerCommand;
import me.slide.immersivechat.service.ConfigService;
import me.slide.immersivechat.service.FormatService;
import me.slide.immersivechat.service.PlayerApiService;
import me.slide.immersivechat.utilities.FormatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Message extends PlayerCommand {
	@Inject
	private ImmersiveChat plugin;
	@Inject
	private FormatService formatService;
	@Inject
	private PluginMessageController pluginMessageController;
	@Inject
	private PlayerApiService playerApiService;
	@Inject
	private ConfigService configService;

	@Inject
	public Message(String name) {
		super(name);
	}

	@Override
	public void executeCommand(Player sender, String command, String[] args) {
		if (!(sender instanceof Player)) {
			plugin.getServer().getConsoleSender().sendMessage(LocalizedMessage.COMMAND_MUST_BE_RUN_BY_PLAYER.toString());
			return;
		}

		IImmersiveChatPlayer mcp = playerApiService.getOnlineImmersiveChatPlayer((Player) sender);
		if (args.length == 0) {
			mcp.getPlayer().sendMessage(LocalizedMessage.COMMAND_INVALID_ARGUMENTS.toString().replace("{command}", "/" + command).replace("{args}", "[player] [message]"));
			return;
		}

		if (plugin.getConfig().getBoolean("bungeecordmessaging", true)) {
			sendBungeeCordMessage(mcp, command, args);
			return;
		}

		IImmersiveChatPlayer player = playerApiService.getOnlineImmersiveChatPlayer(args[0]);
		if (player == null) {
			mcp.getPlayer().sendMessage(LocalizedMessage.PLAYER_OFFLINE.toString().replace("{args}", args[0]));
			return;
		}
		if (!mcp.getPlayer().canSee(player.getPlayer())) {
			mcp.getPlayer().sendMessage(LocalizedMessage.PLAYER_OFFLINE.toString().replace("{args}", args[0]));
			return;
		}
		if (player.getIgnores().contains(mcp.getUuid())) {
			mcp.getPlayer().sendMessage(LocalizedMessage.IGNORING_MESSAGE.toString().replace("{player}", player.getName()));
			return;
		}
		if (!player.isMessageToggle()) {
			mcp.getPlayer().sendMessage(LocalizedMessage.BLOCKING_MESSAGE.toString().replace("{player}", player.getName()));
			return;
		}

		if (args.length >= 2) {
			String msg = "";
			String echo = "";
			String send = "";
			String spy = "";
			if (args[1].length() > 0) {
				for (int r = 1; r < args.length; r++) {
					msg += " " + args[r];
				}
				if (mcp.isFilterEnabled()) {
					msg = formatService.filterChat(msg);
				}
				if (mcp.getPlayer().hasPermission("venturechat.color.legacy")) {
					msg = FormatUtils.FormatStringLegacyColor(msg);
				}
				if (mcp.getPlayer().hasPermission("venturechat.color")) {
					msg = FormatUtils.FormatStringColor(msg);
				}
				if (mcp.getPlayer().hasPermission("venturechat.format")) {
					msg = FormatUtils.FormatString(msg);
				}

				send = FormatUtils
						.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), plugin.getConfig().getString("tellformatfrom").replaceAll("sender_", "")));
				echo = FormatUtils.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), plugin.getConfig().getString("tellformatto").replaceAll("sender_", "")));
				spy = FormatUtils.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), plugin.getConfig().getString("tellformatspy").replaceAll("sender_", "")));

				send = FormatUtils.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(player.getPlayer(), send.replaceAll("receiver_", ""))) + msg;
				echo = FormatUtils.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(player.getPlayer(), echo.replaceAll("receiver_", ""))) + msg;
				spy = FormatUtils.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(player.getPlayer(), spy.replaceAll("receiver_", ""))) + msg;

				player.setReplyPlayer(mcp.getUuid());
				mcp.setReplyPlayer(player.getUuid());
				player.getPlayer().sendMessage(send);
				mcp.getPlayer().sendMessage(echo);
				if (player.isNotifications()) {
					formatService.playMessageSound(player);
				}
				if (!mcp.getPlayer().hasPermission("venturechat.spy.override")) {
					for (IImmersiveChatPlayer sp : playerApiService.getOnlineMineverseChatPlayers()) {
						if (sp.getName().equals(mcp.getName()) || sp.getName().equals(player.getName())) {
							continue;
						}
						if (configService.isCommandSpy(sp)) {
							sp.getPlayer().sendMessage(spy);
						}
					}
				}
			}
		}
		if (args.length == 1) {
			if (args[0].length() > 0) {
				if (mcp.getConversation() == null || (!mcp.getConversation().toString().equals(player.getUuid().toString()))) {
					mcp.setConversation(player.getUuid());
					if (!mcp.getPlayer().hasPermission("venturechat.spy.override")) {
						for (IImmersiveChatPlayer sp : playerApiService.getOnlineMineverseChatPlayers()) {
							if (sp.getName().equals(mcp.getName())) {
								continue;
							}
							if (configService.isCommandSpy(sp)) {
								sp.getPlayer().sendMessage(LocalizedMessage.ENTER_PRIVATE_CONVERSATION_SPY.toString().replace("{player_sender}", mcp.getName())
										.replace("{player_receiver}", player.getName()));
							}
						}
					}
					mcp.getPlayer().sendMessage(LocalizedMessage.ENTER_PRIVATE_CONVERSATION.toString().replace("{player_receiver}", player.getName()));
				} else {
					mcp.setConversation(null);
					if (!mcp.getPlayer().hasPermission("venturechat.spy.override")) {
						for (IImmersiveChatPlayer sp : playerApiService.getOnlineMineverseChatPlayers()) {
							if (sp.getName().equals(mcp.getName())) {
								continue;
							}
							if (configService.isCommandSpy(sp)) {
								sp.getPlayer().sendMessage(LocalizedMessage.EXIT_PRIVATE_CONVERSATION_SPY.toString().replace("{player_sender}", mcp.getName())
										.replace("{player_receiver}", player.getName()));
							}
						}
					}
					mcp.getPlayer().sendMessage(LocalizedMessage.EXIT_PRIVATE_CONVERSATION.toString().replace("{player_receiver}", player.getName()));
				}
			}
		}
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
		if (plugin.getConfig().getBoolean("bungeecordmessaging", true)) {
			List<String> completions = new ArrayList<>();
			StringUtil.copyPartialMatches(args[args.length - 1], playerApiService.getNetworkPlayerNames(), completions);
			Collections.sort(completions);
			return completions;
		}
		return super.tabComplete(sender, alias, args);
	}

	private void sendBungeeCordMessage(IImmersiveChatPlayer mcp, String command, String[] args) {
		if (args.length < 2) {
			mcp.getPlayer().sendMessage(LocalizedMessage.COMMAND_INVALID_ARGUMENTS.toString().replace("{command}", "/" + command).replace("{args}", "[player] [message]"));
			return;
		}
		ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteOutStream);
		StringBuilder msgBuilder = new StringBuilder();
		for (int r = 1; r < args.length; r++) {
			msgBuilder.append(" " + args[r]);
		}
		String msg = msgBuilder.toString();
		if (mcp.isFilterEnabled()) {
			msg = formatService.filterChat(msg);
		}
		if (mcp.getPlayer().hasPermission("venturechat.color.legacy")) {
			msg = FormatUtils.FormatStringLegacyColor(msg);
		}
		if (mcp.getPlayer().hasPermission("venturechat.color")) {
			msg = FormatUtils.FormatStringColor(msg);
		}
		if (mcp.getPlayer().hasPermission("venturechat.format")) {
			msg = FormatUtils.FormatString(msg);
		}

		String send = FormatUtils.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), plugin.getConfig().getString("tellformatfrom").replaceAll("sender_", "")));
		String echo = FormatUtils.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), plugin.getConfig().getString("tellformatto").replaceAll("sender_", "")));
		String spy = "VentureChat:NoSpy";
		if (!mcp.getPlayer().hasPermission("venturechat.spy.override")) {
			spy = FormatUtils.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), plugin.getConfig().getString("tellformatspy").replaceAll("sender_", "")));
		}
		try {
			out.writeUTF("Message");
			out.writeUTF("Send");
			out.writeUTF(args[0]);
			out.writeUTF(mcp.getUuid().toString());
			out.writeUTF(mcp.getName());
			out.writeUTF(send);
			out.writeUTF(echo);
			out.writeUTF(spy);
			out.writeUTF(msg);
			pluginMessageController.sendPluginMessage(byteOutStream);
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
