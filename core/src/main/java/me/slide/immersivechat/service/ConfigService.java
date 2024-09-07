package me.slide.immersivechat.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.slide.immersivechat.initiators.application.ImmersiveChat;
import me.slide.immersivechat.model.*;
import me.slide.immersivechat.utilities.FormatUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

@Singleton
public class ConfigService {
	private static final String PERMISSION_PREFIX = "venturechat.";
	
	@Inject
	private ImmersiveChat plugin;

	private final HashMap<String, ChatChannel> chatChannels = new HashMap<String, ChatChannel>();
	private final HashMap<String, JsonFormat> jsonFormats = new HashMap<String, JsonFormat>();
	private final List<Alias> aliases = new ArrayList<>();
	private final List<GuiSlot> guiSlots = new ArrayList<>();
	private boolean aliasesRegisteredAsCommands;
	private ChatChannel defaultChatChannel;
	private String defaultColor;

	@Getter
	private boolean ignoreChatEnabled;
	@Getter
	private List<Filter> filters;
	
	public String getValidColor(final String colorRaw) {
		if (FormatUtils.isValidColor(colorRaw)) {
			return String.valueOf(ChatColor.valueOf(colorRaw.toUpperCase()));
		}
		if (FormatUtils.isValidHexColor(colorRaw)) {
			return FormatUtils.convertHexColorCodeToBukkitColorCode(colorRaw);
		}
		return FormatUtils.DEFAULT_COLOR_CODE;
	}

	@Inject
	public void postConstruct() {
		aliasesRegisteredAsCommands = true;
		ConfigurationSection cs = plugin.getConfig().getConfigurationSection("channels");
		for (final String key : cs.getKeys(false)) {
			final String colorRaw = cs.getString(key + ".color", "white");
			final String chatColorRaw = cs.getString(key + ".chatcolor", "white");
			final String name = key;
			final String permission = PERMISSION_PREFIX + cs.getString(key + ".permissions", "None");
			final String speakPermission = PERMISSION_PREFIX + cs.getString(key + ".speak_permissions", "None");
			final boolean mutable = cs.getBoolean(key + ".mutable", false);
			final boolean filter = cs.getBoolean(key + ".filter", true);
			final boolean bungee = cs.getBoolean(key + ".bungeecord", false);
			final String format = cs.getString(key + ".format", "Default");
			final boolean defaultChannel = cs.getBoolean(key + ".default", false);
			final String alias = cs.getString(key + ".alias", "None");
			final double distance = cs.getDouble(key + ".distance", (double) 0);
			final int cooldown = cs.getInt(key + ".cooldown", 0);
			final boolean autojoin = cs.getBoolean(key + ".autojoin", false);
			final String prefix = cs.getString(key + ".channel_prefix");
			final String color = getValidColor(colorRaw);
			final String chatColor = chatColorRaw.equalsIgnoreCase("None") ? "None" : getValidColor(chatColorRaw);
			final ChatChannel chatChannel = new ChatChannel(name, color, colorRaw, chatColor, chatColorRaw, permission, speakPermission, mutable, filter, defaultChannel, alias, distance, autojoin, bungee,
					cooldown, prefix, format);
			chatChannels.put(name.toLowerCase(), chatChannel);
			chatChannels.put(alias.toLowerCase(), chatChannel);
			if (defaultChannel) {
				defaultChatChannel = chatChannel;
				defaultColor = color;
			}
		}
		// Error handling for missing default channel in the config.
		if (defaultChatChannel == null) {
			plugin.getServer().getConsoleSender().sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - &cNo default channel found!"));
			defaultChatChannel = new ChatChannel("MissingDefault", "red", "RED", "red", "RED", ChatChannel.NO_PERMISSIONS, ChatChannel.NO_PERMISSIONS, false, true, true, "md", 0, true, false, 0, "&f[&cMissingDefault&f]",
					"{venturechat_channel_prefix} {vault_prefix}{player_displayname}&c:");
			defaultColor = defaultChatChannel.getColor();
			chatChannels.put("missingdefault", defaultChatChannel);
			chatChannels.put("md", defaultChatChannel);
		}

		jsonFormats.clear();
		ConfigurationSection jsonFormatSection = plugin.getConfig().getConfigurationSection("jsonformatting");
		for (String jsonFormat : jsonFormatSection.getKeys(false)) {
			int priority = jsonFormatSection.getInt(jsonFormat + ".priority", 0);
			List<JsonAttribute> jsonAttributes = new ArrayList<>();
			ConfigurationSection jsonAttributeSection = jsonFormatSection.getConfigurationSection(jsonFormat + ".json_attributes");
			if (jsonAttributeSection != null) {
				for (String attribute : jsonAttributeSection.getKeys(false)) {
					List<String> hoverText = jsonAttributeSection.getStringList(attribute + ".hover_text");
					String clickText = jsonAttributeSection.getString(attribute + ".click_text", "");
					String clickActionText = jsonAttributeSection.getString(attribute + ".click_action", "none");
					ClickAction clickAction = ClickAction.valueOf(clickActionText.toUpperCase());
					jsonAttributes.add(new JsonAttribute(attribute, hoverText, clickAction, clickText));
				}
			}
			jsonFormats.put(jsonFormat.toLowerCase(), new JsonFormat(jsonFormat, priority, jsonAttributes));
		}

		aliases.clear();
		ConfigurationSection configurationSection = plugin.getConfig().getConfigurationSection("alias");
		for (String key : configurationSection.getKeys(false)) {
			String name = key;
			int arguments = configurationSection.getInt(key + ".arguments", 0);
			List<String> components = configurationSection.getStringList(key + ".components");
			String permissions = configurationSection.getString(key + ".permissions", "None");
			aliases.add(new Alias(name, arguments, components, permissions));
		}

		guiSlots.clear();
		cs = plugin.getConfig().getConfigurationSection("venturegui");
		for (String key : cs.getKeys(false)) {
			String name = key;
			String icon = cs.getString(key + ".icon");
			int durability = cs.getInt(key + ".durability");
			String text = cs.getString(key + ".text");
			String permission = cs.getString(key + ".permission");
			String command = cs.getString(key + ".command");
			int slot = cs.getInt(key + ".slot");
			guiSlots.add(new GuiSlot(name, icon, durability, text, permission, command, slot));
		}
		
		filters = plugin.getConfig().getStringList("filters")
				.stream()
				.map(x -> x.split(","))
				.map(x -> new Filter(x[0], x[1]))
				.toList();
		
		ignoreChatEnabled = plugin.getConfig().getBoolean("ignorechat", false);
	}

