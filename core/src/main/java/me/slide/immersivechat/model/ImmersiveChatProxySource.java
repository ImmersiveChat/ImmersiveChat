package me.slide.immersivechat.model;

import java.util.List;

public interface ImmersiveChatProxySource {
	public void sendPluginMessage(String serverName, byte[] data);

	public List<ImmersiveChatProxyServer> getServers();

	public ImmersiveChatProxyServer getServer(String serverName);

	public void sendConsoleMessage(String message);

	public boolean isOfflineServerAcknowledgementSet();
}
