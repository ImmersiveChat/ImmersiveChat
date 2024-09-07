package me.slide.immersivechat.model;

import me.slide.immersivechat.localization.LocalizedMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class PlayerCommand extends PluginCommand {
	protected PlayerCommand(final String name) {
		super(name);
	}

	@Override
	public final boolean execute(final CommandSender sender, final String commandLabel, final String[] args) {
		if (sender instanceof Player) {
			final Player player = (Player) sender;
			executeCommand(player, commandLabel, args);
		} else {
			plugin.getServer().getConsoleSender().sendMessage(LocalizedMessage.COMMAND_MUST_BE_RUN_BY_PLAYER.toString());
		}
		return true;
	}

	protected abstract void executeCommand(final Player player, final String commandLabel, final String[] args);
}
