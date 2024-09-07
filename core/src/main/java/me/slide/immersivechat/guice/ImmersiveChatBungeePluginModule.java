package me.slide.immersivechat.guice;

import com.google.inject.AbstractModule;
import me.slide.immersivechat.initiators.application.ImmersiveChatBungee;

public class ImmersiveChatBungeePluginModule extends AbstractModule {
	private final ImmersiveChatBungee plugin;

	public ImmersiveChatBungeePluginModule(final ImmersiveChatBungee plugin) {
		this.plugin = plugin;
	}

	@Override
	protected void configure() {
		this.bind(ImmersiveChatBungee.class).toInstance(plugin);
	}
}
