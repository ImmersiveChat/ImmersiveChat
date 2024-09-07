package me.slide.immersivechat.guice;

import com.google.inject.AbstractModule;
import me.slide.immersivechat.initiators.application.ImmersiveChat;

public class ImmersiveChatPluginModule extends AbstractModule {
	private final ImmersiveChat plugin;

	public ImmersiveChatPluginModule(final ImmersiveChat plugin) {
		this.plugin = plugin;
	}

	@Override
	protected void configure() {
		this.bind(ImmersiveChat.class).toInstance(plugin);
	}
}
