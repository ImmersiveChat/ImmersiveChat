package me.slide.immersivechat.initiators.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.massivecraft.factions.entity.MPlayer;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import me.clip.placeholderapi.PlaceholderAPI;
import me.slide.immersivechat.api.events.VentureChatEvent;
import me.slide.immersivechat.controllers.PluginMessageController;
import me.slide.immersivechat.initiators.application.ImmersiveChat;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.IChatChannel;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import me.slide.immersivechat.model.IMuteContainer;
import me.slide.immersivechat.model.MuteContainer;
import me.slide.immersivechat.service.ConfigService;
import me.slide.immersivechat.service.FormatService;
import me.slide.immersivechat.service.ImmersiveChatDatabaseService;
import me.slide.immersivechat.service.PlayerApiService;
import me.slide.immersivechat.utilities.FormatUtils;
import net.essentialsx.api.v2.services.discord.DiscordService;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.PluginManager;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Set;

@SuppressWarnings("deprecation")
@Singleton
public class ChatListener implements Listener {
	private boolean essentialsDiscordHook;
	@Inject
	private ImmersiveChat plugin;
	@Inject
	private FormatService formatService;
	@Inject
	private ImmersiveChatDatabaseService databaseService;
	@Inject
	private PluginMessageController pluginMessageController;
	@Inject
	private PlayerApiService playerApiService;
	@Inject
	private ConfigService configService;

	@Inject
	public void postConstruct() {
		essentialsDiscordHook = plugin.getServer().getPluginManager().isPluginEnabled("EssentialsDiscord");
	}

