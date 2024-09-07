package me.slide.immersivechat.initiators.application;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import me.slide.immersivechat.controllers.ProxyController;
import me.slide.immersivechat.controllers.ProxyFlatFileController;
import me.slide.immersivechat.guice.ImmersiveChatBungeePluginModule;
import me.slide.immersivechat.model.ImmersiveChatProxyServer;
import me.slide.immersivechat.model.ImmersiveChatProxySource;
import me.slide.immersivechat.service.ProxyUuidService;
import me.slide.immersivechat.utilities.FormatUtils;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

/**
 * VentureChat Minecraft plugin for BungeeCord.
 *
 * @author Aust1n46
 */
public class ImmersiveChatBungee extends Plugin implements Listener, ImmersiveChatProxySource {
	private static Configuration bungeeConfig;
	private File bungeePlayerDataDirectory;

	@Inject
	private ProxyUuidService uuidService;
	@Inject
	private ProxyFlatFileController proxyFlatFileController;
	@Inject
	private ProxyController proxy;

	@Override
	public void onEnable() {
		final ImmersiveChatBungeePluginModule pluginModule = new ImmersiveChatBungeePluginModule(this);
		final Injector injector = Guice.createInjector(pluginModule);
		injector.injectMembers(this);

		if (!getDataFolder().exists()) {
			getDataFolder().mkdir();
		}
		File config = new File(getDataFolder(), "bungeeconfig.yml");
		try {
			if (!config.exists()) {
				Files.copy(getResourceAsStream("bungeeconfig.yml"), config.toPath());
			}
			bungeeConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "bungeeconfig.yml"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		bungeePlayerDataDirectory = new File(getDataFolder().getAbsolutePath() + "/PlayerData");
		proxyFlatFileController.loadLegacyBungeePlayerData(bungeePlayerDataDirectory, this);
		proxyFlatFileController.loadProxyPlayerData(bungeePlayerDataDirectory, this);

		this.getProxy().registerChannel(ProxyController.PLUGIN_MESSAGING_CHANNEL_STRING);
		this.getProxy().getPluginManager().registerListener(this, this);
	}

	@Override
	public void onDisable() {
		proxyFlatFileController.saveProxyPlayerData(bungeePlayerDataDirectory, this);
	}

	@EventHandler
	public void onPlayerJoin(ServerSwitchEvent event) {
		updatePlayerNames();
	}

	@EventHandler
	public void onPlayerLeave(ServerDisconnectEvent event) {
		updatePlayerNames();
	}

	@EventHandler
	public void onPlayerJoinNetwork(PostLoginEvent event) {
		uuidService.checkOfflineUUIDWarningProxy(event.getPlayer().getUniqueId(), this);
	}

	private void updatePlayerNames() {
		try {
			ByteArrayOutputStream outstream = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(outstream);
			out.writeUTF("PlayerNames");
			out.writeInt(getProxy().getPlayers().size());
			for (ProxiedPlayer pp : getProxy().getPlayers()) {
				out.writeUTF(pp.getName());
			}

			for (String send : getProxy().getServers().keySet()) {
				if (getProxy().getServers().get(send).getPlayers().size() > 0) {
					getProxy().getServers().get(send).sendData(ProxyController.PLUGIN_MESSAGING_CHANNEL_STRING, outstream.toByteArray());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@EventHandler
	public void onPluginMessage(PluginMessageEvent event) {
		if (!event.getTag().equals(ProxyController.PLUGIN_MESSAGING_CHANNEL_STRING) && !event.getTag().contains("viaversion:")) {
			return;
		}
		if (!(event.getSender() instanceof Server)) {
			return;
		}
		String serverName = ((Server) event.getSender()).getInfo().getName();
		proxy.onPluginMessage(event.getData(), serverName, this);
	}

	@Override
	public void sendPluginMessage(String serverName, byte[] data) {
		getProxy().getServers().get(serverName).sendData(ProxyController.PLUGIN_MESSAGING_CHANNEL_STRING, data);
	}

	@Override
	public List<ImmersiveChatProxyServer> getServers() {
		return getProxy().getServers().values().stream().map(bungeeServer -> new ImmersiveChatProxyServer(bungeeServer.getName(), bungeeServer.getPlayers().isEmpty()))
				.collect(Collectors.toList());
	}

	@Override
	public ImmersiveChatProxyServer getServer(String serverName) {
		ServerInfo server = (ServerInfo) getProxy().getServers().get(serverName);
		return new ImmersiveChatProxyServer(serverName, server.getPlayers().isEmpty());
	}

	@Override
	public void sendConsoleMessage(String message) {
		ProxyServer.getInstance().getConsole().sendMessage(TextComponent.fromLegacyText(FormatUtils.FormatStringAll(message)));
	}

	@Override
	public boolean isOfflineServerAcknowledgementSet() {
		return bungeeConfig.getBoolean("offline_server_acknowledgement");
	}
}
