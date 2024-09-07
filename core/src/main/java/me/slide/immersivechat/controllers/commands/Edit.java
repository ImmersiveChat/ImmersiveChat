package me.slide.immersivechat.controllers.commands;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.google.inject.Inject;
import me.slide.immersivechat.initiators.application.ImmersiveChat;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.IChatMessage;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import me.slide.immersivechat.model.UniversalCommand;
import me.slide.immersivechat.service.FormatService;
import me.slide.immersivechat.service.PlayerApiService;
import me.slide.immersivechat.utilities.FormatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Edit extends UniversalCommand {
    @Inject
    private ImmersiveChat plugin;
    @Inject
	private FormatService formatService;
    @Inject
	private PlayerApiService playerApiService;
    
    private PacketContainer emptyLinePacketContainer;
    private WrappedChatComponent messageDeletedComponentPlayer;
    
    @Inject
	public Edit(String name) {
		super(name);
	}

    @Inject
    public void postConstruct() {
    	emptyLinePacketContainer = formatService.createPacketPlayOutChat("{\"extra\":[\" \"],\"text\":\"\"}");
    }
    
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void executeCommand(CommandSender sender, String command, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(LocalizedMessage.COMMAND_INVALID_ARGUMENTS.toString()
                    .replace("{command}", "/edit")
                    .replace("{args}", "[hashcode]"));
            return;
        }
        final int hash;
        try {
            hash = Integer.parseInt(args[0]);
        } catch (Exception e) {
            sender.sendMessage(LocalizedMessage.INVALID_HASH.toString());
            return;
        }
        new BukkitRunnable() {
            public void run() {
                final Map<Player, List<PacketContainer>> packets = new HashMap();
                for (IImmersiveChatPlayer p : playerApiService.getOnlineMineverseChatPlayers()) {
                    List<IChatMessage> messages = p.getMessages();
                    List<PacketContainer> playerPackets = new ArrayList();
                    boolean resend = false;
                    for (int fill = 0; fill < 100 - messages.size(); fill++) {
                        playerPackets.add(Edit.this.emptyLinePacketContainer);
                    }
                    for (IChatMessage message : messages) {
                        if (message.getHash() == hash) {
                            WrappedChatComponent removedComponent = p.getPlayer().hasPermission("venturechat.message.bypass") ? Edit.this.getMessageDeletedChatComponentAdmin(message) : Edit.this.getMessageDeletedChatComponentPlayer();
                            message.setComponent(removedComponent);
                            message.setHash(-1);
                            playerPackets.add(formatService.createPacketPlayOutChat(removedComponent));
                            resend = true;
                            continue;
                        }
                        if (message.getMessage().contains(FormatUtils.FormatStringAll(plugin.getConfig().getString("messageremovericon")))) {
                            String submessage = message.getMessage().substring(0, message.getMessage().length() - plugin.getConfig().getString("messageremovericon").length() - 1).replaceAll("(�([a-z0-9]))", "");
                            if (submessage.hashCode() == hash) {
                                WrappedChatComponent removedComponent = p.getPlayer().hasPermission("venturechat.message.bypass") ? Edit.this.getMessageDeletedChatComponentAdmin(message) : Edit.this.getMessageDeletedChatComponentPlayer();
                                message.setComponent(removedComponent);
                                message.setHash(-1);
                                playerPackets.add(formatService.createPacketPlayOutChat(removedComponent));
                                resend = true;
                                continue;
                            }
                        }
                        playerPackets.add(formatService.createPacketPlayOutChat(message.getComponent()));
                    }
                    if (resend) {
                        packets.put(p.getPlayer(), playerPackets);
                    }
                }
                new BukkitRunnable() {
                    public void run() {
                        for (Player p : packets.keySet()) {
                            List<PacketContainer> pPackets = packets.get(p);
                            for (PacketContainer c : pPackets) {
                                formatService.sendPacketPlayOutChat(p, c);
                            }
                        }
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    public WrappedChatComponent getMessageDeletedChatComponentPlayer() {
    	if (messageDeletedComponentPlayer == null) { // avoid errors on startup from this non functional command on different game versions
    		messageDeletedComponentPlayer = WrappedChatComponent.fromJson("{\"text\":\"\",\"extra\":[{\"text\":\"<message removed>\",\"color\":\"red\",\"italic\":\"true\",\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"\",\"extra\":[{\"text\":\"" + FormatUtils.FormatStringAll(plugin.getConfig().getString("messageremoverpermissions")) + "\"}]}}}]}");
    	}
        return this.messageDeletedComponentPlayer;
    }

    public WrappedChatComponent getMessageDeletedChatComponentAdmin(IChatMessage message) {
        String oMessage = message.getComponent().getJson().substring(1, message.getComponent().getJson().length() - 11);
        if (message.getMessage().contains(FormatUtils.FormatStringAll(plugin.getConfig().getString("messageremovericon")))) {
            oMessage = oMessage.substring(0, oMessage.length() - plugin.getConfig().getString("messageremovericon").length() - 4) + "\"}]";
        }
        return WrappedChatComponent.fromJson(FormatUtils.FormatStringAll("{\"text\":\"\",\"extra\":[{\"text\":\"&c&o<message removed>\",\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"&7Message: \"," + oMessage + "}}}]}"));
    }
}