	// this event isn't always asynchronous even though the event's name starts with
	// "Async"
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onAsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
		event.setCancelled(true);
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				handleTrueAsyncPlayerChatEvent(event);
			}
		});
	}

	private void processPrivateMessageConversation(final IImmersiveChatPlayer ventureChatPlayer, final String chat) {
		IImmersiveChatPlayer tp = playerApiService.getImmersiveChatPlayer(ventureChatPlayer.getConversation());
		if (!tp.isOnline()) {
			ventureChatPlayer.getPlayer().sendMessage(ChatColor.RED + tp.getName() + " is not available.");
			if (!ventureChatPlayer.getPlayer().hasPermission("venturechat.spy.override")) {
				for (IImmersiveChatPlayer p : playerApiService.getOnlineMineverseChatPlayers()) {
					if (p.getName().equals(ventureChatPlayer.getName())) {
						continue;
					}
					if (configService.isSpy(p)) {
						p.getPlayer().sendMessage(LocalizedMessage.EXIT_PRIVATE_CONVERSATION_SPY.toString().replace("{player_sender}", ventureChatPlayer.getName())
								.replace("{player_receiver}", tp.getName()));
					}
				}
			}
			ventureChatPlayer.setConversation(null);
		} else {
			if (tp.getIgnores().contains(ventureChatPlayer.getUuid())) {
				ventureChatPlayer.getPlayer().sendMessage(LocalizedMessage.IGNORING_MESSAGE.toString().replace("{player}", tp.getName()));
				return;
			}
			if (!tp.isMessageToggle()) {
				ventureChatPlayer.getPlayer().sendMessage(LocalizedMessage.BLOCKING_MESSAGE.toString().replace("{player}", tp.getName()));
				return;
			}
			String filtered = chat;
			String echo = "";
			String send = "";
			String spy = "";
			if (ventureChatPlayer.isFilterEnabled()) {
				filtered = formatService.filterChat(filtered);
			}
			if (ventureChatPlayer.getPlayer().hasPermission("venturechat.color.legacy")) {
				filtered = FormatUtils.FormatStringLegacyColor(filtered);
			}
			if (ventureChatPlayer.getPlayer().hasPermission("venturechat.color")) {
				filtered = FormatUtils.FormatStringColor(filtered);
			}
			if (ventureChatPlayer.getPlayer().hasPermission("venturechat.format")) {
				filtered = FormatUtils.FormatString(filtered);
			}
			filtered = " " + filtered;

			send = FormatUtils.FormatStringAll(
					PlaceholderAPI.setBracketPlaceholders(ventureChatPlayer.getPlayer(), plugin.getConfig().getString("tellformatfrom").replaceAll("sender_", "")));
			echo = FormatUtils
					.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(ventureChatPlayer.getPlayer(), plugin.getConfig().getString("tellformatto").replaceAll("sender_", "")));
			spy = FormatUtils
					.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(ventureChatPlayer.getPlayer(), plugin.getConfig().getString("tellformatspy").replaceAll("sender_", "")));

			send = FormatUtils.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(tp.getPlayer(), send.replaceAll("receiver_", ""))) + filtered;
			echo = FormatUtils.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(tp.getPlayer(), echo.replaceAll("receiver_", ""))) + filtered;
			spy = FormatUtils.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(tp.getPlayer(), spy.replaceAll("receiver_", ""))) + filtered;

			if (!ventureChatPlayer.getPlayer().hasPermission("venturechat.spy.override")) {
				for (IImmersiveChatPlayer p : playerApiService.getOnlineMineverseChatPlayers()) {
					if (p.getName().equals(ventureChatPlayer.getName()) || p.getName().equals(tp.getName())) {
						continue;
					}
					if (configService.isSpy(p)) {
						p.getPlayer().sendMessage(spy);
					}
				}
			}
			tp.getPlayer().sendMessage(send);
			ventureChatPlayer.getPlayer().sendMessage(echo);
			if (tp.isNotifications()) {
				formatService.playMessageSound(tp);
			}
			ventureChatPlayer.setReplyPlayer(tp.getUuid());
			tp.setReplyPlayer(ventureChatPlayer.getUuid());
			if (databaseService.isEnabled()) {
				databaseService.writeVentureChat(ventureChatPlayer.getUuid().toString(), ventureChatPlayer.getName(), "Local", "Messaging_Component", chat.replace("'", "''"),
						"Chat");
			}
		}
	}

	private void processPartyChat(final IImmersiveChatPlayer ventureChatPlayer, final String chat) {
		if (ventureChatPlayer.getParty() != null) {
			String partyformat = "";
			for (IImmersiveChatPlayer p : playerApiService.getOnlineMineverseChatPlayers()) {
				if ((p.getParty() != null && p.getParty().toString().equals(ventureChatPlayer.getParty().toString()) || configService.isSpy(p))) {
					String filtered = chat;
					if (ventureChatPlayer.isFilterEnabled()) {
						filtered = formatService.filterChat(filtered);
					}
					if (ventureChatPlayer.getPlayer().hasPermission("venturechat.color.legacy")) {
						filtered = FormatUtils.FormatStringLegacyColor(filtered);
					}
					if (ventureChatPlayer.getPlayer().hasPermission("venturechat.color")) {
						filtered = FormatUtils.FormatStringColor(filtered);
					}
					if (ventureChatPlayer.getPlayer().hasPermission("venturechat.format")) {
						filtered = FormatUtils.FormatString(filtered);
					}
					filtered = " " + filtered;
					if (plugin.getConfig().getString("partyformat").equalsIgnoreCase("Default")) {
						partyformat = ChatColor.GREEN + "[" + playerApiService.getImmersiveChatPlayer(ventureChatPlayer.getParty()).getName() + "'s Party] "
								+ ventureChatPlayer.getName() + ":" + filtered;
					} else {
						partyformat = FormatUtils.FormatStringAll(
								plugin.getConfig().getString("partyformat").replace("{host}", playerApiService.getImmersiveChatPlayer(ventureChatPlayer.getParty()).getName())
										.replace("{player}", ventureChatPlayer.getName()))
								+ filtered;
					}
					p.getPlayer().sendMessage(partyformat);
				}
			}
			plugin.getServer().getConsoleSender().sendMessage(partyformat);
			if (databaseService.isEnabled()) {
				databaseService.writeVentureChat(ventureChatPlayer.getUuid().toString(), ventureChatPlayer.getName(), "Local", "Party_Component", chat.replace("'", "''"), "Chat");
			}
			return;
		}
		ventureChatPlayer.getPlayer().sendMessage(ChatColor.RED + "You are not in a party.");
	}

	private void processMute(final IImmersiveChatPlayer ventureChatPlayer, final IChatChannel channel) {
		IMuteContainer muteContainer = ventureChatPlayer.getMutes().get(channel.getName());
		if (muteContainer.getDuration() > 0) {
			long dateTimeMillis = System.currentTimeMillis();
			long muteTimeMillis = muteContainer.getDuration();
			long remainingMuteTime = muteTimeMillis - dateTimeMillis;
			if (remainingMuteTime < 1000) {
				remainingMuteTime = 1000;
			}
			String timeString = FormatUtils.parseTimeStringFromMillis(remainingMuteTime);
			if (!muteContainer.getReason().isEmpty()) {
				ventureChatPlayer.getPlayer().sendMessage(LocalizedMessage.CHANNEL_MUTED_TIMED_REASON.toString().replace("{channel_color}", channel.getColor())
						.replace("{channel_name}", channel.getName()).replace("{time}", timeString).replace("{reason}", muteContainer.getReason()));
			} else {
				ventureChatPlayer.getPlayer().sendMessage(LocalizedMessage.CHANNEL_MUTED_TIMED.toString().replace("{channel_color}", channel.getColor())
						.replace("{channel_name}", channel.getName()).replace("{time}", timeString));
			}
		} else {
			if (!muteContainer.getReason().isEmpty()) {
				ventureChatPlayer.getPlayer().sendMessage(LocalizedMessage.CHANNEL_MUTED_REASON.toString().replace("{channel_color}", channel.getColor())
						.replace("{channel_name}", channel.getName()).replace("{reason}", muteContainer.getReason()));
			} else {
				ventureChatPlayer.getPlayer()
						.sendMessage(LocalizedMessage.CHANNEL_MUTED.toString().replace("{channel_color}", channel.getColor()).replace("{channel_name}", channel.getName()));
			}
		}
		ventureChatPlayer.setQuickChat(false);
	}

	private void handleTrueAsyncPlayerChatEvent(final AsyncPlayerChatEvent event) {
		String chat = event.getMessage();
		String format;
		IImmersiveChatPlayer mcp = playerApiService.getOnlineImmersiveChatPlayer(event.getPlayer());
		IChatChannel eventChannel = mcp.getCurrentChannel();
		if (mcp.isEditing()) {
			mcp.getPlayer().sendMessage(FormatUtils.FormatStringAll(chat));
			mcp.setEditing(false);
			return;
		}
		if (mcp.isQuickChat()) {
			eventChannel = mcp.getQuickChannel();
		}
		if (mcp.getConversation() != null && !mcp.isQuickChat()) {
			processPrivateMessageConversation(mcp, chat);
			return;
		}
		if (mcp.isPartyChat() && !mcp.isQuickChat()) {
			processPartyChat(mcp, chat);
			return;
		}
		if (eventChannel.isPermissionRequired() && !mcp.getPlayer().hasPermission(eventChannel.getPermission())) {
			mcp.getPlayer().sendMessage(LocalizedMessage.CHANNEL_NO_PERMISSION.toString());
			mcp.setQuickChat(false);
			mcp.getListening().remove(eventChannel.getName());
			mcp.setCurrentChannel(configService.getDefaultChannel());
			return;
		} else {
			mcp.getListening().add(eventChannel.getName());
		}
		if (eventChannel.isSpeakPermissionRequired() && !mcp.getPlayer().hasPermission(eventChannel.getSpeakPermission())) {
			mcp.getPlayer().sendMessage(LocalizedMessage.CHANNEL_NO_SPEAK_PERMISSIONS.toString());
			mcp.setQuickChat(false);
			return;
		}
		if (mcp.getMutes().containsKey(eventChannel.getName())) {
			processMute(mcp, eventChannel);
			return;
		}

		long dateTimeSeconds = System.currentTimeMillis() / FormatUtils.MILLISECONDS_PER_SECOND;
		int chCooldown = 0;
		if (eventChannel.getCooldown() > 0) {
			chCooldown = eventChannel.getCooldown();
		}
		try {
			if (mcp.getCooldowns().containsKey(eventChannel)) {
				long cooldownTime = mcp.getCooldowns().get(eventChannel).longValue();
				if (dateTimeSeconds < cooldownTime) {
					long remainingCooldownTime = cooldownTime - dateTimeSeconds;
					String cooldownString = FormatUtils.parseTimeStringFromMillis(remainingCooldownTime * FormatUtils.MILLISECONDS_PER_SECOND);
					mcp.getPlayer().sendMessage(LocalizedMessage.CHANNEL_COOLDOWN.toString().replace("{cooldown}", cooldownString));
					mcp.setQuickChat(false);
					return;
				}
			}
			if (eventChannel.getCooldown() > 0) {
				if (!mcp.getPlayer().hasPermission("venturechat.cooldown.bypass")) {
					mcp.getCooldowns().put(eventChannel, dateTimeSeconds + chCooldown);
				}
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}

		// TODO Refactor and clarify this. Refactor how this is initialized and stored as player data
		if (mcp.getSpam().containsKey(eventChannel) && plugin.getConfig().getConfigurationSection("antispam").getBoolean("enabled")
				&& !mcp.getPlayer().hasPermission("venturechat.spam.bypass")) {
			long spamcount = mcp.getSpam().get(eventChannel).get(0);
			long spamtime = mcp.getSpam().get(eventChannel).get(1);
			long spamtimeconfig = plugin.getConfig().getConfigurationSection("antispam").getLong("spamnumber");
			String mutedForTime = plugin.getConfig().getConfigurationSection("antispam").getString("mutetime", "0");
			long dateTime = System.currentTimeMillis();
			if (dateTimeSeconds < spamtime + plugin.getConfig().getConfigurationSection("antispam").getLong("spamtime")) {
				if (spamcount + 1 >= spamtimeconfig) {
					long time = FormatUtils.parseTimeStringToMillis(mutedForTime);
					if (time > 0) {
						mcp.getMutes().put(eventChannel.getName(), new MuteContainer(eventChannel.getName(), dateTime + time, LocalizedMessage.SPAM_MUTE_REASON_TEXT.toString()));
						String timeString = FormatUtils.parseTimeStringFromMillis(time);
						mcp.getPlayer()
								.sendMessage(LocalizedMessage.MUTE_PLAYER_PLAYER_TIME_REASON.toString().replace("{channel_color}", eventChannel.getColor())
										.replace("{channel_name}", eventChannel.getName()).replace("{time}", timeString)
										.replace("{reason}", LocalizedMessage.SPAM_MUTE_REASON_TEXT.toString()));
					} else {
						mcp.getMutes().put(eventChannel.getName(), new MuteContainer(eventChannel.getName(), 0, LocalizedMessage.SPAM_MUTE_REASON_TEXT.toString()));
						mcp.getPlayer().sendMessage(LocalizedMessage.MUTE_PLAYER_PLAYER_REASON.toString().replace("{channel_color}", eventChannel.getColor())
								.replace("{channel_name}", eventChannel.getName()).replace("{reason}", LocalizedMessage.SPAM_MUTE_REASON_TEXT.toString()));
					}
					if (eventChannel.isBungeeEnabled()) {
						pluginMessageController.synchronize(mcp, true);
					}
					mcp.getSpam().get(eventChannel).set(0, 0L);
					mcp.setQuickChat(false);
					return;
				} else {
					if (spamtimeconfig % 2 != 0) {
						spamtimeconfig++;
					}
					if (spamcount + 1 == spamtimeconfig / 2) {
						mcp.getPlayer().sendMessage(LocalizedMessage.SPAM_WARNING.toString());
					}
					mcp.getSpam().get(eventChannel).set(0, spamcount + 1);
				}
			} else {
				mcp.getSpam().get(eventChannel).set(0, 1L);
				mcp.getSpam().get(eventChannel).set(1, dateTimeSeconds);
			}
		} else {
			mcp.getSpam().put(eventChannel, new ArrayList<>());
			mcp.getSpam().get(eventChannel).add(0, 1L);
			mcp.getSpam().get(eventChannel).add(1, dateTimeSeconds);
		}

		format = FormatUtils.FormatStringAll(eventChannel.getFormat());

		if (eventChannel.isFiltered() && mcp.isFilterEnabled()) {
			chat = formatService.filterChat(chat);
		}
		PluginManager pluginManager = plugin.getServer().getPluginManager();
		Set<Player> recipients = event.getRecipients();
		int recipientCount = recipients.size(); // Don't count vanished players
		for (IImmersiveChatPlayer p : playerApiService.getOnlineMineverseChatPlayers()) {
			if (p.getPlayer() != mcp.getPlayer()) {
				if (!configService.isListening(p, eventChannel.getName())) {
					recipients.remove(p.getPlayer());
					recipientCount--;
					continue;
				}
				if (plugin.getConfig().getBoolean("ignorechat", false) && p.getIgnores().contains(mcp.getUuid())) {
					recipients.remove(p.getPlayer());
					recipientCount--;
					continue;
				}
				if (plugin.getConfig().getBoolean("enable_towny_channel") && pluginManager.isPluginEnabled("Towny")) {
					try {
						TownyUniverse towny = TownyUniverse.getInstance();
						if (eventChannel.getName().equalsIgnoreCase("Town")) {
							Resident r = towny.getResident(p.getName());
							Resident pp = towny.getResident(mcp.getName());
							if (!pp.hasTown()) {
								recipients.remove(p.getPlayer());
								recipientCount--;
								continue;
							} else if (!r.hasTown()) {
								recipients.remove(p.getPlayer());
								recipientCount--;
								continue;
							} else if (!(r.getTown().getName().equals(pp.getTown().getName()))) {
								recipients.remove(p.getPlayer());
								recipientCount--;
								continue;
							}
						}
						if (eventChannel.getName().equalsIgnoreCase("Nation")) {
							Resident r = towny.getResident(p.getName());
							Resident pp = towny.getResident(mcp.getName());
							if (!pp.hasNation()) {
								recipients.remove(p.getPlayer());
								recipientCount--;
								continue;
							} else if (!r.hasNation()) {
								recipients.remove(p.getPlayer());
								recipientCount--;
								continue;
							} else if (!(r.getTown().getNation().getName().equals(pp.getTown().getNation().getName()))) {
								recipients.remove(p.getPlayer());
								recipientCount--;
								continue;
							}
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}

				if (plugin.getConfig().getBoolean("enable_factions_channel") && pluginManager.isPluginEnabled("Factions")) {
					try {
						if (eventChannel.getName().equalsIgnoreCase("Faction")) {
							MPlayer mplayer = MPlayer.get(mcp.getPlayer());
							MPlayer mplayerp = MPlayer.get(p.getPlayer());
							if (!mplayer.hasFaction()) {
								recipients.remove(p.getPlayer());
								recipientCount--;
							} else if (!mplayerp.hasFaction()) {
								recipients.remove(p.getPlayer());
								recipientCount--;
							} else if (!(mplayer.getFactionName().equals(mplayerp.getFactionName()))) {
								recipients.remove(p.getPlayer());
								recipientCount--;
							}
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}

				double chDistance = Math.max(eventChannel.getDistance(), 0);
				if (chDistance > (double) 0 && !eventChannel.isBungeeEnabled() && !configService.isRangedSpy(p)) {
					final Location locreceip = p.getPlayer().getLocation();
					if (locreceip.getWorld() == mcp.getPlayer().getWorld()) {
						final Location locsender = mcp.getPlayer().getLocation();
						final Location diff = locreceip.subtract(locsender);
						if (Math.abs(diff.getX()) > chDistance || Math.abs(diff.getZ()) > chDistance || Math.abs(diff.getY()) > chDistance) {
							recipients.remove(p.getPlayer());
							recipientCount--;
							continue;
						}
						if (!mcp.getPlayer().canSee(p.getPlayer())) {
							recipientCount--;
							continue;
						}
					} else {
						recipients.remove(p.getPlayer());
						recipientCount--;
						continue;
					}
				}
				if (!mcp.getPlayer().canSee(p.getPlayer())) {
					recipientCount--;
					continue;
				}
			}
		}

		if (mcp.getPlayer().hasPermission("venturechat.color.legacy")) {
			chat = FormatUtils.FormatStringLegacyColor(chat);
		}
		if (mcp.getPlayer().hasPermission("venturechat.color")) {
			chat = FormatUtils.FormatStringColor(chat);
		}
		if (mcp.getPlayer().hasPermission("venturechat.format")) {
			chat = FormatUtils.FormatString(chat);
		}
		if (!mcp.isQuickChat()) {
			chat = " " + chat;
		}
		final String curColor = eventChannel.getChatColor();
		if (curColor.equalsIgnoreCase("None")) {
			// Format the placeholders and their color codes to determine the last color
			// code to use for the chat message color
			chat = formatService.getLastCode(FormatUtils.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), format))) + chat;
		} else {
			chat = curColor + chat;
		}

		String globalJSON = formatService.convertToJson(mcp, format, chat);
		format = FormatUtils.FormatStringAll(PlaceholderAPI.setBracketPlaceholders(mcp.getPlayer(), FormatUtils.FormatStringAll(format)));
		String message = FormatUtils.stripColor(format + chat); // UTF-8 encoding issues.
		final VentureChatEvent ventureChatEvent = new VentureChatEvent(mcp, mcp.getName(), plugin.getVaultPermission().getPrimaryGroup(mcp.getPlayer()), eventChannel, recipients,
				recipientCount, format, chat, globalJSON, message.hashCode(), eventChannel.isBungeeEnabled());
		plugin.getServer().getPluginManager().callEvent(ventureChatEvent);
		handleVentureChatEvent(ventureChatEvent);
		mcp.setQuickChat(false);
	}

	private void handleVentureChatEvent(VentureChatEvent event) {
		IImmersiveChatPlayer ventureChatPlayer = event.getVentureChatPlayer();
		IChatChannel channel = event.getChannel();
		Set<Player> recipients = event.getRecipients();
		int recipientCount = event.getRecipientCount();
		String format = event.getFormat();
		String chat = event.getChat();
		String consoleChat = event.getConsoleChat();
		String globalJSON = event.getGlobalJSON();
		int hash = event.getHash();
		boolean bungee = event.isBungee();
		if (essentialsDiscordHook && channel.isDefaultChannel()) {
			plugin.getServer().getServicesManager().load(DiscordService.class).sendChatMessage(ventureChatPlayer.getPlayer(), chat);
		}
		if (!bungee) {
			if (databaseService.isEnabled()) {
				databaseService.writeVentureChat(ventureChatPlayer.getUuid().toString(), ventureChatPlayer.getName(), "Local", channel.getName(), chat.replace("'", "''"), "Chat");
			}
			if (recipientCount == 1) {
				if (!plugin.getConfig().getString("emptychannelalert", "&6No one is listening to you.").isEmpty()) {
					ventureChatPlayer.getPlayer().sendMessage(FormatUtils.FormatStringAll(plugin.getConfig().getString("emptychannelalert", "&6No one is listening to you.")));
				}
			}
			formatService.createAndSendChatMessage(globalJSON, channel.getName(), hash, recipients, ventureChatPlayer.getName());
			plugin.getServer().getConsoleSender().sendMessage(consoleChat);
			return;
		} else {
			ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(byteOutStream);
			try {
				out.writeUTF("Chat");
				out.writeUTF(channel.getName());
				out.writeUTF(ventureChatPlayer.getName());
				out.writeUTF(ventureChatPlayer.getUuid().toString());
				out.writeBoolean(ventureChatPlayer.isBungeeToggle());
				out.writeInt(hash);
				out.writeUTF(format);
				out.writeUTF(chat);
				if (plugin.getConfig().getString("loglevel", "info").equals("debug")) {
					System.out.println(out.size() + " size bytes without json");
				}
				out.writeUTF(globalJSON);
				if (plugin.getConfig().getString("loglevel", "info").equals("debug")) {
					System.out.println(out.size() + " bytes size with json");
				}
				out.writeUTF(plugin.getVaultPermission().getPrimaryGroup(ventureChatPlayer.getPlayer())); // look into not sending this
				final String displayName = ventureChatPlayer.getPlayer().getDisplayName();
				out.writeUTF(displayName);
				pluginMessageController.sendPluginMessage(byteOutStream);
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
	}
}
