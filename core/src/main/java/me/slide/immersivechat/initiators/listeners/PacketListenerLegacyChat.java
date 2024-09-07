package me.slide.immersivechat.initiators.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.slide.immersivechat.initiators.application.ImmersiveChat;
import me.slide.immersivechat.model.ChatMessage;
import me.slide.immersivechat.model.IChatMessage;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import me.slide.immersivechat.service.FormatService;
import me.slide.immersivechat.service.PlayerApiService;
import me.slide.immersivechat.xcut.VersionService;

import java.util.List;

@Singleton
public class PacketListenerLegacyChat extends PacketAdapter {
	@Inject
	private FormatService formatter;
	@Inject
	private PlayerApiService playerApiService;
	@Inject
	private VersionService versionService;

	@Inject
	public PacketListenerLegacyChat(final ImmersiveChat plugin) {
		super(plugin, ListenerPriority.MONITOR, new PacketType[] { PacketType.Play.Server.CHAT });
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		if (event.isCancelled()) {
			return;
		}

		IImmersiveChatPlayer mcp = playerApiService.getOnlineImmersiveChatPlayer(event.getPlayer());
		if (mcp == null) {
			return;
		}

		PacketContainer packet = event.getPacket();
		WrappedChatComponent chat = packet.getChatComponents().read(0);
		if (chat == null) {
			return;
		}

		try {
			if (versionService.is1_7()) {
				packet.getBooleans().getField(0).setAccessible(true);
				if (!((boolean) packet.getBooleans().getField(0).get(packet.getHandle()))) {
					return;
				}
			} else if (versionService.is1_8() || versionService.is1_9() || versionService.is1_10() || versionService.is1_11()) {
				packet.getBytes().getField(0).setAccessible(true);
				if (((Byte) packet.getBytes().getField(0).get(packet.getHandle())).intValue() > 1) {
					return;
				}
			} else {
				packet.getChatTypes().getField(0).setAccessible(true);
				if (packet.getChatTypes().getField(0).get(packet.getHandle()) == packet.getChatTypes().getField(0).getType().getEnumConstants()[2]) {
					return;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		String message = formatter.toPlainText(chat.getHandle(), chat.getHandleType());
		String coloredMessage = formatter.toColoredText(chat.getHandle(), chat.getHandleType());
		if (message == null) {
			return;
		}
		int hash = message.hashCode();
		final List<IChatMessage> messages = mcp.getMessages();
		if (messages.size() >= 100) {
			messages.remove(0);
		}
		messages.add(new ChatMessage(chat, message, coloredMessage, hash));
	}
}
