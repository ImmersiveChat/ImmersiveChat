package me.slide.immersivechat.initiators.application;

import com.comphenix.protocol.ProtocolLibrary;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import lombok.Getter;
import me.slide.immersivechat.controllers.CommandController;
import me.slide.immersivechat.controllers.PluginMessageController;
import me.slide.immersivechat.controllers.SpigotFlatFileController;
import me.slide.immersivechat.guice.ImmersiveChatPluginModule;
import me.slide.immersivechat.initiators.listeners.*;
import me.slide.immersivechat.initiators.schedulers.UnmuteScheduler;
import me.slide.immersivechat.localization.Localization;
import me.slide.immersivechat.placeholderapi.VentureChatPlaceholders;
import me.slide.immersivechat.service.PlayerApiService;
import me.slide.immersivechat.utilities.FormatUtils;
import me.slide.immersivechat.xcut.VersionService;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;

/**
 * VentureChat Minecraft plugin for servers running Spigot or Paper software.
 *
 * @author Aust1n46
 */
@Singleton
public class ImmersiveChat extends JavaPlugin implements PluginMessageListener {
	@Inject
	private LoginListener loginListener;
	@Inject
	private ChatListener chatListener;
	@Inject
	private SignListener signListener;
	@Inject
	private PreProcessCommandListener commandListener;
	@Inject
	private PacketListenerLegacyChat packetListener;
	@Inject
	private VentureChatPlaceholders ventureChatPlaceholders;
	@Inject
	private SpigotFlatFileController spigotFlatFileService;
	@Inject
	private PlayerApiService playerApiService;
	@Inject
	private PluginMessageController pluginMessageController;
	@Inject
	private VersionService versionService;

	@Getter
	private Permission vaultPermission;
	private Injector injector;
	
	public void injectDependencies(Object o) {
		injector.injectMembers(o);
	}

	@Override
	public void onEnable() {
		final ImmersiveChatPluginModule pluginModule = new ImmersiveChatPluginModule(this);
		injector = Guice.createInjector(pluginModule);
		injector.injectMembers(this);
		injector.injectMembers(new CommandController());
		injector.injectMembers(new UnmuteScheduler());

		try {
			getServer().getConsoleSender().sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - Initializing..."));
			if (!getDataFolder().exists()) {
				getDataFolder().mkdirs();
			}
			File file = new File(getDataFolder(), "config.yml");
			if (!file.exists()) {
				getServer().getConsoleSender().sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - Config not found! Generating file."));
				saveDefaultConfig();
			} else {
				getServer().getConsoleSender().sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - Config found! Loading file."));
			}

			saveResource("example_config_always_up_to_date!.yml", true);
		} catch (Exception ex) {
			getServer().getConsoleSender().sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - &cCould not load configuration! Something unexpected went wrong!"));
		}

		getServer().getConsoleSender().sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - Checking for Vault..."));

		if (!setupPermissions()) {
			getServer().getConsoleSender().sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - &cCould not find Vault and/or a Vault compatible permissions plugin!"));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		Localization.initialize(this);

		getServer().getConsoleSender().sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - Loading player data"));
		spigotFlatFileService.loadLegacyPlayerData();
		spigotFlatFileService.loadPlayerData();

		registerListeners();
		getServer().getConsoleSender().sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - Registering Listeners"));

		getServer().getConsoleSender().sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - Registering BungeeCord channels"));
		getServer().getMessenger().registerOutgoingPluginChannel(this, PluginMessageController.PLUGIN_MESSAGING_CHANNEL);
		getServer().getMessenger().registerIncomingPluginChannel(this, PluginMessageController.PLUGIN_MESSAGING_CHANNEL, this);

		PluginManager pluginManager = getServer().getPluginManager();
		if (pluginManager.isPluginEnabled("Towny")) {
			getServer().getConsoleSender().sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - Enabling Towny Formatting"));
		}
		if (pluginManager.isPluginEnabled("Jobs")) {
			getServer().getConsoleSender().sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - Enabling Jobs Formatting"));
		}
		if (pluginManager.isPluginEnabled("Factions")) {
			final String version = pluginManager.getPlugin("Factions").getDescription().getVersion();
			getServer().getConsoleSender().sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - Enabling Factions Formatting version " + version));
		}
		if (pluginManager.isPluginEnabled("PlaceholderAPI")) {
			getServer().getConsoleSender().sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - Enabling PlaceholderAPI Hook"));
		}

		ventureChatPlaceholders.register();

		startRepeatingTasks();

		getServer().getConsoleSender().sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - Enabled Successfully"));
	}

	@Override
	public void onDisable() {
		spigotFlatFileService.savePlayerData();
		playerApiService.clearMineverseChatPlayerMap();
		playerApiService.clearNameMap();
		playerApiService.clearOnlineMineverseChatPlayerMap();
		getServer().getConsoleSender().sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - Disabling..."));
		getServer().getConsoleSender().sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - Disabled Successfully"));
	}

	private void startRepeatingTasks() {
		BukkitScheduler scheduler = getServer().getScheduler();
		scheduler.runTaskTimerAsynchronously(this, new Runnable() {
			@Override
			public void run() {
				spigotFlatFileService.savePlayerData();
				if (getConfig().getString("loglevel", "info").equals("debug")) {
					getServer().getConsoleSender().sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - Saving Player Data"));
				}
			}
		}, 0L, getConfig().getInt("saveinterval") * 1200); // one minute * save interval
	}

	private void registerListeners() {
		PluginManager pluginManager = getServer().getPluginManager();
		pluginManager.registerEvents(chatListener, this);
		pluginManager.registerEvents(signListener, this);
		pluginManager.registerEvents(commandListener, this);
		pluginManager.registerEvents(loginListener, this);
		if (versionService.isUnder_1_19()) {
			ProtocolLibrary.getProtocolManager().addPacketListener(packetListener);
		}
	}

	private boolean setupPermissions() {
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
		if (permissionProvider != null) {
			vaultPermission = permissionProvider.getProvider();
		}
		return (vaultPermission != null);
	}

	@Override
	public void onPluginMessageReceived(final String channel, final Player player, final byte[] inputStream) {
		pluginMessageController.processInboundPluginMessage(channel, player, inputStream);
	}
}
