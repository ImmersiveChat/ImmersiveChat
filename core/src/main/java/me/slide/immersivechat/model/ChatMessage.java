package me.slide.immersivechat.model;

import com.comphenix.protocol.wrappers.WrappedChatComponent;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage implements IChatMessage {
	private WrappedChatComponent component;
	private String message;
	private String coloredMessage;
	private int hash;
}
