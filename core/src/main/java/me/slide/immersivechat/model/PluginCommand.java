package me.slide.immersivechat.model;

import com.google.inject.Inject;
import me.slide.immersivechat.initiators.application.ImmersiveChat;
import org.bukkit.command.Command;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;

public abstract class PluginCommand extends Command implements PluginIdentifiableCommand {
	@Inject
	protected ImmersiveChat plugin;

	protected PluginCommand(final String name) {
		super(name);
	}

	@Override
	public final Plugin getPlugin() {
		return plugin;
	}
}
