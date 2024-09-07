package me.slide.immersivechat.initiators.listeners;

import me.slide.immersivechat.controllers.PluginMessageController;
import me.slide.immersivechat.controllers.SpigotFlatFileController;
import me.slide.immersivechat.initiators.application.ImmersiveChat;
import me.slide.immersivechat.model.ChatChannel;
import me.slide.immersivechat.model.ImmersiveChatPlayer;
import me.slide.immersivechat.service.ConfigService;
import me.slide.immersivechat.service.PlayerApiService;
import me.slide.immersivechat.service.UuidService;
import me.slide.immersivechat.xcut.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoginListenerTest {
	@Mock
	private ImmersiveChat plugin;
	@Mock
	private UuidService uuidService;
	@Mock
	private SpigotFlatFileController spigotFlatFileController;
	@Mock
	private PluginMessageController pluginMessageController;
	@Mock
	private PlayerApiService playerApiService;
	@Mock
	private ConfigService configService;
	@Mock
	private Logger log;
	@InjectMocks
	private LoginListener loginListener;

	@Mock
	private PlayerQuitEvent mockPlayerQuitEvent;
	@Mock
	private PlayerJoinEvent mockPlayerJoinEvent;
	@Mock
	private Player mockPlayer;
	@Mock
	private ImmersiveChatPlayer mockImmersiveChatPlayer;
	@Mock
	private ChatChannel mockDefaultChannel;

	@Test
	public void testPlayerQuit() {
		when(mockPlayerQuitEvent.getPlayer()).thenReturn(mockPlayer);
		when(playerApiService.getOnlineImmersiveChatPlayer(mockPlayer)).thenReturn(mockImmersiveChatPlayer);
		loginListener.onPlayerQuit(mockPlayerQuitEvent);
	}

	@Test
	public void testPlayerQuit_playerNull() {
		when(mockPlayerQuitEvent.getPlayer()).thenReturn(mockPlayer);
		when(playerApiService.getOnlineImmersiveChatPlayer(mockPlayer)).thenReturn(null);
		loginListener.onPlayerQuit(mockPlayerQuitEvent);
		assertDoesNotThrow(() -> loginListener.onPlayerQuit(mockPlayerQuitEvent));
	}

	@Test
	public void testPlayerJoin_existingPlayer_NoProxy() {
		when(mockPlayerJoinEvent.getPlayer()).thenReturn(mockPlayer);
		when(configService.getDefaultChannel()).thenReturn(mockDefaultChannel);
		when(mockPlayer.getName()).thenReturn("Aust1n46");
		when(configService.isProxyEnabled()).thenReturn(false);
		loginListener.onPlayerJoin(mockPlayerJoinEvent);
	}

	@Test
	public void testPlayerJoin_existingPlayer_Proxy() {
		when(mockPlayerJoinEvent.getPlayer()).thenReturn(mockPlayer);
		when(configService.getDefaultChannel()).thenReturn(mockDefaultChannel);
		when(mockPlayer.getName()).thenReturn("Aust1n46");
		when(configService.isProxyEnabled()).thenReturn(true);
		loginListener.onPlayerJoin(mockPlayerJoinEvent);
	}
}
