package me.slide.immersivechat.initiators.schedulers;

import com.google.inject.Inject;
import me.slide.immersivechat.controllers.PluginMessageController;
import me.slide.immersivechat.initiators.application.ImmersiveChat;
import me.slide.immersivechat.localization.LocalizedMessage;
import me.slide.immersivechat.model.ChatChannel;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import me.slide.immersivechat.model.IMuteContainer;
import me.slide.immersivechat.service.ConfigService;
import me.slide.immersivechat.service.PlayerApiService;
import me.slide.immersivechat.utilities.FormatUtils;
import org.bukkit.Server;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Iterator;

public class UnmuteScheduler {
	@Inject
	private ImmersiveChat plugin;
	@Inject
	private PluginMessageController pluginMessageController;
	@Inject
	private PlayerApiService playerApiService;
	@Inject
	private ConfigService configService;

	@Inject
	public void postConstruct() {
		final Server server = plugin.getServer();
		BukkitScheduler scheduler = server.getScheduler();
		scheduler.runTaskTimerAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				for (IImmersiveChatPlayer p : playerApiService.getOnlineMineverseChatPlayers()) {
					long currentTimeMillis = System.currentTimeMillis();
					Iterator<IMuteContainer> iterator = p.getMutes().values().iterator();
					while (iterator.hasNext()) {
						IMuteContainer mute = iterator.next();
						if (configService.isChannel(mute.getChannel())) {
							ChatChannel channel = configService.getChannel(mute.getChannel());
							long timemark = mute.getDuration();
							if (timemark == 0) {
								continue;
							}
							if (plugin.getConfig().getString("loglevel", "info").equals("trace")) {
								System.out.println(currentTimeMillis + " " + timemark);
							}
							if (currentTimeMillis >= timemark) {
								iterator.remove();
								p.getPlayer().sendMessage(LocalizedMessage.UNMUTE_PLAYER_PLAYER.toString().replace("{player}", p.getName())
										.replace("{channel_color}", channel.getColor()).replace("{channel_name}", mute.getChannel()));
								if (channel.isBungeeEnabled()) {
									pluginMessageController.synchronize(p, true);
								}
							}
						}
					}
				}
				if (plugin.getConfig().getString("loglevel", "info").equals("trace")) {
					server.getConsoleSender().sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - Updating Player Mutes"));
				}
			}
		}, 0L, 60L); // three second interval
	}
}
