package me.slide.immersivechat.model;

import lombok.*;
import lombok.Builder.Default;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Wrapper for {@link Player}
 * 
 * @author Aust1n46
 */
@Getter
@Setter
@ToString
@Builder
public class ImmersiveChatPlayer implements IImmersiveChatPlayer {
	@Setter(value = AccessLevel.NONE)
	private UUID uuid;
	private String name;
	private Player player;
	private boolean online;
	private IChatChannel currentChannel;
	private boolean quickChat;
	private IChatChannel quickChannel;
	private UUID conversation;
	private UUID replyPlayer;
	private boolean hasPlayed;
	private boolean modified;
	private boolean rangedSpy;
	private boolean spy;
	private boolean commandSpy;
	private UUID party;
	private boolean host;
	private boolean partyChat;
	private boolean editing;
	private int editHash;
	@Default
	private boolean filterEnabled = true;
	@Default
	private boolean notifications = true;
	@Default
	private boolean messageToggle = true;
	@Default
	private boolean bungeeToggle = true;
	@Default
	private String jsonFormat = "Default";
	@Default
	private Set<String> listening = new HashSet<>();
	@Default
	private Map<String, IMuteContainer> mutes = new HashMap<>();
	@Default
	private Set<UUID> ignores = new HashSet<>();
	@Default
	private Map<IChatChannel, Long> cooldowns = new HashMap<>();
	@Default
	private Map<IChatChannel, List<Long>> spam = new HashMap<>();
	@Default
	private Set<String> blockedCommands = new HashSet<>();
	@Default
	private List<IChatMessage> messages = new ArrayList<>();
}
