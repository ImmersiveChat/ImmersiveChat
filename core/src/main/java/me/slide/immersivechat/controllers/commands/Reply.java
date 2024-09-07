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
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class Reply extends PlayerCommand {
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
	public Reply(String name) {
		super(name);
	}

	@Override
	public void executeCommand(Player sender, String command, String[] args) {
		if (!(sender instanceof Player)) {
			plugin.getServer().getConsoleSender().sendMessage(LocalizedMessage.COMMAND_MUST_BE_RUN_BY_PLAYER.toString());
			return;
		}
		IImmersiveChatPlayer mcp = playerApiService.getOnlineImmersiveChatPlayer(sender);
		if (args.length > 0) {
			if (mcp.getReplyPlayer() != null) {
				if (plugin.getConfig().getBoolean("bungeecordmessaging", true)) {
					sendBungeeCordReply(mcp, args);
					return;
				}

				IImmersiveChatPlayer player = playerApiService.getOnlineImmersiveChatPlayer(mcp.getReplyPlayer());
				if (player == null) {
					mcp.getPlayer().sendMessage(LocalizedMessage.NO_PLAYER_TO_REPLY_TO.toString());
					return;
				}
				if (!mcp.getPlayer().canSee(player.getPlayer())) {
					mcp.getPlayer().sendMessage(LocalizedMessage.NO_PLAYER_TO_REPLY_TO.toString());
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
				String msg = "";
				String echo = "";
				String send = "";
				String spy = "";
				if (args.length > 0) {
					for (int r = 0; r < args.length; r++)
						msg += " " + args[r];
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
							.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), plugin.getConfig().getString("replyformatfrom").replaceAll("sender_", "")));
					echo = FormatUtils
							.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), plugin.getConfig().getString("replyformatto").replaceAll("sender_", "")));
					spy = FormatUtils
							.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), plugin.getConfig().getString("replyformatspy").replaceAll("sender_", "")));

					send = FormatUtils.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(player.getPlayer(), send.replaceAll("receiver_", ""))) + msg;
					echo = FormatUtils.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(player.getPlayer(), echo.replaceAll("receiver_", ""))) + msg;
					spy = FormatUtils.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(player.getPlayer(), spy.replaceAll("receiver_", ""))) + msg;

					if (!mcp.getPlayer().hasPermission("venturechat.spy.override")) {
						for (IImmersiveChatPlayer p : playerApiService.getOnlineMineverseChatPlayers()) {
							if (p.getName().equals(mcp.getName()) || p.getName().equals(player.getName())) {
								continue;
							}
							if (configService.isSpy(p)) {
								p.getPlayer().sendMessage(spy);
							}
						}
					}
					player.getPlayer().sendMessage(send);
					mcp.getPlayer().sendMessage(echo);
					if (player.isNotifications()) {
						formatService.playMessageSound(player);
					}
					player.setReplyPlayer(mcp.getUuid());
					return;
				}
			}
			mcp.getPlayer().sendMessage(LocalizedMessage.NO_PLAYER_TO_REPLY_TO.toString());
			return;
		}
		mcp.getPlayer().sendMessage(LocalizedMessage.COMMAND_INVALID_ARGUMENTS.toString().replace("{command}", "/reply").replace("{args}", "[message]"));
	}

	private void sendBungeeCordReply(IImmersiveChatPlayer mcp, String[] args) {
		ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteOutStream);
		StringBuilder msgBuilder = new StringBuilder();
		for (int r = 0; r < args.length; r++) {
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

		String send = FormatUtils
				.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), plugin.getConfig().getString("replyformatfrom").replaceAll("sender_", "")));
		String echo = FormatUtils.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), plugin.getConfig().getString("replyformatto").replaceAll("sender_", "")));
		String spy = "VentureChat:NoSpy";
		if (!mcp.getPlayer().hasPermission("venturechat.spy.override")) {
			spy = FormatUtils.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), plugin.getConfig().getString("replyformatspy").replaceAll("sender_", "")));
		}
		try {
			out.writeUTF("Message");
			out.writeUTF("Send");
			out.writeUTF(playerApiService.getImmersiveChatPlayer(mcp.getReplyPlayer()).getName());
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
