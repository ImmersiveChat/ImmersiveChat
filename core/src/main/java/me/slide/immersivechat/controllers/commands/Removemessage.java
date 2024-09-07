package me.slide.immersivechat.controllers.commands;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.google.inject.Inject;
import me.slide.immersivechat.controllers.PluginMessageController;
import me.slide.immersivechat.initiators.application.ImmersiveChat;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.IChatMessage;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import me.slide.immersivechat.model.UniversalCommand;
import me.slide.immersivechat.service.ConfigService;
import me.slide.immersivechat.service.FormatService;
import me.slide.immersivechat.service.PlayerApiService;
import me.slide.immersivechat.utilities.FormatUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Removemessage extends UniversalCommand {
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

	private PacketContainer emptyLinePacketContainer;
	private WrappedChatComponent messageDeletedComponentPlayer;

	@Inject
	public Removemessage(String name) {
		super(name);
	}

	@Inject
	public void postConstruct() {
		emptyLinePacketContainer = formatService.createPacketPlayOutChat("{\"extra\":[\" \"],\"text\":\"\"}");
		messageDeletedComponentPlayer = WrappedChatComponent.fromJson("{\"text\":\"\",\"extra\":[{\"text\":\"\",\"extra\":["
				+ formatService.convertToJsonColors(FormatUtils.FormatStringAll(plugin.getConfig().getString("messageremovertext")))
				+ "],\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"\",\"extra\":["
				+ formatService.convertToJsonColors(FormatUtils.FormatStringAll(plugin.getConfig().getString("messageremoverpermissions"))) + "]}}}]}");
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void executeCommand(CommandSender sender, String command, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(LocalizedMessage.COMMAND_INVALID_ARGUMENTS.toString().replace("{command}", "/removemessage").replace("{args}", "[hashcode] {channel}"));
			return;
		}
		final int hash;
		try {
			hash = Integer.parseInt(args[0]);
		} catch (Exception e) {
			sender.sendMessage(LocalizedMessage.INVALID_HASH.toString());
			return;
		}
		if (args.length > 1 && configService.isChannel(args[1]) && configService.getChannel(args[1]).isBungeeEnabled()) {
			ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(byteOutStream);
			try {
				out.writeUTF("RemoveMessage");
				out.writeUTF(String.valueOf(hash));
				pluginMessageController.sendPluginMessage(byteOutStream);
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		} else {
			new BukkitRunnable() {
				public void run() {
					final Map<Player, List<PacketContainer>> packets = new HashMap();
					for (IImmersiveChatPlayer p : playerApiService.getOnlineMineverseChatPlayers()) {
						List<IChatMessage> messages = p.getMessages();
						List<PacketContainer> playerPackets = new ArrayList();
						boolean resend = false;
						for (int fill = 0; fill < 100 - messages.size(); fill++) {
							playerPackets.add(Removemessage.this.emptyLinePacketContainer);
						}
						for (IChatMessage message : messages) {
							if (message.getHash() == hash) {
								WrappedChatComponent removedComponent = p.getPlayer().hasPermission("venturechat.message.bypass")
										? Removemessage.this.getMessageDeletedChatComponentAdmin(message)
										: Removemessage.this.getMessageDeletedChatComponentPlayer();
								message.setComponent(removedComponent);
								message.setHash(-1);
								playerPackets.add(formatService.createPacketPlayOutChat(removedComponent));
								resend = true;
								continue;
							}
							if (message.getMessage().contains(ChatColor.stripColor(FormatUtils.FormatStringAll(plugin.getConfig().getString("guiicon"))))) {
								String submessage = message.getMessage().substring(0,
										message.getMessage().length() - ChatColor.stripColor(FormatUtils.FormatStringAll(plugin.getConfig().getString("guiicon"))).length());
								if (submessage.hashCode() == hash) {
									WrappedChatComponent removedComponent = p.getPlayer().hasPermission("venturechat.message.bypass")
											? Removemessage.this.getMessageDeletedChatComponentAdmin(message)
											: Removemessage.this.getMessageDeletedChatComponentPlayer();
									message.setComponent(removedComponent);
									message.setHash(-1);
									playerPackets.add(formatService.createPacketPlayOutChat(removedComponent));
									resend = true;
									continue;
								}
							}
							playerPackets.add(formatService.createPacketPlayOutChat(message.getComponent()));

						}
						if (resend) {
							packets.put(p.getPlayer(), playerPackets);
						}
					}
					new BukkitRunnable() {
						public void run() {
							for (Player p : packets.keySet()) {
								List<PacketContainer> pPackets = packets.get(p);
								for (PacketContainer c : pPackets) {
									formatService.sendPacketPlayOutChat(p, c);
								}
							}
						}
					}.runTask(plugin);
				}
			}.runTaskAsynchronously(plugin);
		}
	}

	public WrappedChatComponent getMessageDeletedChatComponentPlayer() {
		return this.messageDeletedComponentPlayer;
	}

	public WrappedChatComponent getMessageDeletedChatComponentAdmin(IChatMessage message) {
		return WrappedChatComponent.fromJson("[{\"text\":\"\",\"extra\":[{\"text\":\"\",\"extra\":["
				+ formatService.convertToJsonColors(FormatUtils.FormatStringAll(plugin.getConfig().getString("messageremovertext")))
				+ "],\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"\"," + message.getColoredMessage() + "}}}]}]");
	}
}
