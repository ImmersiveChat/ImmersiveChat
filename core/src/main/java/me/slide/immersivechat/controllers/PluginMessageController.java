package me.slide.immersivechat.controllers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.clip.placeholderapi.PlaceholderAPI;
import me.slide.immersivechat.api.events.VentureChatEvent;
import me.slide.immersivechat.initiators.application.ImmersiveChat;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.*;
import me.slide.immersivechat.service.ConfigService;
import me.slide.immersivechat.service.FormatService;
import me.slide.immersivechat.service.ImmersiveChatDatabaseService;
import me.slide.immersivechat.service.PlayerApiService;
import me.slide.immersivechat.utilities.FormatUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class PluginMessageController {
	public static final String PLUGIN_MESSAGING_CHANNEL = "venturechat:data";

	@Inject
	private ImmersiveChat plugin;
	@Inject
	private ImmersiveChatDatabaseService databaseService;
	@Inject
	private FormatService formatService;
	@Inject
	private PlayerApiService playerApiService;
	@Inject
	private ConfigService configService;

	public void sendPluginMessage(ByteArrayOutputStream byteOutStream) {
		if (playerApiService.getOnlineMineverseChatPlayers().size() > 0) {
			playerApiService.getOnlineMineverseChatPlayers().iterator().next().getPlayer().sendPluginMessage(plugin, PLUGIN_MESSAGING_CHANNEL, byteOutStream.toByteArray());
		}
	}

	public void sendDiscordSRVPluginMessage(String chatChannel, String message) {
		if (playerApiService.getOnlineMineverseChatPlayers().size() == 0) {
			return;
		}
		ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteOutStream);
		try {
			out.writeUTF("DiscordSRV");
			out.writeUTF(chatChannel);
			out.writeUTF(message);
			sendPluginMessage(byteOutStream);
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void synchronizeWithDelay(final ImmersiveChatPlayer vcp, final boolean changes) {
		final long delayInTicks = 20L;
		plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, new Runnable() {
			public void run() {
				synchronize(vcp, false);
			}
		}, delayInTicks);
	}

	public void synchronize(IImmersiveChatPlayer mcp, boolean changes) {
		// System.out.println("Sync started...");
		ByteArrayOutputStream outstream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(outstream);
		try {
			out.writeUTF("Sync");
			if (!changes) {
				out.writeUTF("Receive");
				// System.out.println(mcp.getPlayer().getServer().getServerName());
				// out.writeUTF(mcp.getPlayer().getServer().getServerName());
				out.writeUTF(mcp.getUuid().toString());
				plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, new Runnable() {
					@Override
					public void run() {
						if (!mcp.isOnline() || mcp.isHasPlayed()) {
							return;
						}
						synchronize(mcp, false);
					}
				}, 20L); // one second delay before running again
			} else {
				out.writeUTF("Update");
				out.writeUTF(mcp.getUuid().toString());
				// out.writeUTF("Channels");
				int channelCount = 0;
				for (String c : mcp.getListening()) {
					ChatChannel channel = configService.getChannel(c);
					if (channel.isBungeeEnabled()) {
						channelCount++;
					}
				}
				out.write(channelCount);
				for (String c : mcp.getListening()) {
					ChatChannel channel = configService.getChannel(c);
					if (channel.isBungeeEnabled()) {
						out.writeUTF(channel.getName());
					}
				}
				// out.writeUTF("Mutes");
				int muteCount = 0;
				for (IMuteContainer mute : mcp.getMutes().values()) {
					ChatChannel channel = configService.getChannel(mute.getChannel());
					if (channel.isBungeeEnabled()) {
						muteCount++;
					}
				}
				// System.out.println(muteCount + " mutes");
				out.write(muteCount);
				for (IMuteContainer mute : mcp.getMutes().values()) {
					ChatChannel channel = configService.getChannel(mute.getChannel());
					if (channel.isBungeeEnabled()) {
						out.writeUTF(channel.getName());
						out.writeLong(mute.getDuration());
						out.writeUTF(mute.getReason());
					}
				}
				int ignoreCount = 0;
				for (@SuppressWarnings("unused")
				UUID c : mcp.getIgnores()) {
					ignoreCount++;
				}
				out.write(ignoreCount);
				for (UUID c : mcp.getIgnores()) {
					out.writeUTF(c.toString());
				}
				out.writeBoolean(configService.isSpy(mcp));
				out.writeBoolean(mcp.isMessageToggle());
			}
			sendPluginMessage(outstream);
			// System.out.println("Sync start bottom...");
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void processInboundPluginMessage(final String channel, final Player player, final byte[] inputStream) {
		if (!channel.equals(PLUGIN_MESSAGING_CHANNEL)) {
			return;
		}
		try {
			DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(inputStream));
			if (plugin.getConfig().getString("loglevel", "info").equals("debug")) {
				System.out.println(msgin.available() + " size on receiving end");
			}
			String subchannel = msgin.readUTF();
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(stream);
			if (subchannel.equals("Chat")) {
				final String server = msgin.readUTF();
				final String chatChannelName = msgin.readUTF();
				final String senderName = msgin.readUTF();
				final UUID senderUUID = UUID.fromString(msgin.readUTF());
				final int hash = msgin.readInt();
				final String format = msgin.readUTF();
				final String chat = msgin.readUTF();
				final String consoleChat = format + chat;
				final String globalJSON = msgin.readUTF();
				final String primaryGroup = msgin.readUTF();
				final ChatChannel chatChannel = configService.getChannel(chatChannelName);
				if (chatChannel == null || !chatChannel.isBungeeEnabled()) {
					return;
				}
				final Set<Player> recipients = playerApiService.getOnlineMineverseChatPlayers()
					.stream()
					.filter(vcp -> configService.isListening(vcp, chatChannelName))
					.filter(vcp -> vcp.isBungeeToggle() || playerApiService.getOnlineImmersiveChatPlayer(senderName) == null)
					.filter(vcp -> !configService.isIgnoreChatEnabled() || !vcp.getIgnores().contains(senderUUID))
					.map(IImmersiveChatPlayer::getPlayer)
					.collect(Collectors.toSet());
				plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
					@Override
					public void run() {
						final VentureChatEvent ventureChatEvent = new VentureChatEvent(null, senderName, primaryGroup, chatChannel, recipients, recipients.size(), format, chat,
								globalJSON, hash, false);
						// This event cannot be modified or cancelled
						plugin.getServer().getPluginManager().callEvent(ventureChatEvent);
					}
				});
				if (databaseService.isEnabled()) {
					databaseService.writeVentureChat(senderUUID.toString(), senderName, server, chatChannelName, chat.replace("'", "''"), "Chat");
				}
				formatService.createAndSendChatMessage(globalJSON, chatChannelName, hash, recipients, senderName);
				plugin.getServer().getConsoleSender().sendMessage(consoleChat);
			}
			if (subchannel.equals("DiscordSRV")) {
				String chatChannel = msgin.readUTF();
				String message = msgin.readUTF();
				if (!configService.isChannel(chatChannel)) {
					return;
				}
				ChatChannel chatChannelObj = configService.getChannel(chatChannel);
				if (!chatChannelObj.isBungeeEnabled()) {
					return;
				}
				formatService.createAndSendExternalChatMessage(message, chatChannelObj.getName(), "Discord");
			}
			if (subchannel.equals("PlayerNames")) {
				playerApiService.clearNetworkPlayerNames();
				int playerCount = msgin.readInt();
				for (int a = 0; a < playerCount; a++) {
					playerApiService.addNetworkPlayerName(msgin.readUTF());
				}
			}
			if (subchannel.equals("Chwho")) {
				String identifier = msgin.readUTF();
				if (identifier.equals("Get")) {
					String server = msgin.readUTF();
					String sender = msgin.readUTF();
					String chatchannel = msgin.readUTF();
					List<String> listening = new ArrayList<String>();
					if (configService.isChannel(chatchannel)) {
						for (IImmersiveChatPlayer mcp : playerApiService.getOnlineMineverseChatPlayers()) {
							if (configService.isListening(mcp, chatchannel)) {
								String entry = "&f" + mcp.getName();
								if (mcp.getMutes().containsKey(chatchannel)) {
									entry = "&c" + mcp.getName();
								}
								listening.add(entry);
							}
						}
					}
					out.writeUTF("Chwho");
					out.writeUTF("Receive");
					out.writeUTF(server);
					out.writeUTF(sender);
					out.writeUTF(chatchannel);
					out.writeInt(listening.size());
					for (String s : listening) {
						out.writeUTF(s);
					}
					sendPluginMessage(stream);
				}
				if (identifier.equals("Receive")) {
					String sender = msgin.readUTF();
					String stringchannel = msgin.readUTF();
					IImmersiveChatPlayer mcp = playerApiService.getOnlineImmersiveChatPlayer(UUID.fromString(sender));
					ChatChannel chatchannel = configService.getChannel(stringchannel);
					String playerList = "";
					int size = msgin.readInt();
					for (int a = 0; a < size; a++) {
						playerList += msgin.readUTF() + ChatColor.WHITE + ", ";
					}
					if (playerList.length() > 2) {
						playerList = playerList.substring(0, playerList.length() - 2);
					}
					mcp.getPlayer().sendMessage(LocalizedMessage.CHANNEL_PLAYER_LIST_HEADER.toString().replace("{channel_color}", chatchannel.getColor().toString())
							.replace("{channel_name}", chatchannel.getName()));
					mcp.getPlayer().sendMessage(FormatUtils.FormatStringAll(playerList));
				}
			}
			if (subchannel.equals("RemoveMessage")) {
				String hash = msgin.readUTF();
				plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "removemessage " + hash);
			}
			if (subchannel.equals("Sync")) {
				if (plugin.getConfig().getString("loglevel", "info").equals("debug")) {
					plugin.getServer().getConsoleSender().sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - Received update..."));
				}
				String uuid = msgin.readUTF();
				IImmersiveChatPlayer p = playerApiService.getOnlineImmersiveChatPlayer(UUID.fromString(uuid));
				if (p == null || p.isHasPlayed()) {
					return;
				}
				for (Object ch : p.getListening().toArray()) {
					String c = ch.toString();
					ChatChannel cha = configService.getChannel(c);
					if (cha.isBungeeEnabled()) {
						p.getListening().remove(c);
					}
				}
				int size = msgin.read();
				for (int a = 0; a < size; a++) {
					String ch = msgin.readUTF();
					if (configService.isChannel(ch)) {
						ChatChannel cha = configService.getChannel(ch);
						if (!cha.isPermissionRequired() || p.getPlayer().hasPermission(cha.getPermission())) {
							p.getListening().add(ch);
						}
					}
				}
				p.getMutes().values().removeIf(mute -> configService.getChannel(mute.getChannel()).isBungeeEnabled());
				int sizeB = msgin.read();
				// System.out.println(sizeB + " mute size");
				for (int b = 0; b < sizeB; b++) {
					String ch = msgin.readUTF();
					long muteTime = msgin.readLong();
					String muteReason = msgin.readUTF();
					// System.out.println(ch);
					if (configService.isChannel(ch)) {
						p.getMutes().put(ch, new MuteContainer(ch, muteTime, muteReason));
					}
				}
				// System.out.println(msgin.available() + " available before");
				p.setSpy(msgin.readBoolean());
				p.setMessageToggle(msgin.readBoolean());
				// System.out.println(msgin.available() + " available after");
				for (Object o : p.getIgnores().toArray()) {
					p.getIgnores().remove((UUID) o);
				}
				int sizeC = msgin.read();
				// System.out.println(sizeC + " ignore size");
				for (int c = 0; c < sizeC; c++) {
					String i = msgin.readUTF();
					// System.out.println(i);
					p.getIgnores().add(UUID.fromString(i));
				}
				if (!p.isHasPlayed()) {
					boolean isThereABungeeChannel = false;
					for (ChatChannel ch : configService.getAutojoinList()) {
						if ((!ch.isPermissionRequired() || p.getPlayer().hasPermission(ch.getPermission())) && !configService.isListening(p, ch.getName())) {
							p.getListening().add(ch.getName());
							if (ch.isBungeeEnabled()) {
								isThereABungeeChannel = true;
							}
						}
					}
					p.setHasPlayed(true);
					// Only run a sync update if the player joined a BungeeCord channel
					if (isThereABungeeChannel) {
						synchronize(p, true);
					}
				}
			}
			if (subchannel.equals("Ignore")) {
				String identifier = msgin.readUTF();
				if (identifier.equals("Send")) {
					String server = msgin.readUTF();
					String receiver = msgin.readUTF();
					IImmersiveChatPlayer p = playerApiService.getOnlineImmersiveChatPlayer(receiver);
					UUID sender = UUID.fromString(msgin.readUTF());
					if (!plugin.getConfig().getBoolean("bungeecordmessaging", true) || p == null || !p.isOnline()) {
						out.writeUTF("Ignore");
						out.writeUTF("Offline");
						out.writeUTF(server);
						out.writeUTF(receiver);
						out.writeUTF(sender.toString());
						sendPluginMessage(stream);
						return;
					}
					if (p.getPlayer().hasPermission("venturechat.ignore.bypass")) {
						out.writeUTF("Ignore");
						out.writeUTF("Bypass");
						out.writeUTF(server);
						out.writeUTF(receiver);
						out.writeUTF(sender.toString());
						sendPluginMessage(stream);
						return;
					}
					out.writeUTF("Ignore");
					out.writeUTF("Echo");
					out.writeUTF(server);
					out.writeUTF(p.getUuid().toString());
					out.writeUTF(receiver);
					out.writeUTF(sender.toString());
					sendPluginMessage(stream);
					return;
				}
				if (identifier.equals("Offline")) {
					String receiver = msgin.readUTF();
					UUID sender = UUID.fromString(msgin.readUTF());
					IImmersiveChatPlayer p = playerApiService.getOnlineImmersiveChatPlayer(sender);
					p.getPlayer().sendMessage(LocalizedMessage.PLAYER_OFFLINE.toString().replace("{args}", receiver));
				}
				if (identifier.equals("Echo")) {
					UUID receiver = UUID.fromString(msgin.readUTF());
					String receiverName = msgin.readUTF();
					UUID sender = UUID.fromString(msgin.readUTF());
					IImmersiveChatPlayer p = playerApiService.getOnlineImmersiveChatPlayer(sender);

					if (p.getIgnores().contains(receiver)) {
						p.getPlayer().sendMessage(LocalizedMessage.IGNORE_PLAYER_OFF.toString().replace("{player}", receiverName));
						p.getIgnores().remove(receiver);
						synchronize(p, true);
						return;
					}

					p.getIgnores().add(receiver);
					p.getPlayer().sendMessage(LocalizedMessage.IGNORE_PLAYER_ON.toString().replace("{player}", receiverName));
					synchronize(p, true);
				}
				if (identifier.equals("Bypass")) {
					String receiver = msgin.readUTF();
					UUID sender = UUID.fromString(msgin.readUTF());
					IImmersiveChatPlayer p = playerApiService.getOnlineImmersiveChatPlayer(sender);
					p.getPlayer().sendMessage(LocalizedMessage.IGNORE_PLAYER_CANT.toString().replace("{player}", receiver));
				}
			}
			if (subchannel.equals("Mute")) {
				String identifier = msgin.readUTF();
				if (identifier.equals("Send")) {
					String server = msgin.readUTF();
					String senderIdentifier = msgin.readUTF();
					String temporaryDataInstanceUUIDString = msgin.readUTF();
					String playerToMute = msgin.readUTF();
					String channelName = msgin.readUTF();
					long time = msgin.readLong();
					String reason = msgin.readUTF();
					IImmersiveChatPlayer playerToMuteMCP = playerApiService.getOnlineImmersiveChatPlayer(playerToMute);
					if (playerToMuteMCP == null) {
						out.writeUTF("Mute");
						out.writeUTF("Offline");
						out.writeUTF(server);
						out.writeUTF(temporaryDataInstanceUUIDString);
						out.writeUTF(senderIdentifier);
						out.writeUTF(playerToMute);
						sendPluginMessage(stream);
						return;
					}
					if (!configService.isChannel(channelName)) {
						return;
					}
					ChatChannel chatChannelObj = configService.getChannel(channelName);
					if (playerToMuteMCP.getMutes().containsKey(chatChannelObj.getName())) {
						out.writeUTF("Mute");
						out.writeUTF("AlreadyMuted");
						out.writeUTF(server);
						out.writeUTF(senderIdentifier);
						out.writeUTF(playerToMute);
						out.writeUTF(channelName);
						sendPluginMessage(stream);
						return;
					}
					if (time > 0) {
						long datetime = System.currentTimeMillis();
						if (reason.isEmpty()) {
							playerToMuteMCP.getMutes().put(chatChannelObj.getName(), new MuteContainer(chatChannelObj.getName(), datetime + time, ""));
							String timeString = FormatUtils.parseTimeStringFromMillis(time);
							playerToMuteMCP.getPlayer().sendMessage(LocalizedMessage.MUTE_PLAYER_PLAYER_TIME.toString().replace("{channel_color}", chatChannelObj.getColor())
									.replace("{channel_name}", chatChannelObj.getName()).replace("{time}", timeString));
						} else {
							playerToMuteMCP.getMutes().put(chatChannelObj.getName(), new MuteContainer(chatChannelObj.getName(), datetime + time, reason));
							String timeString = FormatUtils.parseTimeStringFromMillis(time);
							playerToMuteMCP.getPlayer().sendMessage(LocalizedMessage.MUTE_PLAYER_PLAYER_TIME_REASON.toString().replace("{channel_color}", chatChannelObj.getColor())
									.replace("{channel_name}", chatChannelObj.getName()).replace("{time}", timeString).replace("{reason}", reason));
						}
					} else {
						if (reason.isEmpty()) {
							playerToMuteMCP.getMutes().put(chatChannelObj.getName(), new MuteContainer(chatChannelObj.getName(), 0, ""));
							playerToMuteMCP.getPlayer().sendMessage(LocalizedMessage.MUTE_PLAYER_PLAYER.toString().replace("{channel_color}", chatChannelObj.getColor())
									.replace("{channel_name}", chatChannelObj.getName()));
						} else {
							playerToMuteMCP.getMutes().put(chatChannelObj.getName(), new MuteContainer(chatChannelObj.getName(), 0, reason));
							playerToMuteMCP.getPlayer().sendMessage(LocalizedMessage.MUTE_PLAYER_PLAYER_REASON.toString().replace("{channel_color}", chatChannelObj.getColor())
									.replace("{channel_name}", chatChannelObj.getName()).replace("{reason}", reason));
						}
					}
					synchronize(playerToMuteMCP, true);
					out.writeUTF("Mute");
					out.writeUTF("Valid");
					out.writeUTF(server);
					out.writeUTF(senderIdentifier);
					out.writeUTF(playerToMute);
					out.writeUTF(channelName);
					out.writeLong(time);
					out.writeUTF(reason);
					sendPluginMessage(stream);
					return;
				}
				if (identifier.equals("Valid")) {
					String senderIdentifier = msgin.readUTF();
					String playerToMute = msgin.readUTF();
					String channelName = msgin.readUTF();
					long time = msgin.readLong();
					String reason = msgin.readUTF();
					if (!configService.isChannel(channelName)) {
						return;
					}
					ChatChannel chatChannelObj = configService.getChannel(channelName);
					if (time > 0) {
						String timeString = FormatUtils.parseTimeStringFromMillis(time);
						if (reason.isEmpty()) {
							if (senderIdentifier.equals("VentureChat:Console")) {
								plugin.getServer().getConsoleSender().sendMessage(LocalizedMessage.MUTE_PLAYER_SENDER_TIME.toString().replace("{player}", playerToMute)
										.replace("{channel_color}", chatChannelObj.getColor()).replace("{channel_name}", chatChannelObj.getName()).replace("{time}", timeString));
							} else {
								UUID sender = UUID.fromString(senderIdentifier);
								IImmersiveChatPlayer senderMCP = playerApiService.getOnlineImmersiveChatPlayer(sender);
								senderMCP.getPlayer().sendMessage(LocalizedMessage.MUTE_PLAYER_SENDER_TIME.toString().replace("{player}", playerToMute)
										.replace("{channel_color}", chatChannelObj.getColor()).replace("{channel_name}", chatChannelObj.getName()).replace("{time}", timeString));
							}
						} else {
							if (senderIdentifier.equals("VentureChat:Console")) {
								plugin.getServer().getConsoleSender()
										.sendMessage(LocalizedMessage.MUTE_PLAYER_SENDER_TIME_REASON.toString().replace("{player}", playerToMute)
												.replace("{channel_color}", chatChannelObj.getColor()).replace("{channel_name}", chatChannelObj.getName())
												.replace("{time}", timeString).replace("{reason}", reason));
							} else {
								UUID sender = UUID.fromString(senderIdentifier);
								IImmersiveChatPlayer senderMCP = playerApiService.getOnlineImmersiveChatPlayer(sender);
								senderMCP.getPlayer()
										.sendMessage(LocalizedMessage.MUTE_PLAYER_SENDER_TIME_REASON.toString().replace("{player}", playerToMute)
												.replace("{channel_color}", chatChannelObj.getColor()).replace("{channel_name}", chatChannelObj.getName())
												.replace("{time}", timeString).replace("{reason}", reason));
							}
						}
					} else {
						if (reason.isEmpty()) {
							if (senderIdentifier.equals("VentureChat:Console")) {
								plugin.getServer().getConsoleSender().sendMessage(LocalizedMessage.MUTE_PLAYER_SENDER.toString().replace("{player}", playerToMute)
										.replace("{channel_color}", chatChannelObj.getColor()).replace("{channel_name}", chatChannelObj.getName()));
							} else {
								UUID sender = UUID.fromString(senderIdentifier);
								IImmersiveChatPlayer senderMCP = playerApiService.getOnlineImmersiveChatPlayer(sender);
								senderMCP.getPlayer().sendMessage(LocalizedMessage.MUTE_PLAYER_SENDER.toString().replace("{player}", playerToMute)
										.replace("{channel_color}", chatChannelObj.getColor()).replace("{channel_name}", chatChannelObj.getName()));
							}
						} else {
							if (senderIdentifier.equals("VentureChat:Console")) {
								plugin.getServer().getConsoleSender().sendMessage(LocalizedMessage.MUTE_PLAYER_SENDER_REASON.toString().replace("{player}", playerToMute)
										.replace("{channel_color}", chatChannelObj.getColor()).replace("{channel_name}", chatChannelObj.getName()).replace("{reason}", reason));
							} else {
								UUID sender = UUID.fromString(senderIdentifier);
								IImmersiveChatPlayer senderMCP = playerApiService.getOnlineImmersiveChatPlayer(sender);
								senderMCP.getPlayer().sendMessage(LocalizedMessage.MUTE_PLAYER_SENDER_REASON.toString().replace("{player}", playerToMute)
										.replace("{channel_color}", chatChannelObj.getColor()).replace("{channel_name}", chatChannelObj.getName()).replace("{reason}", reason));
							}
						}
					}
					return;
				}
				if (identifier.equals("Offline")) {
					String senderIdentifier = msgin.readUTF();
					String playerToMute = msgin.readUTF();
					if (senderIdentifier.equals("VentureChat:Console")) {
						plugin.getServer().getConsoleSender().sendMessage(LocalizedMessage.PLAYER_OFFLINE.toString().replace("{args}", playerToMute));
						return;
					}
					UUID sender = UUID.fromString(senderIdentifier);
					IImmersiveChatPlayer senderMCP = playerApiService.getOnlineImmersiveChatPlayer(sender);
					senderMCP.getPlayer().sendMessage(LocalizedMessage.PLAYER_OFFLINE.toString().replace("{args}", playerToMute));
					return;
				}
				if (identifier.equals("AlreadyMuted")) {
					String senderIdentifier = msgin.readUTF();
					String playerToMute = msgin.readUTF();
					String channelName = msgin.readUTF();
					if (!configService.isChannel(channelName)) {
						return;
					}
					ChatChannel chatChannelObj = configService.getChannel(channelName);
					if (senderIdentifier.equals("VentureChat:Console")) {
						plugin.getServer().getConsoleSender().sendMessage(LocalizedMessage.PLAYER_ALREADY_MUTED.toString().replace("{player}", playerToMute)
								.replace("{channel_color}", chatChannelObj.getColor()).replace("{channel_name}", chatChannelObj.getName()));
						return;
					}
					UUID sender = UUID.fromString(senderIdentifier);
					IImmersiveChatPlayer senderMCP = playerApiService.getOnlineImmersiveChatPlayer(sender);
					senderMCP.getPlayer().sendMessage(LocalizedMessage.PLAYER_ALREADY_MUTED.toString().replace("{player}", playerToMute)
							.replace("{channel_color}", chatChannelObj.getColor()).replace("{channel_name}", chatChannelObj.getName()));
					return;
				}
			}
			if (subchannel.equals("Unmute")) {
				String identifier = msgin.readUTF();
				if (identifier.equals("Send")) {
					String server = msgin.readUTF();
					String senderIdentifier = msgin.readUTF();
					String temporaryDataInstanceUUIDString = msgin.readUTF();
					String playerToUnmute = msgin.readUTF();
					String channelName = msgin.readUTF();
					IImmersiveChatPlayer playerToUnmuteMCP = playerApiService.getOnlineImmersiveChatPlayer(playerToUnmute);
					if (playerToUnmuteMCP == null) {
						out.writeUTF("Unmute");
						out.writeUTF("Offline");
						out.writeUTF(server);
						out.writeUTF(temporaryDataInstanceUUIDString);
						out.writeUTF(senderIdentifier);
						out.writeUTF(playerToUnmute);
						sendPluginMessage(stream);
						return;
					}
					if (!configService.isChannel(channelName)) {
						return;
					}
					ChatChannel chatChannelObj = configService.getChannel(channelName);
					if (!playerToUnmuteMCP.getMutes().containsKey(chatChannelObj.getName())) {
						out.writeUTF("Unmute");
						out.writeUTF("NotMuted");
						out.writeUTF(server);
						out.writeUTF(senderIdentifier);
						out.writeUTF(playerToUnmute);
						out.writeUTF(channelName);
						sendPluginMessage(stream);
						return;
					}
					playerToUnmuteMCP.getMutes().remove(chatChannelObj.getName());
					playerToUnmuteMCP.getPlayer().sendMessage(LocalizedMessage.UNMUTE_PLAYER_PLAYER.toString().replace("{player}", player.getName())
							.replace("{channel_color}", chatChannelObj.getColor()).replace("{channel_name}", chatChannelObj.getName()));
					synchronize(playerToUnmuteMCP, true);
					out.writeUTF("Unmute");
					out.writeUTF("Valid");
					out.writeUTF(server);
					out.writeUTF(senderIdentifier);
					out.writeUTF(playerToUnmute);
					out.writeUTF(channelName);
					sendPluginMessage(stream);
					return;
				}
				if (identifier.equals("Valid")) {
					String senderIdentifier = msgin.readUTF();
					String playerToUnmute = msgin.readUTF();
					String channelName = msgin.readUTF();
					if (!configService.isChannel(channelName)) {
						return;
					}
					ChatChannel chatChannelObj = configService.getChannel(channelName);
					if (senderIdentifier.equals("VentureChat:Console")) {
						plugin.getServer().getConsoleSender().sendMessage(LocalizedMessage.UNMUTE_PLAYER_SENDER.toString().replace("{player}", playerToUnmute)
								.replace("{channel_color}", chatChannelObj.getColor()).replace("{channel_name}", chatChannelObj.getName()));
					} else {
						UUID sender = UUID.fromString(senderIdentifier);
						IImmersiveChatPlayer senderMCP = playerApiService.getOnlineImmersiveChatPlayer(sender);
						senderMCP.getPlayer().sendMessage(LocalizedMessage.UNMUTE_PLAYER_SENDER.toString().replace("{player}", playerToUnmute)
								.replace("{channel_color}", chatChannelObj.getColor()).replace("{channel_name}", chatChannelObj.getName()));
					}
					return;
				}
				if (identifier.equals("Offline")) {
					String senderIdentifier = msgin.readUTF();
					String playerToUnmute = msgin.readUTF();
					if (senderIdentifier.equals("VentureChat:Console")) {
						plugin.getServer().getConsoleSender().sendMessage(LocalizedMessage.PLAYER_OFFLINE.toString().replace("{args}", playerToUnmute));
						return;
					}
					UUID sender = UUID.fromString(senderIdentifier);
					IImmersiveChatPlayer senderMCP = playerApiService.getOnlineImmersiveChatPlayer(sender);
					senderMCP.getPlayer().sendMessage(LocalizedMessage.PLAYER_OFFLINE.toString().replace("{args}", playerToUnmute));
					return;
				}
				if (identifier.equals("NotMuted")) {
					String senderIdentifier = msgin.readUTF();
					String playerToUnmute = msgin.readUTF();
					String channelName = msgin.readUTF();
					if (!configService.isChannel(channelName)) {
						return;
					}
					ChatChannel chatChannelObj = configService.getChannel(channelName);
					if (senderIdentifier.equals("VentureChat:Console")) {
						plugin.getServer().getConsoleSender().sendMessage(LocalizedMessage.PLAYER_NOT_MUTED.toString().replace("{player}", playerToUnmute)
								.replace("{channel_color}", chatChannelObj.getColor()).replace("{channel_name}", chatChannelObj.getName()));
						return;
					}
					UUID sender = UUID.fromString(senderIdentifier);
					IImmersiveChatPlayer senderMCP = playerApiService.getOnlineImmersiveChatPlayer(sender);
					senderMCP.getPlayer().sendMessage(LocalizedMessage.PLAYER_NOT_MUTED.toString().replace("{player}", playerToUnmute)
							.replace("{channel_color}", chatChannelObj.getColor()).replace("{channel_name}", chatChannelObj.getName()));
					return;
				}
			}
			if (subchannel.equals("Message")) {
				String identifier = msgin.readUTF();
				if (identifier.equals("Send")) {
					String server = msgin.readUTF();
					String receiver = msgin.readUTF();
					IImmersiveChatPlayer p = playerApiService.getOnlineImmersiveChatPlayer(receiver);
					UUID sender = UUID.fromString(msgin.readUTF());
					String sName = msgin.readUTF();
					String send = msgin.readUTF();
					String echo = msgin.readUTF();
					String spy = msgin.readUTF();
					String msg = msgin.readUTF();
					if (!plugin.getConfig().getBoolean("bungeecordmessaging", true) || p == null) {
						out.writeUTF("Message");
						out.writeUTF("Offline");
						out.writeUTF(server);
						out.writeUTF(receiver);
						out.writeUTF(sender.toString());
						sendPluginMessage(stream);
						return;
					}
					if (p.getIgnores().contains(sender)) {
						out.writeUTF("Message");
						out.writeUTF("Ignore");
						out.writeUTF(server);
						out.writeUTF(receiver);
						out.writeUTF(sender.toString());
						sendPluginMessage(stream);
						return;
					}
					if (!p.isMessageToggle()) {
						out.writeUTF("Message");
						out.writeUTF("Blocked");
						out.writeUTF(server);
						out.writeUTF(receiver);
						out.writeUTF(sender.toString());
						sendPluginMessage(stream);
						return;
					}
					p.getPlayer().sendMessage(FormatUtils.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(p.getPlayer(), send.replaceAll("receiver_", ""))) + msg);
					if (p.isNotifications()) {
						formatService.playMessageSound(p);
					}
					if (playerApiService.getImmersiveChatPlayer(sender) == null) {
//						VentureChatPlayer senderMCP = new VentureChatPlayer(sender, sName, configService.getDefaultChannel());
						ImmersiveChatPlayer senderMCP = ImmersiveChatPlayer.builder().uuid(sender).name(sName).currentChannel(configService.getDefaultChannel()).build();
						senderMCP.getListening().add(configService.getDefaultChannel().getName());
						playerApiService.addMineverseChatPlayerToMap(senderMCP);
						playerApiService.addNameToMap(senderMCP);
					}
					p.setReplyPlayer(sender);
					out.writeUTF("Message");
					out.writeUTF("Echo");
					out.writeUTF(server);
					out.writeUTF(receiver);
					out.writeUTF(p.getUuid().toString());
					out.writeUTF(sender.toString());
					out.writeUTF(sName);
					out.writeUTF(FormatUtils.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(p.getPlayer(), echo.replaceAll("receiver_", ""))) + msg);
					out.writeUTF(FormatUtils.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(p.getPlayer(), spy.replaceAll("receiver_", ""))) + msg);
					sendPluginMessage(stream);
					return;
				}
				if (identifier.equals("Offline")) {
					String receiver = msgin.readUTF();
					UUID sender = UUID.fromString(msgin.readUTF());
					IImmersiveChatPlayer p = playerApiService.getOnlineImmersiveChatPlayer(sender);
					p.getPlayer().sendMessage(LocalizedMessage.PLAYER_OFFLINE.toString().replace("{args}", receiver));
					p.setReplyPlayer(null);
				}
				if (identifier.equals("Ignore")) {
					String receiver = msgin.readUTF();
					UUID sender = UUID.fromString(msgin.readUTF());
					IImmersiveChatPlayer p = playerApiService.getOnlineImmersiveChatPlayer(sender);
					p.getPlayer().sendMessage(LocalizedMessage.IGNORING_MESSAGE.toString().replace("{player}", receiver));
				}
				if (identifier.equals("Blocked")) {
					String receiver = msgin.readUTF();
					UUID sender = UUID.fromString(msgin.readUTF());
					IImmersiveChatPlayer p = playerApiService.getOnlineImmersiveChatPlayer(sender);
					p.getPlayer().sendMessage(LocalizedMessage.BLOCKING_MESSAGE.toString().replace("{player}", receiver));
				}
				if (identifier.equals("Echo")) {
					String receiverName = msgin.readUTF();
					UUID receiverUUID = UUID.fromString(msgin.readUTF());
					UUID senderUUID = UUID.fromString(msgin.readUTF());
					IImmersiveChatPlayer senderMCP = playerApiService.getOnlineImmersiveChatPlayer(senderUUID);
					String echo = msgin.readUTF();
					if (playerApiService.getImmersiveChatPlayer(receiverUUID) == null) {
//						VentureChatPlayer receiverMCP = new VentureChatPlayer(receiverUUID, receiverName, configService.getDefaultChannel());
						ImmersiveChatPlayer receiverMCP = ImmersiveChatPlayer.builder().uuid(receiverUUID).name(receiverName).currentChannel(configService.getDefaultChannel()).build();
						receiverMCP.getListening().add(configService.getDefaultChannel().getName());
						playerApiService.addMineverseChatPlayerToMap(receiverMCP);
						playerApiService.addNameToMap(receiverMCP);
					}
					senderMCP.setReplyPlayer(receiverUUID);
					senderMCP.getPlayer().sendMessage(echo);
				}
				if (identifier.equals("Spy")) {
					String receiverName = msgin.readUTF();
					String senderName = msgin.readUTF();
					String spy = msgin.readUTF();
					if (!spy.startsWith("VentureChat:NoSpy")) {
						for (IImmersiveChatPlayer pl : playerApiService.getOnlineMineverseChatPlayers()) {
							if (configService.isSpy(pl) && !pl.getName().equals(senderName) && !pl.getName().equals(receiverName)) {
								pl.getPlayer().sendMessage(spy);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