	public boolean areAliasesRegisteredAsCommands() {
		return aliasesRegisteredAsCommands;
	}

	/**
	 * Get list of chat channels.
	 * 
	 * @return {@link Collection}&lt{@link ChatChannel}&gt
	 */
	public Collection<ChatChannel> getChatChannels() {
		return new HashSet<ChatChannel>(chatChannels.values());
	}

	/**
	 * Get a chat channel by name.
	 * 
	 * @param channelName name of channel to get.
	 * @return {@link ChatChannel}
	 */
	public ChatChannel getChannel(String channelName) {
		return chatChannels.get(channelName.toLowerCase());
	}

	/**
	 * Checks if the chat channel exists.
	 * 
	 * @param channelName name of channel to check.
	 * @return true if channel exists, false otherwise.
	 */
	public boolean isChannel(String channelName) {
		return getChannel(channelName) != null;
	}

	// TODO Investigate if all of this logic should be here
	public boolean isListening(IImmersiveChatPlayer ventureChatPlayer, String channel) {
		if (ventureChatPlayer.isOnline()) {
			if (isChannel(channel)) {
				ChatChannel chatChannel = getChannel(channel);
				if (chatChannel.isPermissionRequired()) {
					if (!ventureChatPlayer.getPlayer().hasPermission(chatChannel.getPermission())) {
						if (ventureChatPlayer.getCurrentChannel().getName().equals(channel)) {
							ventureChatPlayer.setCurrentChannel(getDefaultChannel());
						}
						ventureChatPlayer.getListening().remove(channel);
						return false;
					}
				}
			}
		}
		return ventureChatPlayer.getListening().contains(channel);
	}

	// TODO Investigate if all of this logic should be here
	public boolean isRangedSpy(final IImmersiveChatPlayer ventureChatPlayer) {
		if (ventureChatPlayer.isOnline()) {
			if (!ventureChatPlayer.getPlayer().hasPermission("venturechat.rangedspy")) {
				ventureChatPlayer.setRangedSpy(false);
				return false;
			}
		}
		return ventureChatPlayer.isRangedSpy();
	}

	// TODO Investigate if all of this logic should be here
	public boolean isSpy(final IImmersiveChatPlayer ventureChatPlayer) {
		if (ventureChatPlayer.isOnline()) {
			if (!ventureChatPlayer.getPlayer().hasPermission("venturechat.spy")) {
				ventureChatPlayer.setSpy(false);
				return false;
			}
		}
		return ventureChatPlayer.isSpy();
	}

	// TODO Investigate if all of this logic should be here
	public boolean isCommandSpy(final IImmersiveChatPlayer ventureChatPlayer) {
		if (ventureChatPlayer.isOnline()) {
			if (!ventureChatPlayer.getPlayer().hasPermission("venturechat.commandspy")) {
				ventureChatPlayer.setCommandSpy(false);
				return false;
			}
		}
		return ventureChatPlayer.isCommandSpy();
	}

	/**
	 * Get default chat channel color.
	 * 
	 * @return {@link String}
	 */
	public String getDefaultColor() {
		return defaultColor;
	}

	/**
	 * Get default chat channel.
	 * 
	 * @return {@link ChatChannel}
	 */
	public ChatChannel getDefaultChannel() {
		return defaultChatChannel;
	}

	/**
	 * Get list of chat channels with autojoin set to true.
	 * 
	 * @return {@link List}&lt{@link ChatChannel}&gt
	 */
	public List<ChatChannel> getAutojoinList() {
		List<ChatChannel> joinlist = new ArrayList<ChatChannel>();
		for (ChatChannel c : chatChannels.values()) {
			if (c.isAutoJoinEnabled()) {
				joinlist.add(c);
			}
		}
		return joinlist;
	}

	public Collection<JsonFormat> getJsonFormats() {
		return jsonFormats.values();
	}

	public JsonFormat getJsonFormat(String name) {
		return jsonFormats.get(name.toLowerCase());
	}

	public List<Alias> getAliases() {
		return aliases;
	}

	public List<GuiSlot> getGuiSlots() {
		return guiSlots;
	}

	public boolean isProxyEnabled() {
		try {
			return (plugin.getServer().spigot().getConfig().getBoolean("settings.bungeecord")
					|| plugin.getServer().spigot().getPaperConfig().getBoolean("settings.velocity-support.enabled")
					|| plugin.getServer().spigot().getPaperConfig().getBoolean("proxies.velocity.enabled"));
		} catch (NoSuchMethodError exception) { // Thrown if server isn't Paper.
			return false;
		}
	}
}
