package me.slide.immersivechat.controllers.commands;

import com.google.inject.Inject;
import me.slide.immersivechat.initiators.application.ImmersiveChat;
import me.slide.immersivechat.localization.InternalMessage;
import me.slide.immersivechat.model.UniversalCommand;
import org.bukkit.command.CommandSender;

public class Immersivechat extends UniversalCommand {
	@Inject
	private ImmersiveChat plugin;

	@Inject
	public Immersivechat(String name) {
		super(name);
	}

	@Override
	public void executeCommand(CommandSender sender, String command, String[] args) {
		sender.sendMessage(InternalMessage.VENTURECHAT_VERSION.toString().replace("{version}", plugin.getDescription().getVersion()));
		sender.sendMessage(InternalMessage.VENTURECHAT_AUTHOR.toString());
	}
}
