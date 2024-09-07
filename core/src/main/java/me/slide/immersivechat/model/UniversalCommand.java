package me.slide.immersivechat.model;

import org.bukkit.command.CommandSender;

public abstract class UniversalCommand extends PluginCommand {
	protected UniversalCommand(final String name) {
		super(name);
	}

	@Override
	public final boolean execute(final CommandSender sender, final String commandLabel, final String[] args) {
		executeCommand(sender, commandLabel, args);
		return true;
	}

	protected abstract void executeCommand(final CommandSender sender, final String commandLabel, final String[] args);
}
