package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.ChatChannel;
import me.slide.immersivechat.model.UniversalCommand;
import me.slide.immersivechat.service.ConfigService;
import org.bukkit.command.CommandSender;

public class Chlist extends UniversalCommand {
	@Inject
	private ConfigService configService;
	
	@Inject
	public Chlist(String name) {
		super(name);
	}

    @Override
    public void executeCommand(CommandSender sender, String command, String[] args) {
        sender.sendMessage(LocalizedMessage.CHANNEL_LIST_HEADER.toString());
        for (ChatChannel chname : configService.getChatChannels()) {
            if (chname.isPermissionRequired()) {
                if (sender.hasPermission(chname.getPermission())) {
                    sender.sendMessage(LocalizedMessage.CHANNEL_LIST_WITH_PERMISSIONS.toString()
                            .replace("{channel_color}", (chname.getColor()).toString())
                            .replace("{channel_name}", chname.getName())
                            .replace("{channel_alias}", chname.getAlias()));
                }
            } else {
                sender.sendMessage(LocalizedMessage.CHANNEL_LIST.toString()
                        .replace("{channel_color}", chname.getColor().toString())
                        .replace("{channel_name}", chname.getName())
                        .replace("{channel_alias}", chname.getAlias()));
            }
        }
        return;
    }
}
