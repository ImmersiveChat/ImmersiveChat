package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import me.slide.immersivechat.localization.InternalMessage;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import me.slide.immersivechat.model.UniversalCommand;
import me.slide.immersivechat.service.PlayerApiService;
import org.bukkit.command.CommandSender;

public class Clearchat extends UniversalCommand {
	@Inject
    private PlayerApiService ventureChatApi;
	
	@Inject
	public Clearchat(String name) {
		super(name);
	}

    @Override
    public void executeCommand(CommandSender sender, String command, String[] args) {
        if (sender.hasPermission("venturechat.clearchat")) {
            for (IImmersiveChatPlayer player : ventureChatApi.getOnlineMineverseChatPlayers()) {
                if (!player.getPlayer().hasPermission("venturechat.clearchat.bypass")) {
                    for (int a = 1; a <= 20; a++)
                        player.getPlayer().sendMessage(InternalMessage.EMPTY_STRING.toString());
                    player.getPlayer().sendMessage(LocalizedMessage.CLEAR_CHAT_SERVER.toString());
                }
            }
            sender.sendMessage(LocalizedMessage.CLEAR_CHAT_SENDER.toString());
            return;
        }
        sender.sendMessage(LocalizedMessage.COMMAND_NO_PERMISSION.toString());
        return;
    }
}
